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

package api.templates;

import api.irc.communication.IRCOutput;
import api.runtime.IRCContext;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * A manager capable of applying templates
 */
public interface TemplateManager {
    static String TEMPLATE_FILE_EXTENSION = ".ftl";

    /**
     * Applies the specified template and produces output to send to IRC
     *
     * @param data         the data for the template to use
     * @param templateName the name of the template
     * @param context      the irc context
     * @return a Collection of IRCOutputs to send to the server
     */
    Collection<IRCOutput> createOutputFromTemplate(Map<String, Object> data, String templateName, IRCContext context);

    /**
     * Processes the specified template, using the supplied data, and returns the compiled text
     *
     * @param data         the data to use
     * @param templateName the name of the template file
     * @return a String with the text from the compiled template
     * @throws IOException                 .
     * @throws TemplateProcessingException .
     */
    String processTemplate(Map<String, Object> data, String templateName) throws IOException, TemplateProcessingException;

    public static class TemplateProcessingException extends Exception {
        public TemplateProcessingException(final String message) {
            super(message);
        }

        public TemplateProcessingException(final String message, final Throwable cause) {
            super(message, cause);
        }

        public TemplateProcessingException(final Throwable cause) {
            super(cause);
        }
    }
}
