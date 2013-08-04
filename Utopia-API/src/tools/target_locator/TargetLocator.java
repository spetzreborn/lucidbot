package tools.target_locator;

import api.database.models.BotUser;
import database.models.Province;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.regex.Matcher;

@ParametersAreNonnullByDefault
public interface TargetLocator {
    @Nullable
    Province locateTarget(BotUser user, Matcher matchedRegex);
}
