package ro.appenigne.web.framework.exception;

public class UnsupportedBrowser extends Exception {

    private static final long serialVersionUID = 1L;

    public UnsupportedBrowser() {
        super();
    }

    public UnsupportedBrowser(String message) {
        super(message);
    }

    public UnsupportedBrowser(Throwable cause) {
        super(cause);
    }

    public UnsupportedBrowser(String message, Throwable cause) {
        super(message, cause);
    }

}
