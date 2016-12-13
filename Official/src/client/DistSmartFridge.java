package client;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.avro.AvroRemoteException;

import avro.ProjectPower.ClientType;
import avro.ProjectPower.ControllerCandidate;
import avro.ProjectPower.ControllerComm;
import avro.ProjectPower.ServerData;
import avro.ProjectPower.communicationFridge;
import avro.ProjectPower.communicationFridgeUser;

import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import client.exception.AbsentException;
import client.exception.NoFridgeConnectionException;
import client.util.ConnectionData;

import controller.DistController;

import util.Logger;


// TODO check IP arguments to be valid
// TODO get new Port for the user server when port is already in use.
// TODO add fault tolerence between user and fridge directly, notification to controller if user dies
// TODO add IP Address for the user connection
// TODO be able to start a DistController when being elected
public class DistSmartFridge extends SmartFridge {

	private String f_ownIP;									// The IP address of the client itself
	
	private ConnectionData f_controllerConnection;			// Data of the current connection to the controller of the system
	private ConnectionData f_userConnection;				// Data of the current connection to the user which is using the fridge
	private ConnectionData f_userServerConnection;			// Data of the current connection to the own user server
	
	private boolean f_serverControllerReady;				// Boolean used to determine if the server for the controller has finished setting up.
	private boolean f_userServerReady;						// Boolean used to determine if the server for the user has finished setting up.
	private boolean f_safeToClose;							// TODO test
	
	private Server f_fridgeControllerServer;				// The server for the SmartFridge itself
	private Thread f_fridgeControllerThread;				// The thread used to run the server and handle the requests it gets.
	private Server f_userServer;							// The server for the User
	private Thread f_userServerThread;						// The thread used to run the userServer
	
	
	/// FAULT TOLERENCE & REPLICATION
	private ConnectionData f_originalControllerConnection; 	// Backup of the first controller connection
	private ServerData f_replicatedServerData;				// The replicated data from the DistController
	private DistController f_controller;					// DistController to be used when this object is elected
	private boolean f_isParticipantElection;				// Equivalent to participant_i in slides
	private int f_electionID;								// The index of the client in the election
	
	
	
	/**
	 * Constructor for DistSmartFridge
	 * 
	 * Constructs the DistSmartFridge object (in specified order):
	 * 		- Gets an ID from the controller, using the given arguments @controllerIP @controllerPort
	 * 		- Starts a SmartFridge server on @PORT=ID and @ownIP
	 * 
	 * @param ownIP 
	 * 		The IP address on which the fridge server(s) needs to run.
	 * @param controllerIP
	 * 		The IP address on which the controller server is running.
	 * @param controllerPort
	 * 	 	The Port number on which the controller server is running.
	 * 
	 */
	public DistSmartFridge(String ownIP, String controllerIP, int controllerPort) {
		f_ownIP = ownIP;
		
		f_controllerConnection = new ConnectionData(controllerIP, controllerPort);
		f_userConnection = null;
		f_userServerConnection = new ConnectionData(ownIP, -1);
		
		f_serverControllerReady = false;
		f_userServerReady = false;
		f_safeToClose = true;
		
		f_fridgeControllerServer = null;
		f_fridgeControllerThread = null;
		f_userServer = null;
		f_userServerThread = null;
		
		f_originalControllerConnection = new ConnectionData(f_controllerConnection);
		f_replicatedServerData = null;
		f_controller = null;
		f_electionID = -1;
		f_isParticipantElection = false;
		
		this.setupID();
		this.startControllerServer();
	}
	
