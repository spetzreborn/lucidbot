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

import api.runtime.ThreadingManager;
import api.tools.numbers.NumberUtil;
import api.tools.time.DateFactory;
import internal.settings.Properties;
import lombok.extern.log4j.Log4j;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Collection that holds properties from all the different properties files found at startup
 */
@Log4j
@ParametersAreNonnullByDefault
public final class PropertiesCollection {
    private final Map<String, String> defaults;
    private final Map<String, Properties> keyToPropertiesMapping = new HashMap<>();
    private final ThreadingManager threadingManager;

    public PropertiesCollection(final List<Properties> propertiesList, final Map<String, String> defaults,
                                final ThreadingManager threadingManager) {
        this.defaults = checkNotNull(defaults);
        for (Properties properties : propertiesList) {
            for (String key : properties.asMap().keySet()) {
                keyToPropertiesMapping.put(key, properties);
            }
        }
        this.threadingManager = checkNotNull(threadingManager);
    }

    /**
     * @param key the key of the property
     * @return the property
     */
    @Nullable
    public String get(final String key) {
        String out;

        Properties properties = keyToPropertiesMapping.get(key);
        if (properties == null) out = defaults.get(key);
        else out = properties.get(key);

        return out;
    }

    /**
     * @param key       the key of the property
     * @param separator the separator for the list
     * @return the property value as a list
     */
    @Nullable
    public List<String> getList(final String key, final String separator) {
        String prop = get(key);
        if (prop == null) return null;

        String[] split = prop.split(separator);
        List<String> out = new ArrayList<>(split.length);
        for (String s : split) {
            out.add(s.trim());
        }
        return out;
    }

    /**
     * @param key the key for the property
     * @return the specified property as a Date, or null if the property isn't found
     * @throws IllegalArgumentException if the property isn't a parsable Date
     */
    @Nullable
    public Date getDate(final String key) {
        String prop = get(key);
        if (prop == null) return null;
        return DateFactory.newGMTDate(prop);
    }

    /**
     * @param key the key for the property
     * @return the specified property as a Long, or null if the property isn't found
     * @throws IllegalArgumentException if the property isn't a parsable Long
     */
    @Nullable
    public Long getLong(final String key) {
        String prop = get(key);
        if (prop == null) return null;
        try {
            return NumberUtil.parseLong(prop);
        } catch (IllegalArgumentException e) {
            log.error("Long could not be parsed from properties. Key was: " + key);
            throw e;
        }
    }

    /**
     * @param key the key for the property
     * @return the specified property as a Double, or null if the property isn't found
     * @throws IllegalArgumentException if the property isn't a parsable Double
     */
    @Nullable
    public Double getDouble(final String key) {
        String prop = get(key);
        if (prop == null) return null;
        try {
            return NumberUtil.parseDouble(prop);
        } catch (IllegalArgumentException e) {
            log.error("Double could not be parsed from properties. Key was: " + key);
            throw e;
        }
    }

    /**
     * @param key the key for the property
     * @return the specified property as a Integer, or null if the property isn't found
     * @throws IllegalArgumentException if the property isn't a parsable Integer
     */
    @Nullable
    public Integer getInteger(final String key) {
        String prop = get(key);
        if (prop == null) return null;
        try {
            return NumberUtil.parseInt(prop);
        } catch (IllegalArgumentException e) {
            log.error("Integer could not be parsed from properties. Key was: " + key);
            throw e;
        }
    }

    /**
     * @param key the key of the property
     * @return true if the property is found and can be parsed as true. False if the property exists but isn't matched to true.
     *         Null if the property isn't found at all.
     */
    @Nullable
    public Boolean getBoolean(final String key) {
        String prop = get(key);
        if (prop == null) return null;
        return Boolean.valueOf(prop);
    }

    /**
     * @param key the key to search for
     * @return true if a property with that key exists
     */
    boolean containsKey(final String key) {
        return get(key) != null;
    }

    /**
     * Changes the specified property
     *
     * @param key   the key
     * @param value the property
     * @return true if the property was changed successfully
     */
    public boolean change(final String key, final String value) {
        final Properties properties = keyToPropertiesMapping.get(checkNotNull(key));
        if (properties == null) {
            if (defaults.containsKey(key)) {
                defaults.put(key, value);
                return true;
            } else return false;
        }
        boolean success = properties.put(key, checkNotNull(value));
        if (success) threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                properties.saveToFile();
            }
        });
        return success;
    }

    /**
     * Changes the specified properties
     *
     * @param props a Map with the properties to change
     */
    public void change(final Map<String, String> props) {
        boolean success = true;
        final Set<Properties> toSave = new HashSet<>();
        for (Map.Entry<String, String> entry : props.entrySet()) {
            checkNotNull(entry.getKey());
            checkNotNull(entry.getValue());

            final Properties properties = keyToPropertiesMapping.get(entry.getKey());
            if (properties == null) {
                if (defaults.containsKey(entry.getKey())) {
                    defaults.put(entry.getKey(), entry.getValue());
                    continue;
                }
            }
            if (success) success = properties.put(entry.getKey(), entry.getValue());
            toSave.add(properties);
        }
        if (success) threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                for (Properties properties : toSave) {
                    properties.saveToFile();
                }
            }
        });
    }

    /**
     * Changes the specified property
     *
     * @param key   the key
     * @param value the property
     * @return true if the property was changed successfully
     */
    public boolean change(final String key, final Date value) {
        final Properties properties = keyToPropertiesMapping.get(checkNotNull(key));
        if (properties == null) {
            if (defaults.containsKey(key)) {
                defaults.put(key, DateFactory.getISODateFormat().format(value));
                return true;
            } else return false;
        }
        boolean success = properties.putDate(key, checkNotNull(value));
        if (success) threadingManager.execute(new Runnable() {
            @Override
            public void run() {
                properties.saveToFile();
            }
        });
        return success;
    }

    /**
     * @return a Map of all properties in this collection
     */
    public Map<String, String> getAllProperties() {
        Map<String, String> out = new HashMap<>();
        out.putAll(defaults);
        for (Map.Entry<String, Properties> entry : keyToPropertiesMapping.entrySet()) {
            out.put(entry.getKey(), entry.getValue().get(entry.getKey()));
        }
        return out;
    }
}
