package util;

public class Logger {
	/// Instead of writing to the output medium yourself, use this logger
	/// That way you can write to e.g. a file instead of the terminal without changing a lot of code

	static private Logger f_logger;
	public boolean f_active;
	static private boolean f_initialized;
	
	private Logger(){}
	
	public static final Logger getLogger(){
		if(f_initialized){
			return f_logger;
		}else{
			f_logger = new Logger();
			f_logger.f_active = false;
			f_initialized = true;
			return f_logger;
		}
	}

	public void log(String arg){
		/// Writes your string to the terminal (at least for now)
		if (f_active){
			System.out.print(arg + "\n");
		}
	}

	public void log(String arg, boolean newLine){
		/// Writes your string to the terminal (at least for now)
		if (f_active){
			if (newLine){
				System.out.print(arg + "\n");
			}else{
				System.out.print(arg);
			}
		}
	}

}
