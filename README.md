# Jo

A library to provide "goroutines", "channels", and "select" as in Go.

## Installation

Add this line to your application's Gemfile:

    gem 'jo'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install jo

## Usage

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

gets # prevent exit

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
