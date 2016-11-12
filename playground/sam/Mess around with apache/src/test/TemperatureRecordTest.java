package test;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import controller.TemperatureRecord;

public class TemperatureRecordTest {

	TemperatureRecord record = new TemperatureRecord(5, 0);
	@Before
	public void setUp() throws Exception {
		record = new TemperatureRecord(5, 0);
	}

	@Test
	public void testRecord() {
		assertEquals(0, record.getRecord().size());
		
		for (int i = 0; i < 100; i++){
			record.addValue(i);
			LinkedList<Double> list = record.getRecord();
			for (int j = 0; j < list.size(); j++){
				assertEquals(list.get(j), new Double(i + 1 - list.size() + j));
			}
		}
	}

	@Test
	public void testConstr() {
		for (int i = 1; i < 20; i++){
			TemperatureRecord newRecord = new TemperatureRecord(i, i);
			assertEquals(i, newRecord.getID());
			for (int j = 0; j < 2*i; j++){
				newRecord.addValue(j);
				if (j + 1 > i){
					assertEquals(i, newRecord.getRecord().size());
				}else{
					assertEquals(j + 1, newRecord.getRecord().size());
				}
			}
		}
	}

}
