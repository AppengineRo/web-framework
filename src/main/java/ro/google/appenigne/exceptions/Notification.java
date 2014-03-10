package ro.google.appenigne.exceptions;

/**
 * @author Bogdan Nourescu
 * 
 */
public class Notification extends Exception {
	
	private static final long	serialVersionUID	= 1L;
	
	public Notification() {}
	
	/**
	 * @param message
	 */
	public Notification(String message) {
		super(message);
	}
	
	/**
	 * @param cause
	 */
	public Notification(Throwable cause) {
		super(cause);
	}
	
	/**
	 * @param message
	 * @param cause
	 */
	public Notification(String message, Throwable cause) {
		super(message, cause);
	}
	
}
