package ai.vital.vitalsigns.model.property

import ai.vital.vitalsigns.datatype.Truth

class TruthProperty implements IProperty {

    private static final long serialVersionUID = -111L;
    
    private Truth _value = null
    
    public TruthProperty() {
        this._value = Truth.UNKNOWN
    }
    
    public TruthProperty(Truth val) {
        if(val == null) throw new RuntimeException("Truth property value must not be null")
        this._value = val
    }
    
    public void setValue(Truth val) {
        this._value = val
    }
       
    
    //should it return boolean values ?
    /*
    public boolean booleanValue() {
        if(this._value == Truth.UNKNOWN) {
            throw new RuntimeException("Cannot get boolean value from UKNOWN truth property")
        }
        return _value == Truth.YES;
    }
    
    public boolean asBoolean() {
        return booleanValue()
    }
    */
 
    @Override
    public String toString() {
        return "" + _value;
    }

    @Override
    public Object rawValue() {
        return _value;
    }

    @Override
    public IProperty unwrapped() {
        return this;
    }
    
    public Truth getTruth() {
        return this._value
    }
       
    
    /**
     * method for generic groovy truth interface
     */
    public int asTruth() {
        
        if(_value == Truth.YES) {
            return 1
        } else if(_value == Truth.NO) {
            return -1
        } else if(_value == Truth.UNKNOWN) {
            return 0
        } else if(_value == Truth.MU) {
            return 2
        } else {
            throw new RuntimeException("Unhandled truth property value: " + _value)
        }
        
    }
    
    private String externalURI
    
    public String getURI() {
        if(externalURI != null) return externalURI
        throw new RuntimeException("getURI should be implemented by property trait");
    }
    
    public static TruthProperty createInstance(String propertyURI) {
        TruthProperty bp = new TruthProperty()
        bp.externalURI = propertyURI
        return bp
    }
    
    @Override
    public void setExternalPropertyURI(String externalPropertyURI) {
        this.externalURI = externalPropertyURI;
    }
	
	@Override
	boolean equals(Object n) {

		//      if(n instanceof GeneratedGroovyProxy) {
		//          n = ((GeneratedGroovyProxy)n).getProxyTarget();
		//      }

		if(n instanceof IProperty) {
			n = n.rawValue()
		}

		return this.rawValue().equals(n)

				
	}
	
}
