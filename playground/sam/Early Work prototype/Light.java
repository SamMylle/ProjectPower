package client;

public class Light {
	/// Represents exactly what you think it does
	
	private int status;
	/// An integer instead of a boolean to support all possibilities (e.g. dimming light)
	private int ID;
	/// A unique ID, uniqueness is guaranteed by the controller
	
	
	public Light(){
		/// Makes a light
		/// The standard status is 0
		/// The standard ID is invalid (-1)
		status = 0;
		ID = -1;
	}
	
	public boolean setID(int _id){
		/// _id must be non-negative
		if (_id >= 0){
			ID = _id;
			return true;
		}else{
			return false;
		}
	}
	
	public int getID(){
		return ID;
	}
	
	public int getState(){
		return status;
	}
	
	public boolean setState(int _status){
		/// _status must be non-negative
		if(_status >= 0){
			status = _status;
			return true;
		}else{
			return false;
		}
	}
}
