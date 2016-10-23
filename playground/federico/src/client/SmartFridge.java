package client;

import java.util.HashSet;
import java.util.Set;


enum FridgeStatus {closed, open};

public class SmartFridge {

	private Set<String> items;
	private FridgeStatus status;
	
	
	public SmartFridge() {
		items = new HashSet<String>();
		status = FridgeStatus.closed;
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
	
	public void closeFrigde() {
		status = FridgeStatus.closed;
	}
	
	public String toString() {
		return "items: " + items;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		SmartFridge fridge = new SmartFridge();
		fridge.openFridge();
		fridge.addItem("a");
		fridge.addItem("b");
		fridge.addItem("a");
		fridge.addItem("c");
		System.out.println(fridge);
	}

}
