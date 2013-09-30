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

package api.tools.common;

import lombok.extern.log4j.Log4j;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static api.tools.text.StringUtil.capitalizeFirstLetters;

@Log4j
@ParametersAreNonnullByDefault
public class ReflectionUtil {
    private ReflectionUtil() {
    }

    /**
     * Invokes a method or sets a field value. For method names it automatically appends "set" if it isn't already specified.
     *
     * @param obj       the object to do the invoking/setting on
     * @param valueName the name of the value (corresponds to the method or field name)
     * @param valueType the type of the value
     * @param value     the actual value
     * @return true if a value was set as a result of this call
     */
    public static <E> boolean setMethodOrFieldValue(final Object obj, final String valueName, final Class<E> valueType, final E value) {
        try {
            boolean set = setMethodValue(obj, valueName.startsWith("set") ? valueName : "set" + capitalizeFirstLetters(valueName, false), valueType, value);
            if (!set) set = setFieldValue(obj, valueName, value);
            return set;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new IllegalArgumentException("Could not set value for that object-valueName-value combination", e);
        }
    }

    /**
     * Invokes a method
     *
     * @param obj       the object to do the invoking on
     * @param valueName the name of the value (corresponds to the exact method name)
     * @param valueType the type of the value
     * @param value     the actual value
     * @param <E>       .
     */
    public static <E> boolean setMethodValue(final Object obj, final String valueName, final Class<E> valueType, final E value) throws
            InvocationTargetException,
            IllegalAccessException {
        try {
            Method method = obj.getClass().getDeclaredMethod(valueName, valueType);
            method.setAccessible(true);
            method.invoke(obj, value);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Sets a field value
     *
     * @param obj       the object to do the setting on
     * @param valueName the name of the value (corresponds to field name)
     * @param value     the actual value
     * @param <E>       .
     */
    public static <E> boolean setFieldValue(final Object obj, final String valueName, final E value) throws IllegalAccessException {
        try {
            Field field = obj.getClass().getDeclaredField(valueName);
            field.setAccessible(true);
            field.set(obj, value);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    /**
     * Retrieves the first method (if one exists) which has the specified return type
     *
     * @param subject    the class to search for the method in
     * @param returnType the return type
     * @return the matching method
     * @throws IllegalArgumentException if no method is found
     */
    public static Method getMethodWithReturnType(final Class<?> subject, final Class<?> returnType) {
        for (Method method : subject.getDeclaredMethods()) {
            if (method.getReturnType().equals(returnType)) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new IllegalArgumentException("The subject class had no method with the return type: " + returnType.getSimpleName());
    }

    /**
     * Invokes the specified method on the specific object, provided that the method is found and has the correct returntype
     *
     * @param methodName the name of the method
     * @param object     the object to invoke the method on
     * @param returnType the returntype of the method
     * @param <E>        .
     * @return the result of the invoked method
     * @throws IllegalArgumentException if the invocation failed for some reason
     */
    public static <E> E invokeMethod(final String methodName, final Object object, final Class<E> returnType) {
        try {
            Method method = object.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            if (method.getReturnType().equals(returnType)) {
                Object result = method.invoke(object);
                return result == null ? null : returnType.cast(result);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            ReflectionUtil.log.error("", e);
            throw new IllegalArgumentException(e);
        }
        throw new IllegalArgumentException("The specified method had the wrong return type: " + methodName);
    }
}
