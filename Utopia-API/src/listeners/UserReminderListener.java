package listeners;

import api.database.SimpleTransactionTask;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.events.bot.NonCommandEvent;
import api.irc.communication.IRCAccess;
import api.irc.communication.IRCMessageType;
import api.irc.entities.IRCUser;
import api.runtime.IRCContext;
import api.settings.PropertiesCollection;
import api.templates.TemplateManager;
import api.tools.collections.MapFactory;
import com.google.common.eventbus.Subscribe;
import database.daos.UserActivitiesDAO;
import database.models.UserActivities;
import lombok.extern.log4j.Log4j;
import spi.events.EventListener;
import tools.RecentActivitiesFinder;
import tools.UtopiaPropertiesConfig;
import tools.user_activities.RecentActivityType;
import tools.user_activities.UnseenInfo;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static api.database.Transactions.inTransaction;
import static tools.UtopiaPropertiesConfig.REMINDER_INTERVAL;

@Log4j
public class UserReminderListener implements EventListener {
    private final UserActivitiesDAO userActivitiesDAO;
    private final RecentActivitiesFinder recentActivitiesFinder;
    private final TemplateManager templateManager;
    private final IRCAccess ircAccess;
    private final long reminderInterval;

    private final Collection<RecentActivityType> activityTypes = new HashSet<>();
    private final ConcurrentMap<String, Long> earliestNextReminderMap = new ConcurrentHashMap<>();

    @Inject
    public UserReminderListener(final UserActivitiesDAO userActivitiesDAO, final RecentActivitiesFinder recentActivitiesFinder,
                                final TemplateManager templateManager, final IRCAccess ircAccess,
                                @Named(REMINDER_INTERVAL) final long reminderInterval) {
        this.userActivitiesDAO = userActivitiesDAO;
        this.recentActivitiesFinder = recentActivitiesFinder;
        this.templateManager = templateManager;
        this.ircAccess = ircAccess;
        this.reminderInterval = reminderInterval * 1000;
    }

    @Inject
    public void init(final PropertiesCollection properties) {
        for (String reminder : properties.getList(UtopiaPropertiesConfig.ACTIVE_REMINDERS, ",")) {
            RecentActivityType activityType = RecentActivityType.fromName(reminder.trim());
            if (activityType != null) activityTypes.add(activityType);
        }
    }

    @Subscribe
    public void onNonCommandEvent(final NonCommandEvent event) {
        final IRCContext context = event.getContext();
        final IRCUser ircUser = context.getUser();
        if (ircUser.isAuthenticated()) {
            final long currentTime = System.currentTimeMillis();
            earliestNextReminderMap.putIfAbsent(ircUser.getCurrentNick(), currentTime);
            if (earliestNextReminderMap.get(ircUser.getCurrentNick()) > currentTime)
                return;

            inTransaction(new SimpleTransactionTask() {
                @Override
                public void run(final DelayedEventPoster delayedEventBus) {
                    BotUser user = context.getBotUser();
                    UserActivities userActivities = userActivitiesDAO.getUserActivities(user);

                    List<UnseenInfo> infoList = recentActivitiesFinder.mapUnseenActivities(user, userActivities, activityTypes, false);
                    if (infoList.isEmpty()) return;

                    Map<String, Object> map = MapFactory.newMapWithNamedObjects("unseenInfoOfInterest", infoList);
                    try {
                        String newsBar = templateManager.processTemplate(map, "news_bar.ftl");
                        if (context.getInputType() == IRCMessageType.PRIVATE_MESSAGE) {
                            ircAccess.sendPrivateMessage(context.getReceiver(), ircUser, "Updates: " + newsBar);
                        } else {
                            ircAccess.sendNotice(ircUser, "Updates: " + newsBar);
                        }
                    } catch (final Exception e) {
                        UserReminderListener.log.warn("Could not print the news bar", e);
                    }
                    earliestNextReminderMap.put(ircUser.getCurrentNick(), currentTime + reminderInterval);
                }
            });
        }
    }
}
