package tools.target_locator;

import database.models.SpellOpCharacter;

public interface TargetLocatorFactory {
    TargetLocator createLocator(SpellOpCharacter spellOpCharacter);
}
