package tools.user_activities;

import api.database.models.BotUser;
import database.daos.BuildDAO;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.Date;

public class BuildsActivitiesCounter implements RecentActivitiesCounter {
    private final BuildDAO buildDAO;
    private final BindingsManager bindingsManager;

    @Inject
    public BuildsActivitiesCounter(final BuildDAO buildDAO, final BindingsManager bindingsManager) {
        this.buildDAO = buildDAO;
        this.bindingsManager = bindingsManager;
    }

    @Override
    public int countNewActivities(final Date lastCheck, final BotUser user) {
        return buildDAO.countBuildsForUserAddedAfter(lastCheck, user, bindingsManager);
    }
}
