package client;

import util.Logger;

enum UserStatus {present, absent};
public class User {

	private int ID;
	private UserStatus status;
	
	public User() {
		ID = -1;
		status = UserStatus.absent; //the user is not present untill he is connected to the system
	}
	
	public void setID(int _ID) {
		assert _ID >= 0; //change later on with custom exceptions
		
		ID = _ID;
	}
	
	public void enter() {
		status = UserStatus.present;
		Logger.getLogger().log("User with ID " + ID + " has entered the system.");
	}
	
	public void leave() {
		status = UserStatus.absent;
		Logger.getLogger().log("User with ID " + ID + " has left the system.");
	}

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		User user = new User();
		user.setID(5);
		user.enter();
		user.leave();

	}

}
