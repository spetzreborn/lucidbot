package tools.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {IsValidEnumNameValidator.class})
public @interface IsValidEnumName {
    Class<? extends Enum<?>> enumType();

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
