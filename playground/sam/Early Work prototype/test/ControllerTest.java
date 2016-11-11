package test;

import org.junit.Test;

import util.ClientType;
import static org.junit.Assert.assertEquals;

import controller.Controller;

public class ControllerTest {

	String message = "Hello World";	
	Controller ctrl = new Controller(10);

	@Test
	public void testID() {
		assertEquals(null, ctrl.getClientType(10));

		assertEquals(ctrl.giveNextID(ClientType.Fridge), 0);
		assertEquals(ClientType.Fridge, ctrl.getClientType(0));

		assertEquals(1, ctrl.giveNextID(ClientType.Light));
		assertEquals(ClientType.Light, ctrl.getClientType(1));
		assertEquals(ClientType.Fridge, ctrl.getClientType(0));
		
		assertEquals(2, ctrl.giveNextID(ClientType.TemperatureSensor));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClientType(2));
		assertEquals(ClientType.Light, ctrl.getClientType(1));
		assertEquals(ClientType.Fridge, ctrl.getClientType(0));
		
		assertEquals(3, ctrl.giveNextID(ClientType.User));
		assertEquals(ClientType.User, ctrl.getClientType(3));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClientType(2));
		assertEquals(ClientType.Light, ctrl.getClientType(1));
		assertEquals(ClientType.Fridge, ctrl.getClientType(0));

		ctrl.removeID(1);
		assertEquals(ClientType.User, ctrl.getClientType(3));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClientType(2));
		assertEquals(null, ctrl.getClientType(1));
		assertEquals(ClientType.Fridge, ctrl.getClientType(0));

		ctrl.removeID(1);
		assertEquals(ClientType.User, ctrl.getClientType(3));
		assertEquals(ClientType.TemperatureSensor, ctrl.getClientType(2));
		assertEquals(null, ctrl.getClientType(1));
		assertEquals(ClientType.Fridge, ctrl.getClientType(0));
	}
}
