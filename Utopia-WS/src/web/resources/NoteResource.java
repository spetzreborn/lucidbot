package web.resources;

import api.database.daos.BotUserDAO;
import api.database.models.BotUser;
import api.database.transactions.Transactional;
import api.tools.validation.ValidationEnabled;
import com.google.inject.Provider;
import com.sun.jersey.api.JResponse;
import database.daos.NoteDAO;
import database.models.Bindings;
import database.models.Note;
import tools.BindingsManager;
import web.documentation.Documentation;
import web.models.RS_Note;
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
@Path("notes")
public class NoteResource {
    private final NoteDAO noteDAO;
    private final Provider<BotUserDAO> userDAOProvider;
    private final Provider<BindingsParser> bindingsParserProvider;
    private final Provider<BindingsManager> bindingsManagerProvider;
    private final Provider<Validator> validatorProvider;

    @Inject
    public NoteResource(final NoteDAO noteDAO,
                        final Provider<BotUserDAO> userDAOProvider,
                        final Provider<BindingsParser> bindingsParserProvider,
                        final Provider<BindingsManager> bindingsManagerProvider,
                        final Provider<Validator> validatorProvider) {
        this.noteDAO = noteDAO;
        this.userDAOProvider = userDAOProvider;
        this.bindingsParserProvider = bindingsParserProvider;
        this.bindingsManagerProvider = bindingsManagerProvider;
        this.validatorProvider = validatorProvider;
    }

    @Documentation("Adds a new note and returns the saved object")
    @POST
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Note addNote(@Documentation(value = "The new note to add", itemName = "newNote")
                           @Valid final RS_Note newNote,
                           @Context final WebContext webContext) {
        Bindings bindings = bindingsParserProvider.get().parse(newNote.getBindings());

        Note note = new Note(webContext.getName(), newNote.getMessage(), bindings);
        note = noteDAO.save(note);
        return RS_Note.fromNote(note);
    }

    @Documentation("Returns the note with the specified id, provided the user has access to it")
    @Path("{id : \\d+}")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Note getNote(@PathParam("id") final long id,
                           @Context final WebContext webContext) {
        Note note = noteDAO.getNote(id);

        if (note == null) throw new WebApplicationException(Response.Status.NOT_FOUND);
        else if (!bindingsManagerProvider.get().matchesBindings(note.getBindings(), webContext.getBotUser()))
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        return RS_Note.fromNote(note);
    }

    @Documentation("Returns all notes the current user can see, or optionally just the ones bound to the specified user (admin only)")
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public JResponse<List<RS_Note>> getNotes(@Documentation("The id of the user to limit the response to")
                                             @QueryParam("userId")
                                             final Long userId,
                                             @Context final WebContext webContext) {
        List<RS_Note> notes = new ArrayList<>();
        if (userId == null) {
            List<Note> allNotesForUser = webContext.isInRole(ADMIN_ROLE) ? noteDAO.getAllNotes() :
                    noteDAO.getAllNotesForUser(webContext.getBotUser(), bindingsManagerProvider.get());
            for (Note note : allNotesForUser) {
                notes.add(RS_Note.fromNote(note));
            }
        } else if (webContext.isInRole(ADMIN_ROLE)) {
            BotUser user = userDAOProvider.get().getUser(userId);
            checkNotNull(user, "There's no such user");
            for (Note note : noteDAO.getAllNotesForUser(user, bindingsManagerProvider.get())) {
                notes.add(RS_Note.fromNote(note));
            }
        } else throw new WebApplicationException(Response.Status.FORBIDDEN);
        return JResponse.ok(notes).build();
    }

    @Documentation("Updates the specified note and returns the updated object. Admins can edit any note, regular users can only edit their own")
    @Path("{id : \\d+}")
    @PUT
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Transactional
    public RS_Note updateNote(@PathParam("id") final long id,
                              @Documentation(value = "The updated note", itemName = "updatedNote")
                              final RS_Note updatedNote,
                              @Context final WebContext webContext) {
        Note note = noteDAO.getNote(id);
        if (note == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        validate(updatedNote).using(validatorProvider.get()).forGroups(Update.class).throwOnFailedValidation();

        BotUser user = webContext.getBotUser();
        if (!note.getAddedBy().equals(user) && !user.isAdmin())
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        note.setMessage(updatedNote.getMessage());
        return RS_Note.fromNote(note);
    }

    @Documentation("Deletes the specified note. Admins can remove any note, but regular users can only remove their own")
    @Path("{id : \\d+}")
    @DELETE
    @Transactional
    public void deleteNote(@PathParam("id") final long id,
                           @Context final WebContext webContext) {
        Note note = noteDAO.getNote(id);
        if (note == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

        BotUser user = webContext.getBotUser();
        if (!note.getAddedBy().equals(user) && !user.isAdmin())
            throw new WebApplicationException(Response.Status.FORBIDDEN);

        noteDAO.delete(note);
    }
}
