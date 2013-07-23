package internal.irc;

import api.runtime.ThreadingManager;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

import static api.settings.PropertiesConfig.AUTO_CONNECT_DELAY;

@Singleton
@ThreadSafe
@ParametersAreNonnullByDefault
public class ReconnectScheduler {
    private final ThreadingManager threadingManager;
    private final int reconnectDelay;

    @GuardedBy("this")
    private long nextAvailableReconnectTime;

    @Inject
    public ReconnectScheduler(final ThreadingManager threadingManager, @Named(AUTO_CONNECT_DELAY) final int reconnectDelay) {
        this.threadingManager = threadingManager;
        this.reconnectDelay = reconnectDelay;
    }

    public synchronized void scheduleReconnectAttempt(final Runnable reconnectAction) {
        long currentTime = System.currentTimeMillis();
        long delay;

        synchronized (this) {
            nextAvailableReconnectTime = Math.max(nextAvailableReconnectTime, currentTime);
            delay = nextAvailableReconnectTime - currentTime;
            nextAvailableReconnectTime += reconnectDelay;
        }

        threadingManager.schedule(reconnectAction, delay, TimeUnit.SECONDS);
    }
}
