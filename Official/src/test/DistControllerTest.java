package test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import util.SuppressSystemOut;
import avro.ProjectPower.ClientType;
import avro.ProjectPower.ControllerComm;
import avro.ProjectPower.communicationFridge;
import controller.*;
import client.*;
import avro.ProjectPower.*;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DistControllerTest {
	static SuppressSystemOut suppress;
	static String f_ip;
	static String f_clientip;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		f_ip = (System.getProperty("ip"));
		f_clientip = (System.getProperty("clientip"));
		suppress = new SuppressSystemOut();
//		suppress.suppressOutput();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		suppress.activateOutput();
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testDistController() {
		DistController controller = new DistController(5000, 10, f_ip);
		Exception ex = null;
		
		try {
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(f_ip, 5000));
			
			ControllerComm.Callback proxy =
					SpecificRequestor.getClient(ControllerComm.Callback.class, client);

			/// APPARANTLY you HAVE to call a function when using a transceiver, or stuff is broken in avro
			proxy.getAllClients();
			//System.out.print("lool\n");

			client.close();
		} catch (IOException e) {
			ex = e;
		}
		
		assertEquals(null, ex);
		controller.stopServer();
		controller = null;
	}

	@Test
	public void testServerIsActive() {
		DistController controller = new DistController(5000, 10, f_ip);
		assertTrue(controller.serverIsActive());
		controller.stopServer();
		assertFalse(controller.serverIsActive());
	}

	@Test
	public void testStopServer() {
		DistController controller = new DistController(5000, 10, f_ip);
		
		controller.stopServer();
		assertFalse(controller.serverIsActive());

		Exception ex = null;
		try {
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(5000));
			
			ControllerComm.Callback proxy =
					SpecificRequestor.getClient(ControllerComm.Callback.class, client);

			/// APPARANTLY you HAVE to call
				///a function when using a transceiver, or stuff is broken in avro
			proxy.getAllClients();
			//System.out.print("lool\n");

			client.close();
		} catch (IOException e) {
			ex = e;
		}
		assertNotEquals(null, ex);
		
		controller.stopServer();
	}

	/*@SuppressWarnings("deprecation")
	@Test
	public void testSetupFridgeCommunication() {
		DistController controller = new DistController(5000, 10, f_ip);

		DistSmartFridge fridge = new DistSmartFridge(f_clientip, f_ip, 5000);
		DistSmartFridge fridge2 = new DistSmartFridge(f_clientip, f_ip, 5000);

		try {

			CommData port = controller.setupFridgeCommunication(5001);

			assertEquals(4999, port.ID);
			assertEquals(f_clientip, port.IP);
			
			Vector<Integer> expected = new Vector<Integer>();
			expected.add(new Integer(4999));

			port = controller.setupFridgeCommunication(5002);
			
			assertEquals(4998, port.ID);
			assertEquals(f_clientip, port.IP);
			
			expected.add(new Integer(4998));
			
			/// Should fail
			port = controller.setupFridgeCommunication(5003);
			assertEquals(-1, port.ID);
			assertEquals("", port.IP);
			
			/// Denied access by fridge
			port = controller.setupFridgeCommunication(5001);
			assertEquals(-1, port.ID);
			assertEquals("", port.IP);
		} catch (AvroRemoteException e) {
			e.printStackTrace();
			System.exit(0);
		}
		fridge.logOffController();
		fridge.stopServerController();
		fridge2.logOffController();
		fridge2.stopServerController();
		controller.stopServer();
	}*/

	@Test
	public void testGetFridgeInventory() {
		System.out.println("GETINVENTORY");
		// I could only do it this way, no multiple fridges and actual fuckups
		DistController controller = new DistController(5000, 10, f_ip);

		try {
			assertEquals(new ArrayList<CharSequence>(), controller.getFridgeInventory(5001));
			assertEquals(new ArrayList<CharSequence>(), controller.getFridgeInventory(51223687));
			
			System.out.println("HERE " + f_clientip);
			DistSmartFridge fridge = new DistSmartFridge(f_clientip, f_ip, 5000);
			System.out.println("HERE2 " + f_ip);

			fridge.addItem("Chunks_of_little_children");
			fridge.addItem("A_dwarf_powering_the_fridge");
			fridge.addItem("Pizza");
			fridge.addItem("The_holocaust");
			System.out.println("HERE");
			
			List<CharSequence> actual = controller.getFridgeInventory(5001);
			List<CharSequence> expected = new ArrayList<CharSequence>(fridge.getItems());

			/// TODO tostring is nasty but java is being a cunt
			assertEquals(expected.toString(), actual.toString());
			
			fridge.logOffController();
			fridge.stopServerController();

		} catch (AvroRemoteException e1) {
			e1.printStackTrace();
		}
		controller.stopServer();
		System.out.println("stopinventory");
	}

	@Test
	public void testSetAndGetLight() {
		DistController controller = new DistController(5000, 10, f_ip);
		DistLight light = new DistLight(f_clientip, f_ip);
		light.connectToServer(5000, f_ip);
		
		Exception ex = null;
		
		try {
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(InetAddress.getByName(f_ip), 5000));
			
			ControllerComm.Callback proxy =
					SpecificRequestor.getClient(ControllerComm.Callback.class, client);

			/// APPARANTLY you HAVE to call
				///a function when using a transceiver, or stuff is broken in avro
			assertEquals(0, proxy.setLight(8, 5001));
			assertEquals(8, proxy.getLightState(5001));
			//System.out.print("lool\n");

			client.close();
		} catch (IOException e) {
			ex = e;
		}
		
		assertEquals(null, ex);
		
		light.disconnect();
		controller.stopServer();
	}

	@Test
	public void testGetAllClients() {
		DistController controller = new DistController(5000, 10, f_ip);
		DistLight light = new DistLight(f_clientip, f_ip);
		light.connectToServer(5000, f_ip);
		DistLight light2 = new DistLight(f_clientip, f_ip);
		light2.connectToServer(5000, f_ip);
		
		List<Client> connected = new Vector<Client>();
		connected.add(new Client(ClientType.Light, 5002));
		connected.add(new Client(ClientType.Light, 5001));
		
		Exception ex = null;
		
		try {
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(f_ip, 5000));
			
			ControllerComm.Callback proxy =
					SpecificRequestor.getClient(ControllerComm.Callback.class, client);

			/// APPARANTLY you HAVE to call
				///a function when using a transceiver, or stuff is broken in avro
			List<Client> clients = proxy.getAllClients();
			
			
			
			for (int i = 0; i < clients.size(); i++){
				assertTrue(connected.contains(clients.get(i)));
			}
			//System.out.print("lool\n");

			client.close();
		} catch (IOException e) {
			ex = e;
		}
		
		assertEquals(null, ex);
		
		light.disconnect();
		light2.disconnect();
		controller.stopServer();
	}

}
