package ro.appenigne.web.framework.exception;

public class SilentNotification extends Exception {

    private static final long serialVersionUID = 1L;

    public SilentNotification() {
        super();
    }

    public SilentNotification(String message) {
        super(message);
    }

    public SilentNotification(Throwable cause) {
        super(cause);
    }

    public SilentNotification(String message, Throwable cause) {
        super(message, cause);
    }

}
