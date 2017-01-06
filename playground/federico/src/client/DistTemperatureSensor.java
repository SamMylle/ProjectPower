/**
 * 
 */
package client;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import client.util.ConnectionData;
import avro.ProjectPower.ControllerComm;
import avro.ProjectPower.communicationTempSensor;


public class DistTemperatureSensor 
	extends TemperatureSensor 
	implements communicationTempSensor, Runnable {
	
	private ConnectionData f_controllerConnection;
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverReady;
	private Timer f_timer;
	private String f_ownIP;

	
	public DistTemperatureSensor(double lowTempRange, double highTempRange, int generateInterval, String ownIP, String controllerIP, int controllerPort) {
		super(lowTempRange, highTempRange, generateInterval);
		
		f_controllerConnection = new ConnectionData(controllerIP, controllerPort);
		f_serverReady = false;
		f_ownIP = ownIP;
		this.setupID();
		this.startServer();
		
		f_timer = new Timer();
		f_timer.schedule(new sendTemperatureTask(this), generateInterval, generateInterval);
	}

	private void setupID() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.LogOn(TemperatureSensor.type, f_ownIP));
			transceiver.close();
		}
		catch (Exception e) {
			System.err.println("Could not connect to the controller at startup. Shutting the sensor down.");
			System.exit(1);
		}
	}
	
	private void getNewID() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.retryLogin(this.getID(), TemperatureSensor.type));
			transceiver.close();
		}
		catch (Exception e) {
			System.err.println("Could not connect to the controller at startup. Shutting the sensor down.");
			System.exit(1);
		}
	}
	
	private void startServer() {
		f_serverThread = new Thread(this);
		f_serverThread.start();
		
		while (f_serverReady == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
		}
		this.notifySuccessfulLogin();
	}
	
	public void stopServer() {
		if (f_serverThread == null) {
			return;
		}
		f_serverThread.interrupt();
		f_serverThread = null;
		
		while (f_server != null) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
		}
	}
	
	public void logOffController() {
		try {
			Transceiver transceiver = 
					new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
					(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.logOff(this.getID());
			transceiver.close();
		} catch (Exception e) {}
	}
	
	public void disconnect() {
		this.logOffController();
		this.stopServer();
	}
	
	
	/**
	 * Notifies the controller of successful login.
	 */
	private void notifySuccessfulLogin() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.loginSuccessful(this.getID());
			transceiver.close();
		}
		catch (Exception e) {
			System.err.println("Could not connect to the controller at startup. Shutting the sensor down.");
			System.exit(1);
		}
	}
	
	/**
	 * Gets a new login from the controller, and restarts the server on the potentially new port
	 */
	@Override
	public void reLogin() {
		this.stopServer();
		this.setupID();
		this.startServer();
	}
	
	@Override
	public void run() {
		while (f_serverReady == false) {
			try {
				f_server = new SaslSocketServer(
						new SpecificResponder(communicationTempSensor.class, this), new InetSocketAddress(f_ownIP, this.getID()) );
				f_server.start();
				f_serverReady = true;
			}
			catch (BindException e) {
				this.getNewID();
			}
			catch (IOException e) {
				System.err.println("Failed to start the DistTemperatureSensor server. Shutting down the server.");
				System.exit(1);
			}
		}

		try {
			f_server.join();
		}
		catch (InterruptedException e) {
			f_server.close();
			f_server = null;
			f_serverReady = false;
		}
	}

	public void sendTemperatureToController() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.addTemperature(this.getID(), this.getTemperature());
			transceiver.close();
		}
		catch (Exception e) {
			return;
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
	public Void newServer(CharSequence newServerIP, int newServerID) {
		f_controllerConnection = new ConnectionData(newServerIP.toString(), newServerID);
		return null;
	}

	@Override
	public List<Double> getTemperatureRecords() throws AvroRemoteException {
		return super.getHistory();
	}

	
	public static void main(String[] args) {
		String clientIP = "";
		String serverIP = "";
		int controllerPort = 0;
		try {
			clientIP = System.getProperty("clientip");
			serverIP = System.getProperty("ip");
			controllerPort = Integer.parseInt(System.getProperty("controllerport"));			
		} catch (Exception e) {
			System.err.println("Not all arguments have been given (correctly) when running the program.\nNeeded arguments:(\"ip\", \"clientip\", \"controllerport\")");
			System.exit(1);
		}
		
		DistTemperatureSensor sensor = new DistTemperatureSensor(
				19,22, 1000, clientIP, serverIP, controllerPort);
		try {
			System.in.read();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		sensor.disconnect();
		System.exit(0);
	}
}
