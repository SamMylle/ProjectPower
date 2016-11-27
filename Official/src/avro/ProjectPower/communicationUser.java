/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package avro.ProjectPower;

@SuppressWarnings("all")
/** Methods for the User class */
@org.apache.avro.specific.AvroGenerated
public interface communicationUser {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"communicationUser\",\"namespace\":\"avro.ProjectPower\",\"doc\":\"Methods for the User class\",\"types\":[{\"type\":\"enum\",\"name\":\"ClientType\",\"symbols\":[\"Light\",\"SmartFridge\",\"User\",\"TemperatureSensor\"]},{\"type\":\"enum\",\"name\":\"UserStatus\",\"symbols\":[\"present\",\"absent\"]}],\"messages\":{\"getStatus\":{\"request\":[],\"response\":\"UserStatus\"}}}");
  avro.ProjectPower.UserStatus getStatus() throws org.apache.avro.AvroRemoteException;

  @SuppressWarnings("all")
  /** Methods for the User class */
  public interface Callback extends communicationUser {
    public static final org.apache.avro.Protocol PROTOCOL = avro.ProjectPower.communicationUser.PROTOCOL;
    void getStatus(org.apache.avro.ipc.Callback<avro.ProjectPower.UserStatus> callback) throws java.io.IOException;
  }
}