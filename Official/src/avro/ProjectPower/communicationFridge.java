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
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"communicationFridge\",\"namespace\":\"avro.ProjectPower\",\"doc\":\"Methods for the SmartFridge class, specifically destined for the CONTROLLER\",\"types\":[{\"type\":\"enum\",\"name\":\"ClientType\",\"symbols\":[\"Light\",\"SmartFridge\",\"User\",\"TemperatureSensor\"]}],\"messages\":{\"getItemsRemote\":{\"request\":[],\"response\":{\"type\":\"array\",\"items\":\"string\"}},\"testMethod\":{\"request\":[{\"name\":\"clienttype\",\"type\":\"ClientType\"}],\"response\":\"boolean\"},\"requestFridgeCommunication\":{\"request\":[{\"name\":\"userServerPort\",\"type\":\"int\"}],\"response\":\"boolean\"}}}");
  java.util.List<java.lang.CharSequence> getItemsRemote() throws org.apache.avro.AvroRemoteException;
  boolean testMethod(avro.ProjectPower.ClientType clienttype) throws org.apache.avro.AvroRemoteException;
  boolean requestFridgeCommunication(int userServerPort) throws org.apache.avro.AvroRemoteException;

  @SuppressWarnings("all")
  /** Methods for the SmartFridge class, specifically destined for the CONTROLLER */
  public interface Callback extends communicationFridge {
    public static final org.apache.avro.Protocol PROTOCOL = avro.ProjectPower.communicationFridge.PROTOCOL;
    void getItemsRemote(org.apache.avro.ipc.Callback<java.util.List<java.lang.CharSequence>> callback) throws java.io.IOException;
    void testMethod(avro.ProjectPower.ClientType clienttype, org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
    void requestFridgeCommunication(int userServerPort, org.apache.avro.ipc.Callback<java.lang.Boolean> callback) throws java.io.IOException;
  }
}