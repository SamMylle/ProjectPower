package client.exception;

public class AbsentException extends Exception {

	private static final long serialVersionUID = 1L;

	public AbsentException() { }

	public AbsentException(String arg0) {
		super(arg0);
	}

	public AbsentException(Throwable arg0) {
		super(arg0);
	}

	public AbsentException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public AbsentException(String arg0, Throwable arg1, boolean arg2,
			boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
