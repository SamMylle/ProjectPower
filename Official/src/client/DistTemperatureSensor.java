/**
 * 
 */
package client;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import client.util.ConnectionData;
import controller.DistController;
import util.Logger;
import avro.ProjectPower.ClientType;
import avro.ProjectPower.ControllerComm;
import avro.ProjectPower.communicationTempSensor;
import avro.ProjectPower.communicationUser;


public class DistTemperatureSensor 
	extends TemperatureSensor 
	implements communicationTempSensor, Runnable {
	
	private ConnectionData f_controllerCommunication;
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverReady;
	private Timer f_timer;
	private String f_ownIP;

	
	public DistTemperatureSensor(double lowTempRange, double highTempRange, String ownIP, String controllerIP, int controllerPort) {
		super(lowTempRange, highTempRange);
		
		// TODO check IP arguments to be valid
		
		f_controllerCommunication = new ConnectionData(controllerIP, controllerPort);
		f_serverReady = false;
		f_ownIP = ownIP;
		this.setupID();
		this.setupServer();
		
		f_timer = new Timer();
		f_timer.schedule(new sendTemperatureTask(this), 25, 1000);
		// TODO remove magic number, argument for time period, same for superclass
	}

	private void setupID() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerCommunication.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.LogOn(TemperatureSensor.type, f_ownIP));
			transceiver.close();
		}
		catch (IOException e) {
			// TODO handle exception here
			System.err.println("IOException in constructor for DistTemperatureSensor (getID).");
			// System.exit(1);
		}
	}
	
	private void getNewID() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerCommunication.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.retryLogin(this.getID(), TemperatureSensor.type));
			transceiver.close();
		}
		catch (IOException e) {
			// TODO handle exception here
			System.err.println("IOException at getNewID() at DistTemperatureSensor.");
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
		if (f_serverThread == null) {
			return;
		}
		f_serverThread.interrupt();
		f_serverThread = null;
	}
	
	public void logOffController() {
		try {
			Transceiver transceiver = 
					new SaslSocketTransceiver(f_controllerCommunication.toSocketAddress());
			ControllerComm proxy = 
					(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.logOff(this.getID());
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at logOff() in logOffController.");
		}
		catch (IOException e) {
			System.err.println("IOException at logOff() in logOffController.");
		}
	}
	
	public void disconnect() {
		this.logOffController();
		this.stopServer();
	}
	
	@Override
	public void run() {
		while (f_serverReady == false) {
			try {
				f_server = new SaslSocketServer(
						new SpecificResponder(communicationUser.class, this), new InetSocketAddress(f_ownIP, this.getID()) );
				f_server.start();
				f_serverReady = true;
			}
			catch (BindException e) {
				this.getNewID();
			}
			catch (IOException e) {
				System.err.println("Failed to start the DistTemperatureSensor server.");
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}

		try {
			f_server.join();
		}
		catch (InterruptedException e) {
			f_server.close();
			f_server = null;
		}
	}

	public void sendTemperatureToController() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerCommunication.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.addTemperature(this.getID(), this.getTemperature());
			transceiver.close();
		}
		catch (IOException e) {
			System.err.println("IOException in sentTemperatureToController for DistTemperatureSensor.");
		}
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
		
		final String clientIP = System.getProperty("clientip");
		final String serverIP = System.getProperty("ip");
		final int ControllerPort = 5000;
		
		DistTemperatureSensor sensor = new DistTemperatureSensor(
				19,22, clientIP, serverIP, ControllerPort);
		try {
			System.in.read();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		sensor.stopServer();
		System.exit(0);
		DistController controller = new DistController(ControllerPort, 10, serverIP);
		
		
		DistTemperatureSensor remoteSensor = new DistTemperatureSensor(
				19,22, clientIP, serverIP, ControllerPort);
		
		
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

	/**
	 * Checks if this client is still alive, which it clearly is when answering to the query.
	 * @return true.
	 * @throws AvroRemoteException if something went wrong during transmission.
	 */
	@Override
	public boolean aliveAndKicking() throws AvroRemoteException {
		return true;
	}

	/**
	 * Adjusts the connection to the controller to the new elected controller.
	 */
	@Override
	public void newServer(CharSequence newServerIP, int newServerID) {
		f_controllerCommunication = new ConnectionData(newServerIP.toString(), newServerID);
	}

}
