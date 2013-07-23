package web.tools;

import database.models.BindingsContainer;
import tools.validation.ValidBindingsValidator;

public class MinimumOneRaceOrOnePersonalityBindingsResolver implements ValidBindingsValidator.MinimumEntitiesRequiredValidator {
    @Override
    public boolean isValid(final BindingsContainer bindings) {
        return (bindings.getPersonalities().size() + bindings.getRaces().size()) > 0;
    }
}
