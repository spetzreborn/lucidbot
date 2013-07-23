package tools.user_activities;

import api.database.models.BotUser;
import api.tools.text.StringUtil;
import database.models.UserActivities;

import java.util.Date;

public enum RecentActivityType {
    AID {
        @Override
        public UnseenInfo getUnseen(final RecentActivitiesCounter counter, final BotUser user, final UserActivities userActivities) {
            Date lastAidCheck = userActivities.getLastAidCheck();
            return new UnseenInfo(getTypeName(), counter.countNewActivities(lastAidCheck, user), lastAidCheck);
        }
    },
    BUILDS {
        @Override
        public UnseenInfo getUnseen(final RecentActivitiesCounter counter, final BotUser user, final UserActivities userActivities) {
            Date lastBuildCheck = userActivities.getLastBuildCheck();
            return new UnseenInfo(getTypeName(), counter.countNewActivities(lastBuildCheck, user), lastBuildCheck);
        }
    },
    EVENTS {
        @Override
        public UnseenInfo getUnseen(final RecentActivitiesCounter counter, final BotUser user, final UserActivities userActivities) {
            Date lastEventsCheck = userActivities.getLastEventsCheck();
            return new UnseenInfo(getTypeName(), counter.countNewActivities(lastEventsCheck, user), lastEventsCheck);
        }
    },
    NAPS {
        @Override
        public UnseenInfo getUnseen(final RecentActivitiesCounter counter, final BotUser user, final UserActivities userActivities) {
            Date lastNapsCheck = userActivities.getLastNapsCheck();
            return new UnseenInfo(getTypeName(), counter.countNewActivities(lastNapsCheck, user), lastNapsCheck);
        }
    },
    NOTES {
        @Override
        public UnseenInfo getUnseen(final RecentActivitiesCounter counter, final BotUser user, final UserActivities userActivities) {
            Date lastNotesCheck = userActivities.getLastNotesCheck();
            return new UnseenInfo(getTypeName(), counter.countNewActivities(lastNotesCheck, user), lastNotesCheck);
        }
    },
    ORDERS {
        @Override
        public UnseenInfo getUnseen(final RecentActivitiesCounter counter, final BotUser user, final UserActivities userActivities) {
            Date lastOrdersCheck = userActivities.getLastOrdersCheck();
            return new UnseenInfo(getTypeName(), counter.countNewActivities(lastOrdersCheck, user), lastOrdersCheck);
        }
    },
    TARGETS {
        @Override
        public UnseenInfo getUnseen(final RecentActivitiesCounter counter, final BotUser user, final UserActivities userActivities) {
            Date lastTargetsCheck = userActivities.getLastTargetsCheck();
            return new UnseenInfo(getTypeName(), counter.countNewActivities(lastTargetsCheck, user), lastTargetsCheck);
        }
    },
    WAVE {
        @Override
        public UnseenInfo getUnseen(final RecentActivitiesCounter counter, final BotUser user, final UserActivities userActivities) {
            Date lastWaveCheck = userActivities.getLastWaveCheck();
            return new UnseenInfo(getTypeName(), counter.countNewActivities(lastWaveCheck, user), lastWaveCheck);
        }
    };

    public String getTypeName() {
        return StringUtil.prettifyEnumName(this);
    }

    public static RecentActivityType fromName(final String name) {
        for (RecentActivityType type : values()) {
            if (type.getTypeName().equalsIgnoreCase(name)) return type;
        }
        return null;
    }

    public abstract UnseenInfo getUnseen(final RecentActivitiesCounter counter,
                                         final BotUser user,
                                         final UserActivities userActivities);
}
