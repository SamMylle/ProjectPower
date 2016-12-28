package test;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.avro.AvroRemoteException;
import org.junit.Test;

import avro.ProjectPower.Client;
import avro.ProjectPower.ClientType;
import avro.ProjectPower.UserStatus;

import controller.DistController;
import client.DistLight;
import client.DistSmartFridge;
import client.DistUser;
import client.User;
import client.exception.AbsentException;
import client.exception.ElectionBusyException;
import client.exception.FridgeOccupiedException;
import client.exception.MultipleInteractionException;
import client.exception.NoFridgeConnectionException;
import client.exception.NoTemperatureMeasures;
import client.exception.TakeoverException;
import client.util.LightState;

public class DistUserTest {

	private static final int controllerPort = 5000;
	private static final int maxTemp = 10;
	private final String serverIP = System.getProperty("ip");
	private final String clientIP = System.getProperty("clientip");
	
	@Test
	public void testGetID() {
		DistController controller = new DistController(controllerPort, maxTemp, serverIP);
		DistUser user = new DistUser("test", clientIP, serverIP, controllerPort);
		
		try {
			assertEquals(controller.getClientType(controllerPort+1), User.type);
		} catch (AvroRemoteException e) {
			return;
		}
		user.disconnect();
		
		user = new DistUser("", clientIP, serverIP, controllerPort);
		try {
			assertEquals(controller.getClientType(controllerPort+2), User.type);
		} catch (AvroRemoteException e) {
			return;
		}
		user.disconnect();
		controller.stopServer();
	}
	
	@Test
	public void testStatus() {
		DistController controller = new DistController(controllerPort, maxTemp, serverIP);
		DistUser user = new DistUser("test", clientIP, serverIP, controllerPort);
		
		try {
			assertEquals(user.getStatus(), UserStatus.present);
		} catch (AvroRemoteException e) {
			return;
		}
		user.disconnect();
		controller.stopServer();
	}
	
