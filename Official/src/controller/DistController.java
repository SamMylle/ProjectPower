package controller;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;

import org.apache.avro.AvroRemoteException ;
import org.apache.avro.ipc.CallFuture;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import client.DistSmartFridge;
import client.SmartFridge;

import util.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import avro.ProjectPower.*;

public class DistController extends Controller implements ControllerComm, Runnable{

	//private Controller f_controller;
	private Server f_server;
	private Thread f_serverThread;
	private int f_myPort;
	private boolean f_serverActive;
	private Vector<Integer> f_usedFridgePorts;

	public DistController(int port, int maxTemperatures){
		/// TODO  throws java.net.BindException
		super(port + 1, maxTemperatures);
		
		//f_controller = new Controller(port + 1, 10);
		f_myPort = port;
		f_serverActive = false;
		f_usedFridgePorts = new Vector<Integer>();

		/// Make a thread, the "run" method will execute in a new thread
		/// The run method must be implemented by a Runnable object (see implements in this class)
		/// Since the server must run on this object, "this" is passed to the thread
		/// Below, when starting the thread (thread.start()), the thread will call this.run()
		/// This has to be this way because the server doesn't get out of the eternal loop
		/// This object wouldn't be able to do other interesting stuff if it wasn't for the threads
		f_serverThread = new Thread(this);
		f_serverThread.start();

		while (!f_serverActive){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				Logger.getLogger().f_active = true;
				Logger.getLogger().log("Server startup failed");
				e.printStackTrace();
			}
		}
	}

	public boolean serverIsActive(){
		return f_serverActive;
	}

	@Override
	public void run() {
		/// when thread.start() is invoked, this method is ran
		try{
			InetAddress addr = InetAddress.getByName("192.168.1.6");
			//System.out.print(addr.toString());
			InetSocketAddress ad = new InetSocketAddress(addr, f_myPort);
			f_server = new SaslSocketServer(
					new SpecificResponder(ControllerComm.class,
							this), ad);
		}catch(IOException e){
			System.err.println("[error]Failed to start server");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		f_server.start();
		f_serverActive = true;
		try{
			f_server.join();
		}catch(InterruptedException e){
			f_server.close();
			f_server = null;
		}
	}

	public void stopServer(){
		f_serverThread.interrupt();
		while (f_server != null){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			
		}

		f_serverActive = false;
	}

	private Transceiver setupTransceiver(int ID){
		try{
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(ID));
			return client;
		}catch(IOException e){
			System.err.println("Error connecting to the client server...");
			return null;
		}
	}

	@Override
	public int getID(ClientType clientType) throws AvroRemoteException{
		/// TODO rename to login
		Logger.getLogger().log("give new ID");
		int newID = this.giveNextID(clientType);
		return newID;
	}
	
	@Override
	public int retryLogin(int oldID, ClientType clientType) throws AvroRemoteException{
		/// TODO write test
		Logger.getLogger().log("give renewed ID");
		this.removeID(oldID);
		int newID = this.giveNextID(clientType);
		return newID;
	}

	@Override
	public ClientType getClientType(int ID) throws AvroRemoteException{
		return this.getClType(ID);
	}

	@Override
	public Void logOff(int ID) throws AvroRemoteException{
		/// Remove ID from the system
		/// TODO, special case when the client is a temperatureSensor
		this.removeID(ID);

		return null;
	}

	@Override
	public java.lang.Void addTemperature(int ID, double temperature) throws AvroRemoteException{
		this.addTemperature(temperature, ID);
		return null;
	}

	@Override
	public double averageCurrentTemperature() throws AvroRemoteException{
		return this.averageCurrentTemp();
	}

	@Override
	public boolean hasValidTemperatures() throws AvroRemoteException{
		return this.hasValidTemp();
	}

	@Override
	public int setupFridgeCommunication(int ID) throws AvroRemoteException {
		try {
			if (f_names.get(ID) != ClientType.SmartFridge){
				return -1;
			}
			
			/// TODO catch this?
			Transceiver client = this.setupTransceiver(ID);

			/// Don't think this is necessary
			if (client == null){
				return -1;
			}

			/// Connect to fridge
			communicationFridge.Callback proxy =
					SpecificRequestor.getClient(communicationFridge.Callback.class, client);

			/// Ask the fridge if it's okay to connect a user to it
			int newID = this.getFridgePort(-1);
			if (proxy.requestFridgeCommunication(newID) == true){
				return newID;
			}else{
				f_usedFridgePorts.removeElement(new Integer(newID));
				return -1;
			}
		}catch(IOException e){
			return -1;
		}
	}
	
	
	
	public int getFridgePort(int start){
		/// -1 for default start port
		///  It will NOT take the start port into consideration
		/// TODO test
		int ret = f_myPort;
		
		if (start != -1){
			ret = start;
		}
		
		if (ret < 0){
			return -1;
		}
		
		boolean portAlreadyInUse = true;
		
		if(f_usedFridgePorts.size() == 0){
			portAlreadyInUse = false;
			ret--;
		}

		while(portAlreadyInUse){
			ret--;
			portAlreadyInUse = false;
			
			for (int i = 0; i < f_usedFridgePorts.size(); i++){
				if (f_usedFridgePorts.elementAt(i).equals(new Integer(ret))){
					portAlreadyInUse = true;
					break;
				}
			}
		}
		
		if (ret < 0){
			return -1;
		}
		
		f_usedFridgePorts.add(new Integer(ret));
		
		return ret;
	}
	
	

	@Override
	public int reSetupFridgeCommunication(int fridgeID, int wrongID) throws AvroRemoteException {
		try {
			if (f_names.get(fridgeID) != ClientType.SmartFridge){
				return -1;
			}
			Transceiver client = this.setupTransceiver(fridgeID);

			/// Don't think this is necessary
			if (client == null){
				return -1;
			}

			/// Connect to fridge
			communicationFridge.Callback proxy =
					SpecificRequestor.getClient(communicationFridge.Callback.class, client);

			/// Ask the fridge if it's okay to connect a user to it
			/// TODO fill in request with port
			int newID = this.getFridgePort(wrongID);
			if (proxy.requestFridgeCommunication(newID) == true){
				return newID;
			}else{
				f_usedFridgePorts.removeElement(newID);
				return -1;
			}
		}catch(IOException e){
			return -1;
		}
	}

	@Override
	public Void endFridgeCommunication(int usedPort) throws AvroRemoteException {
		/// TODO test
		for (int i = 0; i < f_usedFridgePorts.size(); i++){
			if(f_usedFridgePorts.elementAt(i) == usedPort){
				f_usedFridgePorts.remove(i);
				break;
			}
		}
		return null;
	}

	@Override
	@Deprecated
	public Void listenToMe(int port, ClientType type) throws AvroRemoteException {
		/// Remote call by e.g. a fridge, to indicate the server can reach him on this port (usually the ID of the client)
		// TODO check if the ID is in the system and in the transceivers
		// TODO maybe change retval to boolean
		//this.setupTransceiver(type, port);
		return null;
	}

	@Override
	public List<CharSequence> getFridgeInventory(int ID) throws AvroRemoteException {
		/// return null on invalid stuff and thangs
		ClientType type = this.f_names.get(ID);

		if(type != ClientType.SmartFridge){
			return null;
		}

		// We know the type is a fridge (AND it exists)
		Transceiver fridge = this.setupTransceiver(ID);

		if (fridge == null){
			// If connection can't be established, just say no to the other guy
			return null;
		}

		/// get the inventory and return it
		/// TODO stuff
		communicationFridge.Callback proxy;
		try {
			proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, fridge);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return proxy.getItemsRemote();
	}
	
	public Vector<Integer> getOccupiedPorts(){
		/// For testing purposes
		return f_usedFridgePorts;
	}

	@Override
	public int setLight(int newState, int ID) throws AvroRemoteException {
		/// return -1 on invalid stuff and thangs
		/// return 0 on success

		ClientType type = this.f_names.get(ID);

		if(type != ClientType.Light){
			return -1;
		}

		// We know the type is a light (AND it exists)
		Transceiver light = this.setupTransceiver(ID);

		if (light == null){
			// If connection can't be established, just say no to the other guy
			return -1;
		}

		try {
			/// set the state and thangs
			LightComm.Callback proxy;
			proxy = SpecificRequestor.getClient(LightComm.Callback.class, light);

			proxy.setState(newState);

			return 0;
		} catch (IOException e) {
			// TODO remove from system?
			e.printStackTrace();
			Logger.getLogger().log("IOEXCEPT for remote light setstate");
			return -1;
		}
	}

	@Override
	public int getLightState(int ID) throws AvroRemoteException {
		ClientType type = this.f_names.get(ID);

		if(type != ClientType.Light){
			return -1;
		}

		// We know the type is a light (AND it exists)
		SaslSocketTransceiver light = null;
		try {
			light = new SaslSocketTransceiver(new InetSocketAddress(ID));
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(0);
		}

		/*if (light == null){
				// This shouldn't happen actually, but you never know
				this.setupTransceiver(ClientType.Light, ID);
				light = f_transceivers.get(ID);
			}*/

		if (light == null){
			// If connection can't be established, just say no to the other guy
			return -1;
		}

		try {
			/// set the state and thangs
			LightComm.Callback proxy;
			proxy = SpecificRequestor.getClient(LightComm.Callback.class, light);



			return proxy.getState();
		} catch (IOException e) {
			// TODO remove from system?
			e.printStackTrace();
			Logger.getLogger().log("IOEXCEPT for remote light setstate");
			return -1;
		}

	}

	@Override
	public java.util.List<Client> getAllClients() throws AvroRemoteException {
		/// TODO test
		List<Client> ret = new Vector<Client>();

		/// Ugliest for loop in the history of for loops
		for (Map.Entry<Integer, ClientType> entry : f_names.entrySet()){
			Client newClient = new Client(entry.getValue(), entry.getKey());
			ret.add(newClient);
		}

		return ret;
	}

	public static void main(String[] args) {
		Logger.getLogger().f_active = true;
		DistController controller = new DistController(4999, 10);
		DistController controller2 = new DistController(5000, 10);

		int ID;
		try {
			ID = controller.getID(ClientType.Light);
			controller.retryLogin(ID, ClientType.Light);
		} catch (AvroRemoteException e1) {
			e1.printStackTrace();
		}
		
		
		try {
			System.out.print("do somethings pls");
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.out.print("Clients:");
		//System.out.print(controller.f_names.get(5001));
		
		controller.stopServer();
		controller2.stopServer();

		/*DistSmartFridge fridge = new DistSmartFridge(5000);
		try {
			fridge.addItemRemote("bacon");
			fridge.addItemRemote("parmesan cheese");
		} catch (AvroRemoteException e1) {
			Logger.getLogger().log("woops");
		}

		Logger.getLogger().log("Servers started");

		try {
			List<CharSequence> items = controller.getFridgeInventory(5001);

			for (int i = 0; i < items.size(); i++){
				Logger.getLogger().log(items.get(i).toString());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}


}
