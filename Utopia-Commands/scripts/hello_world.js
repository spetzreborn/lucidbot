importPackage(Packages.api.commands);

function handles() {
    return CommandFactory.newSimpleCommand("js");
}

function handleCommand(context, params, filters, delayedEventPoster) {
    return CommandResponse.resultResponse("helloWorld", "Hello world from javascript");
}