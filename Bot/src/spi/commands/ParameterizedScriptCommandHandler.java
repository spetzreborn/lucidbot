package spi.commands;

import api.commands.CommandParser;

public interface ParameterizedScriptCommandHandler extends ScriptCommandHandler {

    /**
     * @return the parsers supported by the CommandHandler. One of these must match the command!
     */
    CommandParser[] getParsers();

}
