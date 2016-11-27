package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.Scanner;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.CallFuture;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import thread.ServerThread;
import util.Logger;

import avro.ProjectPower.*;

public class DistLight implements Runnable, LightComm {
	public Light f_light;
	public Transceiver f_transceiver;
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverActive;
	
	
	DistLight(){
		f_light = new Light();
		f_transceiver = null;
		f_server = null;
		f_serverThread = null;
		f_serverActive = false;
	}

	@Override
	public void run() {
		try {
			f_server = new SaslSocketServer(
					new SpecificResponder(LightComm.class,
					this), new InetSocketAddress(f_light.getID()));
		} catch (IOException e) {
			System.err.println("[error]Failed to start light server");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		f_server.start();
		f_serverActive = true;
		try{
			f_server.join();
		}catch(InterruptedException e){}
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
			f_light.setID(ID);
			
			f_serverThread = new Thread(this);
			f_serverThread.start();
			

			while(! f_serverActive){
				Thread.sleep(1000);
			}

			//Logger.getLogger().log("j21");
			proxy.listenToMe(f_light.getID(), ClientType.Light);
			
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
	
	public void disconnect(){
		try{
			if (f_transceiver != null){
				ControllerComm.Callback proxy =
						SpecificRequestor.getClient(ControllerComm.Callback.class, f_transceiver);
				Logger.getLogger().log("kkk000");
				proxy.logOff(f_light.getID());

				Logger.getLogger().log("kkk1");
				f_transceiver.close();
				Logger.getLogger().log("kkk");
			}

			if (f_server != null){
				f_server.close();
			}
		}catch(IOException e){
			System.err.println("Error connecting to server...");
			e.printStackTrace(System.err);
			System.exit(1);
		}catch(java.lang.NullPointerException e){
			Logger.getLogger().log("nullptr");
		}
	}

	@Override
	public Void setState(int state) throws AvroRemoteException {
		f_light.setState(state);
		return null;
	}

	@Override
	public int getState() throws AvroRemoteException {
		return f_light.getState();
	}
	
	public static void main(String[] args) {
		DistLight newLight = new DistLight();
		newLight.connectToServer(5000);
		/*try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		newLight.disconnect();
		Logger.getLogger().log("disconnected\n");*/
	}
}
