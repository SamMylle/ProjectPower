package client;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;

import avro.ProjectPower.*;
import util.Logger;

//enum UserStatus {present, absent};

public class User {
	private int ID;
	private UserStatus status;
	public static final ClientType type = ClientType.User;
	
	
	public User() {
		ID = -1;
		status = UserStatus.absent; //the user is not present until he is connected to the system
	}
	
	public void setID(int _ID) {
		assert _ID >= 0; //change later on with custom exceptions
		
		ID = _ID;
	}
	
	public int getID() {
		return ID;
	}
	
	public void enter() {
		status = UserStatus.present;
		Logger.getLogger().log("User with ID " + ID + " has entered the system.");
	}
	
	public void leave() {
		status = UserStatus.absent;
		Logger.getLogger().log("User with ID " + ID + " has left the system.");
	}

	public UserStatus getStatus() {
		return status;
	}
	
	public static void main(String[] args) {
		
		try {
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(6789));
			communicationFridge proxy = (communicationFridge) SpecificRequestor.getClient(communicationFridge.class, client);
			
			proxy.openFridgeRemote();
			
//			String[] itemlist = {"Cheese", "Butter", "Pizza", "Milk", "Carrots"};
			String[] itemlist = {"Bacon", "Cola", "Chocolate bars", "Eggs", "Pancakes"};
			
			for (int i = 0; i < itemlist.length; i++) {
				proxy.addItemRemote(itemlist[i]);
			}
			proxy.testMethod(ClientType.User);
			proxy.closeFridgeRemote();
			
			client.close();
		}
		catch (IOException e){
			System.err.println("Error connecting to the smartFridge server...");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		
		/*
		User user = new User();
		user.setID(5);
		user.enter();
		user.leave();
		 */
	}

}
