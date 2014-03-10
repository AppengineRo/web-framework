package ro.google.appenigne.exceptions;

public class SilentNotification extends Exception {
	
	private static final long	serialVersionUID	= 1L;
	
	public SilentNotification() {}
	
	/**
	 * @param message
	 */
	public SilentNotification(String message) {
		super(message);
	}
	
	/**
	 * @param cause
	 */
	public SilentNotification(Throwable cause) {
		super(cause);
	}
	
	/**
	 * @param message
	 * @param cause
	 */
	public SilentNotification(String message, Throwable cause) {
		super(message, cause);
	}
	
}
