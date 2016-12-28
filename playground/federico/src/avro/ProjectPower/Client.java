/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package avro.ProjectPower;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class Client extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Client\",\"namespace\":\"avro.ProjectPower\",\"fields\":[{\"name\":\"clientType\",\"type\":{\"type\":\"enum\",\"name\":\"ClientType\",\"symbols\":[\"Light\",\"SmartFridge\",\"User\",\"TemperatureSensor\"]}},{\"name\":\"ID\",\"type\":\"int\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public avro.ProjectPower.ClientType clientType;
  @Deprecated public int ID;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public Client() {}

  /**
   * All-args constructor.
   */
  public Client(avro.ProjectPower.ClientType clientType, java.lang.Integer ID) {
    this.clientType = clientType;
    this.ID = ID;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return clientType;
    case 1: return ID;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: clientType = (avro.ProjectPower.ClientType)value$; break;
    case 1: ID = (java.lang.Integer)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'clientType' field.
   */
  public avro.ProjectPower.ClientType getClientType() {
    return clientType;
  }

  /**
   * Sets the value of the 'clientType' field.
   * @param value the value to set.
   */
  public void setClientType(avro.ProjectPower.ClientType value) {
    this.clientType = value;
  }

  /**
   * Gets the value of the 'ID' field.
   */
  public java.lang.Integer getID() {
    return ID;
  }

  /**
   * Sets the value of the 'ID' field.
   * @param value the value to set.
   */
  public void setID(java.lang.Integer value) {
    this.ID = value;
  }

  /** Creates a new Client RecordBuilder */
  public static avro.ProjectPower.Client.Builder newBuilder() {
    return new avro.ProjectPower.Client.Builder();
  }
  
  /** Creates a new Client RecordBuilder by copying an existing Builder */
  public static avro.ProjectPower.Client.Builder newBuilder(avro.ProjectPower.Client.Builder other) {
    return new avro.ProjectPower.Client.Builder(other);
  }
  
  /** Creates a new Client RecordBuilder by copying an existing Client instance */
  public static avro.ProjectPower.Client.Builder newBuilder(avro.ProjectPower.Client other) {
    return new avro.ProjectPower.Client.Builder(other);
  }
  
  /**
   * RecordBuilder for Client instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Client>
    implements org.apache.avro.data.RecordBuilder<Client> {

    private avro.ProjectPower.ClientType clientType;
    private int ID;

    /** Creates a new Builder */
    private Builder() {
      super(avro.ProjectPower.Client.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(avro.ProjectPower.Client.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.clientType)) {
        this.clientType = data().deepCopy(fields()[0].schema(), other.clientType);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.ID)) {
        this.ID = data().deepCopy(fields()[1].schema(), other.ID);
        fieldSetFlags()[1] = true;
      }
    }
    
    /** Creates a Builder by copying an existing Client instance */
    private Builder(avro.ProjectPower.Client other) {
            super(avro.ProjectPower.Client.SCHEMA$);
      if (isValidValue(fields()[0], other.clientType)) {
        this.clientType = data().deepCopy(fields()[0].schema(), other.clientType);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.ID)) {
        this.ID = data().deepCopy(fields()[1].schema(), other.ID);
        fieldSetFlags()[1] = true;
      }
    }

    /** Gets the value of the 'clientType' field */
    public avro.ProjectPower.ClientType getClientType() {
      return clientType;
    }
    
    /** Sets the value of the 'clientType' field */
    public avro.ProjectPower.Client.Builder setClientType(avro.ProjectPower.ClientType value) {
      validate(fields()[0], value);
      this.clientType = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'clientType' field has been set */
    public boolean hasClientType() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'clientType' field */
    public avro.ProjectPower.Client.Builder clearClientType() {
      clientType = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'ID' field */
    public java.lang.Integer getID() {
      return ID;
    }
    
    /** Sets the value of the 'ID' field */
    public avro.ProjectPower.Client.Builder setID(int value) {
      validate(fields()[1], value);
      this.ID = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'ID' field has been set */
    public boolean hasID() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'ID' field */
    public avro.ProjectPower.Client.Builder clearID() {
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    public Client build() {
      try {
        Client record = new Client();
        record.clientType = fieldSetFlags()[0] ? this.clientType : (avro.ProjectPower.ClientType) defaultValue(fields()[0]);
        record.ID = fieldSetFlags()[1] ? this.ID : (java.lang.Integer) defaultValue(fields()[1]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
