require 'jo'

include Jo

# pinger ponger printer
def pinger(c)
  20.times do
    c << 'ping'
  end
end

def ponger(c)
  20.times do
    c << 'pong'
  end
end

def printer(c)
  40.times do
    puts c.take
    sleep 1
  end
end

c = chan
jo {pinger(c)}
jo {ponger(c)}
jo {printer(c)}

gets
