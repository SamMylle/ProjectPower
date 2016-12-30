package client;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import avro.ProjectPower.ClientType;


public class TemperatureSensor {
	
	private int f_ID;
	private double f_temperature;
	private Timer f_timer;
	protected Double[] f_measures;
	private int f_measureIndex;
	
	public static final ClientType type = ClientType.TemperatureSensor;
	
	public TemperatureSensor(double lowTempRange, double highTempRange) {
		assert highTempRange > lowTempRange; //replace with normal check and possibly custom exception
		
		f_ID = -1;
		if (lowTempRange != highTempRange)
			f_temperature = lowTempRange + (Math.random() * (highTempRange - lowTempRange));
		else
			f_temperature = lowTempRange;
		
		f_measures = new Double[20];		// ARBITRARY BUFFER SIZE
		for (int i = 0; i < 20; i++) {
			f_measures[i] = null;
		}
		
		f_measureIndex = 0;
		
		f_timer = new Timer();
		f_timer.schedule(new generateTempTask(this), 1000, 1000);
	}
	
	public void setID(int _ID) {
		assert _ID >= 0; //replace with normal check and possibly custom exception
		f_ID = _ID;
	}
	
	public int getID() {
		return f_ID;
	}
	
	public double getTemperature() {
		return f_temperature;
	}
	
	synchronized public List<Double> getHistory() {
		List<Double> returnList = new ArrayList<Double>();
		
		for (int i = 0; i < f_measures.length; i++) {
			Double value  = f_measures[(f_measureIndex - 1 - i) % f_measures.length];
			if (value != null) {
				returnList.add(new Double(value));
			}
		}
		
		return returnList;
	}
	
	public void generateTemperature() {
		assert f_ID >= 0;
		double randomValue = Math.random();
		
		this.f_measures[f_measureIndex % f_measures.length] = new Double(f_temperature - 1 + (2 * randomValue));
		f_measureIndex = f_measureIndex + 1;
		this.f_temperature = this.f_temperature - 1 + (2 * randomValue);
	}
	
	class generateTempTask extends TimerTask {
		private TemperatureSensor sensor;
		
		public generateTempTask(TemperatureSensor _sensor) {
			sensor = _sensor;
		}
		
		public void run() {
			sensor.generateTemperature();
		}
	}
	
	public String toString() {
		assert f_ID >= 0;
		
		return "ID: " + f_ID + ", Temperature: " + f_temperature;
	}
	
	
	
	public static void main(String[] args) throws InterruptedException {
		
		TemperatureSensor test = new TemperatureSensor(10,15);
		test.setID(1);
		
		for (int i = 0; i < 10; i++) {
			System.out.println(test);
			TimeUnit.SECONDS.sleep(1);
		}
		System.exit(0);

	}

}
