package controller;

import java.util.LinkedList;
import java.util.Queue;

public class TemperatureRecord {
	public TemperatureRecord(int maxSize){
		f_maxSize = maxSize;
		f_record = new LinkedList<Double>();
	}
	
	public Queue<Double> getRecord(){
		return f_record;
	}
	
	public void addValue(double value){
		if (f_record.size() >= f_maxSize){
			f_record.remove();
		}
		f_record.add(new Double(value));
	}
	
	private int f_maxSize;
	private Queue<Double> f_record;
}
