package controller;

import java.util.LinkedList;
import java.util.Vector;
import java.util.HashMap;

import avro.ProjectPower.ClientType;

public class Controller {
	public Controller(int maxTemperatures){
		f_nextID = 0;
		f_maxTemperatures = maxTemperatures;
		f_names = new HashMap<Integer, ClientType>();
		f_temperatures = new Vector<TemperatureRecord>();
	}
	
	public int giveNextID(ClientType type){
		f_names.put(f_nextID, type);
		int ID = f_nextID;
		f_nextID++;
		
		if (type == ClientType.TemperatureSensor){
			/// TODO test this
			f_temperatures.add(new TemperatureRecord(f_maxTemperatures, ID));
		}
		
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
			
			for (int i = 0; i < f_temperatures.size(); i++){
				if (f_temperatures.elementAt(i).getID() == ID){
					f_temperatures.remove(i);
				}
			}
		}catch(NullPointerException e){}
	}
	
	public boolean unusedID(int ID){
		return true;
	}
	
	public void addTemperature(double newTemp, int ID){
		for (int i = 0; i < f_temperatures.size(); i++){
			if (f_temperatures.elementAt(i).getID() == ID){
				f_temperatures.elementAt(i).addValue(newTemp);
				return;
			}
		}
	}
	
	public double averageCurrentTemp(){
		/// In a non-distributed system i would throw an exception for an empty list or records
		double avg = 0;
		double count = 0;
		
		for (int i = 0; i < f_temperatures.size(); i++){
			LinkedList<Double> list = f_temperatures.elementAt(i).getRecord();
			if (list.size() >= 1){
				avg += list.get(list.size() - 1);
				count++;
			}
		}
		
		if (count == 0){
			return 0;
		}
		return avg/count;
	}
	
	public boolean hasValidTemperatures(){		
		for (int i = 0; i < f_temperatures.size(); i++){
			if (f_temperatures.elementAt(i).getRecord().size() > 0){
				return true;
			}
		}
		return false;
	}
	
	
	
	private int f_nextID;
	private final int f_maxTemperatures;
	private HashMap<Integer, ClientType> f_names;
	private Vector<TemperatureRecord> f_temperatures;
}