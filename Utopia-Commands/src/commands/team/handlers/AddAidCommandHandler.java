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

package commands.team.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.timers.Timer;
import api.timers.TimerManager;
import api.tools.collections.Params;
import api.tools.numbers.NumberUtil;
import database.daos.AidDAO;
import database.daos.ProvinceDAO;
import database.models.Aid;
import database.models.AidImportanceType;
import database.models.AidType;
import database.models.Province;
import events.AidAddedEvent;
import listeners.AidManager;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class AddAidCommandHandler implements CommandHandler {
    private final BotUserDAO userDAO;
    private final ProvinceDAO provinceDAO;
    private final AidDAO aidDAO;
    private final AidManager aidManager;
    private final TimerManager timerManager;
    private final BindingsManager bindingsManager;

    @Inject
    public AddAidCommandHandler(final BotUserDAO userDAO, final ProvinceDAO provinceDAO, final AidDAO aidDAO, final AidManager aidManager,
                                final TimerManager timerManager, final BindingsManager bindingsManager) {
        this.aidDAO = aidDAO;
        this.aidManager = aidManager;
        this.userDAO = userDAO;
        this.provinceDAO = provinceDAO;
        this.timerManager = timerManager;
        this.bindingsManager = bindingsManager;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            BotUser user = params.getParameter("user") == null ? context.getBotUser() : userDAO.getUser(params.getParameter("user"));
            if (user == null) return CommandResponse.errorResponse("User not found");
            Province province = provinceDAO.getProvinceForUser(user);
            if (province == null) return CommandResponse.errorResponse("Can only add aid for users with provinces");
            int amount = (int) NumberUtil.parseDoubleWithK(params.getParameter("amount"));
            AidType type = AidType.fromName(params.getParameter("type"));
            AidImportanceType importance = params.getParameter("importance") == null ? AidImportanceType.MEDIUM_PRIORITY_REQUEST
                    : AidImportanceType
                    .fromName(params.getParameter("importance"));
            Date expiry = null;
            if (params.containsKey("bindings")) expiry = bindingsManager.parseBindings(params.getParameter("bindings")).getExpiryDate();

            Aid aid = aidDAO.getAid(province, type);
            if (aid == null) aid = new Aid(province, type, importance, amount, expiry);
            else {
                aid.setAdded(new Date());
                aid.setImportanceType(importance);
                aid.setAmount(amount);
                aid.setExpiryDate(expiry);
            }
            aidDAO.save(aid);

            if (aid.getExpiryDate() != null) {
                long delay = aid.getExpiryDate().getTime() - System.currentTimeMillis();
                if (delay <= 0) throw new IllegalArgumentException("Expiry must be in the future, not the past");
                timerManager.schedule(new Timer(Aid.class, aid.getId(), aidManager), delay, TimeUnit.MILLISECONDS);
            }
            delayedEventPoster.enqueue(new AidAddedEvent(aid.getId(), context));
            return CommandResponse.resultResponse("aid", aid, "isRequest", aid.getImportanceType() != AidImportanceType.OFFERING_AID);
        } catch (IllegalArgumentException | DBException e) {
            throw new CommandHandlingException(e);
        }
    }
}
