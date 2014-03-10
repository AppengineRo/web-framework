package ro.google.appenigne.exceptions;

/**
 * @author Bogdan Nourescu
 * 
 */
public class RedirectException extends Exception {

	private static final long	serialVersionUID	= 1L;

	public RedirectException() {}

	/**
	 * @param message
	 */
	public RedirectException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public RedirectException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RedirectException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
