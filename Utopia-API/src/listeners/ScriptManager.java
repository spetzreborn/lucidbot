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

package listeners;

import api.commands.Command;
import api.commands.CommandParser;
import api.commands.ParamParsingSpecification;
import api.events.DirectoryChangeEventObserver;
import api.events.bot.CommandRemovedEvent;
import api.runtime.ThreadingManager;
import api.tools.files.FileUtil;
import api.tools.files.JavaFileFilter;
import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.extern.log4j.Log4j;
import spi.commands.CommandHandler;
import spi.commands.ParameterizedScriptCommandHandler;
import spi.commands.ScriptCommandHandler;
import spi.events.EventListener;
import spi.events.ScriptEventListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static api.tools.text.StringUtil.merge;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@Singleton
@Log4j
public class ScriptManager implements DirectoryChangeEventObserver, EventListener {
    private static final Path SCRIPTS_DIR = Paths.get("scripts");
    private static final Path SCRIPTENGINES_DIR = Paths.get("scriptengines");

    private final EventBus eventBus;
    private final ThreadingManager threadingManager;

    private final ConcurrentMap<Command, Path> commandHandledToFileMapping = new ConcurrentHashMap<>();
    private final ConcurrentMap<Command, List<CommandParser>> commandParserMapping = new ConcurrentHashMap<>();
    private final Cache<Command, ScriptCommandHandler> cache = CacheBuilder.newBuilder().expireAfterAccess(6, TimeUnit.HOURS).build();

    private final ConcurrentMap<ScriptEventListener, Class<?>> eventListenerToEventTypeMapping = new ConcurrentHashMap<>();
    private final ConcurrentMap<Path, ScriptEventListener> fileNameToEventListenerMapping = new ConcurrentHashMap<>();

    private ScriptEngineManager manager;

    @Inject
    public ScriptManager(final EventBus eventBus, final ThreadingManager threadingManager) {
        this.eventBus = eventBus;
        this.threadingManager = threadingManager;
    }

    @Inject
    public void init() {
        initScriptEngineManager();
        findScripts();
    }

