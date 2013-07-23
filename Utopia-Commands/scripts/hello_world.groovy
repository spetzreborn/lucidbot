import api.commands.CommandFactory
import api.commands.CommandResponse

def handles() {
    return CommandFactory.newSimpleCommand("groovy");
}

def handleCommand(context, params, filters, delayedEventPoster) {
    return CommandResponse.resultResponse("helloWorld", "Hello world from Groovy");
}