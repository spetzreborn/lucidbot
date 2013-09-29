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

package internal.settings;

import api.tools.time.DateFactory;
import com.google.common.base.Charsets;
import lombok.extern.log4j.Log4j;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static api.tools.text.StringUtil.isNotNullOrEmpty;

/**
 * A container for properties, capable of writing to and reading from a properties file
 */
@ParametersAreNonnullByDefault
@Log4j
public final class Properties {
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("([^=]+)\\s*=\\s*(.*?)");

    private final Map<String, String> properties = new HashMap<>();
    private final ReadWriteLock propertiesLock = new ReentrantReadWriteLock(true);
    private final Path file;

    public Properties(final Path propertiesFile) {
        file = propertiesFile;
    }

    /**
     * @param key the key of the property
     * @return the property
     */
    public String get(final String key) {
        propertiesLock.readLock().lock();
        try {
            return properties.get(key);
        } finally {
            propertiesLock.readLock().unlock();
        }
    }

    /**
     * Adds the specified property
     *
     * @param key   the key
     * @param value the property
     */
    public boolean put(final String key, final String value) {
        boolean success = false;
        propertiesLock.writeLock().lock();
        try {
            properties.put(key, value);
            success = true;
        } finally {
            propertiesLock.writeLock().unlock();
        }
        return success;
    }

    /**
     * Puts the specified Date in this Properties in the correct format
     *
     * @param key  the key of the property
     * @param date the property
     */
    public boolean putDate(final String key, final Date date) {
        return put(key, DateFactory.getISOWithoutSecondsDateTimeFormat().format(date));
    }


    /**
     * Loads the properties from file
     */
    public void loadFromFile() {
        propertiesLock.readLock().lock();
        try {
            for (String line : Files.readAllLines(file, Charsets.UTF_8)) {
                if (isNotNullOrEmpty(line) && !line.trim().startsWith("#")) {
                    String prop = line.trim();
                    Matcher matcher = KEY_VALUE_PATTERN.matcher(prop);
                    if (matcher.matches()) {
                        String key = matcher.group(1);
                        String value = matcher.group(2);
                        if (isNotNullOrEmpty(key)) {
                            put(key.trim(), value.trim());
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not read the properties files", e);
        } finally {
            propertiesLock.readLock().unlock();
        }
    }

    /**
     * Saves the current properties to file
     */
    public void saveToFile() {
        propertiesLock.writeLock().lock();
        try {
            List<String> fileContent = Files.readAllLines(file, Charsets.UTF_8);
            List<String> props = new ArrayList<>(fileContent.size());
            for (String line : fileContent) {
                if (isNotNullOrEmpty(line) && !line.trim().startsWith("#")) {
                    Matcher matcher = KEY_VALUE_PATTERN.matcher(line.trim());
                    if (matcher.matches() && properties.containsKey(matcher.group(1).trim())) {
                        String propName = matcher.group(1).trim();
                        props.add(propName + " = " + properties.get(propName));
                    } else {
                        props.add(line);
                    }
                } else {
                    props.add(line);
                }
            }
            Files.write(file, props, Charsets.UTF_8, StandardOpenOption.WRITE);
        } catch (Exception e) {
            Properties.log.error("Failed to save properties to file", e);
        } finally {
            propertiesLock.writeLock().unlock();
        }
    }

    /**
     * @param key the key to search for
     * @return true if a property with that key exists
     */
    boolean containsKey(final String key) {
        propertiesLock.readLock().lock();
        try {
            return properties.containsKey(key);
        } finally {
            propertiesLock.readLock().unlock();
        }
    }

    /**
     * @return a Map with all the properties
     */
    public Map<String, String> asMap() {
        Map<String, String> out = new HashMap<>();
        propertiesLock.readLock().lock();
        try {
            out.putAll(properties);
        } finally {
            propertiesLock.readLock().unlock();
        }
        return out;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Properties that = (Properties) o;

        return file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }
}
