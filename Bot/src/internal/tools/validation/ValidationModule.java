/*
 * Created by IntelliJ IDEA.
 * User: Fredrik
 * Date: 2013-07-15
 * Time: 08:55
 */
package internal.tools.validation;

import api.tools.validation.ValidationEnabled;
import api.tools.validation.ValidationUtil;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.matcher.Matchers;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.inject.Singleton;
import javax.validation.*;
import javax.validation.executable.ExecutableValidator;

public class ValidationModule extends AbstractModule {

    protected void configure() {
        bindInterceptor(Matchers.annotatedWith(ValidationEnabled.class), Matchers.any(), new MethodArgumentValidationInterceptor());
    }

    private static class MethodArgumentValidationInterceptor implements MethodInterceptor {
        private final ExecutableValidator validator = (ExecutableValidator) Validation.buildDefaultValidatorFactory().getValidator();

        @Override
        public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
            ValidationUtil.validate(methodInvocation.getThis(), methodInvocation.getMethod(), methodInvocation.getArguments())
                    .using(validator).throwOnFailedValidation();
            return methodInvocation.proceed();
        }
    }

    @Provides
    @Singleton
    ConstraintValidatorFactory getConstraintValidatorFactory(final Injector injector) {
        return new ConstraintValidatorFactory() {
            @Override
            public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
                return injector.getInstance(key);
            }

            @Override
            public void releaseInstance(final ConstraintValidator<?, ?> instance) {
            }
        };
    }

    @Provides
    @Singleton
    ExecutableValidator getExecutableValidator(final ConstraintValidatorFactory constraintValidatorFactory) {
        Configuration<?> configuration = Validation.byDefaultProvider().configure().constraintValidatorFactory(constraintValidatorFactory);
        ValidatorFactory validatorFactory = configuration.buildValidatorFactory();
        return (ExecutableValidator) validatorFactory.getValidator();
    }

    @Provides
    @Singleton
    Validator getValidator(final ConstraintValidatorFactory constraintValidatorFactory) {
        Configuration<?> configuration = Validation.byDefaultProvider().configure().constraintValidatorFactory(constraintValidatorFactory);
        ValidatorFactory validatorFactory = configuration.buildValidatorFactory();
        return validatorFactory.getValidator();
    }
}
