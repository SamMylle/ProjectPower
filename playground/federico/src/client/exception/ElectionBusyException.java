package client.exception;

public class ElectionBusyException extends Exception {

	private static final long serialVersionUID = 1L;

	public ElectionBusyException() {
	}

	public ElectionBusyException(String message) {
		super(message);
	}

	public ElectionBusyException(Throwable cause) {
		super(cause);
	}

	public ElectionBusyException(String message, Throwable cause) {
		super(message, cause);
	}

	public ElectionBusyException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
