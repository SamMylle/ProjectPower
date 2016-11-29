package test;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import util.Logger;

public class TestRun{
	public static void main(String[] args) {
		Result result = JUnitCore.runClasses(ControllerTest.class);

		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("ControllerTest: ");
		System.out.println(result.wasSuccessful());



		result = JUnitCore.runClasses(TemperatureRecordTest.class);

		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("TemperatureRecordTest: ");
		System.out.println(result.wasSuccessful());
		

		result = JUnitCore.runClasses(LightTest.class);

		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("LightTest: ");
		System.out.println(result.wasSuccessful());
		
		result = JUnitCore.runClasses(SmartFridgeTest.class);
		
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("SmartFridgeTest: ");
		System.out.println(result.wasSuccessful());
		
//		result = JUnitCore.runClasses(DistUserTest.class);
//		
//		for (Failure failure : result.getFailures()) {
//			System.out.println(failure.toString());
//		}
//		System.out.print("DistUserTest: ");
//		System.out.println(result.wasSuccessful());
		
		
	}
}