    private void initScriptEngineManager() {
        File engineDir = SCRIPTENGINES_DIR.toFile();
        List<URL> urls = new ArrayList<>();
        File[] files = engineDir.listFiles(new JavaFileFilter());
        if (files != null) {
            for (File file : files) {
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException ignore) {
                }
            }
        }
        ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), ClassLoader.getSystemClassLoader());
        manager = new ScriptEngineManager(classLoader);
    }

    private void findScripts() {
        File scriptsDir = SCRIPTS_DIR.toFile();
        File[] scripts = scriptsDir.listFiles();
        if (scripts != null) {
            for (File file : scripts) {
                identifyScript(file.toPath());
            }
        }
    }

    @Override
    public void handleUpdatedDirectory(final Map<Path, WatchEvent.Kind<?>> files) {
        for (Map.Entry<Path, WatchEvent.Kind<?>> entry : files.entrySet()) {
            Path file = entry.getKey();
            if (entry.getValue().equals(ENTRY_MODIFY)) {
                Command handledCommand = getCommandForFile(file);
                if (handledCommand != null) cache.invalidate(handledCommand);
                else {
                    ScriptEventListener eventListener = fileNameToEventListenerMapping.remove(file);
                    if (eventListener != null) {
                        eventListenerToEventTypeMapping.remove(eventListener);
                        identifyScript(file);
                    }
                }
            } else if (entry.getValue().equals(ENTRY_DELETE)) {
                Command handledCommand = getCommandForFile(file);
                if (handledCommand != null) {
                    cache.invalidate(handledCommand);
                    eventBus.post(new CommandRemovedEvent(handledCommand));
                } else {
                    ScriptEventListener eventListener = fileNameToEventListenerMapping.remove(file);
                    if (eventListener != null) {
                        eventListenerToEventTypeMapping.remove(eventListener);
                    }
                }
            }
        }
    }

    @Override
    public Path getDirectoryOfInterest() {
        return SCRIPTS_DIR;
    }

    private Command getCommandForFile(final Path file) {
        for (Map.Entry<Command, Path> entry : commandHandledToFileMapping.entrySet()) {
            if (entry.getValue().equals(file)) return entry.getKey();
        }
        return null;
    }

    private void identifyScript(final Path file) {
        try {
            Invocable invocable = getInvocable(file);
            boolean commandHandlerWasRegistered = registerCommandHandler(file, invocable);
            if (!commandHandlerWasRegistered) registerEventListener(file, invocable);
        } catch (ScriptException e) {
            ScriptManager.log.error("Could not identify the script: " + file.getFileName().toString(), e);
        }
    }


    private Invocable getInvocable(final Path scriptFile) throws ScriptException {
        try {
            String extension = FileUtil.getFileExtension(scriptFile);
            ScriptEngine scriptEngine = manager.getEngineByExtension(extension);
            if (scriptEngine != null) {
                List<String> allLinesInFile = Files.readAllLines(scriptFile, Charsets.UTF_8);
                String content = merge(allLinesInFile, '\n');
                scriptEngine.eval(content);
                return (Invocable) scriptEngine;
            } else throw new IllegalStateException("Unknown script extension (no ScriptEngine exists): " + extension);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    private boolean registerCommandHandler(final Path file, final Invocable scriptEngine) {
        ScriptCommandHandler commandHandler = scriptEngine.getInterface(ParameterizedScriptCommandHandler.class);
        List<CommandParser> parsers = new ArrayList<>();
        if (commandHandler == null) {
            commandHandler = scriptEngine.getInterface(ScriptCommandHandler.class);
            if (commandHandler == null) return false;

            parsers.add(new CommandParser(new ParamParsingSpecification("params", ".*")));
            parsers.add(CommandParser.getEmptyParser());
        } else {
            Collections.addAll(parsers, ((ParameterizedScriptCommandHandler) commandHandler).getParsers());
        }
        Command handler = commandHandler.handles();
        commandHandledToFileMapping.put(handler, file);
        commandParserMapping.put(handler, parsers);
        return true;
    }

    private boolean registerEventListener(final Path file, final Invocable scriptEngine) {
        ScriptEventListener listener = scriptEngine.getInterface(ScriptEventListener.class);
        if (listener == null) return false;
        fileNameToEventListenerMapping.put(file, listener);
        eventListenerToEventTypeMapping.put(listener, listener.handles());
        return true;
    }

    public CommandHandler getHandlerForCommand(final Command command) {
        final Path file = commandHandledToFileMapping.get(command);
        if (file == null) throw new IllegalStateException("Could not find the script file that handles the command: " + command.getName());
        try {
            return cache.get(command, new Callable<ScriptCommandHandler>() {
                @Override
                public ScriptCommandHandler call() throws Exception {
                    Invocable invocable = getInvocable(file);
                    return invocable.getInterface(ScriptCommandHandler.class);
                }
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Command, List<CommandParser>> getAllHandledCommands() {
        return commandParserMapping;
    }

    /**
     * Listens for all types of events in the EventBus and dispatches to any script event listeners that are interested
     *
     * @param event the event
     */
    @Subscribe
    public void onEvent(final Object event) {
        for (final Map.Entry<ScriptEventListener, Class<?>> entry : eventListenerToEventTypeMapping.entrySet()) {
            if (entry.getValue().equals(event.getClass())) {
                threadingManager.execute(new Runnable() {
                    @Override
                    public void run() {
                        entry.getKey().handleEvent(event);
                    }
                });
            }
        }
    }

    /*public static void main(String[] args) {
        ScriptManager scriptManager = new ScriptManager(new EventBus(), new ThreadingManager(5));
        scriptManager.init();
        System.out.println(Paths.get(".").toAbsolutePath());
        Map<Command, List<CommandParser>> allHandledCommands = scriptManager.getAllHandledCommands();
        for (Map.Entry<Command, List<CommandParser>> handledCommand : allHandledCommands.entrySet()) {
            System.out.println(handledCommand.getKey().getName());
            for (CommandParser commandParser : handledCommand.getValue()) {
                System.out.println("!"+handledCommand.getKey().getName()+" "+commandParser.getSyntaxDescription());
            }
        }
    }*/
}
