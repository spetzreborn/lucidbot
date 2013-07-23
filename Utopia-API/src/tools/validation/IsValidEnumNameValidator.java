package tools.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class IsValidEnumNameValidator implements ConstraintValidator<IsValidEnumName, String> {
    private Class<? extends Enum<?>> enumType;

    @Override
    public void initialize(final IsValidEnumName constraintAnnotation) {
        enumType = constraintAnnotation.enumType();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        try {
            Method factoryMethod = enumType.getDeclaredMethod("fromName", new Class[]{String.class});
            factoryMethod.setAccessible(true);
            Object result = factoryMethod.invoke(null, value);
            return result != null;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
