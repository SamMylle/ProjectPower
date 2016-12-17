package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.lang.Void;

import avro.ProjectPower.ClientType;
import util.Logger;



enum FridgeStatus {closed, open};

public class SmartFridge {

	private Set<String> f_items;
	private FridgeStatus f_status;
	private int f_ID;
	public static final ClientType type = ClientType.SmartFridge;
	
	
	public SmartFridge() {
		f_items = new HashSet<String>();
		f_status = FridgeStatus.closed;
		f_ID = 0;
	}
	
	public void setID(int ID) {
		assert ID != 0;
		
		f_ID = ID;
	}
	
	public int getID() {
		return f_ID;
	}
	
	public void addItem(String newItem) {
		assert f_status == FridgeStatus.open;
		
		f_items.add(newItem); //returns boolean to see if the element was added, might be useful later
	}
	
	public boolean removeItem(String oldItem) {
		assert f_items.isEmpty() == false;
		
		if (f_items.contains(oldItem) == false) {
			return false;
		}
		f_items.remove(oldItem);
		return true;
	}
	
	public boolean hasItem(String item) {
		assert item != "";
		
		return f_items.contains(item);
	}
	
	public Set<String> getItems() {
		return f_items;
	}
	
	boolean emptyInventory() {
		return f_items.isEmpty();
	}
	
	public void openFridge() {
		f_status = FridgeStatus.open;
	}
	
	public void closeFridge() {
		f_status = FridgeStatus.closed;
	}
	
	public boolean isOpen() {
		return f_status == FridgeStatus.open;
	}
	
	public String toString() {
		return "items: " + f_items;
	}
	
	public static void main(String[] args) {
		
		
		
		/// temporarily to test functionality
		/// 
		/// server should be opened in separate remote method, invoked by the server to allow direct communication with a user
		
		
		/*
		SmartFridge fridge = new SmartFridge();
		
		Server server = null;
		try {
			server = new SaslSocketServer(new SpecificResponder(communicationFridge.class, fridge),  new InetSocketAddress(6789));
		}
		catch (IOException e) {
			System.err.println("[error] Failed to start SmartFridge server");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		server.start();
		try {
			server.join();
		}
		catch (InterruptedException e) {}
		*/
		
		SmartFridge fridge = new SmartFridge();
		fridge.openFridge();
		fridge.addItem("a");
		fridge.addItem("b");
		fridge.addItem("a");
		fridge.addItem("c");
		System.out.println(fridge);
	}


}
