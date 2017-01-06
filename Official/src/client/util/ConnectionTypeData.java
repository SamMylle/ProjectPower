package client.util;

import avro.ProjectPower.ClientType;

public class ConnectionTypeData extends ConnectionData {

	private ClientType f_clientType;
	
	public ConnectionTypeData() {
		super();
		f_clientType = null;
	}

	public ConnectionTypeData(String IP, int PORT, ClientType type) {
		super(IP, PORT);
		f_clientType = type;
	}
	
	public ClientType getType() {
		return f_clientType;
	}
	
	public void setType(ClientType type) {
		f_clientType = type;
	}
	
	public String toString() {
		return super.toString() + ", Type = " + f_clientType.toString();
	}

	public Boolean equals(ConnectionTypeData o) {
		return super.equals(o) && f_clientType == o.getType();
	}
}
