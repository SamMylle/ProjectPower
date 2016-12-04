package client.exception;

public class NoFridgeConnectionException extends Exception {

	private static final long serialVersionUID = 1L;

	public NoFridgeConnectionException() { }

	public NoFridgeConnectionException(String message) {
		super(message);
	}

	public NoFridgeConnectionException(Throwable cause) {
		super(cause);
	}

	public NoFridgeConnectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoFridgeConnectionException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
