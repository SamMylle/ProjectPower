package client;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import util.Logger;
import controller.DistController;

import avro.ProjectPower.*;
import client.exception.*;
import client.util.ConnectionData;
import client.util.LightState;


// TODO add method to handle notifications of empty fridges (some type of buffer storing messages?)
// TODO add fault tolerence between user and fridge directly
// TODO be able to start a DistController when being elected
public class DistUser extends User implements communicationUser, Runnable {
	
	private String f_ownIP;
	
	private ConnectionData f_controllerConnection;
	
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverReady;
	
	private ConnectionData f_fridgeConnection;
	
	/// FAULT TOLERENCE & REPLICATION
	private ConnectionData f_originalControllerConnection; 	// Backup of the connection to the first DistController
	private ServerData f_replicatedServerData;				// The replicated data from the DistController
	private DistController f_controller;					// DistController to be used when this object is elected
	
	private boolean f_isParticipantElection;					// Equivalent to participant_i in slides
	private int f_electionID;									// The index of the client in the election
	
	
	
	/**
	 * Constructor for DistUser.
	 * @param name
	 * 		The name of the User.
	 * @param ownIP
	 * 		The IP address on which the user server needs to run.
	 * @param controllerIP
	 * 		The IP address on which the controller server is running.
	 * @param controllerPort
	 * 		The Port number on which the controller serveris running.
	 */
	public DistUser(String name, String ownIP, String controllerIP, int controllerPort) {
		super(name);
		assert controllerPort >= 1000;
		
		// TODO check IP arguments to be valid
		f_ownIP = ownIP;
		
		f_controllerConnection = new ConnectionData(controllerIP, controllerPort);
		f_serverReady = false;
		f_fridgeConnection = null;
		
		f_originalControllerConnection = new ConnectionData(f_controllerConnection);
		f_replicatedServerData = null;
		f_controller = null;
		f_isParticipantElection = false;
		f_electionID = -1;
		
		this.setupID();
		this.setupServer();
		super._setStatus(UserStatus.present);
	}
	
