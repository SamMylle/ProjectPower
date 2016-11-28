package test;

import org.junit.Test;

import avro.ProjectPower.ClientType;
import static org.junit.Assert.assertEquals;

import controller.Controller;

public class ControllerTest {

	Controller ctrl = new Controller(5001, 10);

	@Test
	public void testID() {
		int ID = 5001;
		
		assertEquals(null, ctrl.getClientType(10));

		assertEquals(ctrl.giveNextID(ClientType.SmartFridge), ID);
		assertEquals(ClientType.SmartFridge, ctrl.getClientType(ID));

		ID++;
		
		assertEquals(ID, ctrl.giveNextID(ClientType.Light));
		assertEquals(ClientType.Light, ctrl.getClientType(ID));
		assertEquals(ClientType.SmartFridge, ctrl.getClientType(ID - 1));

		ID++;
		
		assertEquals(ID, ctrl.giveNextID(ClientType.TemperatureSensor));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClientType(ID));
		assertEquals(ClientType.Light, ctrl.getClientType(ID - 1));
		assertEquals(ClientType.SmartFridge, ctrl.getClientType(ID - 2));

		ID++;
		
		assertEquals(ID, ctrl.giveNextID(ClientType.User));
		assertEquals(ClientType.User, ctrl.getClientType(ID));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClientType(ID - 1));
		assertEquals(ClientType.Light, ctrl.getClientType(ID - 2));
		assertEquals(ClientType.SmartFridge, ctrl.getClientType(ID - 3));

		ctrl.removeID(ID - 2);
		assertEquals(ClientType.User, ctrl.getClientType(ID));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClientType(ID - 1));
		assertEquals(null, ctrl.getClientType(ID - 2));
		assertEquals(ClientType.SmartFridge, ctrl.getClientType(ID - 3));

		ctrl.removeID(ID - 2);
		assertEquals(ClientType.User, ctrl.getClientType(ID));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClientType(ID - 1));
		assertEquals(null, ctrl.getClientType(ID - 2));
		assertEquals(ClientType.SmartFridge, ctrl.getClientType(ID - 3));

		ctrl.removeID(ID - 1);
		assertEquals(ClientType.User, ctrl.getClientType(ID));
		assertEquals(null, ctrl.getClientType(ID - 1));
		assertEquals(null, ctrl.getClientType(ID - 2));
		assertEquals(ClientType.SmartFridge, ctrl.getClientType(ID - 3));
		/// TODO add test for temperature

		ctrl.removeID(ID - 1);
		assertEquals(ClientType.User, ctrl.getClientType(ID));
		assertEquals(null, ctrl.getClientType(ID - 1));
		assertEquals(null, ctrl.getClientType(ID - 2));
		assertEquals(ClientType.SmartFridge, ctrl.getClientType(ID - 3));
	}
}
