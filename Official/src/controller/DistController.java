package controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.avro.AvroRemoteException ;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import util.Logger;
import thread.ServerThread;

import java.util.HashMap;

import avro.ProjectPower.*;

public class DistController implements ControllerComm{
	
		private Controller f_controller;
		private HashMap<Integer, Transceiver> f_transceivers;
		private ServerThread server;
		private Thread serverThread;
		
		public DistController(int port){
			f_controller = new Controller(port + 1, 10);
			f_transceivers = new HashMap<Integer, Transceiver>();
			
			server = new ServerThread();
			server.setPort(port);
			serverThread = new Thread(server);
			serverThread.start();
			Logger.getLogger().log("when no threads were made, this would've never printed");
			Logger.getLogger().log(
					"Note that the server is delayed (it takes longer to start then printing this stuff)");
			Logger.getLogger().log("Have a look at the constructor");
		}
		
		public void setupTransceiver(ClientType type, int ID){
			try{
				Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(ID));
				f_transceivers.put(new Integer(ID), client);
				Logger.getLogger().log("putting ");
				Logger.getLogger().log(f_transceivers.toString());
			}catch(IOException e){
				System.err.println("Error connecting to the smartFridge server...");
				e.printStackTrace(System.err);
				System.exit(1);
			}
			
		}
		
		@Override
		public int getID(ClientType clientType) throws AvroRemoteException{
			int newID = f_controller.giveNextID(clientType);
			return newID;
		}
		
		@Override
		public ClientType getClientType(int ID) throws AvroRemoteException{
			return f_controller.getClientType(ID);
		}
		
		@Override
		public boolean logOff(int ID) throws AvroRemoteException{
			f_controller.removeID(ID);
			Transceiver toRemove = f_transceivers.get(ID);
			if (toRemove != null){
				try {
					toRemove.close();
				} catch (IOException e) {
					System.err.println("[error]Failed to disconnect (server to client)");
					e.printStackTrace(System.err);
					System.exit(1);
				}
				f_transceivers.remove(ID);
			}
			
			return true;
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
				Logger.getLogger().log(new Integer(ID).toString());
				if (f_transceivers.get(new Integer(ID)) == null){
					Logger.getLogger().log("not good\n");
					return -1;
				}
				communicationFridge.Callback proxy =
						SpecificRequestor.getClient(communicationFridge.Callback.class,
								f_transceivers.get(new Integer(ID)));
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
			// TODO check if the ID is in the system and in the transceivers
			// TODO maybe change retval to boolean
			this.setupTransceiver(type, port);
			return null;
		}
		
		public static void main(String[] args) {
			DistController controller = new DistController(5000);
		}

}
