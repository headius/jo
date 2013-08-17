require 'jo'

include Jo

c1 = chan
c2 = chan

jo do
  loop do
    c1 << "from 1"
    sleep 2
  end
end

jo do
  loop do
    c2 << "from 2"
    sleep 3
  end
end

loop do
  select(
    c1 => proc{|n| puts "even: #{n}"},
    c2 => proc{|n| puts "odd: #{n}"}
  )
end
