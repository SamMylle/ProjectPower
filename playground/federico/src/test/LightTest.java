package test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import client.Light;

public class LightTest {

	Light light;
	@Before
	public void setUp() throws Exception {
		light = new Light();
		light.setID(1);
	}

	@Test
	public void testLight() {
		Light newLight = new Light();
		
		assertEquals(newLight.getState(), 0);
		assertEquals(newLight.getID(), -1);
	}

	@Test
	public void testSetAndGetID(){
		/// Negative ID must fail (note that on initial construction the ID is -1!)
		assertFalse(light.setID(-2));
		assertNotEquals(light.getID(), -2);
		
		/// Since 0 is positive, this should work
		assertTrue(light.setID(0));
		assertEquals(light.getID(), 0);
		
		/// Test any other positive integer
		assertTrue(light.setID(5));
		assertEquals(light.getID(), 5);
		
		/// Note that this should not be equal anymore
		assertNotEquals(light.getID(), 0);
		
	}

	@Test
	public void testGetAndSetState() {
		/// Can't have negative state
		assertFalse(light.setState(-10));
		assertNotEquals(light.getState(), -10);
		
		/// Since 0 is positive, this should work
		assertTrue(light.setState(0));
		assertEquals(light.getState(), 0);
		
		/// Test any other positive integer
		assertTrue(light.setState(5));
		assertEquals(light.getState(), 5);
		
		/// Note that this should not be equal anymore
		assertNotEquals(light.getState(), 0);
	}

}
