package controller;

import java.util.Queue;
import java.util.Vector;
import java.util.Dictionary;

import util.ClientType;

public class Controller {
	public Controller(int maxTemperatures){
		f_nextID = 0;
		f_maxTemperatures = maxTemperatures;
	}
	
	public int giveNextID(ClientType type){
		int ID = f_nextID;
		f_nextID++;
		f_names.put(ID, type);
		return ID;
	}
	
	public boolean unusedID(int ID){
		return true;
	}
	
	public void addTemperature(double newTemp){
		Double toInsert = new Double(newTemp);
		if (f_temperatures.size() > f_maxTemperatures){
			f_temperatures.remove();
		}
		f_temperatures.add(toInsert);
	}
	
	public double averageTemperature(Vector<Double> temperatures){
		double average = 0;
		if (temperatures.size() > 0){
			for (int i = 0; i < temperatures.size(); i++){
				average += temperatures.elementAt(i);
			}
			average /= temperatures.size();
		}
		return average;
	}
	
	private int f_nextID;
	private final int f_maxTemperatures;
	private Dictionary<Integer, ClientType> f_names;
	private Queue<Double> f_temperatures;
}