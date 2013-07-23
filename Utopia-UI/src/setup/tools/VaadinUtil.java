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

package setup.tools;

import com.vaadin.data.Validatable;
import com.vaadin.data.Validator;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.Window;

public class VaadinUtil {
    private VaadinUtil() {
    }

    public static Window createPopupWindow(final Component contentComponent, final String height, final String width) {
        final Window window = new Window();
        window.setModal(true);
        window.setClosable(true);
        window.addComponent(contentComponent);
        window.setHeight(height);
        window.setWidth(width);
        return window;
    }

    public static <E, C extends Field & Validatable> E validate(final C component, final Class<E> returnType) throws
            Validator.InvalidValueException {
        if ((component.getValidators() == null || component.getValidators().isEmpty())) {
            if (component.isRequired() &&
                    (component.getValue() == null || component.getValue() instanceof String && ((String) component.getValue()).isEmpty())) {
                throw new Validator.EmptyValueException("Value may not be empty: " + component.getCaption());
            } else return component.getValue() == null ? null : returnType.cast(component.getValue());
        }

        if (component.getValue() == null || component.getValue() instanceof String && ((String) component.getValue()).isEmpty()) {
            if (component.isRequired()) throw new Validator.EmptyValueException("Value may not be empty: " + component.getCaption());
            else return component.getValue() == null ? null : returnType.cast(component.getValue());
        }

        Object value = component.getValue();
        for (Validator validator : component.getValidators()) {
            if (!validator.isValid(value)) throw new Validator.InvalidValueException("Invalid value for: " + component.getCaption());
        }
        return returnType.cast(value);
    }
}
