package ro.appenigne.web.framework.exception;

public class RepeatTaskException extends Exception {

    private static final long serialVersionUID = 1L;

    public RepeatTaskException() {
        super();
    }

    public RepeatTaskException(String message) {
        super(message);
    }

    public RepeatTaskException(Throwable cause) {
        super(cause);
    }

    public RepeatTaskException(String message, Throwable cause) {
        super(message, cause);
    }

}
