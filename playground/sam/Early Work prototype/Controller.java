package controller;

import java.util.Queue;
import java.util.Vector;
import java.util.HashMap;

import util.ClientType;

public class Controller {
	public Controller(int maxTemperatures){
		f_nextID = 0;
		f_maxTemperatures = maxTemperatures;
		f_names = new HashMap<Integer, ClientType>();
	}
	
	public int giveNextID(ClientType type){
		f_names.put(f_nextID, type);
		int ID = f_nextID;
		f_nextID++;
		return ID;
	}
	
	public ClientType getClientType(int ID){
		try{
			return f_names.get(ID);
		}catch(NullPointerException e){
			return null;
		}
	}
	
	public void removeID(int ID){
		try{
			f_names.remove(ID);
		}catch(NullPointerException e){}
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
	private HashMap<Integer, ClientType> f_names;
	private Queue<Double> f_temperatures;
}