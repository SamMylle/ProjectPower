package controller;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;

import org.apache.avro.AvroRemoteException ;
import org.apache.avro.ipc.CallFuture;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import client.DistLight;
import client.DistSmartFridge;
import client.DistUser;
import client.SmartFridge;
import client.exception.AbsentException;
import client.exception.FridgeOccupiedException;
import client.exception.MultipleInteractionException;
import client.exception.NoFridgeConnectionException;
import util.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
	public Timer f_timer;

	public DistController(int port, int maxTemperatures, String ip){
		/// TODO  throws java.net.BindException
		super(port + 1, maxTemperatures);
		f_isOriginalServer = true;
		
		//f_controller = new Controller(port + 1, 10);
		f_myPort = port;
		f_previousControllerPort = port;
		f_previousControllerIP = ip;
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
		
		/// TODO make dynamic schedule time
		f_timer = new Timer();
		f_timer.schedule(new ClientPoll(), 0, 500);
	}
	
	public boolean equals(DistController otherController){
		Vector<Boolean> mustAllBeTrue = new Vector<Boolean>();

		mustAllBeTrue.add(new Boolean(this.f_IPs.equals(otherController.f_IPs)));
		mustAllBeTrue.add(new Boolean(this.f_maxTemperatures == otherController.f_maxTemperatures));
		mustAllBeTrue.add(new Boolean(this.f_names.equals(otherController.f_names)));
		mustAllBeTrue.add(new Boolean(this.f_nextID == otherController.f_nextID));
		
		if (this.f_temperatures.size() != otherController.f_temperatures.size()){
			System.out.println("NOT TRUE TEMP");
			return false;
		}
		
		for (int i = 0; i < this.f_temperatures.size(); i++){
			if(! this.f_temperatures.elementAt(i).toString().equals(
					otherController.f_temperatures.elementAt(i).toString())){
				System.out.println("NOT TRUE TEMP2");
				return false;
			}
		}
		
		for(int i = 0; i < mustAllBeTrue.size(); i++){
			if (!mustAllBeTrue.elementAt(i)){
				System.out.println(this.f_IPs.toString());
				System.out.println(otherController.f_IPs.toString());
				
				return false;
			}
		}

		System.out.println("TRUE");
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
		
		/// TODO make dynamic schedule time
		f_timer = new Timer();
		f_timer.schedule(new ClientPoll(), 0, 500);
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
			TemperatureRecord newRecord =
					new TemperatureRecord(f_maxTemperatures,
							oldServer.temperaturesIDs.get(i),
							new LinkedList<Double>(oldServer.temperatures.get(i)));
			f_temperatures.add(newRecord);
			
		}
		
		System.out.println("SET: " + f_names.toString());

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
			} catch(Exception e){
				Logger.getLogger().log("Wat\n");
			}
		}
		/// TODO notify clients i am controller if federico fails
		
		this.sendBackupToAll();
		
		/// TODO make dynamic schedule time
		f_timer = new Timer();
		f_timer.schedule(new ClientPoll(), 0, 500);
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
			} catch (InterruptedException e) {}
			
		}

		f_serverActive = false;
	}

	private Transceiver setupTransceiver(int ID, String ip){
		System.out.println("In System: " + f_names.toString());
		System.out.println("Contacting: " + ID + " " + ip);
		if (new Integer(ID).equals(new Integer(this.f_myPort)) &&
				ip.equals(this.f_ownIP)){
			
			return null;
		}
		
		try{
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(InetAddress.getByName(ip), ID));
			return client;
		}catch(Exception e){
			System.err.println("Error lel...");
			return null;
		}
	}

	@Override
	public int LogOn(ClientType clientType, CharSequence ip) throws AvroRemoteException{
		System.out.println("\nADDING");
		int newID = this.giveNextID(clientType);
		ip.toString();
		f_IPs.put(newID, ip.toString());
		
		/// TODO this will have to return and send the backup data afterwards
		/// Suggestion: send successful login to server
		/// this.sendBackupToAll();
		return newID;
	}
	
	@Override
	public int retryLogin(int oldID, ClientType clientType) throws AvroRemoteException{
		/// TODO write test
		Logger.getLogger().log("give renewed ID");
		this.removeID(oldID);
		int newID = this.giveNextID(clientType);
		
		/// TODO this will have to return and send the backup data afterwards
		/// Suggestion: send successful login to server
		/// this.sendBackupToAll();
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
		for (int i = 0; i < f_temperatures.size(); i++){
			if (new Integer(ID).equals(new Integer(f_temperatures.elementAt(i).getID()))){
				f_temperatures.remove(i);
				break;
			}
		}
		this.sendBackupToAll();

		return null;
	}

	@Override
	public java.lang.Void addTemperature(int ID, double temperature) throws AvroRemoteException{
		this.addTemperature(temperature, ID);
		this.sendBackupToAll();
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
				/// TODO error string here? => client == null if the current fridge is a controller
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
				/// TODO return error msg?
				return new ArrayList<CharSequence>();
			}
	
			/// get the inventory and return it
			communicationFridge.Callback proxy;
			proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, fridge);
			return new ArrayList<CharSequence>(proxy.getItemsRemote());
		} catch (Exception e) {
			return new ArrayList<CharSequence>();
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
				return -2;
			}
	
			// We know the type is a light (AND it exists if it hasn't failed)
			Transceiver light = this.setupTransceiver(ID, ip);
	
			if (light == null){
				// If connection can't be established, just say no to the other guy
				return -3;
			}

			/// set the state and thangs
			LightComm.Callback proxy;
			proxy = SpecificRequestor.getClient(LightComm.Callback.class, light);

			proxy.setState(newState);

			return 0;
		} catch (Exception e) {
			e.printStackTrace();
			return -4;
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
				return -2;
			}
	
			// We know the type is a light (AND it exists)
			SaslSocketTransceiver light = null;
			try {
				light = new SaslSocketTransceiver(new InetSocketAddress(ip, ID));
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(0);
			}
	
			if (light == null){
				// If connection can't be established, just say no to the other guy
				return -3;
			}

			/// set the state and thangs
			LightComm.Callback proxy;
			proxy = SpecificRequestor.getClient(LightComm.Callback.class, light);
			return proxy.getState();
		} catch (Exception e) {
			return -4;
		}

	}

	@Override
	public java.util.List<Client> getAllClients() throws AvroRemoteException {
		List<Client> ret = new Vector<Client>();

		/// Ugliest for loop in the history of for loops
		for (Map.Entry<Integer, ClientType> entry : f_names.entrySet()){
			
			if (new Integer(entry.getKey()).equals(new Integer(f_myPort)) && 
					f_IPs.get(new Integer(entry.getKey())).equals(f_ownIP)){
				
				continue;
			}
			
			Client newClient = new Client(entry.getValue(), entry.getKey());
			ret.add(newClient);
		}

		return ret;
	}
	
	public void reaffirmClientsAlive(){
		/// TODO test
		boolean removed = false;
		for(Integer currentID : f_names.keySet()){
			ClientType currentType = f_names.get(currentID);
			String currentIP = f_IPs.get(currentID);
			
			if (this.f_ownIP.equals(f_ownIP) && new Integer(currentID).equals(new Integer(this.f_myPort))){
				continue;
			}
			
			boolean keep = this.reaffirmClientAlive(currentIP, currentID, currentType);
			
			if (! keep){
				removed = true;
				f_names.remove(currentID);
				f_IPs.remove(currentID);
				/// TODO special case if tempsensor
			}
		}
		
		if (removed){
			this.sendBackupToAll();
		}
	}
	
	private boolean reaffirmClientAlive(String ip, int port, ClientType type){
		/// TODO test
		try{
			Transceiver client = this.setupTransceiver(port, ip);
			
			if (type == ClientType.SmartFridge){
				communicationFridge.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, client);
				
				if (proxy.aliveAndKicking()){
					return true;
				}
			}

			
			if (type == ClientType.Light){
				LightComm.Callback proxy;
				proxy = SpecificRequestor.getClient(LightComm.Callback.class, client);
				
				if (proxy.aliveAndKicking()){
					return true;
				}
			}
			
			if (type == ClientType.User){
				communicationUser.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationUser.Callback.class, client);
				
				if (proxy.aliveAndKicking()){
					return true;
				}
			}
			
			if (type == ClientType.TemperatureSensor){
				communicationTempSensor.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationTempSensor.Callback.class, client);
				
				if (proxy.aliveAndKicking()){
					return true;
				}
			}
			
		}catch(Exception e){
			return false;
		}
		return false;
	}
	
	private class ClientPoll extends TimerTask{
		@Override
		public void run() {
			System.out.println("\nPolling...");
			DistController.this.reaffirmClientsAlive();
		}
	}
	
	public void lookForOldServer(){
		/// TODO test
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

			/// NOTE: this call has to be synchronous
				/// because this server can only stop when the other server is done
			proxy.recoverData(this.makeBackup());
			this.stopServer();
				
		}catch(Exception e){
			/// Couldn't contact the previous server, so he's not back yet
			return;
		}
	}

	@Override
	public boolean areYouTheOriginalController() throws AvroRemoteException {
		return f_isOriginalServer;
	}
	
	public void notifyClientsIAmServer(){
		/// TODO test
		for(Integer ID: f_names.keySet()){
			try{
				String ip = f_IPs.get(ID);
				
				Transceiver client = this.setupTransceiver(ID, ip);
				ClientType type = this.f_names.get(ID);


				if (type == ClientType.SmartFridge){
					communicationFridge.Callback proxy;
					proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, client);
					proxy.newServer(f_ownIP, f_myPort);
				}

				
				if (type == ClientType.Light){
					LightComm.Callback proxy;
					proxy = SpecificRequestor.getClient(LightComm.Callback.class, client);
					proxy.newServer(f_ownIP, f_myPort);
				}
				
				if (type == ClientType.User){
					communicationUser.Callback proxy;
					proxy = SpecificRequestor.getClient(communicationUser.Callback.class, client);
					proxy.newServer(f_ownIP, f_myPort);
				}
				
				if (type == ClientType.TemperatureSensor){
					communicationTempSensor.Callback proxy;
					proxy = SpecificRequestor.getClient(communicationTempSensor.Callback.class, client);
					proxy.newServer(f_ownIP, f_myPort);
				}
				
			}catch(Exception e){
				continue;
			}
		}
	}
	
	public ServerData makeBackup(){
		/// TODO test
		ServerData data = new ServerData();
		
		/// TODO remove this
		data.setUsedFridgePorts(new LinkedList<Integer>());
		
		data.setCurrentMaxPort(this.f_nextID);
		data.setIp(f_ownIP);
		data.setPort(f_myPort);
		data.setMaxTemperatures(f_maxTemperatures);
		if (this.f_isOriginalServer){
			data.setOriginalControllerPort(this.f_myPort);
			data.setPreviousControllerIP(this.f_ownIP);
		}else{
			data.setOriginalControllerPort(this.f_previousControllerPort);
			data.setPreviousControllerIP(this.f_previousControllerIP);
		}
		data.setIPsID(new LinkedList<Integer>(f_IPs.keySet()));
		data.setIPsIP(new LinkedList<CharSequence>(f_IPs.values()));
		data.setNamesClientType(new LinkedList<ClientType>(f_names.values()));
		data.setNamesID(new LinkedList<Integer>(f_names.keySet()));
		
		/// TODO add temperatures, don't feel like it ATM
		List<Integer> temperatureIDs = new LinkedList<Integer>();
		List<List<Double>> temperatures = new LinkedList<List<Double>> ();
		
		data.setTemperaturesIDs(temperatureIDs);
		data.setTemperatures(temperatures);
		
		return data;
	}
	
	public void sendBackupToAll(){
		/// TODO test
		for(Integer currentID : f_names.keySet()){
			ClientType currentType = f_names.get(currentID);
			String currentIP = f_IPs.get(currentID);
			this.sendBackupToSpecific(currentIP, currentID, currentType);
		}
	}
	
	private boolean sendBackupToSpecific(String ip, int port, ClientType type){
		/// TODO test
		try{
			Transceiver client = this.setupTransceiver(port, ip);
			
			if (type == ClientType.SmartFridge){
				communicationFridge.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, client);
				proxy.makeBackup(this.makeBackup());
			}
			
			if (type == ClientType.User){
				communicationUser.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationUser.Callback.class, client);
				proxy.makeBackup(this.makeBackup());
			}
			
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return false;
	}

	@Override
	public boolean recoverData(ServerData data) throws AvroRemoteException {
		// TODO Recover data, tell everyone to listen to me, request relogin if needed
		/// TODO test
		Logger.getLogger().log("Came to the recover part.\nData:\n");
		Logger.getLogger().log(data.toString());
		
		List<java.lang.Integer> IPsID = data.getIPsID();
		List<java.lang.CharSequence> IPsIP = data.getIPsIP();
		List<java.lang.Integer> namesID = data.getNamesID();
		List<avro.ProjectPower.ClientType> namesClientType = data.getNamesClientType();
		List<java.lang.Integer> temperaturesIDs = data.getTemperaturesIDs();
		
		for (int i = 0; i < IPsID.size(); i++){
			int currentID = IPsID.get(i);
			if (new Integer(currentID).equals(new Integer(data.getPort()))){
				continue;
			}
			String currentIP = IPsIP.get(i).toString();
			ClientType currentType = null;

			for (int j = 0; j < namesID.size(); j++){
				if (new Integer(namesID.get(j)).equals(new Integer(currentID))){
					currentType = namesClientType.get(j);
					break;
				}
			}
			
			if (currentIP == null || currentType == null){
				continue;
			}
			
			Transceiver client = this.setupTransceiver(currentID, currentIP);
			
			if (client == null){
				continue;
			}
			
			if (currentType == ClientType.SmartFridge){
				communicationFridge.Callback proxy;
				try {
					proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, client);
					/// TODO relogin
				} catch (IOException e) {}
			}

			
			if (currentType == ClientType.Light){
				LightComm.Callback proxy;
				try {
					proxy = SpecificRequestor.getClient(LightComm.Callback.class, client);
					/// TODO relogin
				} catch (IOException e) {}
			}
			
			if (currentType == ClientType.User){
				communicationUser.Callback proxy;
				try {
					proxy = SpecificRequestor.getClient(communicationUser.Callback.class, client);
					/// TODO relogin
				} catch (IOException e) {}
			}
			
			if (currentType == ClientType.TemperatureSensor){
				communicationTempSensor.Callback proxy;
				try {
					proxy = SpecificRequestor.getClient(communicationTempSensor.Callback.class, client);
					/// TODO relogin
				} catch (IOException e) {}
			}
		}
		
		return true;
	}

	public static void main(String[] args) {
		Logger.getLogger().f_active = true;
		DistController controller = new DistController(5000, 10, System.getProperty("ip"));
		
		DistUser user = new DistUser("me", System.getProperty("ip"), System.getProperty("ip"), 5000);
		
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		DistLight light = new DistLight(System.getProperty("ip"), System.getProperty("ip"));
		light.connectToServer(5000, System.getProperty("ip"));
		
		light.disconnect();
		
		//DistSmartFridge
		user.disconnect();
		user.stopServer();
		controller.stopServer();
	}
}
