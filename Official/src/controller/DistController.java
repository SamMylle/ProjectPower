package controller;

import java.io.IOException;
import java.net.InetSocketAddress;
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

	public DistController(int port, int maxTemperatures){
		super(port + 1, maxTemperatures);
		
		//f_controller = new Controller(port + 1, 10);
		f_myPort = port;
		f_serverActive = false;

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
			f_server = new SaslSocketServer(
					new SpecificResponder(ControllerComm.class,
							this), new InetSocketAddress(f_myPort));
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
			Transceiver client = this.setupTransceiver(ID);

			/// Don't think this is necessary
			if (client == null){
				return -1;
			}

			/// Connect to fridge
			communicationFridge.Callback proxy =
					SpecificRequestor.getClient(communicationFridge.Callback.class, client);

			/// Ask the fridge if it's okay to connect a user to it
			if (proxy.requestFridgeCommunication() == true){
				return ID;
			}else{
				return -1;
			}
		}catch(IOException e){
			return -1;
		}
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
		DistController controller = new DistController(5000, 10);
		controller.stopServer();
		DistController controller2 = new DistController(5000, 10);
		controller2.stopServer();

		/*DistSmartFridge fridge = new DistSmartFridge(5000);
		try {
			fridge.addItemRemote("bacon");
			fridge.addItemRemote("parmesan cheese");
		} catch (AvroRemoteException e1) {
			// TODO Auto-generated catch block
			Logger.getLogger().log("woops");
		}

		Logger.getLogger().log("Servers started");

		try {
			List<CharSequence> items = controller.getFridgeInventory(5001);

			for (int i = 0; i < items.size(); i++){
				Logger.getLogger().log(items.get(i).toString());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}


}
