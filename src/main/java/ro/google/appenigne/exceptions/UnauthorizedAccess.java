package ro.google.appenigne.exceptions;

/**
 * @author Bogdan Nourescu
 * 
 */
public class UnauthorizedAccess extends Exception {
	
	private static final long	serialVersionUID	= 1L;
	
	public UnauthorizedAccess() {}
	
	/**
	 * @param message
	 */
	public UnauthorizedAccess(String message) {
		super(message);
	}
	
	/**
	 * @param cause
	 */
	public UnauthorizedAccess(Throwable cause) {
		super(cause);
	}
	
	/**
	 * @param message
	 * @param cause
	 */
	public UnauthorizedAccess(String message, Throwable cause) {
		super(message, cause);
	}
	
}
