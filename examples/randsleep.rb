require 'jo'

include Jo

# print out 1-10 in ten joroutines with 0-250ms delay
def f(n)
  10.times do |i|
    puts "#{n}:#{i}"
    sleep rand(250)/1000.0
  end
end

10.times do |n|
  jo {f(n)}
end
