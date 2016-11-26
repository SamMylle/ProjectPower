package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.apache.avro.ipc.CallFuture;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;

import thread.ServerThread;
import util.Logger;

import avro.ProjectPower.*;

public class DistLight {
	public Light f_light;
	public Transceiver f_transceiver;
	private ServerThread f_server;
	private Thread f_serverThread;
	
	
	DistLight(){
		f_light = new Light();
		f_transceiver = null;
		f_server = null;
		f_serverThread = null;
	}
	
	public void connectToServer(int port){
		try{
			/// Setup connection
			f_transceiver = new SaslSocketTransceiver(new InetSocketAddress(port));
			
			/// Get your ID
			ControllerComm.Callback proxy =
					SpecificRequestor.getClient(ControllerComm.Callback.class, f_transceiver);
			CallFuture<Integer> future = new CallFuture<Integer>(); 
			proxy.getID(ClientType.Light, future);
			int ID = future.get();
			this.f_light.setID(ID);
			
			this.setupServer(ID);
			
		}catch(ExecutionException e){
			System.err.println("Error executing command on server (light)...");
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
	
	public void setupServer(int port){
		f_server = new ServerThread();
		f_server.setPort(port);
		f_serverThread = new Thread(f_server);
		f_serverThread.start();
	}
	
	public void disconnect(){
		try{
			if (f_transceiver != null){
				ControllerComm.Callback proxy =
						SpecificRequestor.getClient(ControllerComm.Callback.class, f_transceiver);
				CallFuture<Boolean> future = new CallFuture<Boolean>();
				proxy.logOff(f_light.getID(), future);
				
				future.get();
				
				f_transceiver.close();
			}
			
			if (f_serverThread != null){
				f_serverThread.wait();
				/// Garbage collection fixes this?
				f_serverThread = null;
				
			}
			if (f_server != null){
				f_server = null;
			}
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
	
	public static void main(String[] args) {
		DistLight newLight = new DistLight();
		newLight.connectToServer(5000);
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		newLight.disconnect();
	}
}
