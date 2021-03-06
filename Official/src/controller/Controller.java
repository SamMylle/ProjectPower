package controller;

import java.util.LinkedList;
import java.util.Vector;
import java.util.HashMap;

import util.Logger;

import avro.ProjectPower.ClientType;

public class Controller {
	public Controller(int startID, int maxTemperatures){
		f_nextID = startID;
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
	
	public ClientType getClType(int ID){
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
	
	@Deprecated
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
	
	public Vector<TemperatureRecord> getRawTemperatures(){
		return f_temperatures;
	}
	
	public double averageCurrentTemp(){
		/// In a non-distributed system i would throw an exception for an empty list or records
		double avg = 0;
		double count = 0;
		
		for (int i = 0; i < f_temperatures.size(); i++){
			LinkedList<Double> list = f_temperatures.elementAt(i).getRecord();
			if (list.size() >= 1){
				avg += list.getFirst();
				count++;
			}
		}
		
		if (count == 0){
			return 0;
		}
		return avg/count;
	}
	
	public boolean hasValidTemp(){
		for (int i = 0; i < f_temperatures.size(); i++){
			if (f_temperatures.elementAt(i).getRecord().size() > 0){
				return true;
			}
		}
		return false;
	}
	
	public Vector<Double> getTemperatureHistory(){
		/// The first temp is the most recent temp
		/// TODO test
		Vector<Double> retVal = new Vector<Double>();
		
		for (int j = 0; j < f_maxTemperatures; j++){
			double count = 0;
			double avg = 0;
			for (int i = 0; i < f_temperatures.size(); i++){
				LinkedList<Double> list = f_temperatures.elementAt(i).getRecord();
				if (list.size() >= j + 1){
					avg += list.get(j);
					count++;
				}
			}
			
			if (count == 0){
				return retVal;
			}else{
				avg /= count;
				retVal.add(new Double(avg));
			}
		}
		
		return retVal;
	}
	
	/// TODO get temperature history
	
	
	
	protected int f_nextID;
	protected final int f_maxTemperatures;
	public HashMap<Integer, ClientType> f_names;
	protected Vector<TemperatureRecord> f_temperatures;
}