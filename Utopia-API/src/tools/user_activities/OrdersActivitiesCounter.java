package tools.user_activities;

import api.database.models.BotUser;
import database.daos.OrderDAO;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.Date;

public class OrdersActivitiesCounter implements RecentActivitiesCounter {
    private final OrderDAO orderDAO;
    private final BindingsManager bindingsManager;

    @Inject
    public OrdersActivitiesCounter(final OrderDAO orderDAO, final BindingsManager bindingsManager) {
        this.orderDAO = orderDAO;
        this.bindingsManager = bindingsManager;
    }

    @Override
    public int countNewActivities(final Date lastCheck, final BotUser user) {
        return orderDAO.countOrdersForUserAddedAfter(lastCheck, user, bindingsManager);
    }
}
