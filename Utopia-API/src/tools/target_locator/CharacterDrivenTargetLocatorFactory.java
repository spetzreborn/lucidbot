package tools.target_locator;

import com.google.inject.Injector;
import database.models.SpellOpCharacter;

import javax.inject.Inject;

public class CharacterDrivenTargetLocatorFactory implements TargetLocatorFactory {
    private final Injector injector;

    @Inject
    public CharacterDrivenTargetLocatorFactory(final Injector injector) {
        this.injector = injector;
    }

    @Override
    public TargetLocator createLocator(final SpellOpCharacter spellOpCharacter) {
        return injector.getInstance(spellOpCharacter.getTargetLocatorType());
    }

}
