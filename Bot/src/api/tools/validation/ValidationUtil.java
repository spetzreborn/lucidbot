package api.tools.validation;

import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Set;

public class ValidationUtil {

    private ValidationUtil() {
    }

    public static ValidatableObjectHolder validate(final Object object) {
        return new ValidatableObjectHolder() {
            @Override
            public ValidationExecutor using(final Validator validator) {
                return new ObjectValidator(object, validator);
            }
        };
    }

    public static ValidatableMethodHolder validate(final Object object, final Method method, final Object[] args) {
        return new ValidatableMethodHolder() {
            @Override
            public ValidationExecutor using(final ExecutableValidator validator) {
                return new MethodValidator(object, method, args, validator);
            }
        };
    }

    public static void throwOnViolations(final Set<ConstraintViolation<Object>> violations) {
        if (!violations.isEmpty()) {
            ConstraintViolation<Object> firstViolation = violations.iterator().next();
            throw new ValidationException("Encountered " + violations.size() + " validation errors. The first error was: " +
                    firstViolation.getMessage());
        }
    }

    public static interface ValidatableObjectHolder {
        ValidationExecutor using(Validator validator);
    }

    public static interface ValidatableMethodHolder {
        ValidationExecutor using(ExecutableValidator validator);
    }

    public static interface ValidationExecutor {
        ValidationExecutor forGroups(Class<?>... groups);

        void throwOnFailedValidation();

        boolean validationSucceeded();
    }

    private static class ObjectValidator implements ValidationExecutor {
        private final Object object;
        private final Validator validator;

        private Class<?>[] groups = new Class[]{};

        private ObjectValidator(final Object object, final Validator validator) {
            this.object = object;
            this.validator = validator;
        }

        @Override
        public ValidationExecutor forGroups(final Class<?>... groups) {
            if (groups == null) this.groups = new Class[]{};
            else this.groups = groups;
            return this;
        }

        @Override
        public void throwOnFailedValidation() {
            Set<ConstraintViolation<Object>> violations = validator.validate(object, groups);
            throwOnViolations(violations);
        }

        @Override
        public boolean validationSucceeded() {
            Set<ConstraintViolation<Object>> violations = validator.validate(object, groups);
            return violations.isEmpty();
        }
    }

    private static class MethodValidator implements ValidationExecutor {
        private final Object object;
        private final Method method;
        private final Object[] args;
        private final ExecutableValidator validator;

        private Class<?>[] groups = new Class[]{};

        private MethodValidator(final Object object, final Method method, final Object[] args, final ExecutableValidator validator) {
            this.object = object;
            this.method = method;
            this.args = args;
            this.validator = validator;
        }

        @Override
        public ValidationExecutor forGroups(final Class<?>... groups) {
            if (groups == null) this.groups = new Class[]{};
            else this.groups = groups;
            return this;
        }

        @Override
        public void throwOnFailedValidation() {
            Set<ConstraintViolation<Object>> violations = validator.validateParameters(object, method, args, groups);
            throwOnViolations(violations);
        }

        @Override
        public boolean validationSucceeded() {
            Set<ConstraintViolation<Object>> violations = validator.validateParameters(object, method, args, groups);
            return violations.isEmpty();
        }
    }
}
