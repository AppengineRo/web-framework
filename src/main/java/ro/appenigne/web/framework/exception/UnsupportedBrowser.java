package ro.appenigne.web.framework.exception;

/**
 * @author Bogdan Nourescu
 * 
 */
public class UnsupportedBrowser extends Exception {
	
	private static final long	serialVersionUID	= 1L;
	
	public UnsupportedBrowser() {}
	
	/**
	 * @param message
	 */
	public UnsupportedBrowser(String message) {
		super(message);
	}
	
	/**
	 * @param cause
	 */
	public UnsupportedBrowser(Throwable cause) {
		super(cause);
	}
	
	/**
	 * @param message
	 * @param cause
	 */
	public UnsupportedBrowser(String message, Throwable cause) {
		super(message, cause);
	}
	
}
