package test;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Vector;
import java.io.IOException;
import java.net.InetSocketAddress;

import util.SuppressSystemOut;
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
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		///suppress = new SuppressSystemOut();
		///suppress.suppressOutput();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		///suppress.activateOutput();
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testDistController() {
		DistController controller = new DistController(5000, 10);
		Exception ex = null;
		
		try {
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(5000));
			
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
		DistController controller = new DistController(5000, 10);
		assertTrue(controller.serverIsActive());
		controller.stopServer();
		assertFalse(controller.serverIsActive());
	}

	@Test
	public void testStopServer() {
		DistController controller = new DistController(5000, 10);
		
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
		// TODO test with non-connected fridge
		DistController controller = new DistController(5000, 10);

		DistSmartFridge fridge = new DistSmartFridge(5000);
		DistSmartFridge fridge2 = new DistSmartFridge(5000);

		assertEquals(new Vector<Integer>(), controller.getOccupiedPorts());
		try {

			int port = controller.setupFridgeCommunication(5001);

			assertEquals(4999, port);
			Vector<Integer> expected = new Vector<Integer>();
			expected.add(new Integer(4999));
			assertEquals(expected, controller.getOccupiedPorts());

			port = controller.setupFridgeCommunication(5002);
			
			assertEquals(4998, port);
			expected.add(new Integer(4998));
			assertEquals(expected, controller.getOccupiedPorts());
			
			/// Should fail
			port = controller.setupFridgeCommunication(5003);
			
			assertEquals(-1, port);
			assertEquals(expected, controller.getOccupiedPorts());
		} catch (AvroRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fridge.logOffController();
		fridge.stopServerController();
		fridge2.logOffController();
		fridge2.stopServerController();
		controller.stopServer();
	}

	@Test
	public void getFridgePort() {
		// TODO test with federico
	}

	@Test
	public void reSetupFridgeCommunication() {
		// TODO test with federico
	}

	@Test
	public void endFridgeCommunication() {
		// TODO test with federico
	}

	@Test
	public void testGetFridgeInventory() {
		// TODO test with federico
	}

	@Test
	public void testSetAndGetLight() {
		DistController controller = new DistController(5000, 10);
		DistLight light = new DistLight();
		light.connectToServer(5000);
		
		Exception ex = null;
		
		try {
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(5000));
			
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
		DistController controller = new DistController(5000, 10);
		DistLight light = new DistLight();
		light.connectToServer(5000);
		DistLight light2 = new DistLight();
		light2.connectToServer(5000);
		
		List<Client> connected = new Vector<Client>();
		connected.add(new Client(ClientType.Light, 5002));
		connected.add(new Client(ClientType.Light, 5001));
		
		
		
		Exception ex = null;
		
		try {
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(5000));
			
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
