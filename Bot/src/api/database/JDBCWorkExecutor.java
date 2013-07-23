package api.database;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;

import javax.inject.Inject;
import javax.inject.Provider;

public class JDBCWorkExecutor {
    private final Provider<Session> sessionProvider;

    @Inject
    public JDBCWorkExecutor(final Provider<Session> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    @Transactional
    public void workWithJDBCConnection(final Work work) {
        try {
            sessionProvider.get().doWork(work);
        } catch (HibernateException e) {
            throw new DBException(e);
        }
    }

    @Transactional
    public <T> T workWithJDBCConnection(final ReturningWork<T> work) {
        try {
            return sessionProvider.get().doReturningWork(work);
        } catch (HibernateException e) {
            throw new DBException(e);
        }
    }
}
