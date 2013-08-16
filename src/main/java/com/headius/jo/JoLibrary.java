package com.headius.jo;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import org.jruby.Finalizable;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

public class JoLibrary implements Library{
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyModule jo = runtime.defineModule("Jo");
        final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
        jo.setInternalVariable("executor", executor);
        RubyClass joFuture = runtime.defineClass("Future", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        RubyClass joChannel = runtime.defineClass("Channel", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        
        jo.defineAnnotatedMethods(JoMethods.class);
        joFuture.defineAnnotatedMethods(JoFuture.class);
        joChannel.defineAnnotatedMethods(JoChannel.class);
        
        runtime.addFinalizer(new Finalizable() {
            public void finalize() {
                executor.shutdown();
            }
        });
    }
    
    public static class JoRoutine implements Callable<IRubyObject> {
        public JoRoutine(Ruby runtime, Block body) {
            this.runtime = runtime;
            this.body = body;
        }

        public IRubyObject call() throws Exception {
            ThreadContext context = runtime.getCurrentContext();
            try {
                return body.call(context);
            } catch (RaiseException re) {
                RubyKernel.puts(context, body.getBinding().getSelf(), new IRubyObject[] {
                        RubyString.newString(runtime, "joroutine terminated with error: " + re.getMessage())});
                RubyKernel.puts(context, body.getBinding().getSelf(), new IRubyObject[] {re.getException().backtrace()});
                throw re;
            }
        }
        
        private final Ruby runtime;
        private final Block body;
    }
    
    public static class JoChannel extends RubyObject {
        public JoChannel(Ruby runtime, RubyClass klass, BlockingQueue<IRubyObject> queue) {
            super(runtime, klass);
            this.queue = queue;
        }
        
        @JRubyMethod(name = "<<")
        public IRubyObject append(ThreadContext context, IRubyObject value) throws InterruptedException {
            queue.put(value);
            return this;
        }
        
        @JRubyMethod
        public IRubyObject take(ThreadContext context) throws InterruptedException {
            return queue.take();
        }
        
        private final BlockingQueue<IRubyObject> queue;
    }
    
    public static class JoFuture extends RubyObject {
        public JoFuture(Ruby runtime, RubyClass klass, Future<IRubyObject> future) {
            super(runtime, klass);
            
            this.future = future;
        }
        
        @JRubyMethod
        public IRubyObject get(ThreadContext context) throws ExecutionException, InterruptedException {
            return future.get();
        }
        
        @JRubyMethod
        public IRubyObject cancel(ThreadContext context) {
            return context.runtime.newBoolean(future.cancel(true));
        }
        
        private final Future<IRubyObject> future;
    }
    
    public static class JoMethods {
        @JRubyMethod(module = true)
        public static IRubyObject jo(ThreadContext context, IRubyObject self, Block block) {
            Ruby runtime = context.runtime;
            RubyModule jo = runtime.getModule("Jo");
            ExecutorService executor = (ExecutorService)jo.getInternalVariable("executor");
            Future<IRubyObject> future = executor.submit(new JoRoutine(runtime, block));
            return new JoFuture(runtime, jo.getClass("Future"), future);
        }
        
        @JRubyMethod(module = true)
        public static IRubyObject chan(ThreadContext context, IRubyObject self, Block block) {
            Ruby runtime = context.runtime;
            return new JoChannel(runtime, (RubyClass)runtime.getClassFromPath("Jo::Channel"), new SynchronousQueue<IRubyObject>(true));
        }
        
        @JRubyMethod(module = true)
        public static IRubyObject select(ThreadContext context, IRubyObject self, IRubyObject hash) {
            Ruby runtime = context.runtime;
            RubyHash cases = hash.convertToHash();
            
            Set<RubyHash.RubyHashEntry> entries = (Set<RubyHash.RubyHashEntry>)cases.directEntrySet();
            Object[] entryArray = entries.toArray();
            OUTER: while (true) {
                for (Object _entry : entryArray) {
                    RubyHash.RubyHashEntry entry = (RubyHash.RubyHashEntry)_entry;
                    IRubyObject _channel = (IRubyObject)entry.getKey();
                    if (!(_channel instanceof JoChannel)) throw runtime.newTypeError(_channel, runtime.getClassFromPath("Jo::Channel"));
                    IRubyObject _proc = (IRubyObject)entry.getValue();
                    IRubyObject value;
                    if ((value = ((JoChannel)_channel).queue.poll()) != null) {
                        _proc.callMethod(context, "call", value);
                        break OUTER;
                    }
                    Thread.yield();
                }
            }
            return context.nil;
        }
    }
}
