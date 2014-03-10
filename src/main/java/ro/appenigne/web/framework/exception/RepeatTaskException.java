package ro.appenigne.web.framework.exception;

/**
 * @author Bogdan Nourescu
 * 
 */
public class RepeatTaskException extends Exception {

	private static final long	serialVersionUID	= 1L;

	public RepeatTaskException() {}

	/**
	 * @param message
	 */
	public RepeatTaskException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public RepeatTaskException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RepeatTaskException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
