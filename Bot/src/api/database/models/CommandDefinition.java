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

package api.database.models;

import api.common.HasNumericId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "command_definition")
@NoArgsConstructor
@EqualsAndHashCode(of = "name")
@Getter
@Setter
public final class CommandDefinition implements Comparable<CommandDefinition>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(lombok.AccessLevel.NONE)
    private Long id;

    /**
     * The name of the command
     */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    /**
     * The syntax description for the command
     */
    @Lob
    @Column(name = "syntax", length = 5000)
    private String syntax;

    /**
     * The help text for the command
     */
    @Lob
    @Column(name = "help_text", length = 5000)
    private String helpText;

    /**
     * The type of command
     */
    @Column(name = "command_type", nullable = false, length = 100)
    private String commandType;

    /**
     * The filename of the template file
     */
    @Column(name = "template_file", length = 100)
    private String templateFile;

    /**
     * The required access level to use the command
     */
    @Column(name = "access_level", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private AccessLevel accessLevel;

    public CommandDefinition(final String name, final String syntax, final String helpText, final String commandType,
                             final String templateFile, final AccessLevel accessLevel) {
        this.name = checkNotNull(name);
        this.syntax = syntax;
        this.helpText = helpText;
        this.commandType = checkNotNull(commandType);
        this.templateFile = checkNotNull(templateFile);
        this.accessLevel = checkNotNull(accessLevel);
    }

    @Override
    public int compareTo(final CommandDefinition o) {
        return getName().compareTo(o.getName());
    }
}
