package client.exception;

public class FridgeOccupiedException extends Exception {

	private static final long serialVersionUID = 1L;

	public FridgeOccupiedException() { }

	public FridgeOccupiedException(String message) {
		super(message);
	}

	public FridgeOccupiedException(Throwable cause) {
		super(cause);
	}

	public FridgeOccupiedException(String message, Throwable cause) {
		super(message, cause);
	}

	public FridgeOccupiedException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
