package tools.validation;

import api.common.HasNumericId;

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
@Constraint(validatedBy = {ExistsInDBValidator.class})
public @interface ExistsInDB {
    /**
     * @return the database mapped type
     */
    Class<? extends HasNumericId> entity();

    /**
     * @return true if the value may either exist in the db or be null
     */
    boolean optional() default false;

    /**
     * @return the error message used if this validation fails
     */
    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
