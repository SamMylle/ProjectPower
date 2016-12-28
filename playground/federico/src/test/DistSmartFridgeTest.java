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
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import avro.ProjectPower.Client;
import avro.ProjectPower.ClientType;
import avro.ProjectPower.CommData;
import avro.ProjectPower.ControllerComm;
import avro.ProjectPower.UserStatus;
import avro.ProjectPower.communicationFridgeUser;
import avro.ProjectPower.communicationTempSensor;
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
//	
	
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
		
		fridge.disconnect();
		controller.stopServer();
	}
	
	@Test
	public void testUserServerMethods() {
		/// setup
		final int controllerPort = 5000;
		final String clientIP = System.getProperty("clientip");
		final String serverIP = System.getProperty("ip");
		
		DistController controller = new DistController(controllerPort, 10, serverIP);
		DistSmartFridge fridge = new DistSmartFridge(clientIP, serverIP, controllerPort);
		Exception ex = null;

		CommData connectiondata = null;
		try {
			connectiondata = controller.setupFridgeCommunication(fridge.getID());
		} catch (AvroRemoteException e) {
			ex = e;
		}
		assertEquals(ex, null);
		
		int fridgeUserServerPort = connectiondata.getID();
		Transceiver transceiver = null;
		communicationFridgeUser proxy = null;
		try {
			transceiver = new SaslSocketTransceiver(new InetSocketAddress(clientIP, fridgeUserServerPort));
			proxy = (communicationFridgeUser) SpecificRequestor.getClient(communicationFridgeUser.class, transceiver);
		} catch (IOException e) {
			ex = e;
		}
		assertEquals(ex, null);
		
		try {
			proxy.openFridgeRemote();
			proxy.addItemRemote("cheese");
			proxy.addItemRemote("soda");
			
			List<CharSequence> _items = proxy.getItemsRemote();
			List<String> items = new ArrayList<String>();
			for (CharSequence item : _items) {
				items.add(item.toString());
			}
			
			assertEquals(items.size(), 2);
			assertTrue(items.contains("soda"));
			assertTrue(items.contains("cheese"));
			
			proxy.removeItemRemote("cheese");
			_items = proxy.getItemsRemote();
			items = new ArrayList<String>();
			for (CharSequence item : _items) {
				items.add(item.toString());
			}
			
			assertEquals(items.size(), 1);
			assertEquals(items.get(0).toString(), "soda");
			
			proxy.closeFridgeRemote();
		} catch (AvroRemoteException e) {
			e.printStackTrace();
			System.out.println(e.getClass().toString());
			ex = e;
		}
		assertEquals(ex, null);
		try {
			transceiver.close();
		} catch (IOException e) {
			ex = e;
		}
		assertEquals(ex, null);
		
		fridge.disconnect();
		controller.stopServer();
	}
	
	@Test
	public void testEmptyInventoryBroadcast() {
		// TODO add this test
	}
	
	@Test
	public void testIDSetup() {
		/// playing devils advocate here, occupying a few ports with random clients to force an increased ID

		

		final String serverIP = System.getProperty("ip");
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
		
		DistController controller = new DistController(controllerPort, 10, serverIP);
		
		// TODO finish this test, after implementation ofcourse
		
		
		server1.close();
		server2.close();
		server3.close();
		controller.stopServer();
	}
	
}
