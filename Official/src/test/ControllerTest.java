package test;

import org.junit.Test;

import util.Logger;

import avro.ProjectPower.ClientType;
import static org.junit.Assert.assertEquals;

import controller.Controller;

public class ControllerTest {

	Controller ctrl = new Controller(5001, 10);

	@Test
	public void testID() {
		int ID = 5001;
		
		assertEquals(null, ctrl.getClType(10));

		assertEquals(ctrl.giveNextID(ClientType.SmartFridge), ID);
		assertEquals(ClientType.SmartFridge, ctrl.getClType(ID));

		ID++;
		
		assertEquals(ID, ctrl.giveNextID(ClientType.Light));
		assertEquals(ClientType.Light, ctrl.getClType(ID));
		assertEquals(ClientType.SmartFridge, ctrl.getClType(ID - 1));

		ID++;
		
		assertEquals(ID, ctrl.giveNextID(ClientType.TemperatureSensor));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClType(ID));
		assertEquals(ClientType.Light, ctrl.getClType(ID - 1));
		assertEquals(ClientType.SmartFridge, ctrl.getClType(ID - 2));

		ID++;
		
		assertEquals(ID, ctrl.giveNextID(ClientType.User));
		assertEquals(ClientType.User, ctrl.getClType(ID));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClType(ID - 1));
		assertEquals(ClientType.Light, ctrl.getClType(ID - 2));
		assertEquals(ClientType.SmartFridge, ctrl.getClType(ID - 3));

		ctrl.removeID(ID - 2);
		assertEquals(ClientType.User, ctrl.getClType(ID));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClType(ID - 1));
		assertEquals(null, ctrl.getClType(ID - 2));
		assertEquals(ClientType.SmartFridge, ctrl.getClType(ID - 3));

		ctrl.removeID(ID - 2);
		assertEquals(ClientType.User, ctrl.getClType(ID));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClType(ID - 1));
		assertEquals(null, ctrl.getClType(ID - 2));
		assertEquals(ClientType.SmartFridge, ctrl.getClType(ID - 3));

		ctrl.removeID(ID - 1);
		assertEquals(ClientType.User, ctrl.getClType(ID));
		assertEquals(null, ctrl.getClType(ID - 1));
		assertEquals(null, ctrl.getClType(ID - 2));
		assertEquals(ClientType.SmartFridge, ctrl.getClType(ID - 3));
		/// TODO add test for temperature

		ctrl.removeID(ID - 1);
		assertEquals(ClientType.User, ctrl.getClType(ID));
		assertEquals(null, ctrl.getClType(ID - 1));
		assertEquals(null, ctrl.getClType(ID - 2));
		assertEquals(ClientType.SmartFridge, ctrl.getClType(ID - 3));
	}
	
	@Test
	public void testClType() {
		/// Test failed case
		assertEquals(ctrl.getClType(1000), null);
		assertEquals(ctrl.getClType(5001), null);
		
		/// Test when stuff is added
		int ID = ctrl.giveNextID(ClientType.Light);
		assertEquals(ctrl.getClType(ID), ClientType.Light);
		
		ID = ctrl.giveNextID(ClientType.SmartFridge);
		assertEquals(ctrl.getClType(ID), ClientType.SmartFridge);
		/// Prove that previous stuff is still in the system
		assertEquals(ctrl.getClType(ID - 1), ClientType.Light);
		
		ID = ctrl.giveNextID(ClientType.User);
		assertEquals(ctrl.getClType(ID), ClientType.User);
		
		ID = ctrl.giveNextID(ClientType.TemperatureSensor);
		assertEquals(ctrl.getClType(ID), ClientType.TemperatureSensor);
		
	}
	
	@Test
	public void testRemoveID() {
		/// Prove that an invalid delete doesn't affect the system
		ctrl.removeID(5000);
		
		/// Add some stuff, delete one, check for presence of all things
		int ID = ctrl.giveNextID(ClientType.Light);
		ID = ctrl.giveNextID(ClientType.SmartFridge);
		ID = ctrl.giveNextID(ClientType.User);
		ID = ctrl.giveNextID(ClientType.TemperatureSensor);
		
		ctrl.removeID(5003);
		assertEquals(ctrl.getClType(5001), ClientType.Light);
		assertEquals(ctrl.getClType(5002), ClientType.SmartFridge);
		assertEquals(ctrl.getClType(5003), null);
		assertEquals(ctrl.getClType(5004), ClientType.TemperatureSensor);
		
	}
}
