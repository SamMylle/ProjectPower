/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package avro.ProjectPower;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class ServerData extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"ServerData\",\"namespace\":\"avro.ProjectPower\",\"fields\":[{\"name\":\"port\",\"type\":\"int\"},{\"name\":\"originalControllerPort\",\"type\":\"int\"},{\"name\":\"maxTemperatures\",\"type\":\"int\"},{\"name\":\"currentMaxPort\",\"type\":\"int\"},{\"name\":\"ip\",\"type\":\"string\"},{\"name\":\"previousControllerIP\",\"type\":\"string\"},{\"name\":\"usedFridgePorts\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"IPsID\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"IPsIP\",\"type\":{\"type\":\"array\",\"items\":\"string\"}},{\"name\":\"namesID\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"namesClientType\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"enum\",\"name\":\"ClientType\",\"symbols\":[\"Light\",\"SmartFridge\",\"User\",\"TemperatureSensor\"]}}},{\"name\":\"temperatures\",\"type\":{\"type\":\"array\",\"items\":{\"type\":\"array\",\"items\":\"double\"}}},{\"name\":\"temperaturesIDs\",\"type\":{\"type\":\"array\",\"items\":\"int\"}},{\"name\":\"IP\",\"type\":\"string\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public int port;
  @Deprecated public int originalControllerPort;
  @Deprecated public int maxTemperatures;
  @Deprecated public int currentMaxPort;
  @Deprecated public java.lang.CharSequence ip;
  @Deprecated public java.lang.CharSequence previousControllerIP;
  @Deprecated public java.util.List<java.lang.Integer> usedFridgePorts;
  @Deprecated public java.util.List<java.lang.Integer> IPsID;
  @Deprecated public java.util.List<java.lang.CharSequence> IPsIP;
  @Deprecated public java.util.List<java.lang.Integer> namesID;
  @Deprecated public java.util.List<avro.ProjectPower.ClientType> namesClientType;
  @Deprecated public java.util.List<java.util.List<java.lang.Double>> temperatures;
  @Deprecated public java.util.List<java.lang.Integer> temperaturesIDs;
  @Deprecated public java.lang.CharSequence IP;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public ServerData() {}

  /**
   * All-args constructor.
   */
  public ServerData(java.lang.Integer port, java.lang.Integer originalControllerPort, java.lang.Integer maxTemperatures, java.lang.Integer currentMaxPort, java.lang.CharSequence ip, java.lang.CharSequence previousControllerIP, java.util.List<java.lang.Integer> usedFridgePorts, java.util.List<java.lang.Integer> IPsID, java.util.List<java.lang.CharSequence> IPsIP, java.util.List<java.lang.Integer> namesID, java.util.List<avro.ProjectPower.ClientType> namesClientType, java.util.List<java.util.List<java.lang.Double>> temperatures, java.util.List<java.lang.Integer> temperaturesIDs, java.lang.CharSequence IP) {
    this.port = port;
    this.originalControllerPort = originalControllerPort;
    this.maxTemperatures = maxTemperatures;
    this.currentMaxPort = currentMaxPort;
    this.ip = ip;
    this.previousControllerIP = previousControllerIP;
    this.usedFridgePorts = usedFridgePorts;
    this.IPsID = IPsID;
    this.IPsIP = IPsIP;
    this.namesID = namesID;
    this.namesClientType = namesClientType;
    this.temperatures = temperatures;
    this.temperaturesIDs = temperaturesIDs;
    this.IP = IP;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return port;
    case 1: return originalControllerPort;
    case 2: return maxTemperatures;
    case 3: return currentMaxPort;
    case 4: return ip;
    case 5: return previousControllerIP;
    case 6: return usedFridgePorts;
    case 7: return IPsID;
    case 8: return IPsIP;
    case 9: return namesID;
    case 10: return namesClientType;
    case 11: return temperatures;
    case 12: return temperaturesIDs;
    case 13: return IP;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: port = (java.lang.Integer)value$; break;
    case 1: originalControllerPort = (java.lang.Integer)value$; break;
    case 2: maxTemperatures = (java.lang.Integer)value$; break;
    case 3: currentMaxPort = (java.lang.Integer)value$; break;
    case 4: ip = (java.lang.CharSequence)value$; break;
    case 5: previousControllerIP = (java.lang.CharSequence)value$; break;
    case 6: usedFridgePorts = (java.util.List<java.lang.Integer>)value$; break;
    case 7: IPsID = (java.util.List<java.lang.Integer>)value$; break;
    case 8: IPsIP = (java.util.List<java.lang.CharSequence>)value$; break;
    case 9: namesID = (java.util.List<java.lang.Integer>)value$; break;
    case 10: namesClientType = (java.util.List<avro.ProjectPower.ClientType>)value$; break;
    case 11: temperatures = (java.util.List<java.util.List<java.lang.Double>>)value$; break;
    case 12: temperaturesIDs = (java.util.List<java.lang.Integer>)value$; break;
    case 13: IP = (java.lang.CharSequence)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'port' field.
   */
  public java.lang.Integer getPort() {
    return port;
  }

  /**
   * Sets the value of the 'port' field.
   * @param value the value to set.
   */
  public void setPort(java.lang.Integer value) {
    this.port = value;
  }

  /**
   * Gets the value of the 'originalControllerPort' field.
   */
  public java.lang.Integer getOriginalControllerPort() {
    return originalControllerPort;
  }

  /**
   * Sets the value of the 'originalControllerPort' field.
   * @param value the value to set.
   */
  public void setOriginalControllerPort(java.lang.Integer value) {
    this.originalControllerPort = value;
  }

  /**
   * Gets the value of the 'maxTemperatures' field.
   */
  public java.lang.Integer getMaxTemperatures() {
    return maxTemperatures;
  }

  /**
   * Sets the value of the 'maxTemperatures' field.
   * @param value the value to set.
   */
  public void setMaxTemperatures(java.lang.Integer value) {
    this.maxTemperatures = value;
  }

  /**
   * Gets the value of the 'currentMaxPort' field.
   */
  public java.lang.Integer getCurrentMaxPort() {
    return currentMaxPort;
  }

  /**
   * Sets the value of the 'currentMaxPort' field.
   * @param value the value to set.
   */
  public void setCurrentMaxPort(java.lang.Integer value) {
    this.currentMaxPort = value;
  }

  /**
   * Gets the value of the 'ip' field.
   */
  public java.lang.CharSequence getIp() {
    return ip;
  }

  /**
   * Sets the value of the 'ip' field.
   * @param value the value to set.
   */
  public void setIp(java.lang.CharSequence value) {
    this.ip = value;
  }

  /**
   * Gets the value of the 'previousControllerIP' field.
   */
  public java.lang.CharSequence getPreviousControllerIP() {
    return previousControllerIP;
  }

  /**
   * Sets the value of the 'previousControllerIP' field.
   * @param value the value to set.
   */
  public void setPreviousControllerIP(java.lang.CharSequence value) {
    this.previousControllerIP = value;
  }

  /**
   * Gets the value of the 'usedFridgePorts' field.
   */
  public java.util.List<java.lang.Integer> getUsedFridgePorts() {
    return usedFridgePorts;
  }

  /**
   * Sets the value of the 'usedFridgePorts' field.
   * @param value the value to set.
   */
  public void setUsedFridgePorts(java.util.List<java.lang.Integer> value) {
    this.usedFridgePorts = value;
  }

  /**
   * Gets the value of the 'IPsID' field.
   */
  public java.util.List<java.lang.Integer> getIPsID() {
    return IPsID;
  }

  /**
   * Sets the value of the 'IPsID' field.
   * @param value the value to set.
   */
  public void setIPsID(java.util.List<java.lang.Integer> value) {
    this.IPsID = value;
  }

  /**
   * Gets the value of the 'IPsIP' field.
   */
  public java.util.List<java.lang.CharSequence> getIPsIP() {
    return IPsIP;
  }

  /**
   * Sets the value of the 'IPsIP' field.
   * @param value the value to set.
   */
  public void setIPsIP(java.util.List<java.lang.CharSequence> value) {
    this.IPsIP = value;
  }

  /**
   * Gets the value of the 'namesID' field.
   */
  public java.util.List<java.lang.Integer> getNamesID() {
    return namesID;
  }

  /**
   * Sets the value of the 'namesID' field.
   * @param value the value to set.
   */
  public void setNamesID(java.util.List<java.lang.Integer> value) {
    this.namesID = value;
  }

  /**
   * Gets the value of the 'namesClientType' field.
   */
  public java.util.List<avro.ProjectPower.ClientType> getNamesClientType() {
    return namesClientType;
  }

  /**
   * Sets the value of the 'namesClientType' field.
   * @param value the value to set.
   */
  public void setNamesClientType(java.util.List<avro.ProjectPower.ClientType> value) {
    this.namesClientType = value;
  }

  /**
   * Gets the value of the 'temperatures' field.
   */
  public java.util.List<java.util.List<java.lang.Double>> getTemperatures() {
    return temperatures;
  }

  /**
   * Sets the value of the 'temperatures' field.
   * @param value the value to set.
   */
  public void setTemperatures(java.util.List<java.util.List<java.lang.Double>> value) {
    this.temperatures = value;
  }

  /**
   * Gets the value of the 'temperaturesIDs' field.
   */
  public java.util.List<java.lang.Integer> getTemperaturesIDs() {
    return temperaturesIDs;
  }

  /**
   * Sets the value of the 'temperaturesIDs' field.
   * @param value the value to set.
   */
  public void setTemperaturesIDs(java.util.List<java.lang.Integer> value) {
    this.temperaturesIDs = value;
  }

  /**
   * Gets the value of the 'IP' field.
   */
  public java.lang.CharSequence getIP() {
    return IP;
  }

  /**
   * Sets the value of the 'IP' field.
   * @param value the value to set.
   */
  public void setIP(java.lang.CharSequence value) {
    this.IP = value;
  }

  /** Creates a new ServerData RecordBuilder */
  public static avro.ProjectPower.ServerData.Builder newBuilder() {
    return new avro.ProjectPower.ServerData.Builder();
  }
  
  /** Creates a new ServerData RecordBuilder by copying an existing Builder */
  public static avro.ProjectPower.ServerData.Builder newBuilder(avro.ProjectPower.ServerData.Builder other) {
    return new avro.ProjectPower.ServerData.Builder(other);
  }
  
  /** Creates a new ServerData RecordBuilder by copying an existing ServerData instance */
  public static avro.ProjectPower.ServerData.Builder newBuilder(avro.ProjectPower.ServerData other) {
    return new avro.ProjectPower.ServerData.Builder(other);
  }
  
  /**
   * RecordBuilder for ServerData instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<ServerData>
    implements org.apache.avro.data.RecordBuilder<ServerData> {

    private int port;
    private int originalControllerPort;
    private int maxTemperatures;
    private int currentMaxPort;
    private java.lang.CharSequence ip;
    private java.lang.CharSequence previousControllerIP;
    private java.util.List<java.lang.Integer> usedFridgePorts;
    private java.util.List<java.lang.Integer> IPsID;
    private java.util.List<java.lang.CharSequence> IPsIP;
    private java.util.List<java.lang.Integer> namesID;
    private java.util.List<avro.ProjectPower.ClientType> namesClientType;
    private java.util.List<java.util.List<java.lang.Double>> temperatures;
    private java.util.List<java.lang.Integer> temperaturesIDs;
    private java.lang.CharSequence IP;

    /** Creates a new Builder */
    private Builder() {
      super(avro.ProjectPower.ServerData.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(avro.ProjectPower.ServerData.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.port)) {
        this.port = data().deepCopy(fields()[0].schema(), other.port);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.originalControllerPort)) {
        this.originalControllerPort = data().deepCopy(fields()[1].schema(), other.originalControllerPort);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.maxTemperatures)) {
        this.maxTemperatures = data().deepCopy(fields()[2].schema(), other.maxTemperatures);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.currentMaxPort)) {
        this.currentMaxPort = data().deepCopy(fields()[3].schema(), other.currentMaxPort);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.ip)) {
        this.ip = data().deepCopy(fields()[4].schema(), other.ip);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.previousControllerIP)) {
        this.previousControllerIP = data().deepCopy(fields()[5].schema(), other.previousControllerIP);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.usedFridgePorts)) {
        this.usedFridgePorts = data().deepCopy(fields()[6].schema(), other.usedFridgePorts);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.IPsID)) {
        this.IPsID = data().deepCopy(fields()[7].schema(), other.IPsID);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.IPsIP)) {
        this.IPsIP = data().deepCopy(fields()[8].schema(), other.IPsIP);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.namesID)) {
        this.namesID = data().deepCopy(fields()[9].schema(), other.namesID);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.namesClientType)) {
        this.namesClientType = data().deepCopy(fields()[10].schema(), other.namesClientType);
        fieldSetFlags()[10] = true;
      }
      if (isValidValue(fields()[11], other.temperatures)) {
        this.temperatures = data().deepCopy(fields()[11].schema(), other.temperatures);
        fieldSetFlags()[11] = true;
      }
      if (isValidValue(fields()[12], other.temperaturesIDs)) {
        this.temperaturesIDs = data().deepCopy(fields()[12].schema(), other.temperaturesIDs);
        fieldSetFlags()[12] = true;
      }
      if (isValidValue(fields()[13], other.IP)) {
        this.IP = data().deepCopy(fields()[13].schema(), other.IP);
        fieldSetFlags()[13] = true;
      }
    }
    
    /** Creates a Builder by copying an existing ServerData instance */
    private Builder(avro.ProjectPower.ServerData other) {
            super(avro.ProjectPower.ServerData.SCHEMA$);
      if (isValidValue(fields()[0], other.port)) {
        this.port = data().deepCopy(fields()[0].schema(), other.port);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.originalControllerPort)) {
        this.originalControllerPort = data().deepCopy(fields()[1].schema(), other.originalControllerPort);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.maxTemperatures)) {
        this.maxTemperatures = data().deepCopy(fields()[2].schema(), other.maxTemperatures);
        fieldSetFlags()[2] = true;
      }
      if (isValidValue(fields()[3], other.currentMaxPort)) {
        this.currentMaxPort = data().deepCopy(fields()[3].schema(), other.currentMaxPort);
        fieldSetFlags()[3] = true;
      }
      if (isValidValue(fields()[4], other.ip)) {
        this.ip = data().deepCopy(fields()[4].schema(), other.ip);
        fieldSetFlags()[4] = true;
      }
      if (isValidValue(fields()[5], other.previousControllerIP)) {
        this.previousControllerIP = data().deepCopy(fields()[5].schema(), other.previousControllerIP);
        fieldSetFlags()[5] = true;
      }
      if (isValidValue(fields()[6], other.usedFridgePorts)) {
        this.usedFridgePorts = data().deepCopy(fields()[6].schema(), other.usedFridgePorts);
        fieldSetFlags()[6] = true;
      }
      if (isValidValue(fields()[7], other.IPsID)) {
        this.IPsID = data().deepCopy(fields()[7].schema(), other.IPsID);
        fieldSetFlags()[7] = true;
      }
      if (isValidValue(fields()[8], other.IPsIP)) {
        this.IPsIP = data().deepCopy(fields()[8].schema(), other.IPsIP);
        fieldSetFlags()[8] = true;
      }
      if (isValidValue(fields()[9], other.namesID)) {
        this.namesID = data().deepCopy(fields()[9].schema(), other.namesID);
        fieldSetFlags()[9] = true;
      }
      if (isValidValue(fields()[10], other.namesClientType)) {
        this.namesClientType = data().deepCopy(fields()[10].schema(), other.namesClientType);
        fieldSetFlags()[10] = true;
      }
      if (isValidValue(fields()[11], other.temperatures)) {
        this.temperatures = data().deepCopy(fields()[11].schema(), other.temperatures);
        fieldSetFlags()[11] = true;
      }
      if (isValidValue(fields()[12], other.temperaturesIDs)) {
        this.temperaturesIDs = data().deepCopy(fields()[12].schema(), other.temperaturesIDs);
        fieldSetFlags()[12] = true;
      }
      if (isValidValue(fields()[13], other.IP)) {
        this.IP = data().deepCopy(fields()[13].schema(), other.IP);
        fieldSetFlags()[13] = true;
      }
    }

    /** Gets the value of the 'port' field */
    public java.lang.Integer getPort() {
      return port;
    }
    
    /** Sets the value of the 'port' field */
    public avro.ProjectPower.ServerData.Builder setPort(int value) {
      validate(fields()[0], value);
      this.port = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'port' field has been set */
    public boolean hasPort() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'port' field */
    public avro.ProjectPower.ServerData.Builder clearPort() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /** Gets the value of the 'originalControllerPort' field */
    public java.lang.Integer getOriginalControllerPort() {
      return originalControllerPort;
    }
    
    /** Sets the value of the 'originalControllerPort' field */
    public avro.ProjectPower.ServerData.Builder setOriginalControllerPort(int value) {
      validate(fields()[1], value);
      this.originalControllerPort = value;
      fieldSetFlags()[1] = true;
      return this; 
    }
    
    /** Checks whether the 'originalControllerPort' field has been set */
    public boolean hasOriginalControllerPort() {
      return fieldSetFlags()[1];
    }
    
    /** Clears the value of the 'originalControllerPort' field */
    public avro.ProjectPower.ServerData.Builder clearOriginalControllerPort() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /** Gets the value of the 'maxTemperatures' field */
    public java.lang.Integer getMaxTemperatures() {
      return maxTemperatures;
    }
    
    /** Sets the value of the 'maxTemperatures' field */
    public avro.ProjectPower.ServerData.Builder setMaxTemperatures(int value) {
      validate(fields()[2], value);
      this.maxTemperatures = value;
      fieldSetFlags()[2] = true;
      return this; 
    }
    
    /** Checks whether the 'maxTemperatures' field has been set */
    public boolean hasMaxTemperatures() {
      return fieldSetFlags()[2];
    }
    
    /** Clears the value of the 'maxTemperatures' field */
    public avro.ProjectPower.ServerData.Builder clearMaxTemperatures() {
      fieldSetFlags()[2] = false;
      return this;
    }

    /** Gets the value of the 'currentMaxPort' field */
    public java.lang.Integer getCurrentMaxPort() {
      return currentMaxPort;
    }
    
    /** Sets the value of the 'currentMaxPort' field */
    public avro.ProjectPower.ServerData.Builder setCurrentMaxPort(int value) {
      validate(fields()[3], value);
      this.currentMaxPort = value;
      fieldSetFlags()[3] = true;
      return this; 
    }
    
    /** Checks whether the 'currentMaxPort' field has been set */
    public boolean hasCurrentMaxPort() {
      return fieldSetFlags()[3];
    }
    
    /** Clears the value of the 'currentMaxPort' field */
    public avro.ProjectPower.ServerData.Builder clearCurrentMaxPort() {
      fieldSetFlags()[3] = false;
      return this;
    }

    /** Gets the value of the 'ip' field */
    public java.lang.CharSequence getIp() {
      return ip;
    }
    
    /** Sets the value of the 'ip' field */
    public avro.ProjectPower.ServerData.Builder setIp(java.lang.CharSequence value) {
      validate(fields()[4], value);
      this.ip = value;
      fieldSetFlags()[4] = true;
      return this; 
    }
    
    /** Checks whether the 'ip' field has been set */
    public boolean hasIp() {
      return fieldSetFlags()[4];
    }
    
    /** Clears the value of the 'ip' field */
    public avro.ProjectPower.ServerData.Builder clearIp() {
      ip = null;
      fieldSetFlags()[4] = false;
      return this;
    }

    /** Gets the value of the 'previousControllerIP' field */
    public java.lang.CharSequence getPreviousControllerIP() {
      return previousControllerIP;
    }
    
    /** Sets the value of the 'previousControllerIP' field */
    public avro.ProjectPower.ServerData.Builder setPreviousControllerIP(java.lang.CharSequence value) {
      validate(fields()[5], value);
      this.previousControllerIP = value;
      fieldSetFlags()[5] = true;
      return this; 
    }
    
    /** Checks whether the 'previousControllerIP' field has been set */
    public boolean hasPreviousControllerIP() {
      return fieldSetFlags()[5];
    }
    
    /** Clears the value of the 'previousControllerIP' field */
    public avro.ProjectPower.ServerData.Builder clearPreviousControllerIP() {
      previousControllerIP = null;
      fieldSetFlags()[5] = false;
      return this;
    }

    /** Gets the value of the 'usedFridgePorts' field */
    public java.util.List<java.lang.Integer> getUsedFridgePorts() {
      return usedFridgePorts;
    }
    
    /** Sets the value of the 'usedFridgePorts' field */
    public avro.ProjectPower.ServerData.Builder setUsedFridgePorts(java.util.List<java.lang.Integer> value) {
      validate(fields()[6], value);
      this.usedFridgePorts = value;
      fieldSetFlags()[6] = true;
      return this; 
    }
    
    /** Checks whether the 'usedFridgePorts' field has been set */
    public boolean hasUsedFridgePorts() {
      return fieldSetFlags()[6];
    }
    
    /** Clears the value of the 'usedFridgePorts' field */
    public avro.ProjectPower.ServerData.Builder clearUsedFridgePorts() {
      usedFridgePorts = null;
      fieldSetFlags()[6] = false;
      return this;
    }

    /** Gets the value of the 'IPsID' field */
    public java.util.List<java.lang.Integer> getIPsID() {
      return IPsID;
    }
    
    /** Sets the value of the 'IPsID' field */
    public avro.ProjectPower.ServerData.Builder setIPsID(java.util.List<java.lang.Integer> value) {
      validate(fields()[7], value);
      this.IPsID = value;
      fieldSetFlags()[7] = true;
      return this; 
    }
    
    /** Checks whether the 'IPsID' field has been set */
    public boolean hasIPsID() {
      return fieldSetFlags()[7];
    }
    
    /** Clears the value of the 'IPsID' field */
    public avro.ProjectPower.ServerData.Builder clearIPsID() {
      IPsID = null;
      fieldSetFlags()[7] = false;
      return this;
    }

    /** Gets the value of the 'IPsIP' field */
    public java.util.List<java.lang.CharSequence> getIPsIP() {
      return IPsIP;
    }
    
    /** Sets the value of the 'IPsIP' field */
    public avro.ProjectPower.ServerData.Builder setIPsIP(java.util.List<java.lang.CharSequence> value) {
      validate(fields()[8], value);
      this.IPsIP = value;
      fieldSetFlags()[8] = true;
      return this; 
    }
    
    /** Checks whether the 'IPsIP' field has been set */
    public boolean hasIPsIP() {
      return fieldSetFlags()[8];
    }
    
    /** Clears the value of the 'IPsIP' field */
    public avro.ProjectPower.ServerData.Builder clearIPsIP() {
      IPsIP = null;
      fieldSetFlags()[8] = false;
      return this;
    }

    /** Gets the value of the 'namesID' field */
    public java.util.List<java.lang.Integer> getNamesID() {
      return namesID;
    }
    
    /** Sets the value of the 'namesID' field */
    public avro.ProjectPower.ServerData.Builder setNamesID(java.util.List<java.lang.Integer> value) {
      validate(fields()[9], value);
      this.namesID = value;
      fieldSetFlags()[9] = true;
      return this; 
    }
    
    /** Checks whether the 'namesID' field has been set */
    public boolean hasNamesID() {
      return fieldSetFlags()[9];
    }
    
    /** Clears the value of the 'namesID' field */
    public avro.ProjectPower.ServerData.Builder clearNamesID() {
      namesID = null;
      fieldSetFlags()[9] = false;
      return this;
    }

    /** Gets the value of the 'namesClientType' field */
    public java.util.List<avro.ProjectPower.ClientType> getNamesClientType() {
      return namesClientType;
    }
    
    /** Sets the value of the 'namesClientType' field */
    public avro.ProjectPower.ServerData.Builder setNamesClientType(java.util.List<avro.ProjectPower.ClientType> value) {
      validate(fields()[10], value);
      this.namesClientType = value;
      fieldSetFlags()[10] = true;
      return this; 
    }
    
    /** Checks whether the 'namesClientType' field has been set */
    public boolean hasNamesClientType() {
      return fieldSetFlags()[10];
    }
    
    /** Clears the value of the 'namesClientType' field */
    public avro.ProjectPower.ServerData.Builder clearNamesClientType() {
      namesClientType = null;
      fieldSetFlags()[10] = false;
      return this;
    }

    /** Gets the value of the 'temperatures' field */
    public java.util.List<java.util.List<java.lang.Double>> getTemperatures() {
      return temperatures;
    }
    
    /** Sets the value of the 'temperatures' field */
    public avro.ProjectPower.ServerData.Builder setTemperatures(java.util.List<java.util.List<java.lang.Double>> value) {
      validate(fields()[11], value);
      this.temperatures = value;
      fieldSetFlags()[11] = true;
      return this; 
    }
    
    /** Checks whether the 'temperatures' field has been set */
    public boolean hasTemperatures() {
      return fieldSetFlags()[11];
    }
    
    /** Clears the value of the 'temperatures' field */
    public avro.ProjectPower.ServerData.Builder clearTemperatures() {
      temperatures = null;
      fieldSetFlags()[11] = false;
      return this;
    }

    /** Gets the value of the 'temperaturesIDs' field */
    public java.util.List<java.lang.Integer> getTemperaturesIDs() {
      return temperaturesIDs;
    }
    
    /** Sets the value of the 'temperaturesIDs' field */
    public avro.ProjectPower.ServerData.Builder setTemperaturesIDs(java.util.List<java.lang.Integer> value) {
      validate(fields()[12], value);
      this.temperaturesIDs = value;
      fieldSetFlags()[12] = true;
      return this; 
    }
    
    /** Checks whether the 'temperaturesIDs' field has been set */
    public boolean hasTemperaturesIDs() {
      return fieldSetFlags()[12];
    }
    
    /** Clears the value of the 'temperaturesIDs' field */
    public avro.ProjectPower.ServerData.Builder clearTemperaturesIDs() {
      temperaturesIDs = null;
      fieldSetFlags()[12] = false;
      return this;
    }

    /** Gets the value of the 'IP' field */
    public java.lang.CharSequence getIP() {
      return IP;
    }
    
    /** Sets the value of the 'IP' field */
    public avro.ProjectPower.ServerData.Builder setIP(java.lang.CharSequence value) {
      validate(fields()[13], value);
      this.IP = value;
      fieldSetFlags()[13] = true;
      return this; 
    }
    
    /** Checks whether the 'IP' field has been set */
    public boolean hasIP() {
      return fieldSetFlags()[13];
    }
    
    /** Clears the value of the 'IP' field */
    public avro.ProjectPower.ServerData.Builder clearIP() {
      IP = null;
      fieldSetFlags()[13] = false;
      return this;
    }

    @Override
    public ServerData build() {
      try {
        ServerData record = new ServerData();
        record.port = fieldSetFlags()[0] ? this.port : (java.lang.Integer) defaultValue(fields()[0]);
        record.originalControllerPort = fieldSetFlags()[1] ? this.originalControllerPort : (java.lang.Integer) defaultValue(fields()[1]);
        record.maxTemperatures = fieldSetFlags()[2] ? this.maxTemperatures : (java.lang.Integer) defaultValue(fields()[2]);
        record.currentMaxPort = fieldSetFlags()[3] ? this.currentMaxPort : (java.lang.Integer) defaultValue(fields()[3]);
        record.ip = fieldSetFlags()[4] ? this.ip : (java.lang.CharSequence) defaultValue(fields()[4]);
        record.previousControllerIP = fieldSetFlags()[5] ? this.previousControllerIP : (java.lang.CharSequence) defaultValue(fields()[5]);
        record.usedFridgePorts = fieldSetFlags()[6] ? this.usedFridgePorts : (java.util.List<java.lang.Integer>) defaultValue(fields()[6]);
        record.IPsID = fieldSetFlags()[7] ? this.IPsID : (java.util.List<java.lang.Integer>) defaultValue(fields()[7]);
        record.IPsIP = fieldSetFlags()[8] ? this.IPsIP : (java.util.List<java.lang.CharSequence>) defaultValue(fields()[8]);
        record.namesID = fieldSetFlags()[9] ? this.namesID : (java.util.List<java.lang.Integer>) defaultValue(fields()[9]);
        record.namesClientType = fieldSetFlags()[10] ? this.namesClientType : (java.util.List<avro.ProjectPower.ClientType>) defaultValue(fields()[10]);
        record.temperatures = fieldSetFlags()[11] ? this.temperatures : (java.util.List<java.util.List<java.lang.Double>>) defaultValue(fields()[11]);
        record.temperaturesIDs = fieldSetFlags()[12] ? this.temperaturesIDs : (java.util.List<java.lang.Integer>) defaultValue(fields()[12]);
        record.IP = fieldSetFlags()[13] ? this.IP : (java.lang.CharSequence) defaultValue(fields()[13]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}