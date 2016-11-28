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

	private Set<String> items;
	private FridgeStatus status;
	private int f_ID;
	public static final ClientType type = ClientType.SmartFridge;
	
	
	public SmartFridge() {
		items = new HashSet<String>();
		status = FridgeStatus.closed;
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
		assert status == FridgeStatus.open;
		
		items.add(newItem); //returns boolean to see if the element was added, might be useful later
	}
	
	public void removeItem(String oldItem) {
		assert items.isEmpty() == false;
		
		if (items.contains(oldItem) == false) {
			return;
		}
		items.remove(oldItem);
		
		if (items.isEmpty() == true) {
			System.out.println("send broadcast"); //placeholder function
		}
	}
	
	public boolean hasItem(String item) {
		assert item != "";
		
		return items.contains(item);
	}
	
	public Set<String> getItems() {
		return items;
	}
	
	public void openFridge() {
		status = FridgeStatus.open;
	}
	
	public void closeFridge() {
		status = FridgeStatus.closed;
	}
	
	public boolean isOpen() {
		return status == FridgeStatus.open;
	}
	
	public String toString() {
		return "items: " + items;
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
