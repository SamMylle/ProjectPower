package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.avro.AvroRemoteException;

import avro.ProjectPower.ClientType;
import avro.ProjectPower.ControllerComm;
import avro.ProjectPower.communicationFridge;
import avro.ProjectPower.communicationFridgeUser;

import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import controller.DistController;

import util.Logger;




public class DistSmartFridge extends SmartFridge {

	private int f_controllerPort;					// The port on which the controller runs
	private int f_userPort;
	private boolean f_serverControllerReady;		// Boolean used to determine if the server has been finished setting up.
	private boolean f_serverUserReady;
	private boolean f_userConnected;
	
	private Server f_fridgeServer;					// The server for the SmartFridge itself
	private Thread f_smartfridgeThread;				// The thread used to run the server and handle the requests it gets.
	private Server f_userServer;					// The server object, which should be used by the user, not the controller
	private Thread f_fridgeuserThread;				// The thread used to run the smartfridge server, destined to be used by the user
	
	
	
	
	
	/**
	 * Constructor for DistSmartFridge
	 * 
	 * Constructs the DistSmartFridge object (in specified order):
	 * 		- Gets an ID from the controller, using the given argument @controllerPort
	 * 		- Starts a SmartFridge server on port=ID
	 * 
	 * @param controllerPort
	 * 	 the port on which the controller server is running
	 * 
	 */
	public DistSmartFridge(int controllerPort) {
		assert controllerPort > 1000; // the controller should not be running on a port lower (or equal) than 1000
		
		f_controllerPort = controllerPort;
		f_userPort = -1;
		f_serverControllerReady = false;
		f_serverUserReady = false;
		f_fridgeuserThread = null;
		f_userServer = null;
		f_userConnected = false;
		
		this.setupID();
		this.startServer();
	}
	
