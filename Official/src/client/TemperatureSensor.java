package client;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class TemperatureSensor {
	
	private int ID;
	private double temperature;
	private Timer timer;
	
	public TemperatureSensor(double lowTempRange, double highTempRange) {
		assert highTempRange > lowTempRange; //replace with normal check and possibly custom exception
		
		ID = -1;
		if (lowTempRange != highTempRange)
			temperature = lowTempRange + (Math.random() * (highTempRange - lowTempRange));
		else
			temperature = lowTempRange;
		
		timer = new Timer();
		timer.schedule(new generateTempTask(this), 1000, 1000);
	}
	
	public void setID(int _ID) {
		assert _ID >= 0; //replace with normal check and possibly custom exception
		ID = _ID;
	}
	
	public double generateTemperature() {
		assert ID >= 0;
		double randomValue = Math.random();
		
		this.temperature = this.temperature - 1 + (2 * randomValue);
		return this.temperature;
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
		assert ID >= 0;
		
		return "ID: " + ID + ", temperature: " + temperature;
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
