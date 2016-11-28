/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package avro.ProjectPower;

@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public interface ControllerComm {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"ControllerComm\",\"namespace\":\"avro.ProjectPower\",\"types\":[{\"type\":\"enum\",\"name\":\"ClientType\",\"symbols\":[\"Light\",\"SmartFridge\",\"User\",\"TemperatureSensor\"]}],\"messages\":{\"getID\":{\"request\":[{\"name\":\"type\",\"type\":\"ClientType\"}],\"response\":\"int\"},\"getClientType\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"ClientType\"},\"logOff\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"null\"},\"addTemperature\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"},{\"name\":\"temperature\",\"type\":\"double\"}],\"response\":\"null\"},\"averageCurrentTemperature\":{\"request\":[],\"response\":\"double\"},\"hasValidTemperatures\":{\"request\":[],\"response\":\"boolean\"},\"setupFridgeCommunication\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"int\"},\"listenToMe\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"},{\"name\":\"type\",\"type\":\"ClientType\"}],\"response\":\"null\"},\"getFridgeInventory\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":{\"type\":\"array\",\"items\":\"string\"}},\"setLight\":{\"request\":[{\"name\":\"newState\",\"type\":\"int\"},{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"int\"},\"getLightState\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"int\"}}}");
  int getID(avro.ProjectPower.ClientType type) throws org.apache.avro.AvroRemoteException;
  avro.ProjectPower.ClientType getClientType(int ID) throws org.apache.avro.AvroRemoteException;
  java.lang.Void logOff(int ID) throws org.apache.avro.AvroRemoteException;
  java.lang.Void addTemperature(int ID, double temperature) throws org.apache.avro.AvroRemoteException;
  double averageCurrentTemperature() throws org.apache.avro.AvroRemoteException;
  boolean hasValidTemperatures() throws org.apache.avro.AvroRemoteException;
  int setupFridgeCommunication(int ID) throws org.apache.avro.AvroRemoteException;
  java.lang.Void listenToMe(int ID, avro.ProjectPower.ClientType type) throws org.apache.avro.AvroRemoteException;
  java.util.List<java.lang.CharSequence> getFridgeInventory(int ID) throws org.apache.avro.AvroRemoteException;
  int setLight(int newState, int ID) throws org.apache.avro.AvroRemoteException;
  int getLightState(int ID) throws org.apache.avro.AvroRemoteException;

  @SuppressWarnings("all")
  public interface Callback extends ControllerComm {
    public static final org.apache.avro.Protocol PROTOCOL = avro.ProjectPower.ControllerComm.PROTOCOL;
    void getID(avro.ProjectPower.ClientType type, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
    void getClientType(int ID, org.apache.avro.ipc.Callback<avro.ProjectPower.ClientType> callback) throws java.io.IOException;
    void logOff(int ID, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void addTemperature(int ID, double temperature, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void averageCurrentTemperature(org.apache.avro.ipc.Callback<java.lang.Double> callback) throws java.io.IOException;
    void hasValidTemperatures(org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void setupFridgeCommunication(int ID, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
    void listenToMe(int ID, avro.ProjectPower.ClientType type, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void getFridgeInventory(int ID, org.apache.avro.ipc.Callback<java.util.List<java.lang.CharSequence>> callback) throws java.io.IOException;
    void setLight(int newState, int ID, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
    void getLightState(int ID, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
  }
}