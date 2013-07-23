package tools.validation;

import api.common.HasNumericId;
import api.database.Transactional;
import com.google.inject.Provider;
import org.hibernate.Session;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ExistsInDBValidator implements ConstraintValidator<ExistsInDB, HasNumericId> {
    private final Provider<Session> sessionProvider;

    private Class<? extends HasNumericId> entityType;
    private boolean optional;

    @Inject
    public ExistsInDBValidator(final Provider<Session> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    @Override
    public void initialize(final ExistsInDB constraintAnnotation) {
        entityType = constraintAnnotation.entity();
        optional = constraintAnnotation.optional();
    }

    public void setEntityType(final Class<? extends HasNumericId> entityType) {
        this.entityType = entityType;
    }

    public void setOptional(final boolean optional) {
        this.optional = optional;
    }

    @Override
    @Transactional
    public boolean isValid(final HasNumericId value, final ConstraintValidatorContext context) {
        return (optional && value == null) || (value.getId() != null && sessionProvider.get().get(entityType, value.getId()) != null);
    }
}
