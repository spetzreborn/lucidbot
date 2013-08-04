package tools.target_locator;

import api.database.models.BotUser;
import database.daos.ProvinceDAO;
import database.models.Province;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.regex.Matcher;

@Log4j
public class SelfTargetLocator implements TargetLocator {
    private final ProvinceDAO provinceDAO;

    @Inject
    public SelfTargetLocator(final ProvinceDAO provinceDAO) {
        this.provinceDAO = provinceDAO;
    }

    @Nullable
    @Override
    public Province locateTarget(final BotUser user, final Matcher matchedRegex) {
        try {
            return provinceDAO.getProvinceForUser(user);
        } catch (HibernateException e) {
            SelfTargetLocator.log.error("", e);
        }
        return null;
    }
}
