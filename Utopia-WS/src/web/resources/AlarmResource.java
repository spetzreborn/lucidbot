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

package web.resources;

import api.database.Transactional;
import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.timers.Timer;
import api.timers.TimerManager;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.AlarmDAO;
import database.models.Aid;
import database.models.Alarm;
import listeners.AlarmManager;
import web.documentation.Documentation;
import web.models.RS_Alarm;
import web.tools.WebContext;
import web.validation.Update;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static api.tools.validation.ValidationUtil.validate;
import static com.google.common.base.Preconditions.checkNotNull;
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("alarms")
public class AlarmResource {
    private final AlarmDAO alarmDAO;
    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<TimerManager> timerManagerProvider;
    private final Provider<AlarmManager> alarmManagerProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public AlarmResource(final AlarmDAO alarmDAO,
                         final Provider<BotUserDAO> userDAOProvider,
                         final Provider<TimerManager> timerManagerProvider,
                         final Provider<AlarmManager> alarmManagerProvider,
                         final Provider<Validator> validatorProvider) {
        this.alarmDAO = alarmDAO;
        this.userDAOProvider = userDAOProvider;
        this.timerManagerProvider = timerManagerProvider;
        this.alarmManagerProvider = alarmManagerProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds an alarm and the appropriate timer for it. Returns the saved alarm. You can only add alarms for yourself unless you're admin")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Alarm addAlarm(@Documentation(value = "The alarm to add", itemName = "newAlarm")
                             @Valid final RS_Alarm newAlarm,
                             @Context final WebContext webContext) {
        if (nonAdminAccessingAlarmForOtherUser(newAlarm.getUser().getId(), webContext)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        BotUser user = userDAOProvider.get().getUser(newAlarm.getUser().getId());

        Alarm alarm = new Alarm(newAlarm.getExpires(), newAlarm.getMessage(), user);
        alarm = alarmManagerProvider.get().saveAlarm(alarm); //This makes sure a timer is set too

        return RS_Alarm.fromAlarm(alarm);
    }

    @Documentation("Returns the alarm with the specified id, provided it's your own or you're an admin")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Alarm getAlarm(@PathParam("id") final long id,
                             @Context final WebContext webContext) {
        Alarm alarm = alarmDAO.getAlarm(id);

        if (alarm == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        else if (nonAdminAccessingAlarmForOtherUser(alarm.getUser().getId(), webContext))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        return RS_Alarm.fromAlarm(alarm);
    }

    @Documentation("Returns all existing alarms, or just the ones for the specified user")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Alarm>> getAlarms(@Documentation("The id of a user you want to get the alarms for. Optional")
                                               @QueryParam("userId") final Long userId) {
        List<RS_Alarm> alarms = new ArrayList<>();
        if (userId == null) {
            for (Alarm alarm : alarmDAO.getAllAlarms()) {
                alarms.add(RS_Alarm.fromAlarm(alarm));
            }
        } else {
            BotUser user = userDAOProvider.get().getUser(userId);
            checkNotNull(user, "There's no such user");

            for (Alarm alarm : alarmDAO.getAllUsersAlarm(user)) {
                alarms.add(RS_Alarm.fromAlarm(alarm));
            }
        }
        return JResponse.ok(alarms).build();
    }

    @Documentation("Updates the alarm with the specified id and returns the updated alarm. Updates the timer if necessary. You can only update your own " +
            "alarms, unless you're an admin")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Alarm updateAlarm(@PathParam("id") final long id,
                                @Documentation(value = "The updated alarm", itemName = "updatedAlarm")
                                final RS_Alarm updatedAlarm,
                                @Context final WebContext webContext) {
        validate(updatedAlarm).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        Alarm alarm = alarmDAO.getAlarm(id);

        if (alarm == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        if (nonAdminAccessingAlarmForOtherUser(alarm.getUser().getId(), webContext))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        if (!Objects.equals(alarm.getAlarmTime(), updatedAlarm.getExpires())) {
            timerManagerProvider.get().cancelTimer(Alarm.class, alarm.getId());
            if (updatedAlarm.getExpires() != null) {
                long delay = updatedAlarm.getExpires().getTime() - System.currentTimeMillis();
                timerManagerProvider.get().schedule(new Timer(Aid.class, alarm.getId(), alarmManagerProvider.get()), delay, TimeUnit.MILLISECONDS);
            }
        }

        RS_Alarm.toAlarm(alarm, updatedAlarm);
        return RS_Alarm.fromAlarm(alarm);
    }

    @Documentation("Deletes the specified alarm and removes its timer, provided it's your own alarm or you're an admin")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteAlarm(@PathParam("id") final long id,
                            @Context final WebContext webContext) {
        Alarm alarm = alarmDAO.getAlarm(id);
        if (alarm == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        if (nonAdminAccessingAlarmForOtherUser(alarm.getUser().getId(), webContext))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        alarmDAO.delete(alarm);
        timerManagerProvider.get().cancelTimer(Alarm.class, alarm.getId());
    }

    private static boolean nonAdminAccessingAlarmForOtherUser(final Long alarmUserId, final WebContext webContext) {
        return !webContext.getBotUser().getId().equals(alarmUserId) &&
                !webContext.isInRole(ADMIN_ROLE);
    }
}
