/*
 * Copyright (c) 2012, Fredrik Yttergren
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name LucidBot nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Fredrik Yttergren BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package commands.management.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import database.daos.OrderCategoryDAO;
import database.daos.OrderDAO;
import database.daos.UserActivitiesDAO;
import database.models.Bindings;
import database.models.Order;
import database.models.OrderCategory;
import events.OrderAddedEvent;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.Collection;

public class AddOrderCommandHandler implements CommandHandler {
    private final BindingsManager bindingsManager;
    private final OrderCategoryDAO orderCategoryDAO;
    private final OrderDAO orderDAO;
    private final UserActivitiesDAO userActivitiesDAO;

    @Inject
    public AddOrderCommandHandler(final OrderDAO orderDAO, final OrderCategoryDAO orderCategoryDAO, final BindingsManager bindingsManager,
                                  final UserActivitiesDAO userActivitiesDAO) {
        this.orderDAO = orderDAO;
        this.orderCategoryDAO = orderCategoryDAO;
        this.bindingsManager = bindingsManager;
        this.userActivitiesDAO = userActivitiesDAO;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Bindings bindings = bindingsManager.parseBindings(params.getParameter("bindings"));
            String categoryName = null;
            if (params.containsKey("category")) {
                categoryName = params.getParameter("category").substring(1);
                categoryName = categoryName.substring(0, categoryName.length() - 1).trim();
            }
            OrderCategory category = categoryName == null ? null : orderCategoryDAO.getOrderCategory(categoryName);
            Order order = new Order(bindings, category, params.getParameter("order"), context.getUser().getMainNick());
            order = orderDAO.save(order);
            userActivitiesDAO.getUserActivities(context.getBotUser()).setLastOrdersCheck(order.getAdded());
            delayedEventPoster.enqueue(new OrderAddedEvent(order.getId(), context));
            return CommandResponse.resultResponse("order", order);
        } catch (final DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
