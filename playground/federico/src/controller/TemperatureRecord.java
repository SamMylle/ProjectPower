package controller;

import java.util.LinkedList;

public class TemperatureRecord {
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
			f_record.remove();
		}
		f_record.add(new Double(value));
	}
	
	public int getID(){
		return f_ID;
	}
	
	private int f_maxSize;
	private LinkedList<Double> f_record;
	private int f_ID;
}
