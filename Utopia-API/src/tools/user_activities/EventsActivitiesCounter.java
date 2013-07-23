package tools.user_activities;

import api.database.models.BotUser;
import database.daos.EventDAO;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.Date;

public class EventsActivitiesCounter implements RecentActivitiesCounter {
    private final EventDAO eventDAO;
    private final BindingsManager bindingsManager;

    @Inject
    public EventsActivitiesCounter(final EventDAO eventDAO, final BindingsManager bindingsManager) {
        this.eventDAO = eventDAO;
        this.bindingsManager = bindingsManager;
    }

    @Override
    public int countNewActivities(final Date lastCheck, final BotUser user) {
        return eventDAO.countEventsForUserAddedAfter(lastCheck, user, bindingsManager);
    }
}
