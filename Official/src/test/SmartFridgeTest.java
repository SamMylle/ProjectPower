package test;

import client.SmartFridge;
import org.junit.Test;
import static org.junit.Assert.*;

public class SmartFridgeTest {
	SmartFridge smartfridge;
	
	@Test
	public void testItems() {
		smartfridge = new SmartFridge();
		smartfridge.openFridge();
		
		smartfridge.addItem("butter");
		smartfridge.addItem("orange juice");
		smartfridge.removeItem("orange juice");

		
		assertFalse(smartfridge.hasItem("orange juice"));
		assertFalse(smartfridge.hasItem("butte"));
		assertTrue(smartfridge.hasItem("butter"));
		
		smartfridge.addItem("butter");
		assertTrue(smartfridge.hasItem("butter"));
	}
	
	@Test
	public void testOpenClose() {
		smartfridge = new SmartFridge();
		
		assertFalse(smartfridge.isOpen());
		smartfridge.openFridge();
		assertTrue(smartfridge.isOpen());
		smartfridge.closeFridge();
		assertFalse(smartfridge.isOpen());
	}
	
	@Test
	public void testID() {
		smartfridge = new SmartFridge();
		
		assertNotEquals(smartfridge.getID(), 2);
		assertEquals(smartfridge.getID(), 0);
		
		smartfridge.setID(5432);
		assertNotEquals(smartfridge.getID(), 0);
		assertEquals(smartfridge.getID(), 5432);
	}
}
