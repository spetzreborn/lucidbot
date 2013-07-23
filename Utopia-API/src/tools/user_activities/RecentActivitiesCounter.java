package tools.user_activities;

import api.database.models.BotUser;

import java.util.Date;

public interface RecentActivitiesCounter {
    int countNewActivities(Date lastCheck, BotUser user);
}
