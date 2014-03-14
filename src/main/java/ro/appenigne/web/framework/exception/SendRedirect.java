package ro.appenigne.web.framework.exception;

public class SendRedirect extends Exception {

    public SendRedirect() {
        super();
    }

    public SendRedirect(String message) {
        super(message);
    }

    public SendRedirect(Throwable cause) {
        super(cause);
    }

    public SendRedirect(String message, Throwable cause) {
        super(message, cause);
    }
}
