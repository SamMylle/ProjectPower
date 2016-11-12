package controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.avro.AvroRemoteException ;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificResponder;
import controller.avro.proto.IDAssignment;

import util.ClientType;
import util.Logger;

public class DistController implements IDAssignment{
	
		private Controller controller;
		
		public DistController(){
			controller = new Controller(10);
		}
		
		public ClientType getClientType(String type){
			if (type.equals("Light")){
				return ClientType.Light;
			}
			if (type.equals("User")){
				return ClientType.User;
			}
			if (type.equals("Fridge")){
				return ClientType.Fridge;
			}
			if (type.equals("TemperatureSensor")){
				return ClientType.TemperatureSensor;
			}
			
			return null;
		}
		
		@Override
		public int getID(CharSequence clientType) throws AvroRemoteException{
			int ID = -1;
			ClientType type = this.getClientType(clientType.toString());
			
			if (type != null){
				ID = controller.giveNextID(type);
			}
			
			return ID;
		}
		public static void main(String[] args) {
			Server server = null;
			try{
				server = new SaslSocketServer(
						new SpecificResponder(IDAssignment.class,
						new DistController()), new InetSocketAddress(6789));
			}catch(IOException e){
				System.err.println("[error]Failed to start server");
				e.printStackTrace(System.err);
				System.exit(1);
			}
			server.start();
			try{
				server.join();
			}catch(InterruptedException e){}
		}

}
