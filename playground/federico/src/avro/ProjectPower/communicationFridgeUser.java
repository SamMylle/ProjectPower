/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package avro.ProjectPower;

@SuppressWarnings("all")
/** Methods for the SmartFridge class, specifically destined for the USER */
@org.apache.avro.specific.AvroGenerated
public interface communicationFridgeUser {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"communicationFridgeUser\",\"namespace\":\"avro.ProjectPower\",\"doc\":\"Methods for the SmartFridge class, specifically destined for the USER\",\"types\":[],\"messages\":{\"addItemRemote\":{\"request\":[{\"name\":\"itemName\",\"type\":\"string\"}],\"response\":\"null\"},\"removeItemRemote\":{\"request\":[{\"name\":\"itemName\",\"type\":\"string\"}],\"response\":\"null\"},\"getItemsRemote\":{\"request\":[],\"response\":{\"type\":\"array\",\"items\":\"string\"}},\"openFridgeRemote\":{\"request\":[],\"response\":\"null\"},\"closeFridgeRemote\":{\"request\":[],\"response\":\"null\"},\"registerUserIP\":{\"request\":[{\"name\":\"userIP\",\"type\":\"string\"},{\"name\":\"userPort\",\"type\":\"int\"}],\"response\":\"null\"}}}");
  java.lang.Void addItemRemote(java.lang.CharSequence itemName) throws org.apache.avro.AvroRemoteException;
  java.lang.Void removeItemRemote(java.lang.CharSequence itemName) throws org.apache.avro.AvroRemoteException;
  java.util.List<java.lang.CharSequence> getItemsRemote() throws org.apache.avro.AvroRemoteException;
  java.lang.Void openFridgeRemote() throws org.apache.avro.AvroRemoteException;
  java.lang.Void closeFridgeRemote() throws org.apache.avro.AvroRemoteException;
  java.lang.Void registerUserIP(java.lang.CharSequence userIP, int userPort) throws org.apache.avro.AvroRemoteException;

  @SuppressWarnings("all")
  /** Methods for the SmartFridge class, specifically destined for the USER */
  public interface Callback extends communicationFridgeUser {
    public static final org.apache.avro.Protocol PROTOCOL = avro.ProjectPower.communicationFridgeUser.PROTOCOL;
    void addItemRemote(java.lang.CharSequence itemName, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void removeItemRemote(java.lang.CharSequence itemName, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void getItemsRemote(org.apache.avro.ipc.Callback<java.util.List<java.lang.CharSequence>> callback) throws java.io.IOException;
    void openFridgeRemote(org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void closeFridgeRemote(org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
    void registerUserIP(java.lang.CharSequence userIP, int userPort, org.apache.avro.ipc.Callback<java.lang.Void> callback) throws java.io.IOException;
  }
}