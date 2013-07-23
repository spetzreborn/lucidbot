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

package commands.news.handlers;

import api.commands.CommandHandlingException;
import api.commands.CommandResponse;
import api.database.DBException;
import api.events.DelayedEventPoster;
import api.runtime.IRCContext;
import api.tools.collections.Params;
import api.tools.text.StringUtil;
import commands.news.SummaryView;
import commands.news.TypeToActivities;
import database.daos.KingdomDAO;
import database.daos.NewsItemDAO;
import database.models.Kingdom;
import database.models.NewsItem;
import spi.commands.CommandHandler;
import spi.filters.Filter;
import tools.time.UtopiaTimeFactory;

import javax.inject.Inject;
import java.util.*;

public class SummaryCommandHandler implements CommandHandler {
    private final UtopiaTimeFactory utopiaTimeFactory;
    private final NewsItemDAO newsItemDAO;
    private final KingdomDAO kingdomDAO;

    @Inject
    public SummaryCommandHandler(final KingdomDAO kingdomDAO, final NewsItemDAO newsItemDAO, final UtopiaTimeFactory utopiaTimeFactory) {
        this.kingdomDAO = kingdomDAO;
        this.newsItemDAO = newsItemDAO;
        this.utopiaTimeFactory = utopiaTimeFactory;
    }

    @Override
    public CommandResponse handleCommand(IRCContext context, Params params, Collection<Filter<?>> filters,
                                         final DelayedEventPoster delayedEventPoster) throws CommandHandlingException {
        try {
            Date start =
                    params.containsKey("from") ? utopiaTimeFactory.newUtopiaTime(params.getParameter("from").substring(5)).getDate() : null;
            Date end = params.containsKey("to") ? utopiaTimeFactory.newUtopiaTime(params.getParameter("to").substring(3)).getDate() : null;

            List<NewsItem> news = new LinkedList<>(newsItemDAO.getNewsBetween(start, end));
            if (params.containsKey("kingdom"))
                removeNonMatchingItems(news, StringUtil.splitOnSpace(params.getParameter("kingdom")));
            if (news.isEmpty()) return CommandResponse.errorResponse("No news added that match the criteria");
            Collections.sort(news);
            NewsItem lastAddedNewsItem = news.get(news.size() - 1);

            Kingdom kingdom = kingdomDAO.getSelfKD();
            SummaryView view = SummaryView.constructFromNews(news, kingdom);
            if (params.containsKey("province")) {
                String province = params.getParameter("province").substring(4).trim();
                List<TypeToActivities> provinceStats = view.getProvinceStats(province);
                if (provinceStats == null)
                    return CommandResponse.errorResponse("No news including that province were found");
                int landExchange = 0;
                int hitsMade = 0;
                int hitsTaken = 0;
                for (TypeToActivities pair : provinceStats) {
                    landExchange += pair.getActivities().getLandTaken();
                    landExchange -= pair.getActivities().getLandLost();
                    hitsMade += pair.getActivities().getHitsMade();
                    hitsTaken += pair.getActivities().getHitsReceived();
                }
                return CommandResponse.resultResponse("stats", provinceStats, "landExchange", landExchange, "province", province,
                        "hitsMade", hitsMade, "hitsTaken", hitsTaken, "lastAddedNewsItem", lastAddedNewsItem);
            } else return CommandResponse.resultResponse("summary", view, "lastAddedNewsItem", lastAddedNewsItem);
        } catch (DBException e) {
            throw new CommandHandlingException(e);
        }
    }

    private static void removeNonMatchingItems(List<NewsItem> news, String... enemyKds) {
        Iterator<NewsItem> iterator = news.iterator();
        while (iterator.hasNext()) {
            NewsItem next = iterator.next();
            boolean removeItem = true;
            for (String kd : enemyKds) {
                if (next.getSource() != null && next.getSource().endsWith(kd) ||
                        next.getTarget() != null && next.getTarget().endsWith(kd)) {
                    removeItem = false;
                    break;
                }
            }
            if (removeItem) iterator.remove();
        }
    }
}
