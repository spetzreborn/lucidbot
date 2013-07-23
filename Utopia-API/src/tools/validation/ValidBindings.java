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
@Constraint(validatedBy = {ValidBindingsValidator.class})
public @interface ValidBindings {
    Class<? extends ValidBindingsValidator.MinimumEntitiesRequiredValidator> minimumEntityBindingsResolver() default ValidBindingsValidator.NoMinimum.class;

    boolean mustHaveExpiry() default false;

    boolean mustHavePublish() default false;

    boolean nillable() default false;

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
