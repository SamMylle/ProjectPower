package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.lang.Void;
import util.Logger;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificResponder;

import avro.client.proto.ClientType;
import avro.client.proto.communicationFridge;


enum FridgeStatus {closed, open};

public class SmartFridge implements communicationFridge{

	private Set<String> items;
	private FridgeStatus status;
	private Server fridgeserver;
	
	
	public SmartFridge() {
		items = new HashSet<String>();
		status = FridgeStatus.closed;
		fridgeserver = null;
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
	
	public void openFridge() {
		status = FridgeStatus.open;
	}
	
	public void closeFridge() {
		status = FridgeStatus.closed;
	}
	
	public String toString() {
		return "items: " + items;
	}
	
	@Override
	public boolean addItemRemote(CharSequence itemName) {
		try {
			this.addItem(itemName.toString());
		} catch(Exception e) {
			return false;
		}
		Logger.getLogger().log(this.toString());
		return true;
	}
	
	@Override
	public boolean openFridgeRemote() {
		try {
			this.openFridge();
		} catch(Exception e) {
			return false;
		}
		Logger.getLogger().log("The fridge has been opened.");
		return true;
	}
	
	@Override
	public boolean closeFridgeRemote() {
		try {
			this.closeFridge();
		} catch(Exception e) {
			return false;
		}
		Logger.getLogger().log("The fridge has been closed.");
		return true;
	}
	
	@Override
	public Void setupServer(int port) {
		assert fridgeserver == null;
		
		try {
			fridgeserver = new SaslSocketServer(new SpecificResponder(communicationFridge.class, this), new InetSocketAddress(port));
		} catch(IOException e) {
			System.err.println("[error] Failed to start SmartFridge server");
			e.printStackTrace(System.err);
			System.exit(1);
		}
		
		fridgeserver.start();
		try {
			fridgeserver.join();
		} catch (InterruptedException e) {
			System.err.println("Couldn't join the SmartFridge server.");
		}
		
		return null;
	}
	
	@Override
	public Void closeServer() {
		assert fridgeserver != null;
		
		fridgeserver.close();
		return null;
	}

	@Override
	public boolean testMethod(ClientType clienttype) throws AvroRemoteException {
		// TODO Auto-generated method stub
		
		if (clienttype == ClientType.User) {
			System.out.println("User used this method.");
		}
		
		return false;
	}
	
	public static void main(String[] args) {
		
		
		
		/// temporarily to test functionality
		/// 
		/// server should be opened in separate remote method, invoked by the server to allow direct communication with a user
		
		
		SmartFridge fridge = new SmartFridge();
		
		fridge.setupServer(6789);
		
		
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
		
		
		/*
		SmartFridge fridge = new SmartFridge();
		fridge.openFridge();
		fridge.addItem("a");
		fridge.addItem("b");
		fridge.addItem("a");
		fridge.addItem("c");
		System.out.println(fridge);
		*/
	}


}
