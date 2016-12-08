package controller;

import java.util.LinkedList;

public class TemperatureRecord{
	public TemperatureRecord(int maxSize, int ID){
		f_maxSize = maxSize;
		f_record = new LinkedList<Double>();
		f_ID = ID;
	}
	
	public LinkedList<Double> getRecord(){
		return f_record;
	}
	
	public void addValue(double value){
		if (f_record.size() >= f_maxSize){
			f_record.removeLast();
		}
		f_record.addFirst(new Double(value));
	}
	
	public int getID(){
		return f_ID;
	}
	
	public String toString(){
		String ret = "For ID " + new Integer(f_ID).toString();
		for (int i = 0; i < f_record.size(); i++){
			ret += " " + f_record.get(i);
		}
		return ret;
	}
	
	private int f_maxSize;
	private LinkedList<Double> f_record;
	private int f_ID;
}
