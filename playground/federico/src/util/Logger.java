package util;


public class Logger {
	/// Instead of writing to the output medium yourself, use this logger
	/// That way you can write to e.g. a file instead of the terminal without changing a lot of code

	static private Logger logger;
	static private boolean initialized;
	
	private Logger(){}
	
	public static final Logger getLogger(){
		if(initialized){
			return logger;
		}else{
			logger = new Logger();
			initialized = true;
			return logger;
		}
	}

	public void log(String arg){
		/// Writes your string to the terminal (at least for now)
		System.out.print(arg + "\n");
	}

	public void log(String arg, boolean newLine){
		/// Writes your string to the terminal (at least for now)
		if (newLine){
			System.out.print(arg + "\n");
		}else{
			System.out.print(arg);
		}
	}

}

