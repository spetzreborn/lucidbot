/*
 * Copyright (c) 2012, Fredrik Yttergren
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name LucidBot nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Fredrik Yttergren BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package api.database.daos;

import api.commands.Command;
import api.database.AbstractDAO;
import api.database.models.CommandDefinition;
import api.database.transactions.Transactional;
import com.google.inject.Provider;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@ParametersAreNonnullByDefault
public class CommandDefinitionDAO extends AbstractDAO<CommandDefinition> {
    @Inject
    public CommandDefinitionDAO(final Provider<Session> sessionProvider) {
        super(CommandDefinition.class, sessionProvider);
    }

    /**
     * Fills in (and overrides) command settings for the specified command
     *
     * @param command the command
     */
    @Transactional
    public void loadDefinitionFromDB(final Command command) {
        CommandDefinition def = get(Restrictions.eq("name", command.getName()));
        fillInDefinition(command, def);
    }

    /**
     * Fills in (and overrides) command settings for the all the specified commands
     *
     * @param commands the command
     */
    @Transactional
    public void loadDefinitionsFromDB(final Collection<Command> commands) {
        Map<String, CommandDefinition> map = new HashMap<>();
        for (final CommandDefinition definition : find()) {
            map.put(definition.getName(), definition);
        }
        for (final Command command : commands) {
            if (map.containsKey(command.getName())) {
                fillInDefinition(command, map.get(command.getName()));
            }
        }
    }

    private static void fillInDefinition(final Command command, final CommandDefinition def) {
        if (def != null) {
            command.setCommandType(def.getCommandType());
            if (def.getHelpText() != null) command.setHelpText(def.getHelpText());
            if (def.getSyntax() != null) command.setSyntax(def.getSyntax());
            if (def.getAccessLevel() != null && (command.isDowngradableAccessLevel() || def.getAccessLevel().compareTo(command.getOriginalRequiredAccessLevel()) >= 0))
                command.setRequiredAccessLevel(def.getAccessLevel());
            if (def.getTemplateFile() != null) command.setTemplateFile(def.getTemplateFile());
        }
    }

    /**
     * Saves the command settings as CommandDefinition
     *
     * @param command the command
     * @return the new CommandDefinition based on the specified command
     */
    @Transactional
    public CommandDefinition createFromCommand(final Command command) {
        CommandDefinition def = new CommandDefinition(command.getName(), command.getSyntax(), command.getHelpText(),
                command.getCommandType(), command.getTemplateFile(), command.getRequiredAccessLevel());
        def = save(def);
        return def;
    }

    /**
     * @param name the name of the command (NOT case sensitive)
     * @return the CommandDefinition for the specified command, or null if none can be found
     */
    @Transactional
    public CommandDefinition getCommandDefinition(final String name) {
        return get(Restrictions.ilike("name", checkNotNull(name)));
    }

    @Transactional
    public List<CommandDefinition> getAllCommandDefinitions() {
        return find();
    }
}
