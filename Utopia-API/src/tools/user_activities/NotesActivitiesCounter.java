package tools.user_activities;

import api.database.models.BotUser;
import database.daos.NoteDAO;
import tools.BindingsManager;

import javax.inject.Inject;
import java.util.Date;

public class NotesActivitiesCounter implements RecentActivitiesCounter {
    private final NoteDAO noteDAO;
    private final BindingsManager bindingsManager;

    @Inject
    public NotesActivitiesCounter(final NoteDAO noteDAO, final BindingsManager bindingsManager) {
        this.noteDAO = noteDAO;
        this.bindingsManager = bindingsManager;
    }

    @Override
    public int countNewActivities(final Date lastCheck, final BotUser user) {
        return noteDAO.countNotesAddedForUserAfter(lastCheck, user, bindingsManager);
    }
}
