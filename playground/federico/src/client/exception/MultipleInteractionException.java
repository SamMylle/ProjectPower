package client.exception;

public class MultipleInteractionException extends Exception {

	private static final long serialVersionUID = 1L;

	public MultipleInteractionException() {	}

	public MultipleInteractionException(String message) {
		super(message);
	}

	public MultipleInteractionException(Throwable cause) {
		super(cause);
	}

	public MultipleInteractionException(String message, Throwable cause) {
		super(message, cause);
	}

	public MultipleInteractionException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
