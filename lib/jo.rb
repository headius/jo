require "jo/version"
require "jo-#{Jo::VERSION}.jar"

com.headius.jo.JoLibrary.new.load(JRuby.runtime, false)
