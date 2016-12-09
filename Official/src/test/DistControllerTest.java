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
	public void testDistControllerExtended() {
		int port = 6000;
		int originalHostPort = 5000;
		int maxTemperatures = 10;
		int currentMaxPort = 5004;
		String ip = f_ip;
		String previousControllerIP = f_ip;
		/// TODO manually test this with nonempty vector
		Vector<Integer> usedFridgePorts = new Vector<Integer>();
		HashMap<Integer, String> IPs = new HashMap<Integer, String>();
		IPs.put(new Integer(5001), f_clientip);
		IPs.put(new Integer(5002), f_clientip);
		IPs.put(new Integer(5003), f_clientip);
		HashMap<Integer, ClientType> names = new HashMap<Integer, ClientType>();
		names.put(new Integer(5001), ClientType.SmartFridge);
		names.put(new Integer(5002), ClientType.Light);
		names.put(new Integer(5003), ClientType.TemperatureSensor);
		Vector<TemperatureRecord> temperatures = new Vector<TemperatureRecord>();
		TemperatureRecord record = new TemperatureRecord(maxTemperatures, 5003);
		record.addValue(20);
		record.addValue(19);
		temperatures.add(record);
		
		//DistController controller = new DistController(port, originalHostPort, maxTemperatures,
		//		currentMaxPort, ip, previousControllerIP, usedFridgePorts, IPs, names, temperatures);
		ServerData data = new ServerData();
		data.port = port;
		data.originalControllerPort = originalHostPort;
		data.maxTemperatures = maxTemperatures;
		data.currentMaxPort = currentMaxPort;
		data.ip = ip;
		data.previousControllerIP = previousControllerIP;
		data.usedFridgePorts = usedFridgePorts;
		data.IPsID = new LinkedList<Integer>(IPs.keySet());
		System.out.print(data.IPsID.toString() + "id \n");
		data.IPsIP = new LinkedList<CharSequence>(IPs.values());
		System.out.print(data.IPsIP.toString() + "ip \n");
		System.out.print(IPs.toString() + "\n");
		data.namesClientType = new LinkedList<ClientType>(names.values());
		data.namesID = new LinkedList<Integer>(names.keySet());
		
		data.temperatures = new ArrayList<List<Double>>();
		for(int i = 0; i < temperatures.size(); i++){
			List<Double> newRecord = new ArrayList<Double>(temperatures.get(i).getRecord());
			data.temperatures.add(newRecord);
		}
		
		data.temperaturesIDs = new ArrayList<Integer>();
		for(int i = 0; i < temperatures.size(); i++){
			Integer newID = new Integer(temperatures.get(i).getID());
			data.temperaturesIDs.add(newID);
		}

		DistController controller = new DistController(data);
		
		DistController OtherController = new DistController(5000, maxTemperatures, f_ip);
		try {
			OtherController.LogOn(ClientType.SmartFridge, f_ip);
			OtherController.LogOn(ClientType.Light, f_ip);
			OtherController.LogOn(ClientType.TemperatureSensor, f_ip);
			OtherController.addTemperature(20.0, 5003);
			OtherController.addTemperature(19.0, 5003);
		} catch (AvroRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.print("TESTEND2\n");
		System.out.print(controller.getRawTemperatures().toString());
		System.out.print(OtherController.getRawTemperatures().toString());
		assertTrue(controller.equals(OtherController));

		System.out.print("TESTEND1\n");
		controller.stopServer();
		OtherController.stopServer();
		System.out.print("TESTEND\n");
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

	@Test
	public void testGetID() {
		// Tested in controller stuff
	}

	@Test
	public void testGetClientType() {
		// Tested in controller stuff
	}

	@Test
	public void testLogOff() {
		// Tested in controller stuff
	}

	@Test
	public void testAddTemperatureIntDouble() {
		// Tested in controller stuff
	}

	@Test
	public void testAverageCurrentTemperature() {
		// Tested in controller stuff
	}

	@Test
	public void testHasValidTemperatures() {
		// Tested in controller stuff
	}

	@Test
	public void testSetupFridgeCommunication() {
		DistController controller = new DistController(5000, 10, f_ip);

		DistSmartFridge fridge = new DistSmartFridge(f_clientip, f_ip, 5000);
		DistSmartFridge fridge2 = new DistSmartFridge(f_clientip, f_ip, 5000);

		assertEquals(new Vector<Integer>(), controller.getOccupiedPorts());
		try {

			CommData port = controller.setupFridgeCommunication(5001);

			assertEquals(4999, port.ID);
			assertEquals(f_clientip, port.IP);
			
			Vector<Integer> expected = new Vector<Integer>();
			expected.add(new Integer(4999));
			assertEquals(expected, controller.getOccupiedPorts());

			port = controller.setupFridgeCommunication(5002);
			
			assertEquals(4998, port.ID);
			assertEquals(f_clientip, port.IP);
			
			expected.add(new Integer(4998));
			assertEquals(expected, controller.getOccupiedPorts());
			
			/// Should fail
			port = controller.setupFridgeCommunication(5003);
			assertEquals(-1, port.ID);
			assertEquals("", port.IP);
			assertEquals(expected, controller.getOccupiedPorts());
			
			/// Denied access by fridge
			port = controller.setupFridgeCommunication(5001);
			assertEquals(-1, port.ID);
			assertEquals("", port.IP);
			assertEquals(expected, controller.getOccupiedPorts());
		} catch (AvroRemoteException e) {
			e.printStackTrace();
			System.exit(0);
		}
		fridge.logOffController();
		fridge.stopServerController();
		fridge2.logOffController();
		fridge2.stopServerController();
		controller.stopServer();
	}

	@Test
	public void getFridgePort() {
		// Tested in testSetupFridgeCommunication
	}

	@Test
	public void reSetupFridgeCommunication() {
		// I could only do it this way, no multiple fridges and actual fuckups
		DistController controller = new DistController(5000, 10, f_ip);

		DistSmartFridge fridge = new DistSmartFridge(f_clientip, f_ip, 5000);

		assertEquals(new Vector<Integer>(), controller.getOccupiedPorts());
		try {

			CommData port = controller.reSetupFridgeCommunication(5001, 3001);

			assertEquals(3000, port.ID);
			assertEquals(f_clientip, port.IP);
			
			Vector<Integer> expected = new Vector<Integer>();
			expected.add(new Integer(3000));
			assertEquals(expected, controller.getOccupiedPorts());

		} catch (AvroRemoteException e) {
			e.printStackTrace();
			System.exit(0);
		}
		fridge.logOffController();
		fridge.stopServerController();
		controller.stopServer();
	}
	

	@Test
	public void testEndFridgeCommunication() {
		// I could only do it this way, no multiple fridges and actual fuckups
		DistController controller = new DistController(5000, 10, f_ip);

		DistSmartFridge fridge = new DistSmartFridge(f_clientip, f_ip, 5000);

		assertEquals(new Vector<Integer>(), controller.getOccupiedPorts());
		try {

			CommData port = controller.reSetupFridgeCommunication(5001, 3001);

			assertEquals(3000, port.ID);
			assertEquals(f_clientip, port.IP);
			
			Vector<Integer> expected = new Vector<Integer>();
			expected.add(new Integer(3000));
			assertEquals(expected, controller.getOccupiedPorts());
			
			controller.endFridgeCommunication(3000);
			expected = new Vector<Integer>();
			assertEquals(expected, controller.getOccupiedPorts());
			
			/// Test this when the port is unused
			controller.endFridgeCommunication(3000);
			assertEquals(expected, controller.getOccupiedPorts());
			

		} catch (AvroRemoteException e) {
			e.printStackTrace();
			System.exit(0);
		}
		fridge.logOffController();
		fridge.stopServerController();
		controller.stopServer();
	}

	@Test
	public void testGetFridgeInventory() {
		// I could only do it this way, no multiple fridges and actual fuckups
		DistController controller = new DistController(5000, 10, f_ip);

		try {
			assertEquals(new ArrayList<CharSequence>(), controller.getFridgeInventory(5001));
			assertEquals(new ArrayList<CharSequence>(), controller.getFridgeInventory(51223687));
			
			DistSmartFridge fridge = new DistSmartFridge(f_clientip, f_ip, 5000);
			fridge.addItem("Chunks_of_little_children");
			fridge.addItem("A_dwarf_powering_the_fridge");
			fridge.addItem("Pizza");
			fridge.addItem("The_holocaust");
			
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
				assertEquals(connected.get(i), clients.get(i));
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
