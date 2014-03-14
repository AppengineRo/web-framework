package ro.appenigne.web.framework.exception;

public class RedirectException extends Exception {

    private static final long serialVersionUID = 1L;

    public RedirectException() {
        super();
    }

    public RedirectException(String message) {
        super(message);
    }

    public RedirectException(Throwable cause) {
        super(cause);
    }

    public RedirectException(String message, Throwable cause) {
        super(message, cause);
    }

}
