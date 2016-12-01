package test;

import java.util.LinkedList;

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

		assertEquals(ctrl.getRawTemperatures().size(), 0);
		assertEquals(ID, ctrl.giveNextID(ClientType.TemperatureSensor));
		/// Test for the amount of temperature records
		assertEquals(ctrl.getRawTemperatures().size(), 1);

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
		/// Temperature records must be deleted
		assertEquals(ctrl.getRawTemperatures().size(), 0);
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

	@Test
	public void testAddTemperature() {
		/// Prove that an invalid delete doesn't affect the system
		ctrl.addTemperature(10, 5000);

		/// Add some sensor, give data
		ctrl.giveNextID(ClientType.TemperatureSensor);
		/// Add data to that sensor and test it
		LinkedList<Double> shouldBe = new LinkedList<Double>();
		for (int i = 0; i < 5; i++){
			ctrl.addTemperature(5 * i, 5001);
			shouldBe.add(new Double(i * 5));
		}

		assertEquals(ctrl.getRawTemperatures().elementAt(0).getID(), 5001);
		assertEquals(ctrl.getRawTemperatures().elementAt(0).getRecord(), shouldBe);

		/// Add some sensor other, give data
		ctrl.giveNextID(ClientType.TemperatureSensor);
		/// Add data to that sensor and test it
		shouldBe = new LinkedList<Double>();
		for (int i = 0; i < 6; i++){
			ctrl.addTemperature(6 * i, 5002);
			shouldBe.add(new Double(i * 6));
		}

		assertEquals(ctrl.getRawTemperatures().elementAt(1).getID(), 5002);
		assertEquals(ctrl.getRawTemperatures().elementAt(1).getRecord(), shouldBe);


		/// Test removing of stuff
		ctrl.removeID(5001);
		assertEquals(ctrl.getRawTemperatures().elementAt(0).getID(), 5002);
		assertEquals(ctrl.getRawTemperatures().elementAt(0).getRecord(), shouldBe);


	}

	@Test
	public void testAverageCurrentTemperature() {
		/// Temperature is 0 if there are no measurements
		assertEquals(new Double(0.0), new Double(ctrl.averageCurrentTemp()));

		/// When there's one sensor
		ctrl.giveNextID(ClientType.TemperatureSensor);
		for (int i = 0; i < 20; i++){
			ctrl.addTemperature(i * 3, 5001);
			assertEquals(new Double(i * 3), new Double(ctrl.averageCurrentTemp()));
		}
		
		/// When there's multiple sensors
		ctrl.giveNextID(ClientType.TemperatureSensor);
		for (int i = 0; i < 5; i++){
			ctrl.addTemperature(i, 5002);
			assertEquals(new Double((19.0 * 3.0 + i) / 2.0), new Double(ctrl.averageCurrentTemp()));
		}
		
		ctrl.giveNextID(ClientType.TemperatureSensor);
		for (int i = 0; i < 13; i++){
			ctrl.addTemperature(i * 2, 5003);
			assertEquals(new Double((19.0 * 3.0 + 4.0 + i * 2.0) / 3.0), new Double(ctrl.averageCurrentTemp()));
		}
	}

	@Test
	public void hasValidTemperature() {
		/// Temperature is 0 if there are no measurements
		assertEquals(ctrl.hasValidTemp(), false);
		
		/// When there's a sensor but there's no records
		ctrl.giveNextID(ClientType.TemperatureSensor);
		assertEquals(ctrl.hasValidTemp(), false);

		/// When there is a sensor with records
		ctrl.addTemperature(10, 5001);
		assertEquals(ctrl.hasValidTemp(), true);
		
		/// When the record is deleted
		ctrl.removeID(5001);
		assertEquals(ctrl.hasValidTemp(), false);
	}
}
