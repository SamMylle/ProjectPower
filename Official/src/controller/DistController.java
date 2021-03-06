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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
	private Set<Integer> f_notConfirmed;

	public DistController(int port, int maxTemperatures, String ip){
		super(port + 1, maxTemperatures);
		f_notConfirmed = new HashSet<Integer>();
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
				e.printStackTrace();
			}
		}

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
		super(currentMaxPort, maxTemperatures);
		f_notConfirmed = new HashSet<Integer>();
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
				e.printStackTrace();
			}
		}

		f_timer = new Timer();
		f_timer.schedule(new ClientPoll(), 0, 5000);
	}

	@SuppressWarnings("deprecation")
	public DistController(ServerData oldServer){
		/// Precondition: valid stuff in data
		/// I'm not proud of this

		super(oldServer.currentMaxPort, oldServer.maxTemperatures);
		synchronized(this) {
			f_notConfirmed = new HashSet<Integer>();
			f_isOriginalServer = false;

			//f_controller = new Controller(port + 1, 10);
			f_myPort = oldServer.port;
			f_previousControllerPort = oldServer.originalControllerPort;
			f_previousControllerIP = oldServer.previousControllerIP.toString();
			f_serverActive = false;

			if (f_IPs == null){
				f_IPs = new HashMap<Integer, String>();
			}

			f_names = new HashMap<Integer, ClientType>();
			f_temperatures = new Vector<TemperatureRecord>();

			for (int i = 0; i < oldServer.IPsID.size(); i++){
				String IP = oldServer.IPsIP.get(i).toString();
				Integer ID = oldServer.IPsID.get(i);
				f_IPs.put(ID, IP);
			}

			f_ownIP = oldServer.ip.toString();

			for (int i = 0; i < oldServer.namesID.size(); i++){
				f_names.put(oldServer.namesID.get(i), oldServer.namesClientType.get(i));
			}

			for (Integer currentID: f_IPs.keySet()){
				if (f_names.get(currentID) != ClientType.TemperatureSensor){
					continue;
				}
				
				TemperatureRecord newRecord =
						new TemperatureRecord(f_maxTemperatures, currentID);
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
					e.printStackTrace();
				} catch(Exception e){
				}
			}

			this.sendBackupToAll();
			this.usersInside();
			this.fixTemperatures();
			f_timer = new Timer();
			f_timer.schedule(new ClientPoll(), 0, 500);    
		}

	}

	private void fixTemperatures(){
		for (Integer ID: f_IPs.keySet()){
			try {
				String ip = this.getIPAddress(ID);

				if (ip == ""){
					return;
				}
				if (f_names.get(ID) != ClientType.TemperatureSensor) {
					continue;
				}
				Transceiver client = this.setupTransceiver(ID, ip);

				if (client == null){
					return;
				}
				
				/// ask the sensor for its temperatures
				communicationTempSensor.Callback proxy =
						SpecificRequestor.getClient(communicationTempSensor.Callback.class, client);

				for (int i = 0; i < f_temperatures.size(); i++){
					if (f_temperatures.elementAt(i).getID() == ID){
						f_temperatures.remove(i);
						break;
					}
				}

				f_temperatures.addElement(new TemperatureRecord(f_maxTemperatures, ID));
				List <Double> temperatures = proxy.getTemperatureRecords();

				ListIterator<Double> iterator = temperatures.listIterator(temperatures.size());

				while (iterator.hasPrevious()){
					Double newTemperature = iterator.next();
					f_temperatures.lastElement().addValue(newTemperature);
				}

				client.close();
				this.usersInside();
			}catch(Exception e){
				return;
			}
		}
	}

	synchronized public boolean serverIsActive(){
		return f_serverActive;
	}

	@Override
	public void run() {
		/// when thread.start() is invoked, this method is ran
		try{
			InetAddress addr = InetAddress.getByName(f_ownIP);
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
			f_timer.cancel();
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

	synchronized private Transceiver setupTransceiver(int ID, String ip){
		if (new Integer(ID).equals(new Integer(this.f_myPort)) &&
				ip.equals(this.f_ownIP)){
			return null;
		}
		try{
			Transceiver client = new SaslSocketTransceiver(
					new InetSocketAddress(InetAddress.getByName(ip), ID));
			return client;
		}catch(Exception e){
			return null;
		}
	}

	@Override
	synchronized public int LogOn(ClientType clientType, CharSequence ip) throws AvroRemoteException{
		int newID = this.giveNextID(clientType);
		f_notConfirmed.add(new Integer(newID));
		f_IPs.put(newID, ip.toString());

		this.sendBackupToAll();
		return newID;
	}

	@Override
	synchronized public int retryLogin(int oldID, ClientType clientType) throws AvroRemoteException{
		/// TODO write test
		this.removeID(oldID);
		f_notConfirmed.remove(new Integer(oldID));
		int newID = this.giveNextID(clientType);
		f_IPs.put(newID, f_IPs.get(new Integer(oldID)));
		f_IPs.remove(new Integer(oldID));
		f_notConfirmed.add(new Integer(newID));

		this.sendBackupToAll();
		return newID;
	}

	@Override
	synchronized public void loginSuccessful(int ID) {
		f_notConfirmed.remove(new Integer(ID));
		if (f_names.get(ID) == ClientType.TemperatureSensor){
			try {
				String ip = this.getIPAddress(ID);

				if (ip == ""){
					return;
				}

				Transceiver client = this.setupTransceiver(ID, ip);

				if (client == null){
					return;
				}

				/// ask the sensor for its temperatures
				communicationTempSensor.Callback proxy =
						SpecificRequestor.getClient(communicationTempSensor.Callback.class, client);

				for (int i = 0; i < f_temperatures.size(); i++){
					if (f_temperatures.elementAt(i).getID() == ID){
						f_temperatures.remove(i);
						break;
					}
				}

				f_temperatures.addElement(new TemperatureRecord(f_maxTemperatures, ID));
				List <Double> temperatures = proxy.getTemperatureRecords();

				ListIterator<Double> iterator = temperatures.listIterator(temperatures.size());

				while (iterator.hasPrevious()){
					Double newTemperature = iterator.next();
					f_temperatures.lastElement().addValue(newTemperature);
				}

				client.close();
			}catch(Exception e){
				return;
			}
		}
		this.usersInside();
		this.sendBackupToAll();
	}

	@Override
	synchronized public ClientType getClientType(int ID) throws AvroRemoteException{
		return this.getClType(ID);
	}

	@Override
	synchronized public Void logOff(int ID) throws AvroRemoteException{
		/// Remove ID from the system
		/// TODO test with tempsensor
		this.removeID(ID);
		this.f_IPs.remove(ID);
		for (int i = 0; i < f_temperatures.size(); i++){
			if (new Integer(ID).equals(new Integer(f_temperatures.elementAt(i).getID()))){
				f_temperatures.remove(i);
				break;
			}
		}
		this.usersInside();
		this.sendBackupToAll();

		return null;
	}

	@Override
	synchronized public java.lang.Void addTemperature(int ID, double temperature) throws AvroRemoteException{
		this.addTemperature(temperature, ID);
		//		this.sendBackupToAll();
		return null;
	}

	@Override
	synchronized public double averageCurrentTemperature() throws AvroRemoteException{
		return this.averageCurrentTemp();
	}

	@Override
	synchronized public boolean hasValidTemperatures() throws AvroRemoteException{
		return this.hasValidTemp();
	}

	synchronized public String getIPAddress(int ID){
		return f_IPs.get(ID);
	}

	@Override
	synchronized public CommData setupFridgeCommunication(int ID) throws AvroRemoteException {
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
			int newPort = proxy.requestFridgeCommunication(f_previousControllerPort - 1);
			if (newPort != -1){
				return new CommData(newPort, ip);
			}else{
				return new CommData(-1, "");
			}
		}catch(Exception e){
			return new CommData(-1, "");
		}
	}
	@Deprecated
	@Override
	public CommData reSetupFridgeCommunication(int fridgeID, int wrongID) throws AvroRemoteException {

		return new CommData(-1, "");
	}

	@Deprecated
	@Override
	public Void endFridgeCommunication(int usedPort) throws AvroRemoteException {

		return null;
	}

	@Override
	@Deprecated
	public Void listenToMe(int port, ClientType type) throws AvroRemoteException {
		return null;
	}

	@Override
	synchronized public List<CharSequence> getFridgeInventory(int ID) throws AvroRemoteException {
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
	synchronized public int setLight(int newState, int ID) throws AvroRemoteException {
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
	synchronized public int getLightState(int ID) throws AvroRemoteException {
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
	synchronized public java.util.List<Client> getAllClients() throws AvroRemoteException {
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

	synchronized public void reaffirmClientsAlive(){
		/// TODO test
		boolean removed = false;
		Set<Integer> keySet = new HashSet<Integer>(f_names.keySet());
		for(Integer currentID : keySet){
			ClientType currentType = f_names.get(currentID);
			String currentIP = f_IPs.get(currentID);

			if ((this.f_ownIP.equals(f_ownIP) && new Integer(currentID).equals(new Integer(this.f_myPort)))
					|| f_notConfirmed.contains(new Integer(currentID))){
				continue;
			}
			boolean keep = this.reaffirmClientAlive(currentIP, currentID, currentType);

			if (! keep){
				removed = true;
				f_names.remove(currentID);
				f_IPs.remove(currentID);
				for (int i = 0; i < f_temperatures.size(); i++){
					if (new Integer(currentID).equals(new Integer(f_temperatures.elementAt(i).getID()))){
						f_temperatures.remove(i);
						break;
					}
				}
			}
		}

		if (removed){
			this.sendBackupToAll();
			if (! this.usersInside()){

			}
		}
	}

	synchronized private boolean reaffirmClientAlive(String ip, int port, ClientType type){
		/// TODO test
		try{
			Transceiver client = this.setupTransceiver(port, ip);
			/// TODO Check if transceiver == null
			// ^isn't this caught by the exception?

			if (type == ClientType.SmartFridge){
				communicationFridge.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, client);

				if (proxy.aliveAndKicking()){
					client.close();
					return true;
				}
			}


			if (type == ClientType.Light){
				LightComm.Callback proxy;
				proxy = SpecificRequestor.getClient(LightComm.Callback.class, client);

				if (proxy.aliveAndKicking()){
					client.close();
					return true;
				}
			}

			if (type == ClientType.User){
				communicationUser.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationUser.Callback.class, client);

				if (proxy.aliveAndKicking()){
					client.close();
					return true;
				}
			}

			if (type == ClientType.TemperatureSensor){
				communicationTempSensor.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationTempSensor.Callback.class, client);

				if (proxy.aliveAndKicking()){
					client.close();
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
		synchronized public void run() {
			DistController.this.reaffirmClientsAlive();
			DistController.this.lookForOldServer();
		}
	}

	synchronized public void lookForOldServer(){
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
	synchronized public boolean areYouTheOriginalController() throws AvroRemoteException {
		return f_isOriginalServer;
	}

	synchronized public void notifyClientIAmServer(String ip, int port, ClientType type){
		/// TODO test
		//for(Integer ID: f_names.keySet()){
		try{
			Transceiver client = this.setupTransceiver(port, ip);

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
		}
	}

	synchronized public ServerData makeBackup(){
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

	synchronized public void sendBackupToAll(){
		/// TODO test
		for(final Integer currentID : f_names.keySet()){
			new Thread(this) {
				public void run() {
					if (f_notConfirmed.contains(new Integer(currentID))){
						return;
					}

					ClientType currentType = f_names.get(currentID);
					String currentIP = f_IPs.get(currentID);

					DistController.this.sendBackupToSpecific(currentIP, currentID, currentType);					
				}
			}.start();

		}
	}

	synchronized private boolean sendBackupToSpecific(String ip, int port, ClientType type){
		/// TODO test
		if (type != ClientType.SmartFridge && type != ClientType.User){
			return false;
		}
		try{
			Transceiver client = this.setupTransceiver(port, ip);

			if (type == ClientType.SmartFridge){
				communicationFridge.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, client);
				proxy.makeBackup(this.makeBackup());
				client.close();
				return true;
			}

			if (type == ClientType.User){
				communicationUser.Callback proxy;
				proxy = SpecificRequestor.getClient(communicationUser.Callback.class, client);
				proxy.makeBackup(this.makeBackup());
				client.close();
				return true;
			}
		}catch(Exception e){
			return false;
		}
		return false;
	}

	@Override
	public boolean recoverData(ServerData data) throws AvroRemoteException {
		/// TODO test

		List<java.lang.Integer> IPsID = data.getIPsID();
		List<java.lang.CharSequence> IPsIP = data.getIPsIP();
		List<java.lang.Integer> namesID = data.getNamesID();
		List<avro.ProjectPower.ClientType> namesClientType = data.getNamesClientType();
		List<java.lang.Integer> temperaturesIDs = data.getTemperaturesIDs();

		/// added this part so that all the clients get notified in a small time window that there is a new controller (well, old one taken back)
		/// otherwise, some clients got informed very late, which means the downtime of some clients would be very big
		for (int i = 0; i < IPsID.size(); i++) {
			final int currentID = IPsID.get(i);
			if (new Integer(currentID).equals(new Integer(data.getPort()))){
				continue;
			}
			final String currentIP = IPsIP.get(i).toString();
			ClientType _currentType = null;

			for (int j = 0; j < namesID.size(); j++){
				if (new Integer(namesID.get(j)).equals(new Integer(currentID))){
					_currentType = namesClientType.get(j);
					break;
				}
			}
			final ClientType currentType = _currentType;

			if (currentIP == null || currentType == null){
				continue;
			}

			new Thread(this) {
				public void run() {
					DistController.this.notifyClientIAmServer(currentIP, currentID, currentType);

					Transceiver client = DistController.this.setupTransceiver(currentID, currentIP);

					if (client == null){
						return;
					}


					if (currentType == ClientType.SmartFridge){
						communicationFridge.Callback proxy;
						try {
							proxy = SpecificRequestor.getClient(communicationFridge.Callback.class, client);
							proxy.reLogin();
						} catch (Exception e) {}
					}


					if (currentType == ClientType.Light){
						LightComm.Callback proxy;
						try {
							proxy = SpecificRequestor.getClient(LightComm.Callback.class, client);
							proxy.reLogin();
						} catch (Exception e) {}
					}

					if (currentType == ClientType.User){
						communicationUser.Callback proxy;
						try {
							proxy = SpecificRequestor.getClient(communicationUser.Callback.class, client);
							proxy.reLogin();

						} catch (Exception e) {}
					}

					if (currentType == ClientType.TemperatureSensor){
						communicationTempSensor.Callback proxy;
						try {
							proxy = SpecificRequestor.getClient(communicationTempSensor.Callback.class, client);
							proxy.reLogin();
						} catch (Exception e) {}
					}
				}
			}.start();
		}

		return true;
	}

	@Override
	synchronized public void fridgeIsEmpty(int ID) {
		for (Integer currentID: f_names.keySet()){
			if (f_notConfirmed.contains(currentID) || f_names.get(currentID) != ClientType.User){
				continue;
			}
			try {
				Transceiver client = this.setupTransceiver(currentID, f_IPs.get(currentID));
				communicationUser proxy;
				proxy = SpecificRequestor.getClient(communicationUser.Callback.class, client);

				proxy.notifyFridgeEmpty(ID);

				client.close();
			} catch (Exception e) {}
		}
	}

	@Override
	synchronized public List<Double> getTempHistory() throws AvroRemoteException {
		return this.getTemperatureHistory();
	}

	private boolean usersInside(){
		/// TODO test
		for (Integer currentID: f_names.keySet()){
			ClientType currentType = f_names.get(currentID);
			String currentIP = f_IPs.get(currentID);

			if (currentIP == "" || currentIP == null || currentType == null){
				continue;
			}

			if (currentType == ClientType.User){
				Transceiver client = this.setupTransceiver(currentID, currentIP);

				try {
					communicationUser.Callback proxy =
							SpecificRequestor.getClient(communicationUser.Callback.class, client);
					System.out.println(proxy.getStatus().toString());
					if (proxy.getStatus() == UserStatus.present){
						this.wasteLights();
						return true;
					}
					client.close();

				} catch (Exception e) {
					continue;
				}
			}
		}
		this.saveLights();
		return false;
	}

	private void saveLights(){
		for (Integer currentID: f_names.keySet()){
			ClientType currentType = f_names.get(currentID);
			String currentIP = f_IPs.get(currentID);

			if (currentIP == "" || currentIP == null || currentType == null){
				continue;
			}

			if (currentType == ClientType.Light){
				Transceiver client = this.setupTransceiver(currentID, currentIP);

				try {
					LightComm.Callback proxy =
							SpecificRequestor.getClient(LightComm.Callback.class, client);
					proxy.powerSavingMode();
					client.close();
				} catch (Exception e) {
					continue;
				}
			}
		}
	}

	private void wasteLights(){
		for (Integer currentID: f_names.keySet()){
			ClientType currentType = f_names.get(currentID);
			String currentIP = f_IPs.get(currentID);

			if (currentIP == "" || currentIP == null || currentType == null){
				continue;
			}

			if (currentType == ClientType.Light){
				Transceiver client = this.setupTransceiver(currentID, currentIP);

				try {
					LightComm.Callback proxy =
							SpecificRequestor.getClient(LightComm.Callback.class, client);
					proxy.powerWastingMode();
					client.close();
				} catch (Exception e) {
					continue;
				}
			}
		}
	}

	@Override
	synchronized public void leftHome(int ID) {
		/// TODO test
		if (f_names.get(ID) == null || f_IPs.get(ID) == null || f_IPs.get(ID) == ""){
			return;
		}

		boolean usersPresent = false;
		for (Integer currentID: f_names.keySet()){
			ClientType currentType = f_names.get(currentID);
			String currentIP = f_IPs.get(currentID);

			if (currentIP == "" || currentIP == null || currentType == null){
				continue;
			}

			if (currentType == ClientType.User){
				Transceiver client = this.setupTransceiver(currentID, currentIP);

				try {
					communicationUser.Callback proxy =
							SpecificRequestor.getClient(communicationUser.Callback.class, client);
					proxy.notifyUserLeft(ID);
					if (proxy.getStatus() == UserStatus.present){
						usersPresent = true;
					}
					client.close();

				} catch (Exception e) {
					continue;
				}
			}
		}

		if (! usersPresent){
			for (Integer currentID: f_names.keySet()){
				ClientType currentType = f_names.get(currentID);
				String currentIP = f_IPs.get(currentID);

				if (currentIP == "" || currentIP == null || currentType == null){
					continue;
				}

				if (currentType == ClientType.Light){
					Transceiver client = this.setupTransceiver(currentID, currentIP);

					try {
						LightComm.Callback proxy =
								SpecificRequestor.getClient(LightComm.Callback.class, client);
						proxy.powerSavingMode();
						client.close();
					} catch (Exception e) {
						continue;
					}
				}
			}
		}
	}

	@Override
	synchronized public void enteredHome(int ID) {
		/// TODO test
		if (f_names.get(ID) == null || f_IPs.get(ID) == null || f_IPs.get(ID) == ""){
			return;
		}

		for (Integer currentID: f_names.keySet()){
			ClientType currentType = f_names.get(currentID);
			String currentIP = f_IPs.get(currentID);

			if (currentIP == "" || currentIP == null || currentType == null){
				continue;
			}

			if (currentType == ClientType.User){
				Transceiver client = this.setupTransceiver(currentID, currentIP);

				try {
					communicationUser.Callback proxy =
							SpecificRequestor.getClient(communicationUser.Callback.class, client);
					proxy.notifyUserEntered(ID);
					client.close();

				} catch (Exception e) {
					continue;
				}
			}
		}

		for (Integer currentID: f_names.keySet()){
			ClientType currentType = f_names.get(currentID);
			String currentIP = f_IPs.get(currentID);

			if (currentIP == "" || currentIP == null || currentType == null){
				continue;
			}

			if (currentType == ClientType.Light){
				Transceiver client = this.setupTransceiver(currentID, currentIP);

				try {
					LightComm.Callback proxy =
							SpecificRequestor.getClient(LightComm.Callback.class, client);
					proxy.powerWastingMode();
					client.close();
				} catch (Exception e) {
					continue;
				}
			}
		}
	}

	public static void main(String[] args) {
		Logger.getLogger().f_active = true;
		String serverIP = "";
		int controllerPort = 0;
		try {
			serverIP = System.getProperty("ip");
			controllerPort = Integer.parseInt(System.getProperty("controllerport"));			
		} catch (Exception e) {
			System.err.println("Not all arguments have been given (correctly) when running the program.\nNeeded arguments:(\"ip\", \"controllerport\")");
			System.exit(1);
		}
		DistController controller = new DistController(controllerPort, 10, serverIP);
	}

}
