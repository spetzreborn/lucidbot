package tools.user_activities;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class UserActivitiesToolsModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(RecentActivitiesCounter.class).annotatedWith(Names.named(RecentActivityType.AID.getTypeName())).to(AidActivitiesCounter.class);
        bind(RecentActivitiesCounter.class).annotatedWith(Names.named(RecentActivityType.BUILDS.getTypeName())).to(BuildsActivitiesCounter.class);
        bind(RecentActivitiesCounter.class).annotatedWith(Names.named(RecentActivityType.EVENTS.getTypeName())).to(EventsActivitiesCounter.class);
        bind(RecentActivitiesCounter.class).annotatedWith(Names.named(RecentActivityType.NAPS.getTypeName())).to(NapsActivitiesCounter.class);
        bind(RecentActivitiesCounter.class).annotatedWith(Names.named(RecentActivityType.NOTES.getTypeName())).to(NotesActivitiesCounter.class);
        bind(RecentActivitiesCounter.class).annotatedWith(Names.named(RecentActivityType.ORDERS.getTypeName())).to(OrdersActivitiesCounter.class);
        bind(RecentActivitiesCounter.class).annotatedWith(Names.named(RecentActivityType.TARGETS.getTypeName())).to(TargetsActivitiesCounter.class);
        bind(RecentActivitiesCounter.class).annotatedWith(Names.named(RecentActivityType.WAVE.getTypeName())).to(WaveActivitiesCounter.class);
    }
}
