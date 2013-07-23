package tools.user_activities;

import api.database.models.BotUser;
import database.daos.EventDAO;

import javax.inject.Inject;
import java.util.Date;

public class WaveActivitiesCounter implements RecentActivitiesCounter {
    private final EventDAO eventDAO;

    @Inject
    public WaveActivitiesCounter(final EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    @Override
    public int countNewActivities(final Date lastCheck, final BotUser user) {
        return eventDAO.waveAddedAfter(lastCheck) ? 1 : 0;
    }
}
