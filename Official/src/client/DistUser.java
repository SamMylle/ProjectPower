package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;

import util.Logger;
import controller.DistController;

import avro.ProjectPower.*;

public class DistUser extends User implements communicationUser, Runnable {
	
	private int f_controllerPort;
	
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverReady;
	
	
	public DistUser(int controllerPort) {
		super();
		assert controllerPort >= 1000;
		
		f_controllerPort = controllerPort;
		f_serverReady = false;
		
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			this.setID(proxy.getID(User.type));
		}
		catch (IOException e) {
			System.err.println("IOException in constructor for DistUser (getID).");
			System.exit(1);
		}
		
		f_serverThread = new Thread(this);
		f_serverThread.start();
		
		while (f_serverReady == false) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean logOffController() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.logOff(this.getID());
			return true;
		}
		catch (AvroRemoteException e) {
			System.out.println("AvroRemoteException at logOff() in DistSmartFridge.");
			return false;
		}
		catch (IOException e) {
			System.out.println("IOException at logOff() in DistSmartFridge.");
			return false;
		}
	}
	
	public void stopServer() {
		f_serverThread.interrupt();
		f_serverThread = null;
	}
	
	@Override
	public void run() {
		try {
			f_server = new SaslSocketServer(
					new SpecificResponder(communicationUser.class, this), new InetSocketAddress(this.getID()) );
			f_server.start();
		}
		catch (IOException e) {
			System.err.println("[error] Failed to start User server.");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		f_serverReady = true;

		try {
			f_server.join();
		}
		catch (InterruptedException e) {
			f_server.close();
			Logger.getLogger().log("Closed the DistUser server.");
		}
		
	}
	
	
	@Override
	public UserStatus getStatusRemote() throws AvroRemoteException {
		return this.getStatus();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DistController controller = new DistController(6789, 10);
		
		DistUser remoteUser = new DistUser(6789);
		
		try {
			Logger logger = Logger.getLogger();
			if (remoteUser.getStatusRemote() == UserStatus.absent) {
				logger.log("Should print this.");
			}
			
			
			if (remoteUser.logOffController() == true) {
				logger.log("Logged off succesfully.");
			}
			else {
				logger.log("Could not log off.");
			}
			remoteUser.stopServer();
			logger.log("Server stopped.");
		}
		catch (AvroRemoteException e) {
			System.err.println("AvroRemoteException at main class in DistSmartFridge.");
		}
		System.exit(0);
	}






}
