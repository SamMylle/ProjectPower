package client;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Vector;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import util.Logger;
import controller.DistController;

import avro.ProjectPower.*;
import avro.ProjectPower.ControllerComm;
import client.exception.*;
import client.util.ConnectionData;
import client.util.LightState;


// TODO add getNewID() to resolve bind exceptions
public class DistUser extends User implements communicationUser, Runnable {
	
	private String f_ownIP;
	
	private ConnectionData f_controllerConnection;
	
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverReady;
	
	private boolean f_connectedToFridge;
	private int f_fridgePort;
	private String f_fridgeIP;
	
	
	public DistUser(String name, String ownIP, String controllerIP, int controllerPort) {
		super(name);
		assert controllerPort >= 1000;
		
		// TODO check IP arguments to be valid
		f_ownIP = ownIP;
		
		f_controllerConnection = new ConnectionData(controllerIP, controllerPort);
		f_serverReady = false;
		f_connectedToFridge = false;
		f_fridgePort = -1;
		f_fridgeIP = "";
		
		this.setupID();
		this.setupServer();
		super._setStatus(UserStatus.present);
	}
	
	private void setupID() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.LogOn(User.type, f_ownIP));
			transceiver.close();
		}
		catch (IOException e) {
			System.err.println("IOException in constructor for DistUser (getID).");
			// System.exit(1);
		}
	}
	
	private void getNewID() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.retryLogin(this.getID(), User.type));
			transceiver.close();
		}
		catch (IOException e) {
			// TODO add handling of exception here, controller not accessible?
			System.err.println("IOException at getNewID() in DistUser.");
		}
	}
	
	public boolean logOffController() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
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
		if (f_serverThread == null) {
			return;
		}
		f_serverThread.interrupt();
		f_serverThread = null;
	}
	
	public void disconnect() {
		this.logOffController();
		this.stopServer();
	}
	
	// TODO make sure exception handling isn't screwed up when using this method
	private SaslSocketTransceiver getControllerTransceiver() throws Exception {
		SaslSocketTransceiver transceiver = null;
		try {
			transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
		}
		catch (IOException e) {
			System.err.println("Could not establish connection with the controller.");
			throw new Exception("");
		}
		
		return transceiver;
	}
	
	// TODO same as above
	private SaslSocketTransceiver getSmartFridgeTransciever() throws Exception {
		SaslSocketTransceiver transceiver = null;
		try {
			transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_fridgeIP, f_fridgePort));
		} catch (IOException e) {
			System.err.println("Could not establish connection with the smartfridge.");
			throw new Exception("");
		}
		
		return transceiver;
	}
	
	
	/**
	 * =====================================================
	 * General methods: communication with controller/fridge
	 * =====================================================
	 */
	

	
	public List<LightState> getLightStates() throws MultipleInteractionException, AbsentException {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}

		if (f_connectedToFridge == true) {
			throw new MultipleInteractionException("The user is connected to the SmartFridge, cannot connect to any other devices.");
		}
		
		List<LightState> lightStates = null;
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
				(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			List<Client> clients = proxy.getAllClients();
			lightStates = new Vector<LightState>();
			
			// TODO check for different options to access fields because the following are deprecated?
			for (Client client : clients) {
				if (client.clientType == ClientType.Light) {
					LightState state = new LightState(client.ID, proxy.getLightState(client.ID));
					lightStates.add(state);
				}
			}
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at requestLightStates() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at requestLightStates() in DistUser.");
		}
		
		return lightStates;
	}
	
	public void setLightState(int newState, int lightID) throws MultipleInteractionException, AbsentException {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}

		if (f_connectedToFridge == true) {
			throw new MultipleInteractionException("The user is connected to the SmartFridge, cannot connect to any other devices.");
		}
		
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
				(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
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
	
	public List<String> getFridgeItems(int fridgeID) throws MultipleInteractionException, AbsentException {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}

		if (f_connectedToFridge == true) {
			throw new MultipleInteractionException("The user is connected to the SmartFridge, cannot connect to any other devices.");
		}
		
		List<CharSequence> _items = null;
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
				(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			_items = proxy.getFridgeInventory(fridgeID);
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at getFridgeItems() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at getFridgeItems() in DistUser.");
		}
		List<String> items = new Vector<String>();
		for (CharSequence item : _items) {
			items.add(item.toString());
		}
		return items;
	}
	
	public double getCurrentTemperatureHouse() throws MultipleInteractionException, AbsentException, NoTemperatureMeasures {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}

		if (f_connectedToFridge == true) {
			throw new MultipleInteractionException("The user is connected to the SmartFridge, cannot connect to any other devices.");
		}
		
		double currentTemp = 0;
		boolean hasTemperatures = false;
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
				(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			currentTemp = proxy.averageCurrentTemperature();
			hasTemperatures = proxy.hasValidTemperatures();
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at getCurrentTemperatureHouse() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at getCurrentTemperatureHouse() in DistUser.");
		}
		if (currentTemp != 0) {
			return currentTemp;
		} else if (hasTemperatures == true) {
			return currentTemp;
		}
		
		throw new NoTemperatureMeasures("No temperature measures are available yet.");
	}
	
	public void getTemperatureHistory() throws MultipleInteractionException, AbsentException {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}

		if (f_connectedToFridge == true) {
			throw new MultipleInteractionException("The user is connected to the SmartFridge, cannot connect to any other devices.");
		}
		
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
				(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
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
	 * Summary: request the controller for a list of all the clients connected to the system.
	 * 
	 * @return A list with all the clients connected to the system.
	 */
	public List<Client> getAllClients() throws MultipleInteractionException, AbsentException {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}
		if (f_connectedToFridge == true) {
			throw new MultipleInteractionException("The user is connected to the SmartFridge, cannot connect to any other devices.");
		}
		
		List<Client> clients = null;
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
				(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			clients = proxy.getAllClients();
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at getAllClients() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at getAllClients() in DistUser.");
		}
		return clients;
	}
	
	public void communicateWithFridge(int fridgeID) 
		throws MultipleInteractionException, AbsentException, FridgeOccupiedException {
		
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}

		if (f_connectedToFridge == true) {
			throw new MultipleInteractionException("The user is connected to the SmartFridge, cannot connect to any other devices.");
		}
		
		CommData fridgeData = null;
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
				(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			// TODO fix this here, with IP addresses
			fridgeData = proxy.setupFridgeCommunication(fridgeID);
//			client = proxy.setupFridgeCommunication(fridgeID);
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at communicateWithFridge() in DistUser.");
			return;
		} 
		catch (IOException e) {
			System.err.println("IOException at communicateWithFridge() in DistUser.");
			return;
		}
		
		if (fridgeData.getID() == -1) {
			throw new FridgeOccupiedException("The fridge is already being used by another user.");
		}
		f_fridgeIP = fridgeData.getIP().toString();
		f_fridgePort = fridgeData.getID();
		f_connectedToFridge = true;
	}
	
	public void addItemFridge(String item) throws NoFridgeConnectionException, AbsentException {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}

		if (f_connectedToFridge == false) {
			throw new NoFridgeConnectionException("No connection has been setup with the SmartFridge yet.");
		}
		
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(new InetSocketAddress(f_fridgeIP, f_fridgePort));
			communicationFridgeUser proxy = 
				(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			proxy.addItemRemote(item);
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at addItemFridge() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at addItemFridge() in DistUser.");
		}
	}
	public void removeItemFridge(String item) throws NoFridgeConnectionException, AbsentException {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}

		if (f_connectedToFridge == false) {
			throw new NoFridgeConnectionException("No connection has been setup with the SmartFridge yet.");
		}
		
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(new InetSocketAddress(f_fridgeIP, f_fridgePort));
			communicationFridgeUser proxy = 
				(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			proxy.removeItemRemote(item);
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at removeItemFridge() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at removeItemFridge() in DistUser.");
		}
	}
	
	public List<String> getFridgeItemsDirectly() throws NoFridgeConnectionException, AbsentException {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}

		if (f_connectedToFridge == false) {
			throw new NoFridgeConnectionException("No connection has been setup with the SmartFridge yet.");
		}
		
		List<CharSequence> _items = null;
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(new InetSocketAddress(f_fridgeIP, f_fridgePort));
			communicationFridgeUser proxy = 
				(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			_items = proxy.getItemsRemote();
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at getFridgeItemsDirectly() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at getFridgeItemsDirectly() in DistUser.");
		}
		List<String> items = new Vector<String>();
		for (CharSequence item : _items) {
			items.add(item.toString());
		}
		return items;
	}
	
	public void openFridge() throws NoFridgeConnectionException, AbsentException {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}

		if (f_connectedToFridge == false) {
			throw new NoFridgeConnectionException("No connection has been setup with the SmartFridge yet.");
		}
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(new InetSocketAddress(f_fridgeIP, f_fridgePort));
			communicationFridgeUser proxy = 
				(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			proxy.openFridgeRemote();
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at openFridge() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at openFridge() in DistUser.");
		}
	}
	
	public void closeFridge() throws NoFridgeConnectionException, AbsentException {
		if (super._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house");
		}
		
		if (f_connectedToFridge == false) {
			throw new NoFridgeConnectionException("No connection has been setup with the SmartFridge yet.");
		}
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(new InetSocketAddress(f_fridgeIP, f_fridgePort));
			communicationFridgeUser proxy = 
				(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			proxy.closeFridgeRemote();
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at openFridge() in DistUser.");
		} 
		catch (IOException e) {
			System.err.println("IOException at openFridge() in DistUser.");
		}
		
		f_connectedToFridge = false;
		f_fridgePort = -1;
	}
	
	/**
	 * Thread run() method, used to run the User server in the background
	 */
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
				System.err.println("Failed to start DistUser server.");
				e.printStackTrace(System.err);
				System.exit(1);
			}
		}

		try {
			f_server.join();
		}
		catch (InterruptedException e) {
			f_server.close();
//			Logger.getLogger().log("Closed the DistUser server.");
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
 		final int controllerPort = 5000;
		DistController controller = new DistController(controllerPort, 10, System.getProperty("ip"));
		
		DistUser remoteUser = 
				new DistUser("Federico Quin", System.getProperty("clientip"), System.getProperty("ip"), controllerPort);
		
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
		controller.stopServer();
		System.exit(0);
	}

}
