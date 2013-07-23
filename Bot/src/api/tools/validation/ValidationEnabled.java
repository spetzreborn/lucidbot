package api.tools.validation;

import java.lang.annotation.*;

/**
 * Marks the class as a target of automatic validation (provided that the class is handled by the dependency injection container).
 * Validates method arguments for all methods called on the class.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ValidationEnabled {
}
