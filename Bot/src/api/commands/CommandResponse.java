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

package api.commands;

import api.tools.collections.MapFactory;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The response from a Command that's been handled. Contains data for the template to use.
 */
@ParametersAreNonnullByDefault
public class CommandResponse {
    private final Map<String, Object> storage;
    private final String errorMessage;

    /**
     * Creates a CommandResponse with the specified objects seen as name-value pairs
     *
     * @param objects name-value pairs to initialize the CommandResponse with
     * @return a new CommandResponse initialized with the specified name-value pairs
     */
    public static CommandResponse resultResponse(final Object... objects) {
        checkNotNull(objects);
        checkArgument(objects.length % 2 == 0, "Number of parameters isn't an even value");
        return new CommandResponse(MapFactory.newMapWithNamedObjects(objects));
    }

    /**
     * @return a new CommandResponse that is empty but is not locked for modification in any way
     */
    public static CommandResponse emptyResponse() {
        return new CommandResponse(new HashMap<String, Object>());
    }

    /**
     * Creates a new CommandResponse with the specified error message set
     *
     * @param errorMessage the error message
     * @return a new CommandResponse with the specified error message
     */
    public static CommandResponse errorResponse(final String errorMessage) {
        checkNotNull(errorMessage);
        return new CommandResponse(errorMessage);
    }

    private CommandResponse(final Map<String, Object> storage) {
        this.storage = storage;
        this.errorMessage = null;
    }

    private CommandResponse(final String errorMessage) {
        this.storage = new HashMap<>();
        this.errorMessage = errorMessage;
    }

    public boolean isError() {
        return errorMessage != null;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public int size() {
        return storage.size();
    }

    public boolean isEmpty() {
        return storage.isEmpty() && !isError();
    }

    public boolean containsKey(final String key) {
        return storage.containsKey(checkNotNull(key));
    }

    @Nullable
    public Object get(final String key) {
        return storage.get(checkNotNull(key));
    }

    @Nullable
    public Object put(final String key, final Object value) {
        return storage.put(checkNotNull(key), value);
    }

    @Nullable
    public Object remove(final String key) {
        return storage.remove(checkNotNull(key));
    }

    public void putAll(final Map<String, ?> m) {
        storage.putAll(checkNotNull(m));
    }

    public void clear() {
        storage.clear();
    }

    public Set<String> keySet() {
        return storage.keySet();
    }

    public Collection<Object> values() {
        return storage.values();
    }

    public Map<String, Object> asMap() {
        return new HashMap<>(storage);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(1000);
        sb.append("CommandResponse");
        sb.append("{storage=").append(storage);
        sb.append(", errorMessage='").append(errorMessage).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
