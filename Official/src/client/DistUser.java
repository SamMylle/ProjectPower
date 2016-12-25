package client;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import util.Logger;
import util.ServerDataUnion;
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
	private boolean f_electionBusy;
	private boolean f_requestedUnion;
	private Timer f_waitForController;
	
	
	
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
		f_electionBusy = false;
		f_requestedUnion = false;
		
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
		if (f_controller != null) {
			f_controller.stopServer();
			this.stopServer();
			return;
		}
		
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
	public synchronized void notifyFridgeEmpty(int fridgeID) {
		this.f_notifications.add("The fridge with ID " + Integer.toString(fridgeID) + " is empty.");
	}

	
	/**
	 * Notifies the user that another user has entered the house.
	 * @param userID The user that has entered the house.
	 */
	@Override
	public synchronized void notifyUserEntered(int userID) {
		this.f_notifications.add("The user with ID " + Integer.toString(userID) + " has entered the house.");
	}
	
	/**
	 * Notifies the user that another user has left the house.
	 * @param userID The user that has left the house.
	 */
	@Override
	public synchronized void notifyUserLeft(int userID) {
		this.f_notifications.add("The user with ID " + Integer.toString(userID) + " has left the house.");
	}
	
	/**
	 * Removes the first element in the notification list.
	 */
	public synchronized void removeFirstNotification() {
		this.f_notifications.remove(0);
	}
	
	/**
	 * Gets the notification list.
	 * @return The list of strings with the notifications.
	 */
	public synchronized List<String> getNotifications() {
		return new ArrayList<String>(f_notifications);
	}

	
	/**
	 * Gets a new login from the controller, and restarts the server on the potentially new port
	 */
	@Override
	public void reLogin() {
		System.out.println("1 testing");
		this.stopServer();
		System.out.println("2 testing");
		this.setupID();
		System.out.println("3 testing");
		this.startServer();
		System.out.println("4 testing");
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
	 * @throws ElectionBusyException 
	 */
	public List<LightState> getLightStates() throws MultipleInteractionException, AbsentException, TakeoverException, ElectionBusyException {
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
			this.setupElection();
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
	 * @throws ElectionBusyException 
	 */
	public void setLightState(int newState, int lightID) throws MultipleInteractionException, AbsentException, TakeoverException, ElectionBusyException {
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
			this.setupElection();
		}
	}
	
	/**
	 * Gets and returns the inventory of a fridge.
	 * @param fridgeID The ID of the fridge from whom the inventory is desired.
	 * @return A list of strings, representing the items in the fridge.
	 * @throws MultipleInteractionException if the user is connected to a fridge.
	 * @throws AbsentException if the user is not present in the house.
	 * @throws TakeoverException if the user has been elected to be the new controller.
	 * @throws ElectionBusyException 
	 */
	public List<String> getFridgeItems(int fridgeID) throws MultipleInteractionException, AbsentException, TakeoverException, ElectionBusyException {
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
			this.setupElection();
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
	 * @throws ElectionBusyException 
	 */
	public double getCurrentTemperatureHouse() 
			throws MultipleInteractionException, AbsentException, NoTemperatureMeasures, TakeoverException, ElectionBusyException {
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
//			if (f_electionBusy == false && f_waitForController == null) {
//				this.startPollTimer(1500);
//			}
//			this.setupElection();
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
	 * @throws ElectionBusyException 
	 */
	public List<Double> getTemperatureHistory() throws MultipleInteractionException, AbsentException, TakeoverException, ElectionBusyException {
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
//			if (f_electionBusy == false && f_waitForController == null) {
//				this.startPollTimer(1500);
//			}
//			this.setupElection();
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
	 * @throws ElectionBusyException 
	 */
	public List<Client> getAllClients() throws MultipleInteractionException, AbsentException, TakeoverException, ElectionBusyException {
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
			this.setupElection();
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
	 * @throws ElectionBusyException 
	 */
	public void communicateWithFridge(int fridgeID) 
		throws MultipleInteractionException, AbsentException, FridgeOccupiedException, TakeoverException, NoFridgeConnectionException, ElectionBusyException {
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
			this.setupElection();
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
	 * @throws ElectionBusyException 
	 */
	public void addItemFridge(String item) 
			throws NoFridgeConnectionException, AbsentException, TakeoverException, ElectionBusyException {
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
	 * @throws ElectionBusyException 
	 */
	public void removeItemFridge(String item) 
			throws NoFridgeConnectionException, AbsentException, TakeoverException, ElectionBusyException {
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
	 * @throws ElectionBusyException 
	 */
	public List<String> getFridgeItemsDirectly() 
			throws NoFridgeConnectionException, AbsentException, TakeoverException, ElectionBusyException {
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
	 * @throws ElectionBusyException 
	 */
	public void openFridge() 
			throws NoFridgeConnectionException, AbsentException, TakeoverException, ElectionBusyException {
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
	 * @throws ElectionBusyException 
	 */
	public void closeFridge() 
			throws NoFridgeConnectionException, AbsentException, TakeoverException, ElectionBusyException {
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
	 * @throws ElectionBusyException 
	 * @throws TakeoverException 
	 * @throws  
	 */
	public void enterHouse() throws TakeoverException, ElectionBusyException {
		try {
			this.checkInvariantExceptions();
		} catch (AbsentException e) {}
		if (super._getStatus() == UserStatus.present) {
			return;
		}
		super.enter();
		this.notifyControllerEnter();
	}
	
	/**
	 * Leaves the house, as well as closing the direct communication if this is setup, and notifying the controller.
	 * @throws ElectionBusyException 
	 * @throws TakeoverException 
	 */
	public void leaveHouse() throws TakeoverException, ElectionBusyException {
		try {
			this.checkInvariantExceptions();
		} catch (AbsentException e) {}
		if (super._getStatus() == UserStatus.absent) {
			return;
		}
		
		if (f_fridgeConnection != null) {
			try {
				this.closeFridge();
			} catch (NoFridgeConnectionException | AbsentException | TakeoverException | ElectionBusyException e) {	}
		}
		super.leave();
		this.notifyControllerLeave();
	}
	
	/**
	 * Notifies the controller that the user has left the house.
	 */
	private void notifyControllerLeave() {
		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.leftHome(this.getID());
			transceiver.close();
		} catch (IOException e) {
			this.setupElection();
		}
	}
	
	/**
	 * Notifies the controller that the user has entered the house.
	 */
	private void notifyControllerEnter() {
		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.enteredHome(this.getID());
			transceiver.close();
		} catch (IOException e) {
			this.setupElection();
		}
	}
	
	private void checkInvariantExceptions() throws AbsentException, TakeoverException, ElectionBusyException {
		if (f_controller != null) {
			throw new TakeoverException("The user has been elected to act as the controller of the system.");
		}
		if (this._getStatus() != UserStatus.present) {
			throw new AbsentException("The user is not present in the house.");
		}
		if (f_electionBusy == true) {
			throw new ElectionBusyException("The user is busy setting up a new controller.");
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
	
	/**
	 * Checks whether this client is still alive.
	 * @return true
	 * @throws AvroRemoteException if something went wrong during message transmission.
	 */
	@Override
	public boolean aliveAndKicking() throws AvroRemoteException {
		if (f_electionBusy == true) {
			this.cleanupElection();
		}
		if (f_waitForController != null) {
			f_waitForController.cancel();
			f_waitForController = null;			
		}
		return true;
	}

	
	
	
	
	
	/// |===================================|
	/// |	Replication & Fault Tolerance	|
	/// |		Enter at your own risk		|
	/// |===================================|
	
	
	private void startPollTimer(int interval) {
		f_waitForController = new Timer();
		f_waitForController.schedule(new controllerPollTimer(), 0, interval);
	}
	
	
	/**
	 * Sets up the election by sending the serverdata around and unifying it in all the clients.
	 */
	private void setupElection() {

		synchronized(this) {
			if (f_electionBusy == true) {
				return;
			}
			f_electionBusy = true;
		}
		f_requestedUnion = true;
		f_electionID = this.getElectionIndex();
		f_isParticipantElection = true;
		
		new Thread() {
			public void run() {
				
				while (true) {
					ConnectionTypeData nextCandidate = DistUser.this.getNextCandidateConnection(false);
					try {
						Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
						if (nextCandidate.getType() == ClientType.SmartFridge) {
							communicationFridge proxy = (communicationFridge) 
									SpecificRequestor.getClient(communicationFridge.class, transceiver);
							// TODO uncomment when implemented
							//proxy.unifyServerData(f_replicatedServerData);
						} else if (nextCandidate.getType() == ClientType.User) {
							communicationUser proxy = (communicationUser) 
									SpecificRequestor.getClient(communicationUser.class, transceiver);
							proxy.unifyServerData(f_replicatedServerData);
						}
						transceiver.close();
						return;
					} catch (IOException e) {
						// do nothing, just try again
					} catch (NullPointerException e) {
						System.out.println("wonElection1");
						DistUser.this.wonElection(false);
						return;
					} catch (Exception e) {
						return;
					}
				}
			}
		}.start();
	}
	
	@Override
	public void unifyServerData(ServerData serverData) {
		
		if (ServerDataUnion.narrowEquals(f_replicatedServerData, serverData) == true && f_requestedUnion == true) {
			this.startElection();
			return;
		}
		f_replicatedServerData = ServerDataUnion.getUnion(f_replicatedServerData, serverData);
		
		f_electionID = this.getElectionIndex();
		f_electionBusy = true;
		
		new Thread() {
			public void run() {
				
				while (true) {
					ConnectionTypeData nextCandidate = DistUser.this.getNextCandidateConnection(false);
					try {
						Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
						if (nextCandidate.getType() == ClientType.SmartFridge) {
							communicationFridge proxy = (communicationFridge) 
									SpecificRequestor.getClient(communicationFridge.class, transceiver);
							// TODO uncomment when implemented
							//proxy.unifyServerData(f_replicatedServerData);
						} else if (nextCandidate.getType() == ClientType.User) {
							communicationUser proxy = (communicationUser) 
									SpecificRequestor.getClient(communicationUser.class, transceiver);
							proxy.unifyServerData(f_replicatedServerData);
						}
						transceiver.close();
						return;
					} catch (IOException e) {
						// do nothing, just try again
					} catch (Exception e) {
						System.out.println("wonElection2");
						DistUser.this.wonElection(false);
						return;
					}
				}
			}
		}.start();
	}
	
	/**
	 * Starts an election with all the other users/smartfridges.
	 */
	private void startElection() {
		System.out.println("started an election");
		
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		int count = 0;
		for (ClientType clientType : clientTypes) {
			if (clientType == ClientType.SmartFridge || clientType == ClientType.User) {
				count++;
			}
		}
		if (count <= 1) {
			System.out.println("wonElection3");
			this.wonElection(false);
			return;
		}
		
		if (this.getNextCandidateConnection(false) == null) {
			// this means that no other candidate was reachable => start controller in this client
			System.out.println("wonElection4");
			this.wonElection(false);
			return;
		}
		
		new Thread() {
			
			public void run() {
				ConnectionTypeData nextCandidate = DistUser.this.getNextCandidateConnection(false);
				while (true) {
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
						return;
					} catch (Exception e) {
						nextCandidate = DistUser.this.getNextCandidateConnection(false);
						if (nextCandidate == null) {
							System.out.println("wonElection5");
							DistUser.this.wonElection(false);
							return;
						}
					}					
				}
			}
			
		}.start();
		
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
			while (f_isParticipantElection == true) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {}
			}
			System.out.println("before starting the server");
			this.startControllerTakeOver();
			this.cleanupElection();
			return;
		}
		
		f_controllerConnection = new ConnectionData(newServerIP.toString(), newServerID);
		f_isParticipantElection = false;
		
		// TODO push this to separate method, where it can also be used for sendSelfElectedNextCandidate
		new Thread() {
			
			public void run() {
				ConnectionTypeData nextCandidate = DistUser.this.getNextCandidateConnection(false);
				while (true) {
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
						return;
					} catch (IOException e) {
						nextCandidate = DistUser.this.getNextCandidateConnection(false);						
						if (nextCandidate == null) {
							return;
						}
					} catch (Exception e) {
						return;
					}
					
				}
			}
			
		}.start();
		
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		int countParticipants = 0;
		for (ClientType clientType : clientTypes) {
			if (clientType == ClientType.SmartFridge || clientType == ClientType.User) {
				countParticipants++;
			}
		}
		int waitTime = (countParticipants * 200) + 1000;
		
		this.startPollTimer(waitTime);
	}
	
	
	private class controllerPollTimer extends TimerTask {
		public controllerPollTimer() {}

		@Override
		public void run() {
			if (f_electionBusy == true) {
				DistUser.this.setupElection();				
			}
		}
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
		System.out.println("index = "  + index + ", ID = " + clientID);
		f_electionID = this.getElectionIndex();
		f_electionBusy = true;
		
		if (index == f_electionID) {
			// Send newServer to all the clients who did not participate in the election, and only to the next client who was involved in the election
			// This is in order to fully replicate the algorithm described in the theory.
			this.sendSelfElectedNextCandidate();
			this.sendNonCandidatesNewServer();
			f_isParticipantElection = false;
			return;
		}
		
		
		new Thread() {
			
			public void run() {
				while (true) {
					System.out.println("stuck here?");
					ConnectionTypeData nextCandidate = DistUser.this.getNextCandidateConnection(false);
					if (clientID > DistUser.this.getID()) {
						f_isParticipantElection = true;
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
							return;
						} catch (IOException e) {
						} catch (NullPointerException e) {
							System.out.println("wonElection7");
							DistUser.this.wonElection(false);
							return;
						} catch (Exception e) {
							System.out.println("got here..." + e.getClass().toString());
						}
						
						
					} else if (clientID <= DistUser.this.getID()) {
						if (DistUser.this.f_isParticipantElection == false) {
							DistUser.this.f_isParticipantElection = true;
							Transceiver transceiver = null;
							try {
								transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
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
								return;
							} catch (IOException e) {
							} catch (NullPointerException e) {
								System.out.println("wonElection8");
								DistUser.this.wonElection(false);
								return;
							} catch (Exception e) {
								System.out.println("got here..." + e.getClass().toString());
							}
						}
					}
				}
			}
			
		}.start();
	}
	
	/**
	 * Gets the ConnectionTypeData of the next client in the ring (IP, Port, Type).
	 * @param checkNewController TODO
	 * @return The ConnectionTypeData of the next client in the ring (that is accessible).
	 */
	private ConnectionTypeData getNextCandidateConnection(boolean checkNewController) {
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
		
		int f_nextCandidateOffset = 1;
		Integer nextCandidateID = new Integer(-1);
		String nextIP = "";
		ClientType type = null;
		
		while (true) {
			try {
				nextCandidateID = participantIDs.get((f_electionID+f_nextCandidateOffset) % participantIDs.size());
				nextIP = clientIPsIP.get( clientIPsID.indexOf(nextCandidateID) ).toString();
				type = participants.get(nextCandidateID);
				
				if (new ConnectionData(nextIP, nextCandidateID).equals(new ConnectionData(f_ownIP, this.getID()))) {
					return null;
				}
				
				ConnectionData nextCandidate = new ConnectionData(nextIP, nextCandidateID.intValue());
				
				if (nextCandidate.equals(f_controllerConnection) && checkNewController) {
					return new ConnectionTypeData(nextCandidate.getIP(), nextCandidate.getPort(), null);
				}
				
				Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
				
				if (type == ClientType.SmartFridge) {
					communicationFridge proxy = 
							(communicationFridge) SpecificRequestor.getClient(communicationFridge.class, transceiver);
					proxy.getItemsRemote();
				} else if (type == ClientType.User) {
					communicationUser proxy = 
							(communicationUser) SpecificRequestor.getClient(communicationUser.class, transceiver);
					proxy.getName();
				}
				transceiver.close();
				break;
			} catch (Exception e) {
				f_nextCandidateOffset += 1;
			}
			
			if (f_nextCandidateOffset > participants.size()) {
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
		System.out.println("send next candidate i'm server");
		final ConnectionTypeData nextCandidate = this.getNextCandidateConnection(false);
		
		if (nextCandidate == null) {
//			this.startControllerTakeOver();
//			this.cleanupElection();
			return;
		}

		new Thread() {
			public void run() {				
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					if (nextCandidate.getType() == ClientType.SmartFridge) {
						System.out.println("Somethings broken");
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
				} catch (UndeclaredThrowableException e) {
					
				} catch (Exception e) {
					System.out.println("got here222... " + e.getClass().toString());
					e.printStackTrace();
				}
				// If you get an UndeclaredThrowableException, it is probably here, catch with base class Exception
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
	
	/**
	 * Function called when this client has won the election.
	 * Sends all the non participants a notification that this client is the new controller, aswell as starting the controller and cleaning up the election.
	 * @param sendNext TODO
	 */
	private void wonElection(boolean sendNext) {
		System.out.println("won the election...");
		this.startControllerTakeOver();
		if (sendNext == true) {
			this.sendSelfElectedNextCandidate();
		}
		this.sendNonCandidatesNewServer();
		this.cleanupElection();
	}

	/**
	 * 	Cleans up all the class variables used in the election
	 */
	private void cleanupElection() {
		f_isParticipantElection = false;
		f_electionID = -1;
		f_electionBusy = false;
		f_requestedUnion = false;
	}
	
	
	private void startControllerTakeOver() {
		this.stopServer();
		if (this.f_fridgeConnection != null) {
			try {
				this.closeFridge();
			} catch (NoFridgeConnectionException e) {} 
			  catch (AbsentException e) {} 
			  catch (TakeoverException e) {} 
			  catch (ElectionBusyException e) {}
		}
		this.f_fridgeConnection = null;
		
		new Thread() {
			public void run() {
				DistUser.this.f_replicatedServerData.setPort(DistUser.this.getID());
				DistUser.this.f_replicatedServerData.setIp(DistUser.this.f_ownIP);
				
				System.out.println("starting a controller");
				DistUser.this.f_controller = new DistController(DistUser.this.f_replicatedServerData);
				while (DistUser.this.f_controller.serverIsActive() == true) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) { }
				}
				System.out.println("stopped the controller");
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
		
		try {
			remoteUser.leaveHouse();
		} catch (TakeoverException | ElectionBusyException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			System.in.read();
		} catch (IOException e) {}
		
		try {
			remoteUser.enterHouse();
		} catch (TakeoverException | ElectionBusyException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			System.in.read();
		} catch (IOException e) {}
		
//		while (true) {
//			System.out.println(remoteUser.f_notifications.toString());
//			
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {}
//		}
//		
//		try {
//			System.in.read();
//		} catch (IOException e) {}
//		
//		
//		System.exit(0);
	}



}