	/**
	 * Summary: gets an ID for the SmartFridge, requesting one from the controller.
	 */
	private void setupID() {
		// TODO retry getting an ID when failing to bind to the port
		try {
			Transceiver transceiver = 
					new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
					(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.LogOn(SmartFridge.type, f_ownIP));
			transceiver.close();
		}
		catch (IOException e) {
			System.err.println("Error connecting to the controller server, the port number might be wrong.");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
	
	/**
	 * Logs off at the controller
	 * @return  success of logging off.
	 */
	public boolean logOffController() {
		try {
			Transceiver transceiver = 
					new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
					(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.logOff(this.getID());
			transceiver.close();
			return true;
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at logOff() in DistSmartFridge.");
			return false;
		}
		catch (IOException e) {
			System.err.println("IOException at logOff() in DistSmartFridge.");
			return false;
		}
	}
	
	/**
	 * Starts a thread, which runs the DistSmartFridge server for the controller. Sleeps until the server is functional.
	 */
	private void startControllerServer() {
		f_fridgeControllerThread = new Thread(new controllerServer());
		f_fridgeControllerThread.start();
		while (f_serverControllerReady == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Interrupts the thread which runs the DistSmartFridge server for the controller, forcing it to close.
	 */
	public void stopServerController() {
		if (f_fridgeControllerThread == null) {
			return;
		}
		f_fridgeControllerThread.interrupt();
		f_fridgeControllerThread = null;		
	}
	
	/**
	 * Interrupts the threads which run the DistSmartFridge servers, forcing them to close, aswell as logging off at the controller. 
	 */
	public void disconnect() {
		this.logOffController();
		this.stopServerController();
		this.stopUserServer();
	}
	
	
	/**
	 * Notifies the controller that the inventory of the fridge is empty.
	 */
	public void notifyControllerEmptyInventory() {
		try {
			Transceiver transceiver = 
					new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
					(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
//			proxy.notifyFridgeEmpty(this.getID());	//TODO add method when implemented in controller/user
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at logOff() in DistSmartFridge.");
		}
		catch (IOException e) {
			System.err.println("IOException at logOff() in DistSmartFridge.");
		}
	}
	
	/**
	 * Asks the controller for a new ID, to be used when the server failed to bind to the given port
	 */
	public void getNewID() {
		try {
			Transceiver transceiver = 
					new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
					(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.retryLogin(this.getID(), SmartFridge.type));
			transceiver.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at getNewID() in DistSmartFridge.");
		}
		catch (IOException e) {
			System.err.println("IOException at getNewID() in DistSmartFridge.");
		}
	}
	
	
	
	/**
	 * Class used to run the DistSmartFridge server used by the Controller
	 * 
	 * This class implements all the methods that the controller needs, 
	 * 		aswell as running the thread for the DistSmartFridge server respectively.
	 */
	public class controllerServer implements Runnable, communicationFridge, ControllerCandidate {
		
		controllerServer() { }
		
		@Override
		public void run() {
			/// repeat untill the server can bind to a valid port
			while (f_serverControllerReady == false) {
				try {
					f_fridgeControllerServer = new SaslSocketServer(
							new SpecificResponder(communicationFridge.class, this), new InetSocketAddress(f_ownIP, getID()) );
					f_fridgeControllerServer.start();
					f_serverControllerReady = true;
				}
				catch (BindException e) {
					getNewID();
				}
				catch (IOException e) {
					System.err.println("Failed to start the SmartFridge server for the controller.");
					e.printStackTrace(System.err);
					System.exit(1);
				}
			}
			
			try {
				f_fridgeControllerServer.join();
			}
			catch (InterruptedException e) {
				f_fridgeControllerServer.close();
				f_fridgeControllerServer = null;
			}
		}

		@Override
		public List<CharSequence> getItemsRemote() throws AvroRemoteException {
			List<CharSequence> items = new ArrayList<CharSequence>();
			
			Set<String> fridgeItems = DistSmartFridge.this.getItems();
			
			for (String item : fridgeItems) {
				items.add(item);
			}
			return items;
		}

		@Override
		public boolean testMethod(ClientType clienttype)
				throws AvroRemoteException {
			return false;
		}

		@Override
		public int requestFridgeCommunication(int userServerPort) throws AvroRemoteException {
			if (f_userServerConnection.getPort() != -1) {
				return -1;
			}
			f_userServerConnection.setPort(userServerPort);
			DistSmartFridge.this.startUserServer();
			return f_userServerConnection.getPort();
		}

		/**
		 * Checks if this client is still alive, which it clearly is when answering to the query.
		 * @throws AvroRemoteException if something went wrong during transmission.
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
			DistSmartFridge.this.f_controllerConnection = new ConnectionData(newServerIP.toString(), newServerID);
			DistSmartFridge.this.f_isParticipantElection = false;
			DistSmartFridge.this.f_electionID = -1;
			
			if (DistSmartFridge.this.getNextCandidate() == DistSmartFridge.this.f_controllerConnection) {
				// Do nothing if the next candidate is the new elected controller itself
				return;
			}
			
			// TODO push this to seperate method, where it can also be used for sendSelfElectedNextCandidate
			new Thread() {
				public void run() {				
					try {
						ConnectionData nextCandidate = DistSmartFridge.this.getNextCandidate();
						Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
						ControlMessages proxy = 
								(ControlMessages) SpecificRequestor.getClient(ControlMessages.class, transceiver);
						proxy.newServer(newServerIP, newServerID);
						transceiver.close();
					} catch (AvroRemoteException e) {
						// TODO handle this more appropriately
						System.err.println("AvroRemoteException at sendSelfElectedNextCandidate() in DistSmartFridge.");
					} catch (IOException e) {
						// TODO handle this more appropriately
						System.err.println("IOException at sendSelfElectedNextCandidate() in DistSmartFridge.");
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
				f_electionID = DistSmartFridge.this.getElectionIndex();
			}
			
			if (index == f_electionID) {
				f_isParticipantElection = false;
				// Send newServer to all the clients who did not participate in the election, and only to the next client who was involved in the election
				// This is in order to fully replicate the algorithm described in the theory.
				DistSmartFridge.this.sendSelfElectedNextCandidate();
				DistSmartFridge.this.sendNonCandidatesNewServer();
				
				// TODO START NEW CONTROLLER SERVER HERE
				
				return;
			}
			
			final ConnectionData nextCandidate = DistSmartFridge.this.getNextCandidate();
			new Thread() {
				public void run() {
					if (clientID > DistSmartFridge.this.getID()) {
						try {
							Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
							ControllerCandidate proxy = 
									(ControllerCandidate) SpecificRequestor.getClient(ControllerCandidate.class, transceiver);
							proxy.electNewController(index, clientID);
							transceiver.close();
						} catch (IOException e) {
							// TODO handle this more appropriately
							System.err.println("IOException at electNewController() in DistSmartFridge.");
						}
					} else if (clientID <= DistSmartFridge.this.getID()) {
						if (DistSmartFridge.this.f_isParticipantElection == false) {
							try {
								Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
								ControllerCandidate proxy = 
										(ControllerCandidate) SpecificRequestor.getClient(ControllerCandidate.class, transceiver);
								proxy.electNewController(DistSmartFridge.this.f_electionID, DistSmartFridge.this.getID());
								transceiver.close();
							} catch (IOException e) {
								// TODO handle this more appropriately
								System.err.println("IOException at electNewController() in DistSmartFridge.");
							}
							DistSmartFridge.this.f_isParticipantElection = true;
						}
					}
				}
			}.start();
		}
	}
	/**
	 * Starts the user server with own IP and legal Port (port is not guaranteed to be consistent every call)
	 */
	private void startUserServer() {
		f_userServerThread = new Thread(new UserServer());
		f_userServerThread.start();
		
		while (f_userServerReady == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) { }
		}
	}
	
	/**
	 * Stops the user server, as well as 'resetting' all the variables associated with the direct user communication
	 */
	private void stopUserServer() {
		f_userServerReady = false;
		f_userConnection = null;
		f_userServerConnection.setPort(-1);
		if (f_userServerThread != null) {
			f_userServerThread.interrupt();
			f_userServerThread = null;
		}
		
		while (f_userServer != null) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) { }
		}
	}
	
	private class UserServer implements Runnable, communicationFridgeUser {

		public UserServer() {}
		
		@Override
		public void run() {
			while (f_userServerReady == false) {
				try {
					f_userServer = new SaslSocketServer(
							new SpecificResponder(communicationFridgeUser.class, this), f_userServerConnection.toSocketAddress());
					f_userServer.start();
					f_userServerReady = true;
				} catch (BindException e) {
					f_userServerConnection.setPort(f_userServerConnection.getPort()-1);
				} catch (IOException e) {
					System.err.println("IOException at run() in UserServer(DistSmartFridge).");
				}
			}
			
			try {
				f_userServer.join();
			} catch (InterruptedException e) {
				while (f_safeToClose == false) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) { }
				}
				try {
					Thread.sleep(50);					
				} catch (InterruptedException e1) { }
				f_userServer.close();
				f_userServer = null;
				f_safeToClose = false;
			}
		}
		
		@Override
		public Void addItemRemote(CharSequence itemName) throws AvroRemoteException {
			DistSmartFridge.this.addItem(itemName.toString());
			return null;
		}

		@Override
		public Void removeItemRemote(CharSequence itemName) throws AvroRemoteException {
			DistSmartFridge.this.removeItem(itemName.toString());
			return null;
		}

		@Override
		public List<CharSequence> getItemsRemote() throws AvroRemoteException {
			List<CharSequence> items = new ArrayList<CharSequence>();
			Set<String> results = DistSmartFridge.this.getItems();
			
			for (String item : results) {
				items.add(item);
			}
			return items;
		}

		@Override
		public Void openFridgeRemote() throws AvroRemoteException {
			DistSmartFridge.this.openFridge();
			return null;
		}

		@Override
		public Void closeFridgeRemote() throws AvroRemoteException {
			DistSmartFridge.this.closeFridge();
			DistSmartFridge.this.stopUserServer();
			return null;
		}

		@Override
		public Void registerUserIP(CharSequence userIP, int userPort) throws AvroRemoteException {
			f_userConnection = new ConnectionData(userIP.toString(), userPort);
			System.out.println(f_userConnection.toString());
			f_safeToClose = true;
			return null;
		}
	}
	
	
	
	/// =============================
	/// =========REPLICATION=========
	/// =============================
	
	
	/**
	 * Starts an election with all the other users/smartfridges.
	 */
	private void startElection() {
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		boolean otherCandidates = false;
		for (ClientType clientType : clientTypes) {
			if (clientType == ClientType.SmartFridge || clientType == ClientType.User) {
				otherCandidates = true;
				break;
			}
		}
		if (otherCandidates == false) {
			this.sendNonCandidatesNewServer();
			
			// TODO START CONTROLLER SERVER HERE
			
			return;
		}
		
		f_electionID = this.getElectionIndex();
		f_isParticipantElection = true;
		
		final ConnectionData nextCandidate = this.getNextCandidate();
		new Thread() {
			public void run() {				
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					ControllerCandidate proxy = 
							(ControllerCandidate) SpecificRequestor.getClient(ControllerCandidate.class, transceiver);
					proxy.electNewController(f_electionID, getID());
					transceiver.close();
				} catch (IOException e) {
					// TODO handle this more appropriately
					System.err.println("IOException at startElection() in DistSmartFridge.");
				}
			}
		}.start();
		
	}
	
	
	/**
	 * Gets the ConnectionData of the next client in the ring.
	 * @return
	 * 		The ConnectionData of the next client in the ring.
	 */
	private ConnectionData getNextCandidate() {
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
		
		String nextIP = clientIPsIP.get( clientIPsID.indexOf(participantIDs.get(f_electionID+1)) ).toString();
		int nextPort = participantIDs.get(f_electionID+1);
		return new ConnectionData(nextIP, nextPort);
	}
	
	
	/**
	 * Gets the index of the participant in the election, according to the data provided by replication.
	 * @return
	 * 		The index of this client in the election process.
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
		new Thread() {
			public void run() {				
				try {
					ConnectionData nextCandidate = DistSmartFridge.this.getNextCandidate();
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					ControlMessages proxy = 
							(ControlMessages) SpecificRequestor.getClient(ControlMessages.class, transceiver);
					proxy.newServer(f_ownIP, DistSmartFridge.this.getID());
					transceiver.close();
				} catch (AvroRemoteException e) {
					// TODO handle this more appropriately
					System.err.println("AvroRemoteException at sendSelfElectedNextCandidate() in DistSmartFridge.");
				} catch (IOException e) {
					// TODO handle this more appropriately
					System.err.println("IOException at sendSelfElectedNextCandidate() in DistSmartFridge.");
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
			
			try {
				Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(clientIP, clientPort));
				ControlMessages proxy = 
						(ControlMessages) SpecificRequestor.getClient(ControlMessages.class, transceiver);
				proxy.newServer(f_ownIP, this.getID());
				transceiver.close();
			} catch (AvroRemoteException e) {
				// TODO handle this more appropriately
				System.err.println("AvroRemoteException at sendNonCandidatesNewServer() in DistSmartFridge.");
			} catch (IOException e) {
				// TODO handle this more appropriately
				System.err.println("IOException at sendNonCandidatesNewServer() in DistSmartFridge.");
			}
		}
	}

	
	private void startControllerTakeOver() {
		this.stopServerController();
		if (this.f_userServerConnection != null) {
			this.closeFridge();
			this.stopUserServer();
		}
		this.f_userServerConnection = null;
		
		new Thread() {
			public void run() {
				DistSmartFridge.this.f_replicatedServerData.setPort(DistSmartFridge.this.getID());
				DistSmartFridge.this.f_replicatedServerData.setIp(DistSmartFridge.this.f_ownIP);
				DistSmartFridge.this.f_controller = new DistController(DistSmartFridge.this.f_replicatedServerData);
				while (DistSmartFridge.this.f_controller.serverIsActive() == true) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) { }
				}
				DistSmartFridge.this.f_controller = null;
				DistSmartFridge.this.startControllerServer();
			}
		}.start();
	}
	
	
	
	public static void main(String[] args) {
		// DistController controller = new DistController(6789, 10);
		
		DistSmartFridge remoteFridge = 
				new DistSmartFridge(System.getProperty("clientip"), System.getProperty("ip"), 5000);

		try {
			System.in.read();
		} catch (IOException e) {

		}

		remoteFridge.logOffController();
		remoteFridge.stopServerController();
		System.exit(0);
		
		// try {
		// 	Logger logger = Logger.getLogger();
		// 	logger.f_active = true;
		// 	logger.log(remoteFridge.toString());
		
		// 	Transceiver transceiverController = new SaslSocketTransceiver(new InetSocketAddress(6790));
		// 	communicationFridge proxyController = (communicationFridge) SpecificRequestor.getClient(communicationFridge.class, transceiverController);
			
		// 	proxyController.requestFridgeCommunication(15000);
			
		// 	Transceiver transceiverUser = new SaslSocketTransceiver(new InetSocketAddress(15000));
		// 	communicationFridgeUser proxyUser = (communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiverUser);
			
		// 	proxyUser.openFridgeRemote();
		// 	proxyUser.addItemRemote("bacon");
		// 	proxyUser.addItemRemote("parmesan cheese");
			
		// 	List<CharSequence> items = proxyController.getItemsRemote();
		// 	logger.log("Items retrieved from remote function in controller: ");
		// 	for (CharSequence item : items) {
		// 		logger.log("\t" + item.toString());
		// 	}
						
		// 	proxyUser.addItemRemote("milk");
		// 	items = proxyUser.getItemsRemote();
		// 	logger.log("");
		// 	logger.log("Items retrieved from remote function in user: ");
		// 	for (CharSequence item : items) {
		// 		logger.log("\t" + item.toString());
		// 	}
		// 	logger.log("");
			
		// 	proxyUser.closeFridgeRemote();
		// 	transceiverUser.close();
			
			
		// 	if (remoteFridge.logOffController() == true) {
		// 		logger.log("Logged off succesfully.");
		// 	}
		// 	else {
		// 		logger.log("Could not log off.");
		// 	}
		// 	remoteFridge.stopServerController();
		// 	transceiverController.close();
		// }
		// catch (AvroRemoteException e) {
		// 	System.err.println("AvroRemoteException at main class in DistSmartFridge.");
		// } catch (IOException e) {
		// 	System.err.println("IOException at main class in DistSmartFridge.");
		// }
		// System.exit(0);
	}

}
