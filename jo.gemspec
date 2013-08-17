# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)
require 'jo/version'

Gem::Specification.new do |spec|
  spec.name          = "jo"
  spec.version       = Jo::VERSION
  spec.authors       = ["Charles Oliver Nutter"]
  spec.email         = ["headius@headius.com"]
  spec.description   = "An implementation of goroutines and channels for Ruby"
  spec.summary       = "An implementation of goroutines and channels (from the Go language) for Ruby"
  spec.homepage      = "https://github.com/headius/jo"
  spec.license       = "MIT"

  spec.files         = `git ls-files`.split($/) + ["target/jo-#{Jo::VERSION}.jar"]
  spec.executables   = spec.files.grep(%r{^bin/}) { |f| File.basename(f) }
  spec.test_files    = spec.files.grep(%r{^(test|spec|features)/})
  spec.require_paths = ["lib", "target"]

  spec.add_development_dependency "bundler", "~> 1.3"
  spec.add_development_dependency "rake"
end
