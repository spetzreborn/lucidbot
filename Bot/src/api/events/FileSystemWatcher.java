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

package api.events;

import api.runtime.ThreadingManager;
import lombok.extern.log4j.Log4j;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * A class that manages changes in the file system. Listeners may register to this class to be notified when any changes occur.
 */
@Log4j
@ParametersAreNonnullByDefault
public final class FileSystemWatcher {
    private final ThreadingManager threadingManager;

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private final ConcurrentMap<Path, Collection<DirectoryChangeEventObserver>> observers = new ConcurrentHashMap<>();

    @Inject
    public FileSystemWatcher(final ThreadingManager threadingManager) {
        this.threadingManager = checkNotNull(threadingManager);
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (final IOException e) {
            log.error("Could not create the file observing service", e);
            throw new RuntimeException(e);
        }
    }

    @Inject
    public void init() {
        threadingManager.submitInfiniteTask(new FileSystemEventListener(this));
    }

    /**
     * Registers the specified observer/listener
     *
     * @param observer the observer/listener that wants to register
     */
    public void registerForDirectoryMonitoring(final DirectoryChangeEventObserver observer) {
        try {
            Path dir = observer.getDirectoryOfInterest();
            if (!observers.containsKey(dir)) {
                observers.putIfAbsent(dir, Collections.synchronizedSet(new HashSet<DirectoryChangeEventObserver>()));
                WatchKey watchKey = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                keys.put(watchKey, dir);
            }
            observers.get(dir).add(observer);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    @ParametersAreNonnullByDefault
    private static final class FileSystemEventListener implements Runnable {
        private final FileSystemWatcher manager;

        private FileSystemEventListener(final FileSystemWatcher manager) {
            this.manager = checkNotNull(manager);
        }

        @Override
        public void run() {
            while (true) {
                WatchKey key;
                try {
                    key = manager.watcher.take();
                } catch (InterruptedException x) {
                    return;
                }

                Path path = manager.keys.get(key);
                if (path == null) {
                    continue;
                }

                Collection<DirectoryChangeEventObserver> dirObs = manager.observers.get(path);

                List<WatchEvent<?>> events = key.pollEvents();
                if (dirObs != null && !dirObs.isEmpty()) {
                    alertDirectoryObservers(path, events, dirObs);
                }

                key.reset();
            }
        }

        private static void alertDirectoryObservers(final Path dir, final List<WatchEvent<?>> events,
                                                    final Collection<DirectoryChangeEventObserver> observers) {
            Map<Path, WatchEvent.Kind<?>> map = new HashMap<>();
            for (WatchEvent<?> event : events) {
                WatchEvent.Kind<?> kind = event.kind();

                if (!kind.equals(OVERFLOW)) {
                    Path name = (Path) event.context();
                    Path child = dir.resolve(name);
                    map.put(child, kind);
                }
            }
            for (DirectoryChangeEventObserver observer : observers) {
                observer.handleUpdatedDirectory(map);
            }
        }
    }
}
