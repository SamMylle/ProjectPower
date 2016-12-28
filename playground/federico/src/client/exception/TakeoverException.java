package client.exception;

public class TakeoverException extends Exception {

	private static final long serialVersionUID = 1L;

	public TakeoverException() {}

	public TakeoverException(String arg0) {
		super(arg0);
	}

	public TakeoverException(Throwable arg0) {
		super(arg0);
	}

	public TakeoverException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public TakeoverException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
