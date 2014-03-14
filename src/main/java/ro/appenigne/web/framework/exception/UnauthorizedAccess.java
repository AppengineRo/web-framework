package ro.appenigne.web.framework.exception;

public class UnauthorizedAccess extends Exception {

    private static final long serialVersionUID = 1L;

    public UnauthorizedAccess() {
        super();
    }

    public UnauthorizedAccess(String message) {
        super(message);
    }

    public UnauthorizedAccess(Throwable cause) {
        super(cause);
    }

    public UnauthorizedAccess(String message, Throwable cause) {
        super(message, cause);
    }

}
