package tools.user_activities;

import api.database.models.BotUser;
import database.daos.KingdomDAO;

import javax.inject.Inject;
import java.util.Date;

public class NapsActivitiesCounter implements RecentActivitiesCounter {
    private final KingdomDAO kingdomDAO;

    @Inject
    public NapsActivitiesCounter(final KingdomDAO kingdomDAO) {
        this.kingdomDAO = kingdomDAO;
    }

    @Override
    public int countNewActivities(final Date lastCheck, final BotUser user) {
        return kingdomDAO.countNapsAddedAfter(lastCheck);
    }
}