	/**
	 * Summary: gets an ID for the SmartFridge, requesting one from the controller.
	 */
	private void setupID() {
		try {
			Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) 
					SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.getID(SmartFridge.type));
			transceiver.close();
		}
		catch (IOException e) {
			System.err.println("Error connecting to the controller server, the port number might be wrong.");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
	
	/**
	 * Summary: logs off at the controller
	 * 
	 * @return  success of logging off.
	 */
	public boolean logOffController() {
		try {
			Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
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
	 * Summary: starts a thread, which runs the DistSmartFridge server for the controller. Sleeps until the server is functional.
	 */
	private void startServer() {
		f_smartfridgeThread = new Thread(new controllerServer());
		f_smartfridgeThread.start();
		while (f_serverControllerReady == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Summary: interrupts the thread which runs the DistSmartFridge server for the controller, forcing it to close.
	 */
	public void stopServerController() {
		f_smartfridgeThread.interrupt();
		f_smartfridgeThread = null;		
	}
	
	/**
	 * Summary: Notifies the controller that the inventory of the fridge is empty.
	 */
	public void notifyControllerEmptyInventory() {
		try {
			Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
//			proxy.notifyFridgeEmpty(this.getID());	//TODO add method when implemented in controller
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
	 * Class used to run the DistSmartFridge server used by the Controller
	 * 
	 * This class implements all the methods that the controller needs, 
	 * 		aswell as running the thread for the DistSmartFridge server respectively.
	 */
	public class controllerServer implements Runnable, communicationFridge {
		
		controllerServer() { }
		
		@Override
		public void run() {
			try {
				f_fridgeServer = new SaslSocketServer(
						new SpecificResponder(communicationFridge.class, this), new InetSocketAddress(getID()) );
				f_fridgeServer.start();
			}
			catch (IOException e) {
				System.err.println("Failed to start the SmartFridge server for the controller.");
				e.printStackTrace(System.err);
				System.exit(1);
			}
			f_serverControllerReady = true;
			try {
				f_fridgeServer.join();
			}
			catch (InterruptedException e) {
				f_fridgeServer.close();
			}
		}

		@Override
		public List<CharSequence> getItemsRemote() throws AvroRemoteException {
			List<CharSequence> items = new ArrayList<CharSequence>();
			
			Set<String> fridgeItems = getItems();
			
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
		public boolean requestFridgeCommunication(int userServerPort) throws AvroRemoteException {
			if (f_userConnected == true) {
				return false;
			}
			f_userPort = userServerPort;
			startUserServer();
			f_userConnected = true;
			return true;
		}
	}
	
	
	public void startUserServer() {
		f_fridgeuserThread = new Thread(new userServer());
		f_fridgeuserThread.start();
		
		
		while (f_serverUserReady == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Class used to run the DistSmartFridge server used by the User
	 * 
	 * This class implements all the methods that the user needs, 
	 * 		aswell as running the thread for the DistSmartFridge server respectively.
	 */
	class userServer implements Runnable, communicationFridgeUser {

		userServer() { }
		
		@Override
		public void run() {
			try {
				//TODO remove magic number
				f_userServer = new SaslSocketServer(
						new SpecificResponder(communicationFridgeUser.class, this), new InetSocketAddress(f_userPort) );
				f_userServer.start();
			}
			catch (IOException e) {
				System.err.println("Failed to start the SmartFridge server for the user.");
				e.printStackTrace(System.err);
				System.exit(1);
			}
			f_serverUserReady = true;
			try {
				f_userServer.join();
			}
			catch (InterruptedException e) {
				f_userServer.close();
			}
		}
		
		@Override
		public boolean openFridgeRemote() throws AvroRemoteException {
			openFridge();
			Logger logger = Logger.getLogger();
			
			logger.f_active = true;
			logger.log("The fridge has been opened.");
			return true;
		}
		
		@Override
		public boolean closeFridgeRemote() throws AvroRemoteException {
			closeFridge();
			this.stopUserServer();
			
			Logger logger = Logger.getLogger();
			logger.f_active = true;
			logger.log("The fridge has been closed.");
			return true;
		}

		
		@Override
		public Void addItemRemote(CharSequence itemName)
				throws AvroRemoteException {
			addItem(itemName.toString());
			return null;
		}
	
		@Override
		public Void removeItemRemote(CharSequence itemName)
				throws AvroRemoteException {
			removeItem((String) itemName);
			
			if (emptyInventory() == true) {
				notifyControllerEmptyInventory();
			}
			
			return null;
		}

		@Override
		public List<CharSequence> getItemsRemote() throws AvroRemoteException {
			List<CharSequence> items = new ArrayList<CharSequence>();
			
			Set<String> fridgeItems = getItems();
			
			for (String item : fridgeItems) {
				items.add(item);
			}
			return items;
		}
		
		public void stopUserServer() {
			f_fridgeuserThread.interrupt();
			f_fridgeuserThread = null;
		}
	}
	

	public static void main(String[] args) {
		DistController controller = new DistController(6789, 10);
		
		DistSmartFridge remoteFridge = new DistSmartFridge(6789);
		
		try {
			Logger logger = Logger.getLogger();
			logger.f_active = true;
			logger.log(remoteFridge.toString());
		
			Transceiver transceiverController = new SaslSocketTransceiver(new InetSocketAddress(6790));
			communicationFridge proxyController = (communicationFridge) SpecificRequestor.getClient(communicationFridge.class, transceiverController);
			
			proxyController.requestFridgeCommunication(15000);
			
			Transceiver transceiverUser = new SaslSocketTransceiver(new InetSocketAddress(15000));
			communicationFridgeUser proxyUser = (communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiverUser);
			
			proxyUser.openFridgeRemote();
			proxyUser.addItemRemote("bacon");
			proxyUser.addItemRemote("parmesan cheese");
			
			List<CharSequence> items = proxyController.getItemsRemote();
			logger.log("Items retrieved from remote function in controller: ");
			for (CharSequence item : items) {
				logger.log("\t" + item.toString());
			}
						
			proxyUser.addItemRemote("milk");
			items = proxyUser.getItemsRemote();
			logger.log("");
			logger.log("Items retrieved from remote function in user: ");
			for (CharSequence item : items) {
				logger.log("\t" + item.toString());
			}
			logger.log("");
			
			proxyUser.closeFridgeRemote();
			transceiverUser.close();
			
			
			if (remoteFridge.logOffController() == true) {
				logger.log("Logged off succesfully.");
			}
			else {
				logger.log("Could not log off.");
			}
			remoteFridge.stopServerController();
			transceiverController.close();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at main class in DistSmartFridge.");
		} catch (IOException e) {
			System.err.println("IOException at main class in DistSmartFridge.");
		}
		System.exit(0);
	}

}
