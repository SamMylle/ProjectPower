package client;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.avro.AvroRemoteException;

import avro.ProjectPower.ClientType;
import avro.ProjectPower.ControllerComm;
import avro.ProjectPower.LightComm;
import avro.ProjectPower.ServerData;
import avro.ProjectPower.communicationFridge;
import avro.ProjectPower.communicationFridgeUser;
import avro.ProjectPower.communicationTempSensor;
import avro.ProjectPower.communicationUser;

import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import client.util.ConnectionData;
import client.util.ConnectionTypeData;
import controller.DistController;
import util.ServerDataUnion;



public class DistSmartFridge extends SmartFridge {

	private String f_ownIP;									// The IP address of the client itself
	
	private ConnectionData f_controllerConnection;			// Data of the current connection to the controller of the system
	private ConnectionData f_userServerConnection;			// Data of the current connection to the own user server
	private ConnectionData f_userConnection;				// Data of the current connection to the user which is using the fridge
	
	private boolean f_controllerServerReady;				// Boolean used to determine if the server for the controller has finished setting up.
	private boolean f_userServerReady;						// Boolean used to determine if the server for the user has finished setting up.
	private boolean f_safeToCloseUserServer;				// Boolean used to check if it is safe to close the user server
	
	private Server f_fridgeControllerServer;				// The server for the SmartFridge itself
	private Thread f_fridgeControllerThread;				// The thread used to run the server and handle the requests it gets.
	private Server f_fridgeUserServer;						// The server for the User
	private Thread f_fridgeUserThread;						// The thread used to run the userServer
	
	
	/// FAULT TOLERENCE & REPLICATION
	private ServerData f_replicatedServerData;				// The replicated data from the DistController
	private DistController f_controller;					// DistController to be used when this object is elected
	
	private boolean f_isParticipantElection;				// Equivalent to participant_i in slides
	private int f_electionID;								// The index of the client in the election
	private boolean f_electionBusy;
	private boolean f_requestedUnion;
	private Timer f_waitForController;
	
	private int f_WAITPERIOD;
	
	
	
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
		
		f_controllerServerReady = false;
		f_userServerReady = false;
		f_safeToCloseUserServer = true;
		
		f_fridgeControllerServer = null;
		f_fridgeControllerThread = null;
		f_fridgeUserServer = null;
		f_fridgeUserThread = null;
		
		f_replicatedServerData = null;
		f_controller = null;
		f_electionID = -1;
		f_isParticipantElection = false;
		f_electionBusy = false;
		f_WAITPERIOD = 1500;
		
