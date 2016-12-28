package util;

import java.io.OutputStream;
import java.io.PrintStream;

public class SuppressSystemOut extends OutputStream{
	private PrintStream f_realSystemOut = System.out;
	private PrintStream f_realSystemErr = System.err;
    @Override
    public void write(int b){
         return;
    }
    @Override
    public void write(byte[] b){
         return;
    }
    @Override
    public void write(byte[] b, int off, int len){
         return;
    }
    public SuppressSystemOut(){
    }
    
    public void suppressOutput(){
    	System.setOut(new PrintStream(this));
    	System.setErr(new PrintStream(this));
    }
    
    public void activateOutput(){
	    System.setOut(f_realSystemOut);
	    System.setErr(f_realSystemErr);
    }
}