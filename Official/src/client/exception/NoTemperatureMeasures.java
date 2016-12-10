package client.exception;

public class NoTemperatureMeasures extends Exception {

	private static final long serialVersionUID = 1L;

	public NoTemperatureMeasures() { }

	public NoTemperatureMeasures(String arg0) {
		super(arg0);
	}

	public NoTemperatureMeasures(Throwable arg0) {
		super(arg0);
	}

	public NoTemperatureMeasures(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public NoTemperatureMeasures(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

}
