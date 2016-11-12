package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.apache.avro.ipc.CallFuture;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;

import avro.ProjectPower.*;

public class DistLight {
	public Light light;
	
	DistLight(){
		light = new Light();
	}
	
	public static void main(String[] args) {
		try{
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(6789));
			ControllerComm.Callback proxy = SpecificRequestor.getClient(ControllerComm.Callback.class,client);
			CallFuture<Integer> future = new CallFuture<Integer>(); 
			proxy.getID(ClientType.Light, future);
			
			proxy.averageCurrentTemperature(null);
			
			DistLight distLight = new DistLight();
			distLight.light.setID(future.get());
			
			client.close();
		}catch(ExecutionException e){
			System.err.println("Error executing command on server...");
			e.printStackTrace(System.err);
			System.exit(1);
		}catch(InterruptedException e){
			System.err.println("Interrupted...");
			e.printStackTrace(System.err);
			System.exit(1);
		}catch(IOException e){
			System.err.println("Error connecting to server...");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
