package util;

import java.awt.List;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.avro.AvroRemoteException;

import controller.DistController;

import avro.ProjectPower.ClientType;
import avro.ProjectPower.ServerData;

public class ServerDataUnion {
	
	/**
	 * Ignores temperatures and usedfridgeports!!!
	 * @param data
	 * @return
	 */
	public static ServerData copy(ServerData data){
		ServerData ret = new ServerData();
		
		ret.setCurrentMaxPort(new Integer(data.getCurrentMaxPort()));
		ret.setIp(new String(data.getIp().toString()));
		ret.setPort(new Integer(data.getPort()));
		ret.setOriginalControllerPort(new Integer(data.getOriginalControllerPort()));
		ret.setMaxTemperatures(new Integer(data.getMaxTemperatures()));
		ret.setCurrentMaxPort(new Integer(data.getCurrentMaxPort()));
		ret.setPreviousControllerIP(new String(data.getPreviousControllerIP().toString()));
		ret.setUsedFridgePorts(new LinkedList<Integer>());
		ret.setIPsIP(new LinkedList<CharSequence>(data.getIPsIP()));
		ret.setIPsID(new LinkedList<Integer>(data.getIPsID()));
		ret.setNamesID(new LinkedList<Integer>(data.getNamesID()));
		ret.setNamesClientType(new LinkedList<ClientType>(data.getNamesClientType()));
		ret.setTemperatures(new LinkedList<java.util.List<Double>>());
		ret.setTemperaturesIDs(new LinkedList<Integer>());
		
		return ret;
	}

	/**
	 * Does not consider temperatures and fridge ports
	 * @param data1
	 * @param data2
	 * @return
	 */
	public static ServerData getUnion(ServerData data1, ServerData data2){
		ServerData ret = copy(data1);
		
		for (int i = 0; i < data2.getIPsIP().size(); i++){
			if (! ret.getIPsIP().contains(data2.getIPsIP().get(i))){
				ret.getIPsIP().add(data2.getIPsIP().get(i));
				ret.getIPsID().add(data2.getIPsID().get(i));
			}
		}
		
		for (int i = 0; i < data2.getNamesID().size(); i++){
			if (! ret.getNamesID().contains(data2.getNamesID().get(i))){
				ret.getNamesID().add(data2.getNamesID().get(i));
				ret.getNamesClientType().add(data2.getNamesClientType().get(i));
			}
		}
		
		/// Stuff below was for checks
//		for (int i = 0; i < data2.getIPsIP().size(); i++){
//			if (! ret.getIPsIP().contains(data2.getIPsIP().get(i))){
//				ret.getIPsIP().add(data2.getIPsIP().get(i));
//				ret.getIPsID().add(data2.getIPsID().get(i));
//				System.out.println("Something went wrong");
//			}
//		}
//		
//		for (int i = 0; i < data2.getNamesID().size(); i++){
//			if (! ret.getNamesID().contains(data2.getNamesID().get(i))){
//				ret.getNamesID().add(data2.getNamesID().get(i));
//				ret.getNamesClientType().add(data2.getNamesClientType().get(i));
//				System.out.println("Something went wrong");
//			}
//		}
		
		return ret;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		DistController controller = new DistController(5000, 20, "192.168.0.128");
		try {
			controller.LogOn(ClientType.Light, "Derp");
			controller.loginSuccessful(5001);
		} catch (AvroRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ServerData copy = null;
		copy = controller.makeBackup();
		try {
			controller.logOff(5001);
			controller.stopServer();
		} catch (AvroRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		controller = new DistController(50001, 20, "192.168.0.128");
		try {
			controller.LogOn(ClientType.TemperatureSensor, "DerpSecundo");
			controller.loginSuccessful(5001);
		} catch (AvroRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ServerData copy2 = null;
		copy2 = controller.makeBackup();
		try {
			controller.logOff(5001);
		} catch (AvroRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		ServerData copy3 = ServerDataUnion.getUnion(copy, copy2);
		System.out.println(copy);
		System.out.println(copy2);
		System.out.println(copy3);
		controller.stopServer();
		
	}

}
