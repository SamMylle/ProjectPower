package client.util;

import java.net.InetSocketAddress;

public class ConnectionData {
	private String f_IP;
	private int f_PORT;
	
	/**
	 * Default Constructor for ConnectionData
	 */
	public ConnectionData() {
		f_IP = "";
		f_PORT = -1;
	}
	
	/**
	 * Constructor for ConnectionData
	 * The Port will be set to -1 with this constructor.
	 * 
	 * @param IP
	 * 		The IP address to connect to
	 */
	public ConnectionData(String IP) {
		f_IP = IP;
	}
	
	/**
	 * Constructor for ConnectionData
	 * @param IP
	 * 		The IP address to connect to.
	 * @param PORT
	 * 		The PORT to connect to.
	 */
	public ConnectionData(String IP, int PORT) {
		f_IP = IP;
		f_PORT = PORT;
	}
	
	/**
	 * Copy constructor for ConnectionData
	 * @param toCopy
	 * 		The object to be copied.
	 */
	public ConnectionData(ConnectionData toCopy) {
		this(toCopy.getIP(), toCopy.getPort());
	}
	
	
	
	public String getIP() {
		return f_IP;
	}
	
	public int getPort() {
		return f_PORT;
	}
	
	public void setIP(String IP) {
		f_IP = IP;
	}
	
	public void setPort(int PORT) {
		f_PORT = PORT;
	}
	
	public InetSocketAddress toSocketAddress() {
		return new InetSocketAddress(f_IP, f_PORT);
	}

}
