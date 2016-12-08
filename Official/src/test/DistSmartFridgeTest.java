package test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketServer;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import avro.ProjectPower.Client;
import avro.ProjectPower.ClientType;
import avro.ProjectPower.ControllerComm;
import avro.ProjectPower.UserStatus;
import avro.ProjectPower.communicationTempSensor;
import client.*;
import client.exception.AbsentException;
import client.exception.FridgeOccupiedException;
import client.exception.MultipleInteractionException;
import client.exception.NoFridgeConnectionException;
import controller.*;
import util.SuppressSystemOut;


public class DistSmartFridgeTest {
	static SuppressSystemOut suppress;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		suppress = new SuppressSystemOut();
		suppress.suppressOutput();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		suppress.activateOutput();
	}

	@Before
	public void setUp() throws Exception {
	}
	
	
	@Test
	public void testControllerServerMethods() {
		final int controllerPort = 5000;
		DistController controller = new DistController(controllerPort, 10, System.getProperty("ip"));
		DistSmartFridge fridge = new DistSmartFridge(System.getProperty("clientip"), System.getProperty("ip"), controllerPort);
		
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
		Exception ex = null;
		List<CharSequence> items = null;
		try {
			items = controller.getFridgeInventory(controllerPort+1);
		} catch (AvroRemoteException e) {
			ex = e;
		}
		assertEquals(ex, null);
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
		final String clientIP = System.getProperty("clientip");
		final String IP = System.getProperty("ip");
		
		DistController controller = new DistController(controllerPort, 10, IP);
		DistSmartFridge fridge = new DistSmartFridge(clientIP, IP, controllerPort);
		DistUser user = new DistUser("Federico Quin", clientIP, IP, controllerPort);
		DistUser user2 = new DistUser("Sam Mylle", clientIP, IP, controllerPort);
		
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
                
                fridge.logOffController();
                user.logOffController();
                user2.logOffController();
                fridge.stopServerController();
                user.stopServer();
                user2.stopServer();
                controller.stopServer();
	}
	
	@Test
	public void testEmptyInventoryBroadcast() {
		// TODO add this test
	}
	
	@Test
	public void testIDSetup() {
		/// playing devils advocate here, occupying a few ports with random clients to force an increased ID
		
		final String IP = System.getProperty("ip");
		final String clientIP = System.getProperty("clientip");
		
		/// setup
		Server server1 = null;
		Server server2 = null;
		Server server3 = null;
		final int controllerPort = 5000;
		
		try{
			server1 = new SaslSocketServer(
					new SpecificResponder(communicationTempSensor.class,
							this), new InetSocketAddress(clientIP, controllerPort+1));
		}catch(IOException e){ }
		server1.start();
		try{
			server2 = new SaslSocketServer(
					new SpecificResponder(communicationTempSensor.class,
							this), new InetSocketAddress(clientIP, controllerPort+2));
		}catch(IOException e){ }
		server2.start();
		try{
			server3 = new SaslSocketServer(
					new SpecificResponder(communicationTempSensor.class,
							this), new InetSocketAddress(clientIP, controllerPort+3));
		}catch(IOException e){ }
		server3.start();

		/// * actual test
		
		// TODO finish this test, after implementation ofcourse
	}
	
}
