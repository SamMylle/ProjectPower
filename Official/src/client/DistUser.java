package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import util.Logger;
import controller.DistController;

import avro.ProjectPower.*;

public class DistUser extends User implements communicationUser, Runnable {
	
	private int f_controllerPort;
	
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverReady;
	
	
	public DistUser(int controllerPort, String name) {
		super(name);
		assert controllerPort >= 1000;
		
		f_controllerPort = controllerPort;
		f_serverReady = false;
		
		this.setupID();
		this.setupServer();
		super._setStatus(UserStatus.present);
	}
	
	private void setupID() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.getID(User.type));
			transceiver.close();
		}
		catch (IOException e) {
			System.err.println("IOException in constructor for DistUser (getID).");
			// System.exit(1);
		}
	}
	
	public boolean logOffController() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.logOff(this.getID());
			transceiver.close();
			return true;
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at logOff() in Distuser.");
			return false;
		}
		catch (IOException e) {
			System.err.println("IOException at logOff() in DistUser.");
			return false;
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
	
	
	/**
	 * ===============
	 * General methods
	 * ===============
	 */
	
	/**
	 * Summary: request the controller for a list of all the clients connected to the system.
	 * 
	 * @return A list with all the clients connected to the system.
	 */
	public List<Client> requestClients() {
		List<Client> clients = null;
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			clients = proxy.getAllClients();
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at requestClients() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at requestClients() in DistUser.");
		}
		return clients;
	}
	
	public void requestLightStates() {
		// TODO change return type
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			// TODO add method here to get all light states
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at requestLightStates() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at requestLightStates() in DistUser.");
		}
	}
	
	public void setLightState(int newState, int lightID) {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.setLight(newState, lightID);
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at setLightState() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at setLightState() in DistUser.");
		}
	}
	
	public List<CharSequence> getFridgeItems(int fridgeID) {
		List<CharSequence> items = null;
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			items = proxy.getFridgeInventory(fridgeID);
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at getFridgeItems() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at getFridgeItems() in DistUser.");
		}
		return items;
	}
	
	public double getCurrentTemperatureHouse() {
		double currentTemp = 0;
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			currentTemp = proxy.averageCurrentTemperature();
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at getCurrentTemperatureHouse() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at getCurrentTemperatureHouse() in DistUser.");
		}
		return currentTemp;
	}
	
	public void getTemperatureHistory() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			//TODO add call to get the history of all the stored temperatures.
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at getCurrentTemperatureHouse() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at getCurrentTemperatureHouse() in DistUser.");
		}
	}
	
	/**
	 * Thread run() method, used to run the User server in the background
	 */
	@Override
	public void run() {
		try {
			f_server = new SaslSocketServer(
					new SpecificResponder(communicationUser.class, this), new InetSocketAddress(this.getID()) );
			f_server.start();
		}
		catch (IOException e) {
			System.err.println("Failed to start DistUser server.");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		f_serverReady = true;

		try {
			f_server.join();
		}
		catch (InterruptedException e) {
			f_server.close();
			Logger.getLogger().log("Closed the DistUser server.");
		}
		
	}
	
	
	/**
	 * communicationUser interface methods
	 */
	@Override
	public UserStatus getStatus() throws AvroRemoteException {
		return super._getStatus();
	}
	
	@Override
	public String getName() throws AvroRemoteException {
		return super._getName();
	}
	

	
	/**
	 * Main function, used for testing
	 */
 	public static void main(String[] args) {
		DistController controller = new DistController(6789, 10);
		
		DistUser remoteUser = new DistUser(6789, "Federico Quin");
		
		try {
			Logger logger = Logger.getLogger();
			logger.f_active = true;
			if (remoteUser.getStatus() == UserStatus.present) {
				logger.log("User status is present.");
			}
			
			
			if (remoteUser.logOffController() == true) {
				logger.log("Logged off succesfully.");
			}
			else {
				logger.log("Could not log off.");
			}
			remoteUser.stopServer();
			logger.log("Server stopped.");
			
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at main class in DistSmartFridge.");
		}
		System.exit(0);
	}






}
