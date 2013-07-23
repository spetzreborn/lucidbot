package tools.user_activities;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import javax.inject.Inject;

public class RecentActivitiesCounterProvider {
    private final Injector injector;

    @Inject
    public RecentActivitiesCounterProvider(final Injector injector) {
        this.injector = injector;
    }

    public RecentActivitiesCounter getCounter(final RecentActivityType activityType) {
        return injector.getInstance(Key.get(RecentActivitiesCounter.class, Names.named(activityType.getTypeName())));
    }
}
