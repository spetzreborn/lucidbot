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

package spi.commands;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import spi.filters.Filter;

import java.util.Collection;

/**
 * A class that handles a Command
 */
public interface CommandHandler {
    /**
     * Handles a command and returns one of the following:
     * * If the command was handled correctly
     * ** A CommandResponse with useful data
     * ** An empty CommandResponse, if no useful data exists but the command was handled successfully
     * * If there were errors while handling the command, such as no user found or something
     * ** A CommandResponse with an error message set
     * * If something blew up
     * ** Throws an exception wrapped in CommandHandlingException
     *
     * @param context            the context of the command call
     * @param params             the parameters parsed from the original message
     * @param filters            the filters found in the original message
     * @param delayedEventPoster an event poster that may be used to enqueue events to be sent after the transaction is over
     * @return see explanation above
     * @throws CommandHandlingException wraps any exceptions that occurred
     */
    CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                  final DelayedEventPoster delayedEventPoster) throws CommandHandlingException;
}
