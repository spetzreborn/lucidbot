package api.commands;

import api.database.models.AccessLevel;
import api.templates.TemplateManager;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder class for {@link Command} objects
 */
@ParametersAreNonnullByDefault
public class CommandBuilder {

    /**
     * Creates a builder for a command with the specified name
     *
     * @param name the name of the command, not null
     * @return a new CommandBuilder
     */
    public static CommandBuilder forCommand(final String name) {
        return new CommandBuilder(checkNotNull(name));
    }

    private final String name;

    private String syntax;
    private String helpText;
    private String commandType = "unspecified";
    private AccessLevel requiredAccessLevel = AccessLevel.USER;
    private boolean accessLevelDowngradable = true;
    private String templateFile;

    private CommandBuilder(final String name) {
        this.name = name;
        this.templateFile = name + TemplateManager.TEMPLATE_FILE_EXTENSION;
    }

    /**
     * Specifies the default syntax description for the command
     *
     * @param syntax the syntax, nullable if you prefer the syntax to be generated automatically
     * @return this CommandBuilder, to allow chained method calls
     */
    public CommandBuilder withSyntax(@Nullable final String syntax) {
        this.syntax = syntax;
        return this;
    }

    /**
     * Specifies the default help text for the command
     *
     * @param helpText the help text, nullable
     * @return this CommandBuilder, to allow chained method calls
     */
    public CommandBuilder withHelpText(@Nullable final String helpText) {
        this.helpText = helpText;
        return this;
    }

    /**
     * The type of the command. Can be anything at all, but should preferably reuse some command type that already exists so that commands can be grouped
     * around what type of functionality they provide. If the command type isn't explicitly defined by using this method, it will default to 'unspecified'.
     *
     * @param commandType the command type, not null
     * @return this CommandBuilder, to allow chained method calls
     * @throws NullPointerException if commandType is null
     */
    public CommandBuilder ofType(final String commandType) {
        this.commandType = checkNotNull(commandType);
        return this;
    }

    /**
     * Sets the required access level for the command. The access level determines who is allowed to use this command.
     *
     * @param requiredAccessLevel the minimum access level to use the command, not null
     * @return this CommandBuilder, to allow chained method calls
     * @throws NullPointerException if requiredAccessLevel is null
     */
    public CommandBuilder requiringAccessLevel(final AccessLevel requiredAccessLevel) {
        this.requiredAccessLevel = checkNotNull(requiredAccessLevel);
        return this;
    }

    /**
     * Allows the access level to be relaxed/downgraded by command definitions.
     *
     * @return this CommandBuilder, to allow chained method calls
     */
    public CommandBuilder withDowngradableAccessLevel() {
        this.accessLevelDowngradable = true;
        return this;
    }

    /**
     * Prevents the access level from being relaxed/downgraded by command definitions.
     *
     * @return this CommandBuilder, to allow chained method calls
     */
    public CommandBuilder withNonDowngradableAccessLevel() {
        this.accessLevelDowngradable = false;
        return this;
    }

    /**
     * Specifies which template file will be used to create the irc output for the command.
     *
     * @param templateFile the name of the template file. Is not allowed to be null, and should end with .ftl
     * @return this CommandBuilder, to allow chained method calls
     * @throws NullPointerException     if templateFile is null
     * @throws IllegalArgumentException if templateFile doesn't end with '.ftl'
     */
    public CommandBuilder usingTemplateFile(final String templateFile) {
        checkNotNull(templateFile);
        checkArgument(templateFile.endsWith(TemplateManager.TEMPLATE_FILE_EXTENSION));
        this.templateFile = templateFile;
        return this;
    }

    /**
     * Uses the default template file for this command, meaning the name of the command with the template file ending.
     *
     * @return this CommandBuilder, to allow chained method calls
     */
    public CommandBuilder usingDefaultTemplateFile() {
        this.templateFile = name + TemplateManager.TEMPLATE_FILE_EXTENSION;
        return this;
    }

    /**
     * Builds the command
     *
     * @return a new Command configured by the parameters of this build
     */
    public Command build() {
        return new Command(name, syntax, helpText, commandType, requiredAccessLevel, requiredAccessLevel, accessLevelDowngradable, templateFile);
    }
}
