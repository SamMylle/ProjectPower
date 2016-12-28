/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package avro.ProjectPower;

@SuppressWarnings("all")
/** Methods for the TemperatureSensor class */
@org.apache.avro.specific.AvroGenerated
public interface communicationTempSensor {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"communicationTempSensor\",\"namespace\":\"avro.ProjectPower\",\"doc\":\"Methods for the TemperatureSensor class\",\"types\":[{\"type\":\"enum\",\"name\":\"ClientType\",\"symbols\":[\"Light\",\"SmartFridge\",\"User\",\"TemperatureSensor\"]}],\"messages\":{\"aliveAndKicking\":{\"request\":[],\"response\":\"boolean\"},\"newServer\":{\"request\":[{\"name\":\"newServerIP\",\"type\":\"string\"},{\"name\":\"newServerID\",\"type\":\"int\"}],\"response\":\"null\"},\"getTemperatureRecords\":{\"request\":[],\"response\":{\"type\":\"array\",\"items\":\"double\"}},\"reLogin\":{\"request\":[],\"response\":\"null\",\"one-way\":true}}}");
  boolean aliveAndKicking() throws org.apache.avro.AvroRemoteException;
  java.lang.Void newServer(java.lang.CharSequence newServerIP, int newServerID) throws org.apache.avro.AvroRemoteException;
  java.util.List<java.lang.Double> getTemperatureRecords() throws org.apache.avro.AvroRemoteException;
  void reLogin();

  @SuppressWarnings("all")
  /** Methods for the TemperatureSensor class */
  public interface Callback extends communicationTempSensor {
    public static final org.apache.avro.Protocol PROTOCOL = avro.ProjectPower.communicationTempSensor.PROTOCOL;
    void aliveAndKicking(org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void newServer(java.lang.CharSequence newServerIP, int newServerID, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void getTemperatureRecords(org.apache.avro.ipc.Callback<java.util.List<java.lang.Double>> callback) throws java.io.IOException;
  }
}