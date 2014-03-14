package ro.appenigne.web.framework.exception;

public class Notification extends Exception {

    private static final long serialVersionUID = 1L;

    public Notification() {
        super();
    }

    public Notification(String message) {
        super(message);
    }

    public Notification(Throwable cause) {
        super(cause);
    }

    public Notification(String message, Throwable cause) {
        super(message, cause);
    }

}
