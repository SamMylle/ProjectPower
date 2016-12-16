/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package avro.ProjectPower;

@SuppressWarnings("all")
/** Methods for the SmartFridge class, specifically destined for the CONTROLLER */
@org.apache.avro.specific.AvroGenerated
public interface communicationFridge {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"communicationFridge\",\"namespace\":\"avro.ProjectPower\",\"doc\":\"Methods for the SmartFridge class, specifically destined for the CONTROLLER\",\"types\":[{\"type\":\"enum\",\"name\":\"ClientType\",\"symbols\":[\"Light\",\"SmartFridge\",\"User\",\"TemperatureSensor\"]},{\"type\":\"record\",\"name\":\"ServerData\",\"fields\":[{\"name\":\"port\",\"type\":\"int\"},{\"name\":\"originalControllerPort\",\"type\":\"int\"},{\"name\":\"maxTemperatures\",\"type\":\"int\"},{\"name\":\"currentMaxPort\",\"type\":\"int\"},{\"name\":\"ip\",\"type\":\"string\"},{\"name\":\"previousControllerIP\",\"type\":\"string\"},{\"name\":\"usedFridgePorts\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"IPsID\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"IPsIP\",\"type\":{\"type\":\"array\",\"items\":\"string\"}},{\"name\":\"namesID\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"namesClientType\",\"type\":{\"type\":\"array\",\"items\":\"ClientType\"}},{\"name\":\"temperatures\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"array\",\"items\":\"double\"}}},{\"name\":\"temperaturesIDs\",\"type\":{\"type\":\"array\",\"items\":\"int\"}}]}],\"messages\":{\"getItemsRemote\":{\"request\":[],\"response\":{\"type\":\"array\",\"items\":\"string\"}},\"testMethod\":{\"request\":[{\"name\":\"clienttype\",\"type\":\"ClientType\"}],\"response\":\"boolean\"},\"requestFridgeCommunication\":{\"request\":[{\"name\":\"userServerPort\",\"type\":\"int\"}],\"response\":\"int\"},\"aliveAndKicking\":{\"request\":[],\"response\":\"boolean\"},\"newServer\":{\"request\":[{\"name\":\"newServerIP\",\"type\":\"string\"},{\"name\":\"newServerID\",\"type\":\"int\"}],\"response\":\"null\",\"one-way\":true},\"makeBackup\":{\"request\":[{\"name\":\"data\",\"type\":\"ServerData\"}],\"response\":\"null\",\"one-way\":true},\"electNewController\":{\"request\":[{\"name\":\"index\",\"type\":\"int\"},{\"name\":\"clientID\",\"type\":\"int\"}],\"response\":\"null\",\"one-way\":true},\"reLogin\":{\"request\":[],\"response\":\"null\",\"one-way\":true}}}");
  java.util.List<java.lang.CharSequence> getItemsRemote() throws org.apache.avro.AvroRemoteException;
  boolean testMethod(avro.ProjectPower.ClientType clienttype) throws org.apache.avro.AvroRemoteException;
  int requestFridgeCommunication(int userServerPort) throws org.apache.avro.AvroRemoteException;
  boolean aliveAndKicking() throws org.apache.avro.AvroRemoteException;
  void newServer(java.lang.CharSequence newServerIP, int newServerID);
  void makeBackup(avro.ProjectPower.ServerData data);
  void electNewController(int index, int clientID);
  void reLogin();

  @SuppressWarnings("all")
  /** Methods for the SmartFridge class, specifically destined for the CONTROLLER */
  public interface Callback extends communicationFridge {
    public static final org.apache.avro.Protocol PROTOCOL = avro.ProjectPower.communicationFridge.PROTOCOL;
    void getItemsRemote(org.apache.avro.ipc.Callback<java.util.List<java.lang.CharSequence>> callback) throws java.io.IOException;
    void testMethod(avro.ProjectPower.ClientType clienttype, org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void requestFridgeCommunication(int userServerPort, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
    void aliveAndKicking(org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
  }
}