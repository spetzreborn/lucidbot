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

package internal.main;

import api.settings.BasicSetup;
import api.settings.PluginServiceLoader;
import com.google.inject.AbstractModule;
import internal.database.DatabaseModule;
import lombok.extern.log4j.Log4j;

import java.util.ArrayList;
import java.util.List;

@Log4j
public class BasicsModule extends AbstractModule {
    @Override
    protected void configure() {
        //Install BasicSetup modules (i.e. those without external dependencies that others are dependant on)
        install(new DatabaseModule());
        PluginServiceLoader<AbstractModule> pluginModulesLoader = PluginServiceLoader.newLoaderFor(AbstractModule.class, "plugins");

        List<AbstractModule> modules = new ArrayList<>();
        for (AbstractModule module : pluginModulesLoader.getInstances()) {
            if (module.getClass().isAnnotationPresent(BasicSetup.class)) {
                modules.add(module);
            }
        }

        for (AbstractModule module : modules) {
            try {
                install(module);
            } catch (Exception e) {
                log.error("Failed to load plugin module", e);
            }
        }
    }
}
