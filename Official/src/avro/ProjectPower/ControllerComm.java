/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package avro.ProjectPower;

@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public interface ControllerComm {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"ControllerComm\",\"namespace\":\"avro.ProjectPower\",\"types\":[{\"type\":\"enum\",\"name\":\"ClientType\",\"symbols\":[\"Light\",\"SmartFridge\",\"User\",\"TemperatureSensor\"]},{\"type\":\"record\",\"name\":\"Client\",\"fields\":[{\"name\":\"clientType\",\"type\":\"ClientType\"},{\"name\":\"ID\",\"type\":\"int\"}]},{\"type\":\"record\",\"name\":\"CommData\",\"fields\":[{\"name\":\"ID\",\"type\":\"int\"},{\"name\":\"IP\",\"type\":\"string\"}]},{\"type\":\"record\",\"name\":\"ServerData\",\"fields\":[{\"name\":\"port\",\"type\":\"int\"},{\"name\":\"originalControllerPort\",\"type\":\"int\"},{\"name\":\"maxTemperatures\",\"type\":\"int\"},{\"name\":\"currentMaxPort\",\"type\":\"int\"},{\"name\":\"ip\",\"type\":\"string\"},{\"name\":\"previousControllerIP\",\"type\":\"string\"},{\"name\":\"usedFridgePorts\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"IPsID\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"IPsIP\",\"type\":{\"type\":\"array\",\"items\":\"string\"}},{\"name\":\"namesID\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"namesClientType\",\"type\":{\"type\":\"array\",\"items\":\"ClientType\"}},{\"name\":\"temperatures\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"array\",\"items\":\"double\"}}},{\"name\":\"temperaturesIDs\",\"type\":{\"type\":\"array\",\"items\":\"int\"}}]}],\"messages\":{\"LogOn\":{\"request\":[{\"name\":\"type\",\"type\":\"ClientType\"},{\"name\":\"ip\",\"type\":\"string\"}],\"response\":\"int\"},\"retryLogin\":{\"request\":[{\"name\":\"oldID\",\"type\":\"int\"},{\"name\":\"type\",\"type\":\"ClientType\"}],\"response\":\"int\"},\"getClientType\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"ClientType\"},\"logOff\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"null\"},\"addTemperature\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"},{\"name\":\"temperature\",\"type\":\"double\"}],\"response\":\"null\"},\"averageCurrentTemperature\":{\"request\":[],\"response\":\"double\"},\"hasValidTemperatures\":{\"request\":[],\"response\":\"boolean\"},\"setupFridgeCommunication\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"CommData\"},\"reSetupFridgeCommunication\":{\"request\":[{\"name\":\"myID\",\"type\":\"int\"},{\"name\":\"wrongID\",\"type\":\"int\"}],\"response\":\"CommData\"},\"endFridgeCommunication\":{\"request\":[{\"name\":\"usedPort\",\"type\":\"int\"}],\"response\":\"null\"},\"listenToMe\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"},{\"name\":\"type\",\"type\":\"ClientType\"}],\"response\":\"null\"},\"getFridgeInventory\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":{\"type\":\"array\",\"items\":\"string\"}},\"setLight\":{\"request\":[{\"name\":\"newState\",\"type\":\"int\"},{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"int\"},\"getLightState\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"int\"},\"getAllClients\":{\"request\":[],\"response\":{\"type\":\"array\",\"items\":\"Client\"}},\"areYouTheOriginalController\":{\"request\":[],\"response\":\"boolean\"},\"recoverData\":{\"request\":[{\"name\":\"data\",\"type\":\"ServerData\"}],\"response\":\"boolean\"},\"loginSuccessful\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"null\",\"one-way\":true},\"fridgeIsEmpty\":{\"request\":[{\"name\":\"ID\",\"type\":\"int\"}],\"response\":\"null\",\"one-way\":true},\"getTempHistory\":{\"request\":[],\"response\":{\"type\":\"array\",\"items\":\"double\"}}}}");
  int LogOn(avro.ProjectPower.ClientType type, java.lang.CharSequence ip) throws org.apache.avro.AvroRemoteException;
  int retryLogin(int oldID, avro.ProjectPower.ClientType type) throws org.apache.avro.AvroRemoteException;
  avro.ProjectPower.ClientType getClientType(int ID) throws org.apache.avro.AvroRemoteException;
  java.lang.Void logOff(int ID) throws org.apache.avro.AvroRemoteException;
  java.lang.Void addTemperature(int ID, double temperature) throws org.apache.avro.AvroRemoteException;
  double averageCurrentTemperature() throws org.apache.avro.AvroRemoteException;
  boolean hasValidTemperatures() throws org.apache.avro.AvroRemoteException;
  avro.ProjectPower.CommData setupFridgeCommunication(int ID) throws org.apache.avro.AvroRemoteException;
  avro.ProjectPower.CommData reSetupFridgeCommunication(int myID, int wrongID) throws org.apache.avro.AvroRemoteException;
  java.lang.Void endFridgeCommunication(int usedPort) throws org.apache.avro.AvroRemoteException;
  java.lang.Void listenToMe(int ID, avro.ProjectPower.ClientType type) throws org.apache.avro.AvroRemoteException;
  java.util.List<java.lang.CharSequence> getFridgeInventory(int ID) throws org.apache.avro.AvroRemoteException;
  int setLight(int newState, int ID) throws org.apache.avro.AvroRemoteException;
  int getLightState(int ID) throws org.apache.avro.AvroRemoteException;
  java.util.List<avro.ProjectPower.Client> getAllClients() throws org.apache.avro.AvroRemoteException;
  boolean areYouTheOriginalController() throws org.apache.avro.AvroRemoteException;
  boolean recoverData(avro.ProjectPower.ServerData data) throws org.apache.avro.AvroRemoteException;
  void loginSuccessful(int ID);
  void fridgeIsEmpty(int ID);
  java.util.List<java.lang.Double> getTempHistory() throws org.apache.avro.AvroRemoteException;

  @SuppressWarnings("all")
  public interface Callback extends ControllerComm {
    public static final org.apache.avro.Protocol PROTOCOL = avro.ProjectPower.ControllerComm.PROTOCOL;
    void LogOn(avro.ProjectPower.ClientType type, java.lang.CharSequence ip, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
    void retryLogin(int oldID, avro.ProjectPower.ClientType type, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
    void getClientType(int ID, org.apache.avro.ipc.Callback<avro.ProjectPower.ClientType> callback) throws java.io.IOException;
    void logOff(int ID, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void addTemperature(int ID, double temperature, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void averageCurrentTemperature(org.apache.avro.ipc.Callback<java.lang.Double> callback) throws java.io.IOException;
    void hasValidTemperatures(org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void setupFridgeCommunication(int ID, org.apache.avro.ipc.Callback<avro.ProjectPower.CommData> callback) throws java.io.IOException;
    void reSetupFridgeCommunication(int myID, int wrongID, org.apache.avro.ipc.Callback<avro.ProjectPower.CommData> callback) throws java.io.IOException;
    void endFridgeCommunication(int usedPort, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void listenToMe(int ID, avro.ProjectPower.ClientType type, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void getFridgeInventory(int ID, org.apache.avro.ipc.Callback<java.util.List<java.lang.CharSequence>> callback) throws java.io.IOException;
    void setLight(int newState, int ID, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
    void getLightState(int ID, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
    void getAllClients(org.apache.avro.ipc.Callback<java.util.List<avro.ProjectPower.Client>> callback) throws java.io.IOException;
    void areYouTheOriginalController(org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void recoverData(avro.ProjectPower.ServerData data, org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void getTempHistory(org.apache.avro.ipc.Callback<java.util.List<java.lang.Double>> callback) throws java.io.IOException;
  }
}