package tools.user_activities;

import api.database.models.BotUser;
import database.daos.TargetDAO;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.Date;

public class TargetsActivitiesCounter implements RecentActivitiesCounter {
    private final TargetDAO targetDAO;
    private final BindingsManager bindingsManager;

    @Inject
    public TargetsActivitiesCounter(final TargetDAO targetDAO, final BindingsManager bindingsManager) {
        this.targetDAO = targetDAO;
        this.bindingsManager = bindingsManager;
    }

    @Override
    public int countNewActivities(final Date lastCheck, final BotUser user) {
        return targetDAO.countTargetForUserAddedAfter(lastCheck, user, bindingsManager);
    }
}
