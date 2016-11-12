package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.apache.avro.ipc.CallFuture;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;

import controller.avro.proto.IDAssignment;

public class DistLight {
	private Light light;
	
	DistLight(){
		light = new Light();
	}
	
	public static void main(String[] args) {
		try{
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(6789));
			IDAssignment.Callback proxy = SpecificRequestor.getClient(IDAssignment.Callback.class,client);
			CallFuture<Integer> future = new CallFuture<Integer>(); 
			proxy.getID("Light", future);
			System.out.println(future.get());
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
