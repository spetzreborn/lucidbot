package tools.target_locator;

import api.database.models.BotUser;
import database.daos.ProvinceDAO;
import database.models.Province;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;

import javax.inject.Inject;
import java.util.regex.Matcher;

@Log4j
public class TargetInTextLocator implements TargetLocator {
    private final ProvinceDAO provinceDAO;

    @Inject
    public TargetInTextLocator(final ProvinceDAO provinceDAO) {
        this.provinceDAO = provinceDAO;
    }

    @Override
    public Province locateTarget(final BotUser user, final Matcher matchedRegex) {
        String target = matchedRegex.group("target");
        String province = target.substring(0, target.indexOf('(')).trim();
        String kingdom = target.substring(target.indexOf('('));

        if (province == null)
            throw new IllegalStateException("Could not find a province in the message even though one was expected");

        return getProvince(provinceDAO, province, kingdom);
    }

    private static Province getProvince(final ProvinceDAO provinceDAO, final String name, final String kingdom) {
        try {
            return provinceDAO.getOrCreateProvince(name, kingdom);
        } catch (HibernateException e) {
            TargetInTextLocator.log.error("", e);
        }
        return null;
    }
}