	/**
	 * Tests getAllClients
	 */
	@Test
	public void testRequestclients() {
		DistController controller = new DistController(controllerPort, maxTemp, serverIP);
		DistUser user = new DistUser("test", clientIP, serverIP, controllerPort);
		DistUser user2 = new DistUser("test", clientIP, serverIP, controllerPort);
		DistUser user3 = new DistUser("test", clientIP, serverIP, controllerPort);
		
		Exception ex = null;
		List<Client> clients = null;
		try {
			clients = user.getAllClients();
		} catch (MultipleInteractionException | AbsentException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		
		assertEquals(clients.size(), 3);
		try {
			assertEquals(clients, user2.getAllClients());
		} catch (MultipleInteractionException | AbsentException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		
		for (Client client : clients) {
			assertEquals(client.clientType, ClientType.User);
		}
		
		user.disconnect();
		user2.disconnect();
		user3.disconnect();
		controller.stopServer();
	}
	
	/**
	 * Tests getLightStates and setLightState
	 */
	@Test
	public void lightStatesTest() {
		DistController controller = new DistController(controllerPort, maxTemp, serverIP);
		DistUser user = new DistUser("temp", clientIP, serverIP, controllerPort);
		DistLight light1 = new DistLight(clientIP, serverIP);
		DistLight light2 = new DistLight(clientIP, serverIP);
		DistLight light3 = new DistLight(clientIP, serverIP);
		light1.connectToServer(controllerPort, serverIP);
		light2.connectToServer(controllerPort, serverIP);
		light3.connectToServer(controllerPort, serverIP);
		Exception ex = null;
		
		List<LightState> lightstates = null;
		try {
			lightstates = user.getLightStates();
		} catch (MultipleInteractionException | AbsentException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		assertEquals(lightstates.size(), 3);
		for (LightState state : lightstates) {
			assertEquals(state.State, 0);
		}
		
		try {
			user.setLightState(1, light2.f_light.getID());
			lightstates = user.getLightStates();
		} catch (MultipleInteractionException | AbsentException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		assertEquals(lightstates.size(), 3);
		LightState lightstate2 = null;
		for (LightState light : lightstates) {
			if (light.ID == light2.f_light.getID())
				lightstate2 = light;
		}
		assertNotEquals(lightstate2, null);
		assertEquals(lightstate2.State, 1);
		
		light1.disconnect();
		light2.disconnect();
		light3.disconnect();
		user.disconnect();
		controller.stopServer();
	}
	
	/**
	 * Tests getFridgeItems (indirectly)
	 */
	@Test
	public void testGetFridgeItems() {
		DistController controller = new DistController(controllerPort, maxTemp, serverIP);
		DistUser user = new DistUser("test", clientIP, serverIP, controllerPort);
		DistSmartFridge fridge = new DistSmartFridge(clientIP, serverIP, controllerPort);
		Exception ex = null;
		
		fridge.addItem("bacon");
		fridge.addItem("eggs");
		fridge.addItem("pancakes");
		
		List<String> items = null;
		try {
			items = user.getFridgeItems(fridge.getID());
		} catch (MultipleInteractionException | AbsentException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		assertEquals(items.size(), 3);
		
		assertTrue(items.contains("bacon"));
		assertTrue(items.contains("eggs"));
		assertTrue(items.contains("pancakes"));
		
		user.disconnect();
		fridge.disconnect();
		controller.stopServer();
	}

	
	/**
	 * Tests communicateWithFridge, openFridge and closeFridge
	 */
	@Test
	public void testSetupFridgeComm() {
		DistController controller = new DistController(controllerPort, maxTemp, serverIP);
		DistUser user = new DistUser("test", clientIP, serverIP, controllerPort);
		DistSmartFridge fridge = new DistSmartFridge(clientIP, serverIP, controllerPort);
		Exception ex = null;
		
		try {
			user.communicateWithFridge(fridge.getID());
		} catch (MultipleInteractionException | AbsentException | FridgeOccupiedException | TakeoverException | NoFridgeConnectionException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		
		try {
			user.openFridge();
		} catch (NoFridgeConnectionException | AbsentException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		assertTrue(fridge.isOpen());
		
		try {
			user.closeFridge();
		} catch (NoFridgeConnectionException | AbsentException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		assertFalse(fridge.isOpen());
		
		try {
			user.removeItemFridge("Cat food");
		} catch (NoFridgeConnectionException e) {
			ex = e;
		} catch (AbsentException e) {
			assertTrue(false);
		} catch (TakeoverException e) {
			assertTrue(false);
		} catch (ElectionBusyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertNotEquals(ex, null);
		
		fridge.disconnect();
		user.disconnect();
		controller.stopServer();
	}
	
	/**
	 * Tests addItemFridge, removeItemFridge and getFridgeItemsDirectly
	 */
	@Test
	public void testFridgeItemsDirectly() {
		DistController controller = new DistController(controllerPort, maxTemp, serverIP);
		DistUser user = new DistUser("test", clientIP, serverIP, controllerPort);
		DistSmartFridge fridge = new DistSmartFridge(clientIP, serverIP, controllerPort);
		Exception ex = null;
		
		List<String> items = null;
		
		try {
			user.communicateWithFridge(fridge.getID());
			user.openFridge();
			user.addItemFridge("Chocolate");
			user.addItemFridge("Soup");
			user.addItemFridge("Beer");
			
			items = user.getFridgeItemsDirectly();
			assertEquals(items.size(), 3);
			assertTrue(items.contains("Chocolate"));
			assertTrue(items.contains("Soup"));
			assertTrue(items.contains("Beer"));
			
			user.removeItemFridge("Bear");
			user.removeItemFridge("Beer");
			items = user.getFridgeItemsDirectly();
			assertEquals(items.size(), 2);
			assertTrue(items.contains("Chocolate"));
			assertTrue(items.contains("Soup"));
			assertFalse(items.contains("Beer"));
			
			user.closeFridge();
		} catch (MultipleInteractionException | AbsentException | FridgeOccupiedException | NoFridgeConnectionException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		
		fridge.disconnect();
		user.disconnect();
		controller.stopServer();
	}
	
	
	/**
	 * Tests getCurrentTemperatureHouse and getTemperatureHistory
	 */
	@Test
	public void testTemperatureData() {
		return;
	}
	
	/// TODO add tests to cover all distributed methods to controller/fridge
	/// 
	/// getCurrentTemperatureHouse
	/// getTemperatureHistory
	
	/// TODO add test cases for exceptions in several methods
	
	/**
	 * Tests if exception gets thrown when the user is trying to perform an action when not being present in the house
	 */
	@Test
	public void testAbsentException() {
		DistController controller = new DistController(controllerPort, maxTemp, serverIP);
		DistUser user = new DistUser("test", clientIP, serverIP, controllerPort);
		Exception ex = null;
		
		user.leave();
		
		try {
			user.getAllClients();
		} catch (MultipleInteractionException e) {
			ex = e;
		} catch (AbsentException e) {
			ex = e;
		} catch (TakeoverException e) {
			ex = e;
		} catch (ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex.getClass(), AbsentException.class);
		ex = null;
		
		try {
			user.getCurrentTemperatureHouse();
		} catch (MultipleInteractionException e) {
			ex = e;
		} catch (AbsentException e) {
			ex = e;
		} catch (NoTemperatureMeasures e) {
			ex = e;
		} catch (TakeoverException e) {
			ex = e;
		} catch (ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex.getClass(), AbsentException.class);
		ex = null;
		
		
		user.disconnect();
		controller.stopServer();
	}
	
	/**
	 * Tests if exception gets thrown when the user tries to interact with other clients, whilst having a direct connection to the fridge
	 */
	@Test
	public void testMultipleInteractionException() {
		DistController controller = new DistController(controllerPort, maxTemp, serverIP);
		DistUser user = new DistUser("test", clientIP, serverIP, controllerPort);
		DistSmartFridge fridge = new DistSmartFridge(clientIP, serverIP, controllerPort);
		Exception ex = null;
		
		try {
			user.communicateWithFridge(fridge.getID());
			user.openFridge();
		} catch (MultipleInteractionException | AbsentException | FridgeOccupiedException | NoFridgeConnectionException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		
		try {
			user.getAllClients();
		} catch (MultipleInteractionException | AbsentException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex.getClass(), MultipleInteractionException.class);
		
		ex = null;
		try {
			user.closeFridge();
		} catch (NoFridgeConnectionException | AbsentException | TakeoverException | ElectionBusyException e) {
			ex = e;
		}
		assertEquals(ex, null);
		
		fridge.disconnect();
		user.disconnect();
		controller.stopServer();
	}
	
	/**
	 * Tests if exception gets thrown when the user is trying to call a fridge remote method when no connection is setup yet.
	 */
	@Test
	public void testNoFridgeConnectionException() {
		return;
	}
	
	/**
	 * Tests if exception gets thrown when the user asks for the current temperature, but with no measurements available yet
	 */
	@Test
	public void testNoTemperatureMeasuresException() {
		return;
	}
	
	/**
	 * Tests if exception gets thrown when the user is trying to connect with a fridge that is already connected with another user
	 */
	@Test
	public void testFridgeOccupiedException() {
		return;
	}
	
}
