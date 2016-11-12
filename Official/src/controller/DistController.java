package controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.avro.AvroRemoteException ;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificResponder;
import avro.proto.ControllerComm;

import avro.proto.ClientType;

public class DistController implements ControllerComm{
	
		private Controller controller;
		
		public DistController(){
			controller = new Controller(10);
		}
		
		@Override
		public int getID(ClientType clientType) throws AvroRemoteException{
			return controller.giveNextID(clientType);
		}
		
		@Override
		public ClientType getClientType(int ID) throws AvroRemoteException{
			return controller.getClientType(ID);
		}
		
		@Override
		public boolean logOff(int ID) throws AvroRemoteException{
			controller.removeID(ID);
			return true;
		}
		
		@Override
		public java.lang.Void addTemperature(int ID, double temperature) throws AvroRemoteException{
			controller.addTemperature(temperature, ID);
			return null;
		}
		
		@Override
		public double averageCurrentTemperature(java.lang.Void nullVal) throws AvroRemoteException{
			return controller.averageCurrentTemp();
		}
		
		@Override
		public boolean hasValidTemperatures(java.lang.Void nullVal) throws AvroRemoteException{
			return controller.hasValidTemperatures();
		}
		
		public static void main(String[] args) {
			Server server = null;
			try{
				server = new SaslSocketServer(
						new SpecificResponder(ControllerComm.class,
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