		this.setupIDInitial();
		this.startControllerServer();
	}
	
	/**
	 * Summary: gets an ID for the SmartFridge, requesting one from the controller.
	 */
	private void setupIDInitial() {
		try {
			Transceiver transceiver = 
					new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
					(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.LogOn(SmartFridge.type, f_ownIP));
			transceiver.close();
		}
		catch (IOException e) {
			System.err.println("Error connecting to the controler at startup... aborting.");
			System.exit(1);
		}
	}
	
	/**
	 * Summary: gets an ID for the SmartFridge, requesting one from the controller.
	 */
	private void setupID() {
		try {
			Transceiver transceiver = 
					new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = 
					(ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.LogOn(SmartFridge.type, f_ownIP));
			transceiver.close();
		}
		catch (IOException e) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
			}
			this.setupID();
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
		catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Starts a thread, which runs the DistSmartFridge server for the controller. Sleeps until the server is functional.
	 */
	private void startControllerServer() {
		f_fridgeControllerThread = new Thread(new controllerServer());
		f_fridgeControllerThread.start();
		while (f_controllerServerReady == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
		}
		this.notifySuccessfulLogin();
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
		
		while (f_fridgeControllerServer != null) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
		}
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
	 * Notifies the controller of successful login.
	 */
	private void notifySuccessfulLogin() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(f_controllerConnection.toSocketAddress());
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.loginSuccessful(this.getID());
			transceiver.close();
		} catch (IOException e) {
			synchronized(this) {
				if (f_electionBusy == false && f_waitForController == null) {
					this.startPollTimer(f_WAITPERIOD);
				}				
			}
		}
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
			proxy.fridgeIsEmpty(this.getID());
			transceiver.close();
		}
		catch (IOException e) {
			synchronized(this) {
				if (f_electionBusy == false && f_waitForController == null) {
					this.startPollTimer(f_WAITPERIOD);
				}				
			}
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
		} catch (IOException e) {
			synchronized(this) {
				if (f_electionBusy == false && f_waitForController == null) {
					this.startPollTimer(f_WAITPERIOD);
				}				
			}
		}
	}
	
	/**
	 * Tests if the user is still connected to the fridge.
	 */
	private boolean userConnected() {
		if (f_userConnection == null) {
			return false;
		} else if (f_userConnection.equals(f_controllerConnection)) {
			return false;
		}
		
		try {
			Transceiver transceiver = new SaslSocketTransceiver(f_userConnection.toSocketAddress());
			communicationUser proxy = (communicationUser) SpecificRequestor.getClient(communicationUser.class, transceiver);
			boolean status = proxy.aliveAndKicking();
			transceiver.close();
			return status;
		} catch (IOException e) {
			this.stopUserServer();
			return false;
		}
	}
	
	
	
	/**
	 * Class used to run the DistSmartFridge server used by the Controller
	 * 
	 * This class implements all the methods that the controller needs, 
	 * 		as well as running the thread for the DistSmartFridge server respectively.
	 */
	private class controllerServer implements Runnable, communicationFridge {
		
		controllerServer() { }
		
		@Override
		public void run() {
			/// repeat until the server can bind to a valid port
			while (f_controllerServerReady == false) {
				try {
					f_fridgeControllerServer = new SaslSocketServer(
							new SpecificResponder(communicationFridge.class, this), new InetSocketAddress(f_ownIP, getID()) );
					f_fridgeControllerServer.start();
					
					if (f_waitForController != null) {
						f_waitForController.cancel();
						f_waitForController = null;
					}
					
					
					f_controllerServerReady = true;
				}
				catch (BindException e) {
					getNewID();
				}
				catch (IOException e) {
					System.err.println("Failed to start the SmartFridge server for the controller.");
					System.exit(1);
				}
			}
			
			try {
				f_fridgeControllerServer.join();
			}
			catch (InterruptedException e) {
				f_fridgeControllerServer.close();
				f_fridgeControllerServer = null;
				f_controllerServerReady = false;
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
		public int requestFridgeCommunication(int userServerPort) throws AvroRemoteException {
			if (DistSmartFridge.this.userConnected() == true) {
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
			if (f_waitForController != null) {
				f_waitForController.cancel();
				f_waitForController = null;			
			}
			return true;
		}
		
		/**
		 * Sets new connection data for the controller.
		 */
		@Override
		public Void newServer(CharSequence newServerIP, int newServerID) {
			f_controllerConnection = new ConnectionData(newServerIP.toString(), newServerID);
			if (f_waitForController != null) {
				f_waitForController.cancel();
				f_waitForController = null;
			}
			return null;
		}
		
		
		@Override
		public void unifyServerData(ServerData serverData) {
			if (ServerDataUnion.narrowEquals(f_replicatedServerData, serverData) == true && f_requestedUnion == true) {
				DistSmartFridge.this.startElection();
				return;
			}
			f_replicatedServerData = ServerDataUnion.getUnion(f_replicatedServerData, serverData);
			
			f_electionID = DistSmartFridge.this.getElectionIndex();
			f_electionBusy = true;
			
			new Thread() {
				public void run() {
					
					ConnectionTypeData nextCandidate = DistSmartFridge.this.getNextCandidateConnection(false);
					try {
						Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
						if (nextCandidate.getType() == ClientType.SmartFridge) {
							communicationFridge proxy = (communicationFridge) 
									SpecificRequestor.getClient(communicationFridge.class, transceiver);
							proxy.unifyServerData(f_replicatedServerData);
						} else if (nextCandidate.getType() == ClientType.User) {
							communicationUser proxy = (communicationUser) 
									SpecificRequestor.getClient(communicationUser.class, transceiver);
							proxy.unifyServerData(f_replicatedServerData);
						}
						transceiver.close();
						return;
					} catch (IOException e) {
						DistSmartFridge.this.cleanupElection();
					} catch (Exception e) {
						DistSmartFridge.this.wonElection(false);
						return;
					}
				}
			}.start();
		}
		

		/**
		 * Equivalent to elected function from slides theory (slide 54 - Coordination)
		 * @param newServerIP The IP address of the newly elected controller.
		 * @param newServerID The Port of the newly elected controller.
		 */
		@Override
		synchronized public void newServerElected(final CharSequence newServerIP, final int newServerID) {
			new Thread() {
				public void run() {
					f_controllerConnection = new ConnectionData(newServerIP.toString(), newServerID);
					f_isParticipantElection = false;
					ConnectionTypeData nextCandidate = DistSmartFridge.this.getNextCandidateConnection(true);
					if (nextCandidate.getType() == null) {
						DistSmartFridge.this.cleanupElection();
						return;
					}
					
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
						
					} catch (Exception e) {
					} finally {
						DistSmartFridge.this.cleanupElection();
					}
				}
				
			}.start();
			
			return;
		}
		
		/**
		 * Equivalent to election function from slides theory (slide 54 - Coordination)
		 * @param index
		 * 		The client index in the election.
		 * @param clientID
		 * 		The ID of the client that is currently the highest.
		 */
		@Override
		synchronized public void electNewController(final int index, final int clientID) {
			new Thread() {
				
				public void run() {
					if (f_waitForController != null) {
						f_waitForController.cancel();
						f_waitForController = null;
					}
					f_electionID = DistSmartFridge.this.getElectionIndex();
					f_electionBusy = true;
					
					if (index == f_electionID) {
						// Send newServer to all the clients who did not participate in the election, and only to the next client who was involved in the election
						// This is in order to fully replicate the algorithm described in the theory.
						DistSmartFridge.this.wonElection(true);
						return;
					}
					ConnectionTypeData nextCandidate = DistSmartFridge.this.getNextCandidateConnection(false);
					if (clientID > DistSmartFridge.this.getID()) {
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
							DistSmartFridge.this.wonElection(false);
							return;
						} catch (Exception e) {
						}
						
						
					} else if (clientID <= DistSmartFridge.this.getID()) {
						if (DistSmartFridge.this.f_isParticipantElection == false) {
							DistSmartFridge.this.f_isParticipantElection = true;
							Transceiver transceiver = null;
							try {
								transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
								if (nextCandidate.getType() == ClientType.SmartFridge) {
									communicationFridge proxy = (communicationFridge) 
											SpecificRequestor.getClient(communicationFridge.class, transceiver);
									proxy.electNewController(DistSmartFridge.this.f_electionID, DistSmartFridge.this.getID());
								} else if (nextCandidate.getType() == ClientType.User) {
									communicationUser proxy = (communicationUser) 
											SpecificRequestor.getClient(communicationUser.class, transceiver);
									proxy.electNewController(DistSmartFridge.this.f_electionID, DistSmartFridge.this.getID());
								}
								transceiver.close();
								return;
							} catch (IOException e) {
							} catch (NullPointerException e) {
								DistSmartFridge.this.wonElection(false);
								return;
							} catch (Exception e) {
							}
						}
					}
				}
				
			}.start();
		}

		/**
		 * Makes a backup of the serverdata (replication).
		 * @param data The data which needs to be backed up.
		 */
		@Override
		public void makeBackup(ServerData data) {
			if (f_electionBusy == true) {
				return;
			}
			if (f_waitForController != null) {
				f_waitForController.cancel();
				f_waitForController = null;
			}
			f_replicatedServerData = data;
			f_WAITPERIOD = 1000 + (250 * f_replicatedServerData.getNamesID().size());
		}

		/**
		 * Gets a new login from the controller, and restarts the server on the potentially new port
		 */
		@Override
		public void reLogin() {
			DistSmartFridge.this.stopServerController();
			DistSmartFridge.this.setupID();
			DistSmartFridge.this.startControllerServer();
		}
	}
	

	/**
	 * Starts the user server with own IP and legal Port (port is not guaranteed to be consistent every call)
	 */
	private void startUserServer() {
		f_fridgeUserThread = new Thread(new UserServer());
		f_fridgeUserThread.start();
		
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
		if (f_fridgeUserThread != null) {
			f_fridgeUserThread.interrupt();
			f_fridgeUserThread = null;
		}
		
		while (f_fridgeUserServer != null) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) { }
		}
	}
	
	private void notifyUserClosingFridge() {
		if (f_userConnection != null) {
			try {
				Transceiver transceiver = new SaslSocketTransceiver(f_userConnection.toSocketAddress());
				communicationUser proxy = (communicationUser) 
						SpecificRequestor.getClient(communicationUser.class, transceiver);
				proxy.notifyFridgeClosed();
				transceiver.close();
			} catch (IOException e) {
				f_userConnection = null;
			}			
		}
	}
	
	private class UserServer implements Runnable, communicationFridgeUser {

		public UserServer() {}
		
		@Override
		public void run() {
			while (f_userServerReady == false) {
				try {
					f_fridgeUserServer = new SaslSocketServer(
							new SpecificResponder(communicationFridgeUser.class, this), f_userServerConnection.toSocketAddress());
					f_fridgeUserServer.start();
					f_userServerReady = true;
				} catch (BindException e) {
					f_userServerConnection.setPort(f_userServerConnection.getPort()-1);
				} catch (IOException e) {
				}
			}
			
			try {
				f_fridgeUserServer.join();
			} catch (NullPointerException e) {
				f_fridgeUserServer = null;
				f_userServerReady = false;
				f_safeToCloseUserServer = false;
			} catch (Exception e) {
				while (f_safeToCloseUserServer == false) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) { }
				}
				try {
					Thread.sleep(50);					
				} catch (InterruptedException e1) { }
				f_fridgeUserServer.close();
				f_fridgeUserServer = null;
				f_userServerReady = false;
				f_safeToCloseUserServer = false;
			}
		}
		
		@Override
		public Void addItemRemote(CharSequence itemName) throws AvroRemoteException {
			DistSmartFridge.this.addItem(itemName.toString());
			return null;
		}

		@Override
		public Void removeItemRemote(CharSequence itemName) throws AvroRemoteException {
			if (DistSmartFridge.this.removeItem(itemName.toString()) == false) {
				return null;
			}
			
			// if the method returned true, check if the fridge is empty
			if (DistSmartFridge.this.emptyInventory() == true) {
				DistSmartFridge.this.notifyControllerEmptyInventory();
			}
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
			f_safeToCloseUserServer = true;
			return null;
		}
	}
	


	
	
	private class controllerPollTimer extends TimerTask {
		public controllerPollTimer() {}

		@Override
		public void run() {
			if (f_electionBusy == false && f_controllerServerReady == true) {
				DistSmartFridge.this.setupElection();
			}
			this.cancel();
			f_waitForController = null;
		}
	}
	
	
	private void startPollTimer(int interval) {
		f_waitForController = new Timer();
		f_waitForController.schedule(new controllerPollTimer(), interval, 100000);
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
				
				ConnectionTypeData nextCandidate = DistSmartFridge.this.getNextCandidateConnection(false);
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					if (nextCandidate.getType() == ClientType.SmartFridge) {
						communicationFridge proxy = (communicationFridge) 
								SpecificRequestor.getClient(communicationFridge.class, transceiver);
						proxy.unifyServerData(f_replicatedServerData);
					} else if (nextCandidate.getType() == ClientType.User) {
						communicationUser proxy = (communicationUser) 
								SpecificRequestor.getClient(communicationUser.class, transceiver);
						proxy.unifyServerData(f_replicatedServerData);
					}
					transceiver.close();
				} catch (IOException e) {
					// do nothing, just try again
				} catch (NullPointerException e) {
					DistSmartFridge.this.wonElection(false);
				} catch (Exception e) {
					DistSmartFridge.this.cleanupElection();
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
			this.wonElection(false);
			return;
		}
		
		if (this.getNextCandidateConnection(false) == null) {
			// this means that no other candidate was reachable => start controller in this client
			this.wonElection(false);
			return;
		}
		
		new Thread() {
			
			public void run() {
				ConnectionTypeData nextCandidate = DistSmartFridge.this.getNextCandidateConnection(false);
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					if (nextCandidate.getType() == ClientType.SmartFridge) {
						communicationFridge proxy = (communicationFridge) 
								SpecificRequestor.getClient(communicationFridge.class, transceiver);
						proxy.electNewController(DistSmartFridge.this.f_electionID, DistSmartFridge.this.getID());
					} else if (nextCandidate.getType() == ClientType.User) {
						communicationUser proxy = (communicationUser) 
								SpecificRequestor.getClient(communicationUser.class, transceiver);
						proxy.electNewController(DistSmartFridge.this.f_electionID, DistSmartFridge.this.getID());
					}
					transceiver.close();
					return;
				} catch (NullPointerException e) {
					DistSmartFridge.this.wonElection(false);
				} catch (Exception e) {
					DistSmartFridge.this.cleanupElection();
				}
			}
			
		}.start();
		
	}
	
	
	/**
	 * Gets the ConnectionTypeData of the next client in the ring (IP, Port, Type).
	 * @param checkNewController If true: checks if next client is the new controller, sets type to null if this is the case
	 * @return The ConnectionTypeData of the next client in the ring (that is accessible).
	 */
	private ConnectionTypeData getNextCandidateConnection(boolean checkNewController) {
		HashMap<Integer, ClientType> participants = new HashMap<Integer, ClientType>();
		List<Integer> participantIDs = new Vector<Integer>();
		
		List<Integer> clientIDs = f_replicatedServerData.getNamesID();
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		List<Integer> clientIPsID = f_replicatedServerData.getIPsID();
		List<CharSequence> clientIPsIP = f_replicatedServerData.getIPsIP();
		
		for (int i = 0; i < clientIDs.size(); i ++) {
			if (clientTypes.get(i) == ClientType.User || clientTypes.get(i) == ClientType.SmartFridge) {
				participants.put(clientIDs.get(i), clientTypes.get(i));
				participantIDs.add(clientIDs.get(i));
			}
		}
		
		Integer nextCandidateID = new Integer(-1);
		String nextIP = "";
		ClientType type = null;
		try {
			nextCandidateID = participantIDs.get((f_electionID+1) % participantIDs.size());
			nextIP = clientIPsIP.get( clientIPsID.indexOf(nextCandidateID) ).toString();
			type = participants.get(nextCandidateID);
			ConnectionData nextCandidate = new ConnectionData(nextIP, nextCandidateID.intValue());
			
			if (nextCandidate.equals(f_controllerConnection) && checkNewController) {
				return new ConnectionTypeData(nextCandidate.getIP(), nextCandidate.getPort(), null);
			}
		} catch (Exception e) {
			return null;
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
		final ConnectionTypeData nextCandidate = this.getNextCandidateConnection(false);
		if (nextCandidate == null) {
			return;
		}
		
		new Thread() {
			public void run() {				
				try {
					Transceiver transceiver = new SaslSocketTransceiver(nextCandidate.toSocketAddress());
					if (nextCandidate.getType() == ClientType.SmartFridge) {
						communicationFridge proxy = (communicationFridge) 
								SpecificRequestor.getClient(communicationFridge.class, transceiver);
						proxy.newServerElected(f_ownIP, DistSmartFridge.this.getID());
					} else if (nextCandidate.getType() == ClientType.User) {
						communicationUser proxy = (communicationUser) 
								SpecificRequestor.getClient(communicationUser.class, transceiver);
						proxy.newServerElected(f_ownIP, DistSmartFridge.this.getID());
					}
					transceiver.close();
				} catch (Exception e) {
					return;
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
		
		for (int i = 0; i < clientIDs.size(); i ++) {
			if (clientTypes.get(i) == ClientType.Light || clientTypes.get(i) == ClientType.TemperatureSensor) {
				nonParticipants.put(clientIDs.get(i), clientTypes.get(i));
			}
		}
		
		// This part is not asynchronous, since it is not really part of the Chang-Roberts algorithm
		Iterator<Entry<Integer, ClientType>> it = nonParticipants.entrySet().iterator();
		while (it.hasNext() == true) {
			Map.Entry<Integer, ClientType> pair = (Entry<Integer, ClientType>)it.next();
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
				// skip the client if it cannot be reached, gets handled in controller later on
			} 
		}
	}
	
	
	synchronized private void removeFromReplicatedData(int clientID) {
		List<Integer> clientIDs = f_replicatedServerData.getNamesID();
		List<ClientType> clientTypes = f_replicatedServerData.getNamesClientType();
		List<Integer> clientIPsID = f_replicatedServerData.getIPsID();
		List<CharSequence> clientIPsIP = f_replicatedServerData.getIPsIP();
		
		int namesIndex = clientIDs.indexOf(new Integer(clientID));
		if (namesIndex == -1) {
			return;
		}
		clientIDs.remove(namesIndex);
		clientTypes.remove(namesIndex);
		
		int IPsIDIndex = clientIPsID.indexOf(new Integer(clientID));
		if (IPsIDIndex == -1) {
			return;
		}
		clientIPsID.remove(IPsIDIndex);
		clientIPsIP.remove(IPsIDIndex);
		
		f_replicatedServerData.setNamesID(clientIDs);
		f_replicatedServerData.setNamesClientType(clientTypes);
		f_replicatedServerData.setIPsID(clientIPsID);
		f_replicatedServerData.setIPsIP(clientIPsIP);
		
		if (f_electionID == 0) {
			f_electionID = clientIDs.size() - 1;
		} else {
			f_electionID = f_electionID - 1;			
		}
	}
	

	/**
	 * Function called when this client has won the election.
	 * Sends all the non participants a notification that this client is the new controller, aswell as starting the controller and cleaning up the election.
	 * @param sendNext True = notify next participant that you are elected
	 */
	private void wonElection(boolean sendNext) {
		if (f_waitForController != null) {
			f_waitForController.cancel();
			f_waitForController = null;
		}
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
		if (this.f_userServerConnection.getPort() != -1) {
			this.closeFridge();
			
			/// notify the user that the direct connection should be stopped, if the user has send his address already, too bad otherwise
			this.notifyUserClosingFridge();
			this.stopUserServer();
		}
		this.stopServerController();
		this.f_userServerConnection.setPort(-1);;
		
		new Thread() {
			public void run() {
				DistSmartFridge.this.f_replicatedServerData.setPort(DistSmartFridge.this.getID());
				DistSmartFridge.this.f_replicatedServerData.setIp(DistSmartFridge.this.f_ownIP);
				DistSmartFridge.this.removeFromReplicatedData(DistSmartFridge.this.getID());
				
				DistSmartFridge.this.f_controller = new DistController(DistSmartFridge.this.f_replicatedServerData);
				while (DistSmartFridge.this.f_controller.serverIsActive() == true) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) { }
				}
				DistSmartFridge.this.setupID();
				DistSmartFridge.this.startControllerServer();
				DistSmartFridge.this.cleanupElection();
				DistSmartFridge.this.f_controller = null;
			}
		}.start();

		while(f_controller == null){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) { }
		}
		
		while (DistSmartFridge.this.f_controller.serverIsActive() == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) { }
		}
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
		
		DistSmartFridge fridge = new DistSmartFridge(clientIP, serverIP, controllerPort);

		try {
			System.in.read();
		} catch (IOException e) {
		}

		fridge.disconnect();
		System.exit(0);
	}

}
