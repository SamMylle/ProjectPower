package test;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import util.Logger;
import util.SuppressSystemOut;

public class TestRun{
	public static void main(String[] args) {
		SuppressSystemOut suppress = new SuppressSystemOut();
		
		suppress.suppressOutput();
		Result result = JUnitCore.runClasses(ControllerTest.class);
		suppress.activateOutput();

		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("ControllerTest: ");
		System.out.println(result.wasSuccessful());



		suppress.suppressOutput();
		result = JUnitCore.runClasses(TemperatureRecordTest.class);
		suppress.activateOutput();

		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("TemperatureRecordTest: ");
		System.out.println(result.wasSuccessful());
		

		suppress.suppressOutput();
		result = JUnitCore.runClasses(LightTest.class);
		suppress.activateOutput();

		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("LightTest: ");
		System.out.println(result.wasSuccessful());
		

		suppress.suppressOutput();
		result = JUnitCore.runClasses(SmartFridgeTest.class);
		suppress.activateOutput();
		
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("SmartFridgeTest: ");
		System.out.println(result.wasSuccessful());
		

		suppress.suppressOutput();
		result = JUnitCore.runClasses(DistUserTest.class);
		suppress.activateOutput();
		
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("DistUserTest: ");
		System.out.println(result.wasSuccessful());
		
		

		suppress.suppressOutput();
		result = JUnitCore.runClasses(DistLightTest.class);
		suppress.activateOutput();
		
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("DistLightTest: ");
		System.out.println(result.wasSuccessful());
		
		

		result = JUnitCore.runClasses(DistControllerTest.class);
		
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("DistControllerTest: ");
		System.out.println(result.wasSuccessful());
		
		
		
		result = JUnitCore.runClasses(DistSmartFridgeTest.class);
		for (Failure failure : result.getFailures()) {
			System.out.println(failure.toString());
		}
		System.out.print("DistSmartFridgeTest: ");
		System.out.println(result.wasSuccessful());
		
		
		
		System.exit(0);
	}
}
