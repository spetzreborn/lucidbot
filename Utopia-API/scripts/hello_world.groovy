import api.commands.CommandResponse

def handles() {
    return CommandBuilder.forCommand("groovy").build();
}

def handleCommand(context, params, filters, delayedEventPoster) {
    return CommandResponse.resultResponse("helloWorld", "Hello world from Groovy");
}