package client;

import avro.ProjectPower.*;
import util.Logger;


public class User {
	private int f_ID;
	private UserStatus f_status;
	private String f_name;
	public static final ClientType type = ClientType.User;
	
	
	public User(String name) {
		f_ID = -1;
		f_status = UserStatus.absent; //the user is not present until he is connected to the system
		f_name = name;
	}
	
	public void setID(int _ID) {
		assert _ID >= 0; //change later on with custom exceptions
		
		f_ID = _ID;
	}
	
	public int getID() {
		return f_ID;
	}
	
	public void enter() {
		f_status = UserStatus.present;
		Logger.getLogger().log("User with ID " + f_ID + " has entered the system.");
	}
	
	public void leave() {
		f_status = UserStatus.absent;
		Logger.getLogger().log("User with ID " + f_ID + " has left the system.");
	}

	public void _setStatus(UserStatus status) {
		f_status = status; 
	}
	
	public UserStatus _getStatus() {
		return f_status;
	}
	
	public String _getName() {
		return f_name;
	}
	
	public static void main(String[] args) {
		
		/*
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
		*/
		
		/*
		User user = new User();
		user.setID(5);
		user.enter();
		user.leave();
		 */
	}

}
