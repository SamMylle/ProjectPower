package util;

import client.Light;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Logger.getLogger().log("test");
		Light light = new Light();
		Logger.getLogger().log(light.toString());
		light.setID(10);
		light.setState(550);
		Logger.getLogger().log(light.toString());
		light.setID(-10);
		light.setState(-550);
		Logger.getLogger().log(light.toString());
	}

}
