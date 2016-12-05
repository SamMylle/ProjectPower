/**
 * 
 */
package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import client.TemperatureSensor.generateTempTask;

import controller.DistController;

import util.Logger;

import avro.ProjectPower.ControllerComm;
import avro.ProjectPower.UserStatus;
import avro.ProjectPower.communicationTempSensor;
import avro.ProjectPower.communicationUser;


public class DistTemperatureSensor 
	extends TemperatureSensor 
	implements communicationTempSensor, Runnable {
	
	private int f_controllerPort;
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverReady;
	private Timer f_timer;

	
	public DistTemperatureSensor(double lowTempRange, double highTempRange, int controllerPort) {
		super(lowTempRange, highTempRange);
		
		f_controllerPort = controllerPort;
		f_serverReady = false;
		this.setupID();
		this.setupServer();
		
		f_timer = new Timer();
		f_timer.schedule(new sendTemperatureTask(this), 25, 1000);
		// TODO remove magic number, argument for time period, same for superclass
	}

	private void setupID() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.getID(TemperatureSensor.type));
			transceiver.close();
		}
		catch (IOException e) {
			System.err.println("IOException in constructor for DistTemperatureSensor (getID).");
			// System.exit(1);
		}
	}
	
	private void setupServer() {
		f_serverThread = new Thread(this);
		f_serverThread.start();
		
		while (f_serverReady == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void stopServer() {
		f_serverThread.interrupt();
		f_serverThread = null;
	}
	
	@Override
	public void run() {
		try {
			f_server = new SaslSocketServer(
					new SpecificResponder(communicationUser.class, this), new InetSocketAddress(this.getID()) );
			f_server.start();
		}
		catch (IOException e) {
			System.err.println("Failed to start the DistTemperatureSensor server.");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		f_serverReady = true;

		try {
			f_server.join();
		}
		catch (InterruptedException e) {
			f_server.close();
			Logger.getLogger().log("Closed the DistTemperatureSensor server.");
		}
	}

	public void sendTemperatureToController() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.addTemperature(this.getID(), this.getTemperature());
			transceiver.close();
		}
		catch (IOException e) {
			System.err.println("IOException in sentTemperatureToController for DistTemperatureSensor.");
		}
	}
	
	@Override
	public boolean isAlive() throws AvroRemoteException {
		return true;
	}

	
	class sendTemperatureTask extends TimerTask {
		private DistTemperatureSensor remoteSensor;
		
		
		public sendTemperatureTask(DistTemperatureSensor sensor) {
			remoteSensor = sensor;
		}
		
		public void run() {
			remoteSensor.sendTemperatureToController();
		}
	}
	
	public static void main(String[] args) {
		
		final int ControllerPort = 6789;
		DistController controller = new DistController(ControllerPort, 10);
		
		
		DistTemperatureSensor remoteSensor = new DistTemperatureSensor(19,22,ControllerPort);
		
		
		try {
			Logger logger = Logger.getLogger();
			logger.f_active = true;
			for (int i = 0; i < 10; i++) {
				logger.log(remoteSensor.toString());
				TimeUnit.SECONDS.sleep(1);
				logger.log("Average in controller: " + new Double(controller.averageCurrentTemperature()).toString());
			}
		} 
		catch (InterruptedException e) { } 
		catch (AvroRemoteException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

}