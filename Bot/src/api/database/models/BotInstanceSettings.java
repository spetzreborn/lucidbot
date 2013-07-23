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
import api.database.BotInstanceSettingsChannel;
import lombok.*;

import javax.persistence.*;
import java.util.*;

import static api.tools.text.StringUtil.lowerCase;
import static com.google.common.base.Preconditions.checkNotNull;

@Entity
@Table(name = "bot_instance_settings")
@NoArgsConstructor
@EqualsAndHashCode(of = "nick")
@Getter
@Setter
public final class BotInstanceSettings implements Comparable<BotInstanceSettings>, HasNumericId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    /**
     * The bot's nickname
     */
    @Column(name = "nick", nullable = false, length = 50, unique = true)
    private String nick;

    /**
     * The password for the nickname
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /**
     * The channels the bot is supposed to be in
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pk.settings", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<BotInstanceSettingsChannel> channels = new HashSet<>();

    public BotInstanceSettings(final String nick, final String password, final Collection<Channel> channels) {
        this.nick = checkNotNull(nick);
        this.password = checkNotNull(password);
        for (Channel channel : channels) {
            this.channels.add(new BotInstanceSettingsChannel(this, channel));
        }
    }

    /**
     * @param inLowerCase whether to lower case all the names
     * @return a Collection of the names of this instance's channels.
     */
    public Collection<String> getChannelNames(final boolean inLowerCase) {
        Collection<String> chans = new HashSet<>(getChannels().size());
        for (Channel channel : getChannels()) {
            chans.add(inLowerCase ? lowerCase(channel.getName()) : channel.getName());
        }
        return chans;
    }

    public List<Channel> getChannels() {
        List<Channel> out = new ArrayList<>(channels.size());
        for (BotInstanceSettingsChannel join : channels) {
            out.add(join.getChannel());
        }
        return out;
    }

    public void setChannels(final Collection<Channel> channels) {
        this.channels.clear();
        for (Channel channel : channels) {
            this.channels.add(new BotInstanceSettingsChannel(this, channel));
        }
    }

    @Override
    public int compareTo(final BotInstanceSettings o) {
        return getNick().compareTo(o.getNick());
    }
}
