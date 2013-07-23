package api.tools.communication;

public class MailException extends Exception {
    public MailException(final String message) {
        super(message);
    }

    public MailException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MailException(final Throwable cause) {
        super(cause);
    }
}
