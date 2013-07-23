package tools.validation;

import api.common.HasNumericId;
import api.database.models.BotUser;
import database.models.BindingsContainer;
import database.models.Personality;
import database.models.Race;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Date;

public class ValidBindingsValidator implements ConstraintValidator<ValidBindings, BindingsContainer> {
    private final ExistsInDBValidator existsInDBValidator;

    private Class<? extends MinimumEntitiesRequiredValidator> minimumEntityBindingsResolver;
    private boolean mustHavePublish;
    private boolean mustHaveExpiry;
    private boolean nillable;

    @Inject
    public ValidBindingsValidator(final ExistsInDBValidator existsInDBValidator) {
        this.existsInDBValidator = existsInDBValidator;
    }

    @Override
    public void initialize(final ValidBindings constraintAnnotation) {
        this.minimumEntityBindingsResolver = constraintAnnotation.minimumEntityBindingsResolver();
        this.mustHavePublish = constraintAnnotation.mustHavePublish();
        this.mustHaveExpiry = constraintAnnotation.mustHaveExpiry();
        this.nillable = constraintAnnotation.nillable();
    }

    @Override
    public boolean isValid(final BindingsContainer value, final ConstraintValidatorContext context) {
        if (nillable && value == null) return true;
        else if (!nillable && value == null) return false;

        try {
            if (!minimumEntityBindingsResolver.newInstance().isValid(value))
                return false;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Programming error, please report that the bindings resolver for validation is bugged");
        }

        existsInDBValidator.setEntityType(BotUser.class);
        for (HasNumericId user : value.getUsers()) {
            if (!existsInDBValidator.isValid(user, context)) return false;
        }

        existsInDBValidator.setEntityType(Personality.class);
        for (HasNumericId personality : value.getPersonalities()) {
            if (!existsInDBValidator.isValid(personality, context)) return false;
        }

        existsInDBValidator.setEntityType(Race.class);
        for (HasNumericId race : value.getRaces()) {
            if (!existsInDBValidator.isValid(race, context)) return false;
        }

        Date now = new Date();

        if (mustHavePublish && value.getPublishDate() == null ||
                value.getPublishDate() != null && !value.getPublishDate().after(now)) return false;

        if (mustHaveExpiry && value.getExpiryDate() == null ||
                value.getExpiryDate() != null && !value.getExpiryDate().after(now)) return false;

        return true;
    }

    public static interface MinimumEntitiesRequiredValidator {
        boolean isValid(BindingsContainer bindings);
    }

    public static class NoMinimum implements MinimumEntitiesRequiredValidator {
        @Override
        public boolean isValid(final BindingsContainer bindings) {
            return true;
        }
    }
}
