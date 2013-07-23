#Works with Jython2.7a2
from api.commands import CommandFactory, CommandResponse


def handles():
    return CommandFactory.newSimpleCommand("jython")


def handleCommand(context, params, filters, delayedEventPoster):
    return CommandResponse.resultResponse("helloWorld", "Hello World from Jython")