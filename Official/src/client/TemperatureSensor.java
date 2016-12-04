package client;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import avro.ProjectPower.ClientType;


public class TemperatureSensor {
	
	private int f_ID;
	private double f_temperature;
	private Timer f_timer;
	public static final ClientType type = ClientType.TemperatureSensor;
	
	public TemperatureSensor(double lowTempRange, double highTempRange) {
		assert highTempRange > lowTempRange; //replace with normal check and possibly custom exception
		
		f_ID = -1;
		if (lowTempRange != highTempRange)
			f_temperature = lowTempRange + (Math.random() * (highTempRange - lowTempRange));
		else
			f_temperature = lowTempRange;
		
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
	
	public double generateTemperature() {
		assert f_ID >= 0;
		double randomValue = Math.random();
		
		this.f_temperature = this.f_temperature - 1 + (2 * randomValue);
		return this.f_temperature;
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
