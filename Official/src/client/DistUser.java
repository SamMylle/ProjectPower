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

public class DistUser implements communicationUser, Runnable {
	
	private User f_user;
	private int f_controllerPort;
	
	private Server f_server;
	private Thread f_serverThread;
	private boolean f_serverReady;
	
	
	public DistUser(int controllerPort) {
		assert controllerPort >= 1000;
		
		f_user = new User();
		f_controllerPort = controllerPort;
		f_serverReady = false;
		
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			f_user.setID(proxy.getID(User.type));
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
		
		registerToController();
	}
	
	public boolean logOffController() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.logOff(f_user.getID());
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
	
	public void registerToController() {
		try {
			SaslSocketTransceiver transceiver = new SaslSocketTransceiver(new InetSocketAddress(f_controllerPort));
			ControllerComm proxy = (ControllerComm) SpecificRequestor.getClient(ControllerComm.class, transceiver);
			proxy.listenToMe(f_user.getID(), User.type);
		}
		catch (IOException e) {
			System.err.println("IOException in constructor for DistUser (listenToMe).");
			System.exit(1);
		}
	}
	
	@Override
	public void run() {
		try {
			f_server = new SaslSocketServer(
					new SpecificResponder(communicationUser.class, this), new InetSocketAddress(f_user.getID()) );
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
	public UserStatus getStatus() throws AvroRemoteException {
		return f_user.getStatus();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DistController controller = new DistController(6789);
		
		DistUser remoteUser = new DistUser(6789);
		
		try {
			Logger logger = Logger.getLogger();
			if (remoteUser.getStatus() == UserStatus.absent) {
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
