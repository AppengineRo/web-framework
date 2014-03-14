package ro.appenigne.web.framework.exception;

public class InvalidAuthCookie extends Exception{
    private static final long	serialVersionUID	= 1L;

    public InvalidAuthCookie() {
        super();
    }

    public InvalidAuthCookie(String message) {
        super(message);
    }

    public InvalidAuthCookie(Throwable cause) {
        super(cause);
    }

    public InvalidAuthCookie(String message, Throwable cause) {
        super(message, cause);
    }
}
