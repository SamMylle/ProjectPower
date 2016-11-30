package test;

import static org.junit.Assert.*;

import org.apache.avro.AvroRemoteException;
import org.junit.Test;

import avro.ProjectPower.UserStatus;

import controller.DistController;
import client.DistUser;
import client.User;

public class DistUserTest {

	private static final int controllerPort = 5000;
	private static final int maxTemp = 10;
	DistUser distuser;
	DistController controller = new DistController(controllerPort, maxTemp);

	@Test
	public void testGetID() {
		distuser = new DistUser(controllerPort);
		
		try {
			assertEquals(controller.getClientType(controllerPort+1), User.type);
		} catch (AvroRemoteException e) {
			return;
		}
		distuser.logOffController();
		distuser.stopServer();
		
		distuser = new DistUser(controllerPort);
		try {
			assertEquals(controller.getClientType(controllerPort+2), User.type);
		} catch (AvroRemoteException e) {
			return;
		}
		distuser.logOffController();
		distuser.stopServer();
	}
	
	@Test
	public void testStatus() {
		distuser = new DistUser(controllerPort);
		
		try {
			assertEquals(distuser.getStatusRemote(), UserStatus.absent);
		} catch (AvroRemoteException e) {
			return;
		}
	}
}
