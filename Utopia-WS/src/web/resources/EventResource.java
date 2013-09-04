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
import api.tools.validation.ValidationEnabled;
import com.google.common.base.Supplier;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.EventDAO;
import database.models.AttendanceStatus;
import database.models.AttendanceType;
import database.models.Bindings;
import database.models.Event;
import events.EventAddedEvent;
import tools.BindingsManager;
import web.documentation.Documentation;
import web.models.RS_AttendanceStatus;
import web.models.RS_Event;
import web.tools.AfterCommitEventPoster;
import web.tools.BindingsParser;
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

import static api.tools.validation.ValidationUtil.validate;
import static com.google.common.base.Preconditions.checkNotNull;
import static web.tools.SecurityHandler.ADMIN_ROLE;

@ValidationEnabled
@Path("events")
public class EventResource {
    private final EventDAO eventDAO;
    private final Provider<BindingsParser> bindingsParserProvider;
    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<BindingsManager> bindingsManagerProvider;
    private final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public EventResource(final EventDAO eventDAO,
                         final Provider<BindingsParser> bindingsParserProvider,
                         final Provider<BotUserDAO> userDAOProvider,
                         final Provider<BindingsManager> bindingsManagerProvider,
                         final Provider<AfterCommitEventPoster> afterCommitEventPosterProvider,
                         final Provider<Validator> validatorProvider) {
        this.eventDAO = eventDAO;
        this.bindingsParserProvider = bindingsParserProvider;
        this.userDAOProvider = userDAOProvider;
        this.bindingsManagerProvider = bindingsManagerProvider;
        this.afterCommitEventPosterProvider = afterCommitEventPosterProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds the specified event, fires off an EventAddedEvent and returns the saved object. Admin only request")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Event addEvent(@Documentation(value = "The event to add", itemName = "newEvent")
                             @Valid final RS_Event newEvent,
                             @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Bindings bindings = bindingsParserProvider.get().parse(newEvent.getBindings());
        final Event event = new Event(Event.EventType.fromName(newEvent.getType()), newEvent.getDescription(), newEvent.getEventTime(), bindings);
        eventDAO.save(event);
        afterCommitEventPosterProvider.get().addEventToPost(new Supplier<Object>() {
            @Override
            public Object get() {
                return new EventAddedEvent(event.getId(), null);
            }
        });
        return RS_Event.fromEvent(event, true);
    }

    @Documentation("Returns the event with the specified id, provided the user is allowed to see it")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Event getEvent(@PathParam("id") final long id,
                             @Context final WebContext webContext) {
        Event event = eventDAO.getEvent(id);

        if (event == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        else if (!webContext.isInRole(ADMIN_ROLE) && !bindingsManagerProvider.get().matchesBindings(event.getBindings(), webContext.getBotUser()))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        return RS_Event.fromEvent(event, true);
    }

    @Documentation("Returns all events the current user is allowed to see, or just the ones for the specified user (admin only)")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Event>> getEvents(@Documentation("The id of the user to list events for")
                                               @QueryParam("userId")
                                               final Long userId,
                                               @Context final WebContext webContext) {
        List<RS_Event> events = new ArrayList<>();
        if (userId == null) {
            List<Event> eventsForUser = webContext.isInRole(ADMIN_ROLE) ? eventDAO.getAllEvents() :
                    eventDAO.getEventsForUser(webContext.getBotUser(), bindingsManagerProvider.get());
            for (Event event : eventsForUser) {
                events.add(RS_Event.fromEvent(event, true));
            }
        } else if (webContext.isInRole(ADMIN_ROLE)) {
            BotUser user = userDAOProvider.get().getUser(userId);
            checkNotNull(user, "There's no such user");
            for (Event event : eventDAO.getEventsForUser(user, bindingsManagerProvider.get())) {
                events.add(RS_Event.fromEvent(event, true));
            }
            Event wave = eventDAO.getWave();
            if (wave != null) events.add(RS_Event.fromEvent(wave, true));
        } else throw new WebApplicationException(Response.Status.FORBIDDEN);
        return JResponse.ok(events).build();
    }

    @Documentation("Updates the specified event and fires off a new EventAddedEvent, then returns the updated object. Admin only request")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Event updateEvent(@PathParam("id") final long id,
                                @Documentation(value = "The updated event", itemName = "updatedEvent")
                                final RS_Event updatedEvent,
                                @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        final Event event = eventDAO.getEvent(id);
        if (event == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedEvent).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        RS_Event.toEvent(event, updatedEvent);
        afterCommitEventPosterProvider.get().addEventToPost(new Supplier<Object>() {
            @Override
            public Object get() {
                return new EventAddedEvent(event.getId(), null);
            }
        });

        return RS_Event.fromEvent(event, true);
    }

    @Documentation("Deletes the specified event. Admin only request")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteEvent(@PathParam("id") final long id,
                            @Context final WebContext webContext) {
        if (!webContext.isInRole(ADMIN_ROLE)) throw new WebApplicationException(Response.Status.FORBIDDEN);

        Event event = eventDAO.getEvent(id);
        if (event == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        eventDAO.delete(event);
    }

    @Documentation("Sets attendance for an event and a specific user. Returns the set attendance status")
    @Path("{id : \\d+}/attendance")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_AttendanceStatus addAttendance(@PathParam("id") final long id,
                                             @Documentation(value = "The attendance status to set", itemName = "newAttendance")
                                             final RS_AttendanceStatus newAttendance) {
        Event event = eventDAO.getEvent(id);
        checkNotNull(event, "No such event");

        validate(newAttendance).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        BotUser user = userDAOProvider.get().getUser(newAttendance.getUser().getId());

        AttendanceType attendanceType = AttendanceType.fromName(newAttendance.getType());

        RS_AttendanceStatus reply = null;
        for (AttendanceStatus attendanceStatus : event.getAttendanceInformation()) {
            if (attendanceStatus.getUser().equals(user)) {
                attendanceStatus.setDetails(newAttendance.getDetails());
                attendanceStatus.setType(attendanceType);
                reply = RS_AttendanceStatus.fromAttendanceStatus(attendanceStatus);
                break;
            }
        }

        if (reply == null) {
            AttendanceStatus attendanceStatus = new AttendanceStatus(user, event, attendanceType, newAttendance.getDetails());
            event.getAttendanceInformation().add(attendanceStatus);
            reply = RS_AttendanceStatus.fromAttendanceStatus(attendanceStatus);
        }

        return reply;
    }
}
