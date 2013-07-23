package tools.user_activities;

import api.database.models.BotUser;
import database.daos.AidDAO;

import javax.inject.Inject;
import java.util.Date;

public class AidActivitiesCounter implements RecentActivitiesCounter {
    private final AidDAO aidDAO;

    @Inject
    public AidActivitiesCounter(final AidDAO aidDAO) {
        this.aidDAO = aidDAO;
    }

    @Override
    public int countNewActivities(final Date lastCheck, final BotUser user) {
        return aidDAO.countAidAddedAfter(lastCheck);
    }
}
