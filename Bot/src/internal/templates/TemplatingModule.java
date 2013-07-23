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

package internal.templates;

import api.templates.TemplateManager;
import api.tools.time.DateFactory;
import com.google.inject.AbstractModule;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import lombok.extern.log4j.Log4j;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

@Log4j
public class TemplatingModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Configuration.class).toInstance(configureTemplateConfiguration());
        bind(TemplateManager.class).to(TemplateManagerImpl.class).in(Singleton.class);
    }

    private Configuration configureTemplateConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setDateFormat(DateFactory.getISODateFormatAsString());
        configuration.setDateTimeFormat(DateFactory.getISOWithoutSecondsDateTimeFormatAsString());
        configuration.setLocale(Locale.US);
        try {
            configuration.setDirectoryForTemplateLoading(new File("templates"));
        } catch (IOException e) {
            throw new RuntimeException("The templates folder is missing", e);
        }

        CompactListDirective compactListDirective = new CompactListDirective();
        requestInjection(compactListDirective);
        configuration.setSharedVariable("compact", compactListDirective);

        ColumnsDirective columnsDirective = new ColumnsDirective();
        configuration.setSharedVariable("columns", columnsDirective);

        IRCMessageDirective ircMessageDirective = new IRCMessageDirective();
        requestInjection(ircMessageDirective);
        configuration.setSharedVariable("ircmessage", ircMessageDirective);

        try {
            BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
            TemplateHashModel staticModels = wrapper.getStaticModels();
            TemplateHashModel stringUtil = (TemplateHashModel) staticModels.get("api.tools.text.StringUtil");
            TemplateHashModel timeUtil = (TemplateHashModel) staticModels.get("internal.templates.TemplateTimeUtil");
            configuration.setSharedVariable("stringUtil", stringUtil);
            configuration.setSharedVariable("timeUtil", timeUtil);
        } catch (TemplateModelException e) {
            log.error("Could not load utils into the freemarker template config");
        }

        return configuration;
    }
}
