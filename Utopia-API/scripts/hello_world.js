importPackage(Packages.api.commands);

function handles() {
    return CommandBuilder.forCommand("js").build();
}

function handleCommand(context, params, filters, delayedEventPoster) {
    var name = "world";
    if (params.containsKey("name")) name = params.getParameter("name");
    return CommandResponse.resultResponse("helloWorld", "Hello " + name + " from javascript");
}

function getParsers() {
    var parsers = java.lang.reflect.Array.newInstance(CommandParser, 2);
    parsers[0] = CommandParser(ParamParsingSpecification("name", "[^ ]+"));
    parsers[1] = CommandParser.getEmptyParser();
    return parsers;
}