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
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import controller.DistController;

import util.Logger;




public class DistSmartFridge 
	extends SmartFridge
	implements communicationFridge, Runnable {

	private int f_controllerPort;					// The port on which the controller runs
	private boolean f_isReady;						// Boolean used to determine if the server has been finished setting up.
	
	private Server f_fridgeServer;					// The server for the SmartFridge itself
	private Thread f_smartfridgeThread;				// The thread used to run the server and handle the requests it gets.

	
	// TODO decide what to for user/controller, in terms of different or shared fridge servers
	// Used for communication between a user and the fridge, without the controller in between
	private Server f_userServer;					// The server object, which should be used by the user, not the controller
	private Thread f_fridgeuserThread;				// The thread used to run the smartfridge server, destined to be used by the user
	
	
	
	
	
	/*
	 * Constructor for DistSmartFridge
	 * 
	 * Constructs the DistSmartFridge object (in specified order):
	 * 		- Sets up a transceiver to communicate with the controller
	 * 		- Gets an ID from the controller
	 * 		- Starts a SmartFridge server on port=ID
	 * 
	 * @param int controllerPort
	 * 	 the port on which the controller server is running
	 * 
	 */
	public DistSmartFridge(int controllerPort) {
		assert controllerPort > 1000; // the controller should not be running on a port lower (or equal) than 1000
		
		f_controllerPort = controllerPort;
		f_isReady = false;
		f_fridgeuserThread = null;
		f_userServer = null;
		
		// the smartfridge needs an ID to function -> ask the controller for the id
		this.setupSmartFridge();
		// The smartfridge should also start a server on the port number equal to his given ID
		// This will be done in a thread
		f_smartfridgeThread = new Thread(this);
		f_smartfridgeThread.start();
		
		
		while (f_isReady == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		try {
			Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.listenToMe(this.getID(), SmartFridge.type);
		}
		catch (IOException e) {
			System.err.println("IOException thrown at DistSmartFridge constructor: listonToMe to controller");
		}
		
	}
	
	/*
	 * Gets an ID for the fridge itself.
	 */
	private void setupSmartFridge() {
		try {
			Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) 
					SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.getID(SmartFridge.type));
		}
		catch (IOException e) {
			System.err.println("Error connecting to the controller server, the port number might be wrong.");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
	
	public boolean logOffController() {
		try {
			Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.logOff(this.getID());
			return true;
		}
		catch (AvroRemoteException e) {
			System.out.println("AvroRemoteException at logOff() in DistSmartFridge.");
			return false;
		}
		catch (IOException e) {
			System.out.println("IOException at logOff() in DistSmartFridge.");
			return false;
		}
	}
	
	public void stopServer() {
		f_smartfridgeThread.interrupt();
		f_smartfridgeThread = null;		
	}
	
	@Override
	public void run() {
		// This function will run the thread which sets up the smartFridge server, used by the controller
		// TODO Similair descision as above - split between user and controller?
		
		try {
			f_fridgeServer = new SaslSocketServer(
					new SpecificResponder(communicationFridge.class, this), new InetSocketAddress(this.getID()) );
			f_fridgeServer.start();
		}
		catch (IOException e) {
			System.err.println("[error] Failed to start SmartFridge server");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		f_isReady = true;

		try {
			f_fridgeServer.join();
		}
		catch (InterruptedException e) {
			f_fridgeServer.close();
		}
		
		
	}

	@Override
	public Void addItemRemote(CharSequence itemName)
			throws AvroRemoteException {
		this.addItem(itemName.toString());
		return null;
	}

	@Override
	public boolean openFridgeRemote() throws AvroRemoteException {
		this.openFridge();
		Logger.getLogger().log("The fridge has been opened.");
		return true;
	}

	@Override
	public boolean closeFridgeRemote() throws AvroRemoteException {
		this.closeFridge();
		Logger.getLogger().log("The fridge has been closed.");
		return true;
	}

	@Override
	public Void setupServer(int port) throws AvroRemoteException {
		// TODO determine what to do with threads and edit/delete this method accordingly
		return null;
	}

	@Override
	public Void closeServer() throws AvroRemoteException {
		// TODO determine what to do with threads and edit/delete this method accordingly
		return null;
	}

	@Override
	public boolean testMethod(ClientType clienttype) throws AvroRemoteException {
		// test method used to test some functionality
		return false;
	}

	@Override
	public boolean requestFridgeCommunication() throws AvroRemoteException {
		// TODO determine what to do with threads and edit/delete this method accordingly
		return true;
	}
	
	@Override
	public List<CharSequence> getItemsRemote() throws AvroRemoteException {
		List<CharSequence> items = new ArrayList<CharSequence>();
		
		Set<String> fridgeItems = this.getItems();
		
		for (String item : fridgeItems) {
			items.add(item);
		}
		return items;
	}
	
	

	public static void main(String[] args) {
		DistController controller = new DistController(6789);
		
		DistSmartFridge remoteFridge = new DistSmartFridge(6789);
		
		try {
			remoteFridge.addItemRemote("bacon");
			remoteFridge.addItemRemote("parmesan cheese");
			Logger logger = Logger.getLogger();
			
			logger.log(remoteFridge.toString());
			List<CharSequence> items = remoteFridge.getItemsRemote();
			logger.log("Items retrieved from remote function: ");
			for (CharSequence item : items) {
				logger.log("\t" + item.toString());
			}
			
			if (remoteFridge.logOffController() == true) {
				logger.log("Logged off succesfully.");
			}
			else {
				logger.log("Could not log off.");
			}
			remoteFridge.stopServer();
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at main class in DistSmartFridge.");
		}
		System.exit(0);
	}

}
