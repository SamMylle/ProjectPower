/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package controller.avro.proto;

@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public interface IDAssignment {
  public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"IDAssignment\",\"namespace\":\"avro.proto\",\"types\":[],\"messages\":{\"getID\":{\"request\":[{\"name\":\"type\",\"type\":\"string\"}],\"response\":\"int\"}}}");
  int getID(java.lang.CharSequence type) throws org.apache.avro.AvroRemoteException;

  @SuppressWarnings("all")
  public interface Callback extends IDAssignment {
    public static final org.apache.avro.Protocol PROTOCOL = controller.avro.proto.IDAssignment.PROTOCOL;
    void getID(java.lang.CharSequence type, org.apache.avro.ipc.Callback<java.lang.Integer> callback) throws java.io.IOException;
  }
}