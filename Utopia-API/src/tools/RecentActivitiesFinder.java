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

package tools;

import api.database.models.BotUser;
import database.models.UserActivities;
import tools.user_activities.RecentActivitiesCounter;
import tools.user_activities.RecentActivitiesCounterProvider;
import tools.user_activities.RecentActivityType;
import tools.user_activities.UnseenInfo;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RecentActivitiesFinder {
    private final RecentActivitiesCounterProvider counterProvider;

    @Inject
    public RecentActivitiesFinder(final RecentActivitiesCounterProvider counterProvider) {
        this.counterProvider = counterProvider;
    }

    public List<UnseenInfo> mapUnseenActivities(final BotUser user, final UserActivities userActivities) {
        RecentActivityType[] allTypes = RecentActivityType.values();
        Collection<RecentActivityType> types = new ArrayList<>(allTypes.length);
        Collections.addAll(types, allTypes);
        return mapUnseenActivities(user, userActivities, types, true);
    }

    public List<UnseenInfo> mapUnseenActivities(final BotUser user, final UserActivities userActivities,
                                                final Collection<RecentActivityType> types, final boolean includeEmpty) {
        List<UnseenInfo> activities = new ArrayList<>(types.size());
        for (RecentActivityType type : types) {
            RecentActivitiesCounter counter = counterProvider.getCounter(type);
            UnseenInfo unseen = type.getUnseen(counter, user, userActivities);
            if (includeEmpty || unseen.getUnseen() > 0)
                activities.add(unseen);
        }
        return activities;
    }
}
