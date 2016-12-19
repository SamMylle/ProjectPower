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
import client.util.ConnectionTypeData;
import client.util.LightState;


// TODO add fault tolerence between user and fridge directly
// TODO make a difference between a fridge server being taken over or just disconnecting
// TODO add exception to notify the user that the election has started
public class DistUser extends User implements communicationUser, Runnable {
	
	private String f_ownIP;	
	private ConnectionData f_controllerConnection;
	private ConnectionData f_fridgeConnection;
	
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverReady;
	
	public List<String> f_notifications;
	
	/// FAULT TOLERENCE & REPLICATION
	private ServerData f_replicatedServerData;				// The replicated data from the DistController
	private DistController f_controller;					// DistController to be used when this object is elected
	
	private boolean f_isParticipantElection;				// Equivalent to participant_i in slides
	private int f_electionID;								// The index of the client in the election
	private int f_nextCandidateOffset;						// Offset used when the next Candidate in line cannot be used
	
	
	
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
	public DistUser(String name, String ownIP, String controllerIP, int controllerPort) throws IOControllerException {
		super(name);
		
		f_ownIP = ownIP;
		
		f_controllerConnection = new ConnectionData(controllerIP, controllerPort);
		f_serverReady = false;
		f_fridgeConnection = null;
		f_notifications = new Vector<String>();
		
		f_replicatedServerData = null;
		f_controller = null;
		f_isParticipantElection = false;
		f_electionID = -1;
		f_nextCandidateOffset = 1;
		
		this.setupID();
		if (this.getID() == -1) {
			throw new IOControllerException("Could not connect to the controller.");
		}
		this.startServer();
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
		catch (IOException e) {	}
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
		catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Starts the server.
	 */
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
		catch (AvroRemoteException e) {
			// TODO handle more appropriately
			System.err.println("AvroRemoteException at notifySuccessfulLogin() in DistUser.");
		}
		catch (IOException e) {
			System.err.println("IOException at notifySuccessfulLogin() in DistUser.");
		}
	}
	
	/**
	 * Notifies the user that a fridge is empty.
	 * @param fridgeID The ID of the now empty fridge.
	 */
	@Override
	public void notifyFridgeEmpty(int fridgeID) {
		this.f_notifications.add("The fridge with ID " + Integer.toString(fridgeID) + " is empty.");
	}

	
	/**
	 * Notifies the user that another user has entered the house.
	 * @param userID The user that has entered the house.
	 */
	@Override
	public void notifyUserEntered(int userID) {
		this.f_notifications.add("The user with ID " + Integer.toString(userID) + " has entered the house.");
	}
	
