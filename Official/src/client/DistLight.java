package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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

public class DistLight implements Runnable, LightComm{
	public Light f_light;
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverActive;
	private int f_serverPort;
	private String f_ip;
	private String f_serverip;


	public DistLight(String ip, String serverIP){
		f_light = new Light();
		f_server = null;
		f_serverThread = null;
		f_serverActive = false;
		f_serverPort = -1;
		f_ip = new String(ip);
		f_serverip = new String(serverIP);
	}

	@Override
	public void run() {
		while(!f_serverActive){
			try {
				f_server = new SaslSocketServer(
						new SpecificResponder(LightComm.class,
								this), new InetSocketAddress(f_ip, f_light.getID()));
				f_server.start();
				f_serverActive = true;
			} catch (IOException e) {
				Transceiver transceiver;
				try {
					System.out.println("PORT " + f_serverPort);
					transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_serverip, f_serverPort));
					/// Get the proxy
					ControllerComm.Callback proxy =
							SpecificRequestor.getClient(ControllerComm.Callback.class, transceiver);
					
					/// Get your ID from the server
					int ID = proxy.retryLogin(f_light.getID(), ClientType.Light);
					
					f_light.setID(ID);
					
				} catch (IOException e1) {
					System.exit(1);
				}
	
			}
		}
		try{
			f_server.join();
		}catch(InterruptedException e){
			f_server.close();
		}
	}

	public boolean addressInUse(){ 

		boolean available = true; 
		try {
			new Socket(f_ip, f_light.getID()).close();
		} 
		catch (Exception e){
			available = false;
		}
		return available;   
	}

	public boolean serverRunning(){
		return f_serverActive;
	}

	public int getServerPort(){
		return f_serverPort;
	}

	public void connectToServer(int port, String serverIP){
		try{
			/// Setup connection
			Transceiver transceiver =
					new SaslSocketTransceiver(new InetSocketAddress(serverIP, port));

			/// Get the proxy
			ControllerComm.Callback proxy =
					SpecificRequestor.getClient(ControllerComm.Callback.class, transceiver);

			/// Get your ID from the server
			int ID = proxy.LogOn(ClientType.Light, f_ip);

			f_light.setID(ID);
			f_serverPort = port;

			f_serverThread = new Thread(this);

			f_serverThread.start();

			while(! f_serverActive){
				Thread.sleep(50);
			}
			proxy.loginSuccessful(f_light.getID());
			transceiver.close();

			//Logger.getLogger().log("j21");
			///proxy.listenToMe(f_light.getID(), ClientType.Light);


		}catch(InterruptedException e){
			System.err.println("Interrupted...");
			System.exit(1);
		}catch(IOException e){
			/// Server isn't running, just return
			f_serverPort = -1;
			return;
		}
	}

	public void disconnect(){
		try{
			Transceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_serverip, f_serverPort));

			if (transceiver != null){
				ControllerComm.Callback proxy =
						SpecificRequestor.getClient(ControllerComm.Callback.class, transceiver);
				proxy.logOff(f_light.getID());
				transceiver.close();
			}

			if (f_server != null){
				f_serverThread.interrupt();
			}

			f_serverPort = -1;
			f_light.reset();
			f_serverActive = false;

		}catch(IOException e){
			System.err.println("Error logging of...");
			System.exit(1);
		}catch(java.lang.NullPointerException e){
			// TODO remove catch
		}
	}

	@Override
	public Void setState(int state) throws AvroRemoteException {
		f_light.setState(state);
		Logger.getLogger().log("Setting to: ", false);
		Logger.getLogger().log(new Integer(f_light.getState()).toString());
		return null;
	}

	@Override
	public int getState() throws AvroRemoteException {
		Logger.getLogger().log("returning: ", false);
		Logger.getLogger().log(new Integer(f_light.getState()).toString());
		return f_light.getState();
	}

	@Override
	public boolean aliveAndKicking() throws AvroRemoteException {
		return true;
	}

	@Override
	public Void newServer(CharSequence newServerIP, int newServerID) {
		/// TODO test
		f_serverip = newServerIP.toString();
		f_serverPort = newServerID;
		return null;
	}

	@Override
	public void reLogin() {
		/// Assumes valid stuff 

		f_serverThread.interrupt();
		f_server = null;
		f_serverThread = null;
		f_serverActive = false;

		this.connectToServer(f_serverPort, f_serverip);
	}

	@Override
	public void powerSavingMode() {
		f_light.powSavingMode();
	}

	@Override
	public void powerWastingMode() {
		f_light.recoverFromPowerSavingMode();
	}

	public static void main(String[] args) {
		String clientIP = "";
		String serverIP = "";
		int controllerPort = 0;
		try {
			clientIP = System.getProperty("clientip");
			serverIP = System.getProperty("ip");
			controllerPort = Integer.parseInt(System.getProperty("controllerport"));			
		} catch (Exception e) {
			System.err.println("Not all arguments have been given (correctly) when running the program.\nNeeded arguments:(\"ip\", \"clientip\", \"controllerport\")");
			System.exit(1);
		}
		
		DistLight newLight = new DistLight(clientIP, serverIP);
		newLight.connectToServer(controllerPort, serverIP);
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
		newLight.disconnect();
		//		Logger.getLogger().log("disconnected\n");
		System.exit(0);
	}
}
