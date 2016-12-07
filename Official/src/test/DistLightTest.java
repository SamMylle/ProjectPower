package test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.avro.ipc.SaslSocketTransceiver;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.junit.Before;
import org.junit.Test;

import avro.ProjectPower.LightComm;
import avro.ProjectPower.communicationFridge;

import controller.DistController;

import client.DistLight;

public class DistLightTest {

	static String f_ip;
	static String f_clientip;
	@Before
	public void setUp() throws Exception {
		f_ip = System.getProperty("ip");
		f_clientip = System.getProperty("clientip");
	}

	@Test
	public void testDistLight() {
		DistLight light = new DistLight(f_clientip, f_ip);
		/// Test that you can't connect to the DistLight
		assertFalse(light.serverRunning());
		assertEquals(-1, light.getServerPort());
	}

	@Test
	public void testConnectToServer() {
		DistLight light = new DistLight(f_clientip, f_ip);
		
		/// Test connect to non-existing server
		light.connectToServer(5000, f_ip);
		assertFalse(light.serverRunning());
		assertEquals(-1, light.getServerPort());
		
		/// Test connect server with existing server
		DistController controller = new DistController(5000, 10, f_ip);
		light.connectToServer(5000, f_ip);
		assertTrue(light.serverRunning());
		assertEquals(5000, light.getServerPort());
		assertEquals(5001, light.f_light.getID());
		
		light.disconnect();
		controller.stopServer();
	}

	@Test
	public void testDisconnect() {
		DistLight light = new DistLight(f_clientip, f_ip);
		
		/// Test connect server with existing server
		DistController controller = new DistController(5000, 10, f_ip);
		light.connectToServer(5000, f_ip);
		assertTrue(light.serverRunning());
		assertEquals(5000, light.getServerPort());
		assertEquals(5001, light.f_light.getID());
		
		light.disconnect();
		assertFalse(light.serverRunning());
		assertEquals(-1, light.getServerPort());
		assertEquals(-1, light.f_light.getID());
		
		controller.stopServer();
	}

	@Test
	public void testSetAndGetState() {
		DistLight light = new DistLight(f_clientip, f_ip);
		
		/// Test connect server with existing server
		DistController controller = new DistController(5000, 10, f_ip);
		light.connectToServer(5000, f_ip);
		try {
			Transceiver client = new SaslSocketTransceiver(new InetSocketAddress(f_ip, 5001));
			LightComm.Callback proxy =
					SpecificRequestor.getClient(LightComm.Callback.class, client);
			
			proxy.setState(10);
			assertEquals(10, proxy.getState());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		light.disconnect();
		controller.stopServer();
	}

}