	/**
	 * Asks the controller for an initial ID.
	 */
	private void setupID() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.LogOn(User.type, f_ownIP));
			transceiver.close();
		}
		catch (IOException e) {
			// TODO server not reachable, start election
			System.err.println("IOException in constructor for DistUser (getID).");
		}
	}
	
	/**
	 * Requests and sets a new ID, given by the controller.
	 */
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
	
	/**
	 * Logs off at the controller.
	 * @return success of logging off.
	 */
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
	
	/**
	 * Starts the server.
	 */
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
	
	/**
	 * Stops the server.
	 */
	public void stopServer() {
		if (f_serverThread == null) {
			return;
		}
		f_serverThread.interrupt();
		f_serverThread = null;
		
		while (f_server != null) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) { }
		}
	}
	
	/**
	 * Log off at the controller and stop the server.
	 */
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
			transceiver = new SaslSocketTransceiver(f_fridgeConnection.toSocketAddress());
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
	

	/**
	 * Gets all the lights and their states.
	 * @return A list of LightState objects, containing the light IDs and their states respectively.
	 * @throws MultipleInteractionException if the user is connected to a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public List<LightState> getLightStates() throws MultipleInteractionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();

		if (f_fridgeConnection != null) {
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
	
	/**
	 * Sets the state of a light.
	 * @param newState The new state of the chosen light.
	 * @param lightID The ID of the light from whom the state should be changed.
	 * @throws MultipleInteractionException if the user is connected to a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public void setLightState(int newState, int lightID) throws MultipleInteractionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();

		if (f_fridgeConnection != null) {
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
	
	/**
	 * Gets and returns the inventory of a fridge.
	 * @param fridgeID The ID of the fridge from whom the inventory is desired.
	 * @return A list of strings, representing the items in the fridge.
	 * @throws MultipleInteractionException if the user is connected to a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public List<String> getFridgeItems(int fridgeID) throws MultipleInteractionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();

		if (f_fridgeConnection != null) {
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
	
	/**
	 * Gets the current temperature in the house.
	 * @return The current temperature in the house.
	 * @throws MultipleInteractionException if the user is connected to a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws NoTemperatureMeasures if no temperature measures are available yet.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public double getCurrentTemperatureHouse() throws MultipleInteractionException, AbsentException, NoTemperatureMeasures, TakeoverException {
		this.checkInvariantExceptions();

		if (f_fridgeConnection != null) {
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
	
	/**
	 * Gets the history of temperature measurements in the house.
	 * @throws MultipleInteractionException if the user is connected to a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public void getTemperatureHistory() throws MultipleInteractionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();

		if (f_fridgeConnection != null) {
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
	 * Request the controller for a list of all the clients connected to the system.
	 * 
	 * @return A list with all the clients connected to the system.
	 * @throws MultipleInteractionException if the user is connected to a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public List<Client> getAllClients() throws MultipleInteractionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();
		if (f_fridgeConnection != null) {
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
	
	/**
	 * Requests direct communication with a fridge, as well as sending user IP and Port to the fridge when connection was accepted.
	 * @param fridgeID
	 * 		The ID of the fridge to which the connection is desired.
	 * @throws MultipleInteractionException if the user is already connected to a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws FridgeOccupiedException if the fridge is occupied by another user.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public void communicateWithFridge(int fridgeID) 
		throws MultipleInteractionException, AbsentException, FridgeOccupiedException, TakeoverException {
		this.checkInvariantExceptions();
		if (f_fridgeConnection != null) {
			throw new MultipleInteractionException("The user is already connected to a fridge, cannot start another connection.");
		}
		CommData connection = null;
		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			connection = proxy.setupFridgeCommunication(fridgeID);
			transceiver.close();
		} catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at communicateWithFridge() in DistUser.");
		} catch (IOException e) {
			System.err.println("IOException at communicateWithFridge() in DistUser.");
		}
		if (connection.ID == -1) {
			throw new FridgeOccupiedException("The fridge is already occupied by another user.");
		}
		f_fridgeConnection = new ConnectionData(connection);
		
		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_fridgeConnection.toSocketAddress());
			communicationFridgeUser proxy = 
					(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			proxy.registerUserIP(f_ownIP, this.getID());
			transceiver.close();
		} catch (AvroRemoteException e) {
			// TODO handle this more appropriately
			System.err.println("AvroRemoteException at communicateWithFridge() in DistUser.");
			f_fridgeConnection = null;
		} catch (IOException e) {
			// TODO handle this more appropriately
			System.err.println("IOException at communicateWithFridge() in DistUser.");
			f_fridgeConnection = null;
		}
	}
	
	/**
	 * Adds an item to the fridge.
	 * @param item
	 * 		The item that gets added.
	 * @throws NoFridgeConnectionException if no connection has been established with a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public void addItemFridge(String item) 
			throws NoFridgeConnectionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();
		if (f_fridgeConnection == null) {
			throw new NoFridgeConnectionException("The user is not connected to a fridge, need to establish connection first.");
		}

		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_fridgeConnection.toSocketAddress());
			communicationFridgeUser proxy = 
					(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			proxy.addItemRemote(item);
			transceiver.close();
		} catch (AvroRemoteException e) {
			// TODO handle this more appropriately
			System.err.println("AvroRemoteException at addItemFridge() in DistUser.");
		} catch (IOException e) {
			// TODO handle this more appropriately
			System.err.println("IOException at addItemFridge() in DistUser.");
		}
	}
	
	/**
	 * Removes an items from the fridge.
	 * @param item
	 * 		The item that needs to get removed.
	 * @throws NoFridgeConnectionException if no connection has been established with a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public void removeItemFridge(String item) 
			throws NoFridgeConnectionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();
		if (f_fridgeConnection == null) {
			throw new NoFridgeConnectionException("The user is not connected to a fridge, need to establish connection first.");
		}

		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_fridgeConnection.toSocketAddress());
			communicationFridgeUser proxy = 
					(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			proxy.removeItemRemote(item);
			transceiver.close();
		} catch (AvroRemoteException e) {
			// TODO handle this more appropriately
			System.err.println("AvroRemoteException at removeItemRemote() in DistUser.");
		} catch (IOException e) {
			// TODO handle this more appropriately
			System.err.println("IOException at removeItemRemote() in DistUser.");
		}
	}
	
	/**
	 * Gets the inventory of a fridge directly (direct communication).
	 * @return A list of strings, containing the items in the fridge.
	 * @throws NoFridgeConnectionException if no connection has been established with a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public List<String> getFridgeItemsDirectly() 
			throws NoFridgeConnectionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();
		if (f_fridgeConnection == null) {
			throw new NoFridgeConnectionException("The user is not connected to a fridge, need to establish connection first.");
		}

		List<String> items = new ArrayList<String>();
		List<CharSequence> _items = null;
		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_fridgeConnection.toSocketAddress());
			communicationFridgeUser proxy = 
					(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			_items = proxy.getItemsRemote();
			transceiver.close();
		} catch (AvroRemoteException e) {
			// TODO handle this more appropriately
			System.err.println("AvroRemoteException at getFridgeItemsDirectly() in DistUser.");
		} catch (IOException e) {
			// TODO handle this more appropriately
			System.err.println("IOException at getFridgeItemsDirectly() in DistUser.");
		}
		
		for (CharSequence item : _items) {
			items.add(item.toString());
		}
		return items;
	}
	
	/**
	 * Opens the fridge.
	 * @throws NoFridgeConnectionException if no connection has been established with a fridge.
	 * @throws AbsentException if the user is not present in the house.
 	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public void openFridge() 
			throws NoFridgeConnectionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();
		if (f_fridgeConnection == null) {
			throw new NoFridgeConnectionException("The user is not connected to a fridge, need to establish connection first.");
		}

		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_fridgeConnection.toSocketAddress());
			communicationFridgeUser proxy = 
					(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			proxy.openFridgeRemote();
			transceiver.close();
		} catch (AvroRemoteException e) {
			// TODO handle this more appropriately
			System.err.println("AvroRemoteException at openFridge() in DistUser.");
		} catch (IOException e) {
			// TODO handle this more appropriately
			System.err.println("IOException at openFridge() in DistUser.");
		}
	}
	
	/**
	 * Closes the fridge, as well as the connection with the fridge.
	 * @throws NoFridgeConnectionException if no connection has been established with a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public void closeFridge() 
			throws NoFridgeConnectionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();
		if (f_fridgeConnection == null) {
			throw new NoFridgeConnectionException("The user is not connected to a fridge, need to establish connection first.");
		}
		
		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_fridgeConnection.toSocketAddress());
			communicationFridgeUser proxy = 
					(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			proxy.closeFridgeRemote();
			transceiver.close();
		} catch (AvroRemoteException e) {
			// TODO handle this more appropriately
			System.err.println("AvroRemoteException at closeFridge() in DistUser.");
		} catch (IOException e) {
			// TODO handle this more appropriately
			System.err.println("IOException at closeFridge() in DistUser.");
		}
		f_fridgeConnection = null;
	}
	
	
	private void checkInvariantExceptions() throws AbsentException, TakeoverException {
		if (this._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house.");
		}
		if (f_controller != null) {
			throw new TakeoverException("The user has been elected to act as the controller of the system.");
		}
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
			f_server = null;
//			Logger.getLogger().log("Closed the DistUser server.");
		}
		
	}
	
	
	/**
	 * Gets the status of the user.
	 * @return The current status of the user.
	 * @throws AvroRemoteException if something goes wrong during transmission.
	 */
	@Override
	public UserStatus getStatus() throws AvroRemoteException {
		return super._getStatus();
	}
	
	/**
	 * Gets the name of the user.
	 * @return The name of the user.
	 * @throws AvroRemoteException if something goes wrong during transmission.
	 */
	@Override
	public String getName() throws AvroRemoteException {
		return super._getName();
	}
	
	/**
	 * Notification that the fridge has been closed.
	 */
	@Override
	public void notifyFridgeClosed() {
		System.out.println("\n\nFridge connection closed in the user.");
		f_fridgeConnection = null;
	}
	
	
	
	/// |===================================|
	/// |	Replication & Fault Tolerence	|
	/// |		Enter at your own risk		|
	/// |===================================|
	
	
	/**
	 * Starts an election with all the other users/smartfridges.
	 */
	public void startElection() {
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		boolean otherCandidates = false;
		int count = 0;
		for (ClientType clientType : clientTypes) {
			if (clientType == ClientType.SmartFridge || clientType == ClientType.User) {
				count++;
			}
		}
		if (count <= 1) {
			this.sendNonCandidatesNewServer();
			this.startControllerTakeOver();
			return;
		}
		
		f_isParticipantElection = true;
		f_electionID = this.getElectionIndex();
		
		final ConnectionData nextCandidate = this.getNextCandidateConnection();
		final ClientType nextCandidateType = this.getNextCandidateType();
		new Thread() {
			public void run() {				
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					if (nextCandidateType == ClientType.SmartFridge) {
						communicationFridge proxy = (communicationFridge) 
								SpecificRequestor.getClient(communicationFridge.class, transceiver);
						proxy.electNewController(DistUser.this.f_electionID, DistUser.this.getID());
					} else if (nextCandidateType == ClientType.User) {
						communicationUser proxy = (communicationUser) 
								SpecificRequestor.getClient(communicationUser.class, transceiver);
						proxy.electNewController(DistUser.this.f_electionID, DistUser.this.getID());
					}
					transceiver.close();
				} catch (IOException e) {
					// TODO handle this more appropriately
					System.err.println("IOException at startElection() in DistUser.");
				}
			}
		}.start();
		
	}
	
	/**
	 * Checks whether this client is still alive.
	 * @return true
	 * @throws AvroRemoteException if something went wrong during message transmission.
	 */
	@Override
	public boolean aliveAndKicking() throws AvroRemoteException {
		return true;
	}

	/**
	 * Equivalent to elected function from slides theory (slide 54 - Coordination)
	 * @param newServerIP
	 * 		The IP address of the newly elected controller.
	 * @param newServerID
	 * 		The Port of the newly elected controller.
	 * @return
	 * 		Void.
	 */
	@Override
	public void newServer(final CharSequence newServerIP, final int newServerID) {
		
		if (new ConnectionData(newServerIP.toString(), newServerID).equals(new ConnectionData(f_ownIP, getID()))) {
			this.startControllerTakeOver();
			f_electionID = -1;
			return;
		}
		final ConnectionData nextCandidate = DistUser.this.getNextCandidateConnection();
		final ClientType nextCandidateType = DistUser.this.getNextCandidateType();
		f_controllerConnection = new ConnectionData(newServerIP.toString(), newServerID);
		f_isParticipantElection = false;
		f_electionID = -1;
		
		System.out.println("");
		System.out.println("New controller address (" + f_controllerConnection.toString() + ") in user with ID=" + this.getID());
		System.out.println("");
		
		
		// TODO push this to separate method, where it can also be used for sendSelfElectedNextCandidate
		new Thread() {
			public void run() {				
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					if (nextCandidateType == ClientType.SmartFridge) {
						communicationFridge proxy = (communicationFridge) 
								SpecificRequestor.getClient(communicationFridge.class, transceiver);
						proxy.newServer(newServerIP, newServerID);
					} else if (nextCandidateType == ClientType.User) {
						communicationUser proxy = (communicationUser) 
								SpecificRequestor.getClient(communicationUser.class, transceiver);
						proxy.newServer(newServerIP, newServerID);
					}
					transceiver.close();
				} catch (AvroRemoteException e) {
					// TODO handle this more appropriately
					System.err.println("AvroRemoteException at sendSelfElectedNextCandidate() in DistUser.");
				} catch (IOException e) {
					// TODO handle this more appropriately
					System.err.println("IOException at sendSelfElectedNextCandidate() in DistUser.");
				}
			}
		}.start();
	}

	/**
	 * Equivalent to election function from slides theory (slide 54 - Coordination)
	 * @param index
	 * 		The client index in the election.
	 * @param clientID
	 * 		The ID of the client that is currently the highest.
	 */
	@Override
	public void electNewController(final int index, final int clientID) {
		// Setup index in case of first call
		if (f_electionID == -1) {
			f_electionID = this.getElectionIndex();
		}
		
		if (index == f_electionID) {
			f_isParticipantElection = false;
			// Send newServer to all the clients who did not participate in the election, and only to the next client who was involved in the election
			// This is in order to fully replicate the algorithm described in the theory.
			this.sendSelfElectedNextCandidate();
			this.sendNonCandidatesNewServer();
			
			return;
		}
		
		final ConnectionData nextCandidate = this.getNextCandidateConnection();
		final ClientType nextCandidateType = this.getNextCandidateType();
		new Thread() {
			public void run() {
				if (clientID > DistUser.this.getID()) {
					try {
						Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
						if (nextCandidateType == ClientType.SmartFridge) {
							communicationFridge proxy = (communicationFridge) 
									SpecificRequestor.getClient(communicationFridge.class, transceiver);
							proxy.electNewController(index, clientID);
						} else if (nextCandidateType == ClientType.User) {
							communicationUser proxy = (communicationUser) 
									SpecificRequestor.getClient(communicationUser.class, transceiver);
							proxy.electNewController(index, clientID);
						}
						transceiver.close();
					} catch (IOException e) {
						// TODO handle this more appropriately
						System.err.println("IOException at electNewController() in DistUser.");
					}
				} else if (clientID <= DistUser.this.getID()) {
					if (DistUser.this.f_isParticipantElection == false) {
						DistUser.this.f_isParticipantElection = true;
						try {
							Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
							if (nextCandidateType == ClientType.SmartFridge) {
								communicationFridge proxy = (communicationFridge) 
										SpecificRequestor.getClient(communicationFridge.class, transceiver);
								proxy.electNewController(DistUser.this.f_electionID, DistUser.this.getID());
							} else if (nextCandidateType == ClientType.User) {
								communicationUser proxy = (communicationUser) 
										SpecificRequestor.getClient(communicationUser.class, transceiver);
								proxy.electNewController(DistUser.this.f_electionID, DistUser.this.getID());
							}
							transceiver.close();
						} catch (IOException e) {
							// TODO handle this more appropriately
							System.err.println("IOException at electNewController() in DistUser.");
						}
					}
				}
			}
		}.start();
	}
	
	/**
	 * Gets the ConnectionData of the next client in the ring.
	 * @return
	 * 		The ConnectionData of the next client in the ring.
	 */
	private ConnectionData getNextCandidateConnection() {
		HashMap<Integer, ClientType> participants = new HashMap<Integer, ClientType>();
		List<Integer> participantIDs = new Vector<Integer>();
		
		List<Integer> clientIDs = f_replicatedServerData.getNamesID();
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		List<Integer> clientIPsID = f_replicatedServerData.getIPsID();
		List<CharSequence> clientIPsIP = f_replicatedServerData.getIPsIP();
		
		/// This is written in a general way, need to make some changes in order to make this more general
		// TODO write this more generic if time allows it
		for (int i = 0; i < clientIDs.size(); i ++) {
			if (clientTypes.get(i) == ClientType.User || clientTypes.get(i) == ClientType.SmartFridge) {
				participants.put(clientIDs.get(i), clientTypes.get(i));
				participantIDs.add(clientIDs.get(i));
			}
		}
		
		if (f_electionID == participantIDs.size()-1){
			String nextIP = clientIPsIP.get(clientIPsID.indexOf(participantIDs.get(0))).toString();
			return new ConnectionData(nextIP, participantIDs.get(0).intValue());
		}
		
		String nextIP = clientIPsIP.get( clientIPsID.indexOf(new Integer(participantIDs.get(f_electionID+1)) ) ).toString();
		int nextPort = participantIDs.get(f_electionID+1);
		return new ConnectionData(nextIP, nextPort);
	}
	
	/**
	 * Gets the type of the next candidate in the ring.
	 * @return The type of the client that is next in the ring.
	 */
	private ClientType getNextCandidateType() {
		HashMap<Integer, ClientType> participants = new HashMap<Integer, ClientType>();
		List<Integer> participantIDs = new Vector<Integer>();
		
		List<Integer> clientIDs = f_replicatedServerData.getNamesID();
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		
		/// This is written in a general way, need to make some changes in order to make this more general
		// TODO write this more generic if time allows it
		for (int i = 0; i < clientIDs.size(); i ++) {
			if (clientTypes.get(i) == ClientType.User || clientTypes.get(i) == ClientType.SmartFridge) {
				participants.put(clientIDs.get(i), clientTypes.get(i));
				participantIDs.add(clientIDs.get(i));
			}
		}
		
		if (f_electionID == participantIDs.size()-1) {
			return participants.get(participantIDs.get(0));
		}
		return participants.get(participantIDs.get(f_electionID+1));
	}
	
	/**
	 * Gets the index of the participant in the election, according to the data provided by replication.
	 * @return The index of this client in the election process.
	 */
	private int getElectionIndex() {
		List<Integer> participants = new Vector<Integer>();
		
		List<Integer> clientIDs = f_replicatedServerData.getNamesID();
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		
		/// This is written in a general way, need to make some changes in order to make this more general
		// TODO write this more generic if time allows it
		for (int i = 0; i < clientIDs.size(); i ++) {
			if (clientTypes.get(i) == ClientType.User || clientTypes.get(i) == ClientType.SmartFridge) {
				participants.add(clientIDs.get(i));
			}
		}
		
		return participants.indexOf(new Integer(this.getID()));
	}
	
	/**
	 * Notifies the next participant in the ring that this client has been elected.
	 */
	private void sendSelfElectedNextCandidate() {
		final ConnectionData nextCandidate = this.getNextCandidateConnection();
		final ClientType nextCandidateType = this.getNextCandidateType();

		new Thread() {
			public void run() {				
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					if (nextCandidateType == ClientType.SmartFridge) {
						communicationFridge proxy = (communicationFridge) 
								SpecificRequestor.getClient(communicationFridge.class, transceiver);
						proxy.newServer(f_ownIP, DistUser.this.getID());
					} else if (nextCandidateType == ClientType.User) {
						communicationUser proxy = (communicationUser) 
								SpecificRequestor.getClient(communicationUser.class, transceiver);
						proxy.newServer(f_ownIP, DistUser.this.getID());
					}
					transceiver.close();
				} catch (AvroRemoteException e) {
					// TODO handle this more appropriately
					System.err.println("AvroRemoteException at sendSelfElectedNextCandidate() in DistUser.");
				} catch (IOException e) {
					// TODO handle this more appropriately
					System.err.println("IOException at sendSelfElectedNextCandidate() in DistUser.");
				}
			}
		}.start();
	}
	
	/**
	 * Notifies all the clients that did not participate in the election that this client was elected.
	 */
	private void sendNonCandidatesNewServer() {
		HashMap<Integer, ClientType> nonParticipants = new HashMap<Integer, ClientType>();
		
		List<Integer> clientIDs = f_replicatedServerData.getNamesID();
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		List<Integer> clientIPsID = f_replicatedServerData.getIPsID();
		List<CharSequence> clientIPsIP = f_replicatedServerData.getIPsIP();
		
		/// This is written in a general way, need to make some changes in order to make this more general
		// TODO write this more generic if time allows it
		for (int i = 0; i < clientIDs.size(); i ++) {
			if (clientTypes.get(i) == ClientType.Light || clientTypes.get(i) == ClientType.TemperatureSensor) {
				nonParticipants.put(clientIDs.get(i), clientTypes.get(i));
			}
		}
		
		// TODO test this extensively
		
		// This part is not asynchronous, since it is not really part of the Roberts-Chang algorithm
		Iterator it = nonParticipants.entrySet().iterator();
		while (it.hasNext() == true) {
			Map.Entry pair = (Map.Entry)it.next();
			String clientIP = clientIPsIP.get(clientIPsID.indexOf(pair.getKey())).toString();
			Integer clientPort = (Integer) pair.getKey();
			ClientType clientType = nonParticipants.get(clientPort);
			
			try {
				Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(clientIP, clientPort));
				if (clientType == ClientType.Light) {
					LightComm proxy = (LightComm) 
							SpecificRequestor.getClient(LightComm.class, transceiver);
					proxy.newServer(f_ownIP, this.getID());
				} else if (clientType == ClientType.TemperatureSensor) {
					communicationTempSensor proxy = (communicationTempSensor) 
							SpecificRequestor.getClient(communicationTempSensor.class, transceiver);
					proxy.newServer(f_ownIP, this.getID());
				}
				transceiver.close();
			} catch (AvroRemoteException e) {
				// TODO handle this more appropriately
				System.err.println("AvroRemoteException at sendNonCandidatesNewServer() in DistUser.");
			} catch (IOException e) {
				// TODO handle this more appropriately
				System.err.println("IOException at sendNonCandidatesNewServer() in DistUser.");
			}
		}
	}

	
	private void startControllerTakeOver() {
		this.stopServer();
		if (this.f_fridgeConnection != null) {
			try {
				this.closeFridge();
			} catch (NoFridgeConnectionException e) {} 
			  catch (AbsentException e) {} 
			  catch (TakeoverException e) {}
		}
		this.f_fridgeConnection = null;
		
		new Thread() {
			public void run() {
				DistUser.this.f_replicatedServerData.setPort(DistUser.this.getID());
				DistUser.this.f_replicatedServerData.setIp(DistUser.this.f_ownIP);
				DistUser.this.f_controller = new DistController(DistUser.this.f_replicatedServerData);
				while (DistUser.this.f_controller.serverIsActive() == true) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) { }
				}
				DistUser.this.f_controller = null;
				DistUser.this.setupServer();
			}
		}.start();
		
		while(f_controller == null){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) { }
		}
		
		while (DistUser.this.f_controller.serverIsActive() == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) { }
		}
	}
	

	/**
	 * Makes a backup of the serverdata (replication).
	 * @param data The data which needs to be backed up.
	 */
	@Override
	public void makeBackup(ServerData data) {
		f_replicatedServerData = data;
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
