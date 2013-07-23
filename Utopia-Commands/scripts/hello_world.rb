require 'java'

def handles()
  Java::api.commands.CommandFactory.newSimpleCommand "jruby"
end

def handleCommand(context, params, filters, delayedEventPoster)
  Java::api.commands.CommandResponse.resultResponse "helloWorld", "Hello World from JRuby"
end