package listeners;

import api.database.SimpleTransactionTask;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.events.bot.UserLoginEvent;
import api.runtime.ThreadingManager;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Provider;
import database.daos.NotificationDAO;
import database.daos.WaitDAO;
import database.models.Notification;
import database.models.NotificationType;
import database.models.Wait;
import lombok.extern.log4j.Log4j;
import org.hibernate.HibernateException;
import spi.events.EventListener;
import tools.communication.NotificationDeliverer;

import javax.inject.Inject;
import java.util.List;

import static api.database.Transactions.inTransaction;

@Log4j
class UserLoginListener implements EventListener {
    private final Provider<BotUserDAO> botUserDAOProvider;
    private final Provider<WaitDAO> waitDAOProvider;
    private final Provider<NotificationDAO> notificationDAOProvider;
    private final Provider<NotificationDeliverer> delivererProvider;
    private final ThreadingManager threadingManager;

    @Inject
    UserLoginListener(final Provider<BotUserDAO> botUserDAOProvider,
                      final Provider<NotificationDAO> notificationDAOProvider,
                      final Provider<WaitDAO> waitDAOProvider,
                      final Provider<NotificationDeliverer> delivererProvider,
                      final ThreadingManager threadingManager) {
        this.botUserDAOProvider = botUserDAOProvider;
        this.notificationDAOProvider = notificationDAOProvider;
        this.waitDAOProvider = waitDAOProvider;
        this.delivererProvider = delivererProvider;
        this.threadingManager = threadingManager;
    }

    @Subscribe
    public void onUserLogin(final UserLoginEvent event) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                inTransaction(new SimpleTransactionTask() {
                    @Override
                    public void run(final DelayedEventPoster delayedEventBus) {
                        try {
                            BotUser user = botUserDAOProvider.get().getUser(event.getUserId());

                            NotificationDeliverer notificationDeliverer = delivererProvider.get();
                            WaitDAO waitDAO = waitDAOProvider.get();
                            List<Wait> list = waitDAO.getWaitingFor(user);
                            for (Wait wait : list) {
                                BotUser waitingUser = wait.getUser();
                                for (Notification notification : notificationDAOProvider.get().getNotifications(waitingUser,
                                        NotificationType.WAIT)) {
                                    notification.getMethod().deliver(notificationDeliverer, waitingUser, "User you waited for logged in",
                                            user.getMainNick() + " just logged in");
                                }
                            }
                            waitDAO.delete(list);
                        } catch (HibernateException e) {
                            UserLoginListener.log.error("", e);
                        }
                    }
                });
            }
        };
        threadingManager.execute(runnable);
    }
}
