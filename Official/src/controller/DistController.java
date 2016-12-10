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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
	private HashMap<Integer, String> f_IPs;
	private String f_ownIP;
	private boolean f_isOriginalServer;
	private int f_previousControllerPort;
	private String f_previousControllerIP;

	public DistController(int port, int maxTemperatures, String ip){
		/// TODO  throws java.net.BindException
		super(port + 1, maxTemperatures);
		f_isOriginalServer = true;
		
		//f_controller = new Controller(port + 1, 10);
		f_myPort = port;
		f_previousControllerPort = -1;
		f_previousControllerIP = "";
		f_serverActive = false;
		f_IPs = new HashMap<Integer, String>();
		f_ownIP = new String(ip);

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
				Logger.getLogger().f_active = true;
				Logger.getLogger().log("Server startup failed");
				e.printStackTrace();
			}
		}
	}
	
	public boolean equals(DistController otherController){
		Vector<Boolean> mustAllBeTrue = new Vector<Boolean>();

		mustAllBeTrue.add(new Boolean(this.f_IPs.equals(otherController.f_IPs)));
		mustAllBeTrue.add(new Boolean(this.f_maxTemperatures == otherController.f_maxTemperatures));
		mustAllBeTrue.add(new Boolean(this.f_names.equals(otherController.f_names)));
		mustAllBeTrue.add(new Boolean(this.f_nextID == otherController.f_nextID));
		
		if (this.f_temperatures.size() != otherController.f_temperatures.size()){
			return false;
		}
		
		for (int i = 0; i < this.f_temperatures.size(); i++){
			if(! this.f_temperatures.elementAt(i).toString().equals(
					otherController.f_temperatures.elementAt(i).toString())){
				return false;
			}
		}
		
		for(int i = 0; i < mustAllBeTrue.size(); i++){
			if (!mustAllBeTrue.elementAt(i)){
				return false;
			}
		}

		return true;
	}
	
	@Deprecated
	public DistController(int port, int originalControllerPort, int maxTemperatures, int currentMaxPort, String ip,
			String previousControllerIP, Vector<Integer> usedFridgePorts, HashMap<Integer, String> IPs,
			HashMap<Integer, ClientType> names, Vector<TemperatureRecord> temperatures){
		/// TODO  throws java.net.BindException
		super(currentMaxPort, maxTemperatures);
		f_isOriginalServer = false;
		
		//f_controller = new Controller(port + 1, 10);
		f_myPort = port;
		f_previousControllerPort = originalControllerPort;
		f_previousControllerIP = previousControllerIP;
		f_serverActive = false;
		f_IPs = IPs;
		f_ownIP = new String(ip);
		f_names = names;
		f_temperatures = temperatures;

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
				Logger.getLogger().f_active = true;
				Logger.getLogger().log("Server startup failed");
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	public DistController(ServerData oldServer){
		/// Precondition: valid stuff in data
		/// I'm not proud of this
		super(oldServer.currentMaxPort, oldServer.maxTemperatures);
		f_isOriginalServer = false;
		
		//f_controller = new Controller(port + 1, 10);
		f_myPort = oldServer.port;
		f_previousControllerPort = oldServer.originalControllerPort;
		f_previousControllerIP = oldServer.previousControllerIP.toString();
		f_serverActive = false;

		if (f_IPs == null){
			f_IPs = new HashMap<Integer, String>();
		}
		
			if (f_names == null){
		f_names = new HashMap<Integer, ClientType>();
			}

		if (f_temperatures == null){
			f_temperatures = new Vector<TemperatureRecord>();
		}
		
		for (int i = 0; i < oldServer.IPsID.size(); i++){
			String IP = oldServer.IPsIP.get(i).toString();
			Integer ID = oldServer.IPsID.get(i);
			f_IPs.put(ID, IP);
		}

		f_ownIP = oldServer.ip.toString();

		for (int i = 0; i < oldServer.namesID.size(); i++){
			f_names.put(oldServer.namesID.get(i), oldServer.namesClientType.get(i));
		}

		for (int i = 0; i < oldServer.temperatures.size(); i++){
			System.out.print("adding ");
			TemperatureRecord newRecord =
					new TemperatureRecord(f_maxTemperatures,
							oldServer.temperaturesIDs.get(i),
							new LinkedList<Double>(oldServer.temperatures.get(i)));
			f_temperatures.add(newRecord);
			
		}

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
			InetAddress addr = InetAddress.getByName(f_ownIP);
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
			System.out.print("Closing server\n");
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

	private Transceiver setupTransceiver(int ID, String ip){
		try{
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(InetAddress.getByName(ip), ID));
			return client;
		}catch(IOException e){
			System.err.println("Error connecting to the client server...");
			return null;
		}
	}

	@Override
	public int LogOn(ClientType clientType, CharSequence ip) throws AvroRemoteException{
		System.out.print("give new ID");
		int newID = this.giveNextID(clientType);
		ip.toString();
		f_IPs.put(newID, ip.toString());
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
		this.f_IPs.remove(ID);

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

	public String getIPAddress(int ID){
		/// TODO write test
		return f_IPs.get(ID);
	}
	
	@Override
	public CommData setupFridgeCommunication(int ID) throws AvroRemoteException {
		/// TODO test this better (i tested it but only short, tests were successful)
		try {
			if (f_names.get(ID) != ClientType.SmartFridge){
				return new CommData(-1, "");
			}
			
			String ip = this.getIPAddress(ID);

			if (ip == ""){
				return new CommData(-1, "");
			}
			
			Transceiver client = this.setupTransceiver(ID, ip);

			if (client == null){
				return new CommData(-1, "");
			}

			/// Connect to fridge
			communicationFridge.Callback proxy =
					SpecificRequestor.getClient(communicationFridge.Callback.class, client);

			/// Ask the fridge if it's okay to connect a user to it
			int newPort = proxy.requestFridgeCommunication(f_myPort - 1);
			if (newPort != -1){
				return new CommData(newPort, ip);
			}else{
				return new CommData(-1, "");
			}
		}catch(Exception e){
			return new CommData(-1, "");
		}
	}
	
	
	/*@Deprecated
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
	}*/
	
	
	@Deprecated
	@Override
	public CommData reSetupFridgeCommunication(int fridgeID, int wrongID) throws AvroRemoteException {
		/*/// Only needs a port
		try {
			if (f_names.get(fridgeID) != ClientType.SmartFridge){
				return new CommData(-1, "");
			}
			
			String ip = this.getIPAddress(fridgeID);
			
			if (ip == ""){
				return new CommData(-1, "");
			}

			Transceiver client = this.setupTransceiver(fridgeID, ip);

			/// Don't think this is necessary
			if (client == null){
				return new CommData(-1, "");
			}

			/// Connect to fridge
			communicationFridge.Callback proxy =
					SpecificRequestor.getClient(communicationFridge.Callback.class, client);

			/// Ask the fridge if it's okay to connect a user to it
			int newID = this.getFridgePort(wrongID);
			if (proxy.requestFridgeCommunication(newID) == true){
				return new CommData(newID, ip);
			}else{
				f_usedFridgePorts.removeElement(newID);
				return new CommData(-1, "");
			}
		}catch(Exception e){
			return new CommData(-1, "");
		}*/
		return new CommData(-1, "");
	}

	@Deprecated
	@Override
	public Void endFridgeCommunication(int usedPort) throws AvroRemoteException {
		/// TODO test
		/*for (int i = 0; i < f_usedFridgePorts.size(); i++){
			if(f_usedFridgePorts.elementAt(i) == usedPort){
				f_usedFridgePorts.remove(i);
				break;
			}
		}*/
		return null;
	}

	@Override
	@Deprecated
	public Void listenToMe(int port, ClientType type) throws AvroRemoteException {
		/// Remote call by e.g. a fridge, to indicate the server can reach him on this port
			/// (usually the ID of the client)
		// TODO check if the ID is in the system and in the transceivers
		// TODO maybe change retval to boolean
		//this.setupTransceiver(type, port);
		return null;
	}

	@Override
	public List<CharSequence> getFridgeInventory(int ID) throws AvroRemoteException {
		/// return null on invalid stuff and thangs
		try{
			if (f_names.get(ID) != ClientType.SmartFridge){
				return new ArrayList<CharSequence>();
			}
			
			String ip = this.getIPAddress(ID);
			
			if (ip == ""){
				return new ArrayList<CharSequence>();
			}
			
			Transceiver fridge = this.setupTransceiver(ID, ip);
	
			if (fridge == null){
				// If connection can't be established, just say no to the other guy
				return null;
			}
	
			/// get the inventory and return it
			communicationFridge.Callback proxy;
			proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, fridge);
			return new ArrayList<CharSequence>(proxy.getItemsRemote());
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public int setLight(int newState, int ID) throws AvroRemoteException {
		/// return -1 on invalid stuff and thangs
		/// return 0 on success

		try {
			if (f_names.get(ID) != ClientType.Light){
				return -1;
			}
			
			String ip = this.getIPAddress(ID);
			
			if (ip == ""){
				return -1;
			}
	
			// We know the type is a light (AND it exists if it hasn't failed)
			Transceiver light = this.setupTransceiver(ID, ip);
	
			if (light == null){
				// If connection can't be established, just say no to the other guy
				return -1;
			}

			/// set the state and thangs
			LightComm.Callback proxy;
			proxy = SpecificRequestor.getClient(LightComm.Callback.class, light);

			proxy.setState(newState);

			return 0;
		} catch (Exception e) {
			return -1;
		}
	}

	@Override
	public int getLightState(int ID) throws AvroRemoteException {
		try {
			if (f_names.get(ID) != ClientType.Light){
				return -1;
			}
			
			String ip = this.getIPAddress(ID);
			
			if (ip == ""){
				return -1;
			}
	
			// We know the type is a light (AND it exists)
			SaslSocketTransceiver light = null;
			try {
				/// TODO correct ip
				light = new SaslSocketTransceiver(new InetSocketAddress(ip, ID));
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(0);
			}
	
			if (light == null){
				// If connection can't be established, just say no to the other guy
				return -1;
			}

			/// set the state and thangs
			LightComm.Callback proxy;
			proxy = SpecificRequestor.getClient(LightComm.Callback.class, light);

			return proxy.getState();
		} catch (Exception e) {
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
	
	public void reaffirmClientsAlive(){
		/// TODO test and make timer
		for(Integer currentID : f_names.keySet()){
			ClientType currentType = f_names.get(currentID);
			String currentIP = f_IPs.get(currentID);
			
			boolean keep = this.reaffirmClientAlive(currentIP, currentID, currentType);
			
			if (! keep){
				f_names.remove(currentID);
				f_IPs.remove(currentID);
			}
		}
	}
	
	private boolean reaffirmClientAlive(String ip, int port, ClientType type){
		try{
			Transceiver client = this.setupTransceiver(port, ip);
			
			if (type == ClientType.Light){
				LightComm.Callback proxy;
				proxy = SpecificRequestor.getClient(LightComm.Callback.class, client);
				
				/// TODO check isAlive
				return true;
			}
			
			if (type == ClientType.SmartFridge){
				communicationFridge.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, client);
				
				/// TODO check isAlive
				return true;
			}
			
			if (type == ClientType.TemperatureSensor){
				communicationTempSensor.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationTempSensor.Callback.class, client);
				
				/// TODO check isAlive
				return true;
			}
			
			if (type == ClientType.User){
				communicationUser.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationUser.Callback.class, client);
				
				/// TODO check isAlive
				return true;
			}
		}catch(Exception e){
			return false;
		}
		return false;
	}
	
	public void lookForOldServer(){
		if (f_isOriginalServer){
			return;
		}
		
		try{
			Transceiver client = this.setupTransceiver(f_previousControllerPort, f_previousControllerIP);
			
			ControllerComm.Callback proxy;
			proxy = SpecificRequestor.getClient(ControllerComm.Callback.class, client);
			
			if (! proxy.areYouTheOriginalController()){
				return;
			}
			
			/// TODO copy my data to server, tell clients to no longer listen to me, but to the original dude
				
		}catch(Exception e){
			/// Couldn't contact the previous server, so he's not back yet
			return;
		}
	}

	@Override
	public boolean areYouTheOriginalController() throws AvroRemoteException {
		return f_isOriginalServer;
	}

	public static void main(String[] args) {
		Logger.getLogger().f_active = true;
		DistController controller = new DistController(5000, 10, System.getProperty("ip"));;

		DistSmartFridge fridge = new DistSmartFridge(System.getProperty("clientip"), System.getProperty("ip"), 5000);
		fridge.addItem("bacon");
		Logger.getLogger().log("Servers started");
		
		fridge.stopServerController();
		fridge = null;
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		try {
			controller.setupFridgeCommunication(5001);
		} catch (AvroRemoteException e) {
			System.out.print("here\n");
			e.printStackTrace();
		}
		System.out.print("here toos\n");
		
		
		controller.stopServer();
	}
}
