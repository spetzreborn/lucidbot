package tools.target_locator;

import api.database.models.BotUser;
import database.daos.UserSpellOpTargetDAO;
import database.models.Province;
import database.models.UserSpellOpTarget;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;

import javax.inject.Inject;
import java.util.regex.Matcher;

@Log4j
public class UserSpecifiedTargetLocator implements TargetLocator {
    private final UserSpellOpTargetDAO userSpellOpTargetDAO;

    @Inject
    public UserSpecifiedTargetLocator(final UserSpellOpTargetDAO userSpellOpTargetDAO) {
        this.userSpellOpTargetDAO = userSpellOpTargetDAO;
    }

    @Override
    public Province locateTarget(final BotUser user, final Matcher matchedRegex) {
        UserSpellOpTarget userSpellOpTarget = null;
        try {
            userSpellOpTarget = userSpellOpTargetDAO.getUserSpellOpTarget(user);
        } catch (HibernateException e) {
            UserSpecifiedTargetLocator.log.error("", e);
        }
        return userSpellOpTarget == null ? null : userSpellOpTarget.getTarget();
    }
}
