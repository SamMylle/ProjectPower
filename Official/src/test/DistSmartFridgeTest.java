package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.avro.AvroRemoteException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import avro.ProjectPower.Client;
import avro.ProjectPower.ClientType;
import avro.ProjectPower.UserStatus;
import client.*;
import client.exception.AbsentException;
import client.exception.FridgeOccupiedException;
import client.exception.MultipleInteractionException;
import client.exception.NoFridgeConnectionException;
import controller.*;
import util.SuppressSystemOut;


public class DistSmartFridgeTest {
//	static SuppressSystemOut suppress;
//	@BeforeClass
//	public static void setUpBeforeClass() throws Exception {
//		suppress = new SuppressSystemOut();
//		suppress.suppressOutput();
//	}
//
//	@AfterClass
//	public static void tearDownAfterClass() throws Exception {
//		suppress.activateOutput();
//	}
//
//	@Before
//	public void setUp() throws Exception {
//	}
	
	
	@Test
	public void testControllerServerMethods() {
		final int controllerPort = 5000;
		DistController controller = new DistController(controllerPort, 10);
		DistSmartFridge fridge = new DistSmartFridge(controllerPort);
		
		/// making sure the fridge is entered correctly in the system
		List<Client> clients = null;
		try {
			clients = controller.getAllClients();
		} catch (AvroRemoteException e) {
			e.printStackTrace();
		}
		assertEquals(clients.size(), 1);
		assertEquals(clients.get(0).clientType, ClientType.SmartFridge);
		
		
		/// testing the getItems method
		List<CharSequence> items = null;
		try {
			items = controller.getFridgeInventory(controllerPort+1);
		} catch (AvroRemoteException e) {
			e.printStackTrace();
		}
		assertEquals(items.size(), 0);
		
		fridge.addItem("cheese");
		fridge.addItem("apple");
		try {
			items = controller.getFridgeInventory(controllerPort+1);
		} catch (AvroRemoteException e) {
			e.printStackTrace();
		}
		assertEquals(items.size(), 2);
		assertEquals(items.get(0).toString(), "apple");
		assertEquals(items.get(1).toString(), "cheese");
		/// apple first since the items get ordered alphabetically in the set of string, inside the smartfridge
		
		fridge.removeItem("cheese");
		fridge.removeItem("parmaham");
		try {
			items = controller.getFridgeInventory(controllerPort+1);
		} catch (AvroRemoteException e) {
			e.printStackTrace();
		}
		assertEquals(items.size(), 1);
		assertEquals(items.get(0).toString(), "apple");
		
		fridge.logOffController();
		fridge.stopServerController();
		controller.stopServer();
	}
	
	@Test
	public void testUserServerMethods() {
		/// setup
		final int controllerPort = 5000;
		DistController controller = new DistController(controllerPort, 10);
		DistSmartFridge fridge = new DistSmartFridge(controllerPort);
		DistUser user = new DistUser(controllerPort, "Federico Quin");
		DistUser user2 = new DistUser(controllerPort, "Sam Mylle");
		
		Exception ex = null;
		
		assertEquals(user._getStatus(), UserStatus.present);
		try {
			user.communicateWithFridge(controllerPort+1);
		} catch (MultipleInteractionException e) {
			ex = e;
		} catch (AbsentException e) {
			ex = e;
		} catch (FridgeOccupiedException e) {
			ex = e;
		}
		assertEquals(ex, null);
		
		/// * actual tests below
		
		assertEquals(user2._getStatus(), UserStatus.present);
		try {
			user2.communicateWithFridge(controllerPort+1);
		} catch (MultipleInteractionException e) {
			ex = e;
		} catch (AbsentException e) {
			ex = e;
		} catch (FridgeOccupiedException e) {
			ex = e;
		}
		assertNotEquals(ex, null);
		ex = null;
		
		try {
			user.openFridge();
			user.addItemFridge("cheese");
			user.addItemFridge("soda");
			
			List<CharSequence> items = user.getFridgeItemsDirectly();
			Set<String> items2 = fridge.getItems();
			
			assertEquals(items.size(), items2.size());
			assertEquals(items.size(), 2);
			assertEquals(items.get(0).toString(), "cheese");
			assertTrue(items2.contains("cheese"));
			assertEquals(items.get(1).toString(), "soda");
			assertTrue(items2.contains("soda"));
			
			user.removeItemFridge("cheese");
			items = user.getFridgeItemsDirectly();
			items2 = fridge.getItems();
			
			assertEquals(items.size(), items2.size());
			assertEquals(items.size(), 1);
			assertEquals(items.get(0).toString(), "soda");
			assertTrue(items2.contains("soda"));
			assertFalse(items2.contains("cheese"));
			
			user.closeFridge();
		} catch (NoFridgeConnectionException e) {
			e.printStackTrace();
		} catch (AbsentException e) {
			e.printStackTrace();
		}
		assertEquals(ex, null);
	}
	
	@Test
	public void testEmptyInventoryBroadcast() {
		// TODO add this test
	}
	
}
