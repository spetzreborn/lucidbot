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

package api.settings;

import api.tools.files.FileUtil;
import api.tools.files.JavaFileFilter;
import api.tools.text.StringUtil;
import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class capable of loading service classes from plugins
 *
 * @param <S> the type of service
 */
@Log4j
@ParametersAreNonnullByDefault
public final class PluginServiceLoader<S> {
    private static final Pattern CLASS_NAME = Pattern.compile("([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*");
    private static final String PREFIX = "META-INF/services/";
    private static final Path LIB_DIR = Paths.get("lib");

    /**
     * Creates a new loader for the specified service
     *
     * @param service the type of the service
     * @param <S>     .
     * @return a new PluginServiceLoader
     */
    public static <S> PluginServiceLoader<S> newLoaderFor(final Class<S> service, final String dir) {
        FileUtil.createDirectoryIfNotExists(dir);
        File pluginDir = new File(dir);

        List<URI> urls = new ArrayList<>();
        for (File file : pluginDir.listFiles(new JavaFileFilter())) {
            urls.add(file.toURI());
        }

        ClassLoader classLoader = PluginClassLoader.get(urls);
        return new PluginServiceLoader<>(service, classLoader);
    }

    private final Class<S> service;
    private final ClassLoader loader;

    private PluginServiceLoader(final Class<S> service, final ClassLoader loader) {
        this.service = checkNotNull(service);
        this.loader = checkNotNull(loader);
    }

    /**
     * @return a Set containing all the classes that were found to implement the service specified for this loader
     */
    public Set<Class<? extends S>> getClasses() {
        Set<Class<? extends S>> out = new HashSet<>();
        try {
            String fullName = PREFIX + service.getName();
            Enumeration<URL> configs = loader.getResources(fullName);
            while (configs.hasMoreElements()) {
                URL url = configs.nextElement();
                List<String> classNames = parse(url);
                for (String className : classNames) {
                    Class<?> aClass = Class.forName(className, false, loader);
                    if (service.isAssignableFrom(aClass)) {
                        Class<? extends S> provider = (Class<? extends S>) aClass;
                        out.add(provider);
                    } else log.warn("This class is registered as a service (" + service.getName() + ") but is not " +
                                    "assignable from that service: " + aClass.getName());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            log.error("", e);
        }
        return out;
    }

    /**
     * @return creates and returns instances of all the found classes
     */
    public List<S> getInstances() {
        Set<Class<? extends S>> classes = getClasses();
        List<S> out = new ArrayList<>(classes.size());
        try {
            for (Class<? extends S> aClass : classes) {
                out.add(aClass.getConstructor().newInstance());
            }
        } catch (Exception e) {
            log.error("Failed to instantiate a service", e);
        }
        return out;
    }

    private static List<String> parse(final URL url) {
        List<String> names = new ArrayList<>();
        try (InputStream in = url.openStream();
             BufferedReader r = new BufferedReader(new InputStreamReader(in, "utf-8"))) {
            String line;
            Matcher matcher;
            while ((line = r.readLine()) != null) {
                matcher = CLASS_NAME.matcher(line);
                if (matcher.matches()) names.add(line);
            }
        } catch (IOException e) {
            log.error("Failed to retrieve service description", e);
        }
        return names;
    }

    private static class PluginClassLoader extends URLClassLoader {
        private final Set<String> alreadyLoaded = Collections.synchronizedSet(new HashSet<String>());

        public static ClassLoader get(final List<URI> uris) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (uris.isEmpty()) return contextClassLoader;

            if (contextClassLoader instanceof PluginClassLoader) {
                PluginClassLoader classLoader = (PluginClassLoader) contextClassLoader;
                List<URI> notAlreadyLoaded = new ArrayList<>(uris.size());
                for (URI url : uris) {
                    if (!classLoader.alreadyLoaded.contains(url.toString())) notAlreadyLoaded.add(url);
                }
                if (notAlreadyLoaded.isEmpty()) return classLoader;
                else return newClassLoader(notAlreadyLoaded, classLoader);
            } else {
                return newClassLoader(uris, contextClassLoader);
            }
        }

        private static PluginClassLoader newClassLoader(final List<URI> uris, final ClassLoader parent) {
            Set<URI> urisToLoad = new HashSet<>(uris);
            if (parent instanceof PluginClassLoader) {
                PluginClassLoader parentAsPluginCL = (PluginClassLoader) parent;
                for (URI uri : uris) {
                    for (URI dep : loadDependencies(uri)) {
                        if (!parentAsPluginCL.alreadyLoaded.contains(dep.toString())) urisToLoad.add(dep);
                    }
                }
            } else {
                for (URI uri : uris) {
                    urisToLoad.addAll(loadDependencies(uri));
                }
            }

            PluginClassLoader classLoader = new PluginClassLoader(transformToURL(urisToLoad.toArray(new URI[urisToLoad.size()])), parent);
            for (URI uri : uris) {
                classLoader.alreadyLoaded.add(uri.toString());
                if (parent instanceof PluginClassLoader) {
                    PluginClassLoader parentCL = (PluginClassLoader) parent;
                    classLoader.alreadyLoaded.addAll(parentCL.alreadyLoaded);
                }
            }
            Thread.currentThread().setContextClassLoader(classLoader);
            return classLoader;
        }

        private static Set<URI> loadDependencies(final URI uri) {
            Set<URI> uris = new HashSet<>();
            try {
                Path path = Paths.get(uri);
                JarFile jarFile = new JarFile(path.toFile());
                Manifest manifest = jarFile.getManifest();
                if (manifest != null) {
                    for (Map.Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
                        if ("Class-Path".equals(entry.getKey().toString())) {
                            List<String> dependencies = Lists.newArrayList(StringUtil.splitOnSpace(entry.getValue().toString()));
                            File libs = LIB_DIR.toFile();
                            for (File file : libs.listFiles()) {
                                if (dependencies.contains(file.getName())) {
                                    uris.add(file.toURI());
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                PluginServiceLoader.log.warn("", e);
            }
            return uris;
        }

        private static URL[] transformToURL(final URI... uris) {
            URL[] urls = new URL[uris.length];
            for (int i = 0; i < urls.length; i++) {
                try {
                    urls[i] = uris[i].toURL();
                } catch (MalformedURLException e) {
                    PluginServiceLoader.log.warn("Malformed URL", e);
                }
            }
            return urls;
        }

        private PluginClassLoader(final URL[] urls, final ClassLoader parent) {
            super(urls, parent);
        }
    }
}

