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

package api.tools.collections;

/**
 * A container for parameters
 */
public interface Params {
    /**
     * @param name the name of the param
     * @return the named parameter, if found, or null otherwise
     */
    String getParameter(final String name);

    /**
     * @param name the name of the param
     * @return the named parameter, if found, or null otherwise
     */
    String[] getParameters(final String name);

    /**
     * @param name the name of the param
     * @return the named parameter, if found, or -1 otherwise
     * @throws IllegalArgumentException if the param cannot be parsed into an int
     */
    int getIntParameter(final String name);

    /**
     * @param name the name of the param
     * @return the named parameter, if found, or -1 otherwise
     * @throws IllegalArgumentException if the param cannot be parsed into a long
     */
    long getLongParameter(final String name);

    /**
     * @param name the name of the param
     * @return the named parameter, if found, or -1 otherwise
     * @throws IllegalArgumentException if the param cannot be parsed into a double
     */
    double getDoubleParameter(final String name);

    /**
     * @param name the name of the param
     * @return the named parameter, if found, or false otherwise
     */
    boolean getBooleanParameter(final String name);

    /**
     * @param key the key of the param
     * @return true if this Params contains a value mapped to the specified key that isn't null
     */
    boolean containsKey(String key);

    /**
     * @return the amount of params
     */
    int size();

    /**
     * @return true if the amount of params is 0
     */
    boolean isEmpty();
}