	/**
	 * Notifies the user that another user has left the house.
	 * @param userID The user that has left the house.
	 */
	@Override
	public void notifyUserLeft(int userID) {
		this.f_notifications.add("The user with ID " + Integer.toString(userID) + " has left the house.");
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
		
		List<LightState> lightStates = new Vector<LightState>();
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
				(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			List<Client> clients = proxy.getAllClients();
			
			// TODO check for different options to access fields because the following are deprecated?
			for (Client client : clients) {
				if (client.getClientType() == ClientType.Light) {
					LightState state = new LightState(client.getID(), proxy.getLightState(client.getID()));
					lightStates.add(state);
				}
			}
			transceiver.close();
		}
		catch (IOException e) {
			this.startElection();
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
		catch (IOException e) {
			this.startElection();
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
		catch (IOException e) {
			this.startElection();
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
	public double getCurrentTemperatureHouse() 
			throws MultipleInteractionException, AbsentException, NoTemperatureMeasures, TakeoverException {
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
		catch (IOException e) {
			this.startElection();
		}
		if (currentTemp != 0) {
			return currentTemp;
		} else if (hasTemperatures == true) {
			return currentTemp;
		}
		
		throw new NoTemperatureMeasures("No temperature measures are available at this moment.");
	}
	
	/**
	 * Gets the history of temperature measurements in the house.
	 * @throws MultipleInteractionException if the user is connected to a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 */
	public List<Double> getTemperatureHistory() throws MultipleInteractionException, AbsentException, TakeoverException {
		this.checkInvariantExceptions();
		if (f_fridgeConnection != null) {
			throw new MultipleInteractionException("The user is connected to the SmartFridge, cannot connect to any other devices.");
		}
		
		List<Double> values = new ArrayList<Double>();
		
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
				(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			values = proxy.getTempHistory();
			transceiver.close();
		}
		catch (IOException e) {
			this.startElection();
		}
		
		return values;
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
		
		List<Client> clients = new ArrayList<Client>();
		try {
			SaslSocketTransceiver transceiver = 
				new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
				(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			clients = proxy.getAllClients();
			transceiver.close();
		}
		catch (IOException e) {
			this.startElection();
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
		throws MultipleInteractionException, AbsentException, FridgeOccupiedException, TakeoverException, NoFridgeConnectionException {
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
			this.startElection();
			// TODO replace this with exception?
			return;
		}
		
		if (connection.getID() == -1) {
			throw new FridgeOccupiedException("The fridge is already occupied by another user.");
		}
		f_fridgeConnection = new ConnectionData(connection);
		
		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_fridgeConnection.toSocketAddress());
			communicationFridgeUser proxy = 
					(communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
			proxy.registerUserIP(f_ownIP, this.getID());
			transceiver.close();
		} catch (IOException e) {
			f_fridgeConnection = null;
			throw new NoFridgeConnectionException("The connection with the fridge has been lost.");
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
		} catch (IOException e) {
			f_fridgeConnection = null;
			throw new NoFridgeConnectionException("The connection with the fridge has been lost.");
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
		} catch (IOException e) {
			f_fridgeConnection = null;
			throw new NoFridgeConnectionException("The connection with the fridge has been lost.");
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
		} catch (IOException e) {
			f_fridgeConnection = null;
			throw new NoFridgeConnectionException("The connection with the fridge has been lost.");
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
		} catch (IOException e) {
			f_fridgeConnection = null;
			throw new NoFridgeConnectionException("The connection with the fridge has been lost.");
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
		} catch (IOException e) {
			f_fridgeConnection = null;
			throw new NoFridgeConnectionException("The connection to the fridge has been lost.");
		}
		f_fridgeConnection = null;
	}
	
	/**
	 * Enters the house.
	 */
	public void enterHouse() {
		super.enter();
	}
	
	/**
	 * Leaves the house, as well as closing the direct communication if this is setup, and notifying the controller.
	 */
	public void leaveHouse() {
		if (super._getStatus() == UserStatus.absent) {
			return;
		}
		
		if (f_fridgeConnection != null) {
			try {
				this.closeFridge();
			} catch (NoFridgeConnectionException | AbsentException | TakeoverException e) {	}
		}
		super.leave();
		this.notifyControllerLeave();
	}
	
	/**
	 * Notifies the controller that the user has left the house.
	 */
	private void notifyControllerLeave() {
		// TODO write this method
		return;
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
			f_serverReady = false;
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
		f_fridgeConnection = null;
	}
	
	
	/**
	 * Sets new connection data for the controller.
	 */
	@Override
	public Void newServer(CharSequence newServerIP, int newServerID) {
		f_controllerConnection = new ConnectionData(newServerIP.toString(), newServerID);
		return null;
	}
	
	
	
	/// |===================================|
	/// |	Replication & Fault Tolerance	|
	/// |		Enter at your own risk		|
	/// |===================================|
	
	
	/**
	 * Starts an election with all the other users/smartfridges.
	 */
	public void startElection() {
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
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
		
		final ConnectionTypeData nextCandidate = this.getNextCandidateConnection();
		new Thread() {
			public void run() {				
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					if (nextCandidate.getType() == ClientType.SmartFridge) {
						communicationFridge proxy = (communicationFridge) 
								SpecificRequestor.getClient(communicationFridge.class, transceiver);
						proxy.electNewController(DistUser.this.f_electionID, DistUser.this.getID());
					} else if (nextCandidate.getType() == ClientType.User) {
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
	 * @param newServerID The Port of the newly elected controller.
	 */
	@Override
	public void newServerElected(final CharSequence newServerIP, final int newServerID) {
		
		if (new ConnectionData(newServerIP.toString(), newServerID).equals(new ConnectionData(f_ownIP, getID()))) {
			this.startControllerTakeOver();
			f_electionID = -1;
			return;
		}
		final ConnectionTypeData nextCandidate = DistUser.this.getNextCandidateConnection();
		f_controllerConnection = new ConnectionData(newServerIP.toString(), newServerID);
		f_isParticipantElection = false;
		f_electionID = -1;
		
		
		// TODO push this to separate method, where it can also be used for sendSelfElectedNextCandidate
		new Thread() {
			public void run() {				
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					if (nextCandidate.getType() == ClientType.SmartFridge) {
						communicationFridge proxy = (communicationFridge) 
								SpecificRequestor.getClient(communicationFridge.class, transceiver);
						proxy.newServerElected(newServerIP, newServerID);
					} else if (nextCandidate.getType() == ClientType.User) {
						communicationUser proxy = (communicationUser) 
								SpecificRequestor.getClient(communicationUser.class, transceiver);
						proxy.newServerElected(newServerIP, newServerID);
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
		
		final ConnectionTypeData nextCandidate = this.getNextCandidateConnection();
		new Thread() {
			public void run() {
				if (clientID > DistUser.this.getID()) {
					try {
						Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
						if (nextCandidate.getType() == ClientType.SmartFridge) {
							communicationFridge proxy = (communicationFridge) 
									SpecificRequestor.getClient(communicationFridge.class, transceiver);
							proxy.electNewController(index, clientID);
						} else if (nextCandidate.getType() == ClientType.User) {
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
							if (nextCandidate.getType() == ClientType.SmartFridge) {
								communicationFridge proxy = (communicationFridge) 
										SpecificRequestor.getClient(communicationFridge.class, transceiver);
								proxy.electNewController(DistUser.this.f_electionID, DistUser.this.getID());
							} else if (nextCandidate.getType() == ClientType.User) {
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
	 * Gets the ConnectionTypeData of the next client in the ring (IP, Port, Type).
	 * @return The ConnectionTypeData of the next client in the ring (that is accessible).
	 */
	private ConnectionTypeData getNextCandidateConnection() {
		HashMap<Integer, ClientType> participants = new HashMap<Integer, ClientType>();
		List<Integer> participantIDs = new Vector<Integer>();
		
		List<Integer> clientIDs = f_replicatedServerData.getNamesID();
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		List<Integer> clientIPsID = f_replicatedServerData.getIPsID();
		List<CharSequence> clientIPsIP = f_replicatedServerData.getIPsIP();
		
		/// This is written in a general way, need to make some changes in order to make this more general
		for (int i = 0; i < clientIDs.size(); i ++) {
			if (clientTypes.get(i) == ClientType.User || clientTypes.get(i) == ClientType.SmartFridge) {
				participants.put(clientIDs.get(i), clientTypes.get(i));
				participantIDs.add(clientIDs.get(i));
			}
		}
		
		f_nextCandidateOffset = 1;
		Integer nextCandidateID = new Integer(-1);
		String nextIP = "";
		ClientType type = null;
		
		while (true) {
			try {
				nextCandidateID = participantIDs.get((f_electionID+f_nextCandidateOffset) % participantIDs.size());
				nextIP = clientIPsIP.get( clientIPsID.indexOf(nextCandidateID) ).toString();
				type = participants.get(nextCandidateID);
				ConnectionData nextCandidate = new ConnectionData(nextIP, nextCandidateID.intValue());
				boolean active = false;
				
				Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
				
				if (type == ClientType.SmartFridge) {
					communicationFridge proxy = 
							(communicationFridge) SpecificRequestor.getClient(communicationFridge.class, transceiver);
					active = proxy.aliveAndKicking();
				} else if (type == ClientType.User) {
					communicationUser proxy = 
							(communicationUser) SpecificRequestor.getClient(communicationUser.class, transceiver);
					active = proxy.aliveAndKicking();
				}
				transceiver.close();					
				if (active == true) {
					break;
				}
				throw new IOException();
			} catch (IOException | NullPointerException e) {
				f_nextCandidateOffset += 1;
			}
			
			if (f_nextCandidateOffset > participants.size()) {
				/// should not be able to get here
				/// if it gets here though, it means that all the participants (including this object itself) are not reachable
				
				// TODO check if this is the desired effect
				System.exit(1);
				return null;
			}
		}
		return new ConnectionTypeData(nextIP, nextCandidateID.intValue(), type);
	}
	
	/**
	 * Gets the index of the participant in the election, according to the data provided by replication.
	 * @return The index of this client in the election process.
	 */
	private int getElectionIndex() {
		List<Integer> participants = new Vector<Integer>();
		
		List<Integer> clientIDs = f_replicatedServerData.getNamesID();
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		
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
		final ConnectionTypeData nextCandidate = this.getNextCandidateConnection();

		new Thread() {
			public void run() {				
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					if (nextCandidate.getType() == ClientType.SmartFridge) {
						communicationFridge proxy = (communicationFridge) 
								SpecificRequestor.getClient(communicationFridge.class, transceiver);
						proxy.newServerElected(f_ownIP, DistUser.this.getID());
					} else if (nextCandidate.getType() == ClientType.User) {
						communicationUser proxy = (communicationUser) 
								SpecificRequestor.getClient(communicationUser.class, transceiver);
						proxy.newServerElected(f_ownIP, DistUser.this.getID());
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
		for (int i = 0; i < clientIDs.size(); i ++) {
			if (clientTypes.get(i) == ClientType.Light || clientTypes.get(i) == ClientType.TemperatureSensor) {
				nonParticipants.put(clientIDs.get(i), clientTypes.get(i));
			}
		}
		
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
			} catch (IOException e) {
				// skip the client if it cannot be reached
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
				DistUser.this.setupID();
				DistUser.this.startServer();
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
		String clientip = System.getProperty("clientip");
		String serverip = System.getProperty("ip");
//		DistController controller = new DistController(5000, 10, serverip);
		DistUser remoteUser = null;
		try {
			remoteUser = new DistUser("Federico Quin", clientip, serverip, 5000);
			
		} catch (IOControllerException e) {
			System.out.println(e.getMessage());
		}
		
		try {
			System.in.read();
		} catch (IOException e) {}
		
		
		System.exit(0);
	}

}
