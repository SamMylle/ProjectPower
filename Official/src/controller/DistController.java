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

import util.Logger;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import avro.ProjectPower.*;

public class DistController implements ControllerComm, Runnable{
	
		private Controller f_controller;
		private HashMap<Integer, Transceiver> f_transceivers;
		private Server f_server;
		private Thread f_serverThread;
		private int f_myPort;
		private boolean f_serverActive;
		
		public DistController(int port){
			f_controller = new Controller(port + 1, 10);
			f_transceivers = new HashMap<Integer, Transceiver>();
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
			}catch(InterruptedException e){}
		}
		
		public void setupTransceiver(ClientType type, int ID){
			try{
				Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(ID));
				f_transceivers.put(new Integer(ID), client);
			}catch(IOException e){
				System.err.println("Error connecting to the smartFridge server...");
				e.printStackTrace(System.err);
				System.exit(1);
			}
			
		}
		
		@Override
		public int getID(ClientType clientType) throws AvroRemoteException{
			Logger.getLogger().log("give new ID");
			int newID = f_controller.giveNextID(clientType);
			return newID;
		}
		
		@Override
		public ClientType getClientType(int ID) throws AvroRemoteException{
			return f_controller.getClientType(ID);
		}
		
		@Override
		public Void logOff(int ID) throws AvroRemoteException{
			/// Remove ID from the system
			/// TODO, special case when the client is a temperatureSensor
			f_controller.removeID(ID);
			
			/// If there's a transceiver present, remove it
			Transceiver toRemove = f_transceivers.get(ID);
			
			if (toRemove != null){
				try {
					Logger.getLogger().log("k");
					toRemove.close();
					Logger.getLogger().log("k2");
				} catch (IOException e) {
					System.err.println("[error]Failed to disconnect client from server");
					e.printStackTrace(System.err);
					/// TODO return false?
					System.exit(1);
				}
				Logger.getLogger().log("k3");
				f_transceivers.remove(ID);
				Logger.getLogger().log("k4");
			}else{

				Logger.getLogger().log("NOT");
			}
			return null;
		}
		
		@Override
		public java.lang.Void addTemperature(int ID, double temperature) throws AvroRemoteException{
			f_controller.addTemperature(temperature, ID);
			return null;
		}
		
		@Override
		public double averageCurrentTemperature() throws AvroRemoteException{
			return f_controller.averageCurrentTemp();
		}
		
		@Override
		public boolean hasValidTemperatures() throws AvroRemoteException{
			return f_controller.hasValidTemperatures();
		}
		
		@Override
		public int setupFridgeCommunication(int ID) throws AvroRemoteException {
			try {
				if (f_transceivers.get(new Integer(ID)) == null){
					/// Fridge is not in the system
					return -1;
				}
				/// Connect to fridge
				communicationFridge.Callback proxy =
						SpecificRequestor.getClient(communicationFridge.Callback.class,
								f_transceivers.get(new Integer(ID)));
				
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
		public Void listenToMe(int port, ClientType type) throws AvroRemoteException {
			/// Remote call by e.g. a fridge, to indicate the server can reach him on this port (usually the ID of the client)
			// TODO check if the ID is in the system and in the transceivers
			// TODO maybe change retval to boolean
			this.setupTransceiver(type, port);
			return null;
		}
		
		public static void main(String[] args) {
			DistController controller = new DistController(5000);
			
			while(! controller.serverIsActive()){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					Logger.getLogger().log("Server startup error");
					e.printStackTrace();
				}
			}
			Logger.getLogger().log("Server started");
			try {
				System.in.read();
				controller.f_transceivers.clear();
				Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(5001));

				LightComm.Callback proxy =
						SpecificRequestor.getClient(LightComm.Callback.class, client);
				
				CallFuture<Integer> future = new CallFuture<Integer>(); 
				proxy.getState(future);
				int ID = future.get();
				
				Logger.getLogger().log(new Integer(ID).toString());
			} catch (IOException | InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

}
