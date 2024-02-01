package ai.vital.vitalsigns.model.property;

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

public class BooleanProperty implements IProperty {

	static {
//		GNumbersEqualsFix.fixGNumbersEquals();
	}
	
	private static final long serialVersionUID = -381786561836555784L;
	
	private boolean _value = false;
		
	public BooleanProperty() {
	}

	public BooleanProperty(boolean value) {
		_value = value;
	}
	
	public BooleanProperty(String strValue) {
		_value = Boolean.parseBoolean(strValue);
	}
	
	public void setValue(boolean val) {
		this._value = val
	}
	
	public boolean booleanValue() {
		return _value;
	}
	
	public boolean asBoolean() {
		return _value
	}

	/*
	static BooleanProperty valueOf(boolean v) {
		return new BooleanProperty(v)
	}
	
	static BooleanProperty valueOf(String s) {
		return new BooleanProperty(s)
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

	
	private String externalURI
	
	public String getURI() {
		if(externalURI != null) return externalURI
		throw new RuntimeException("getURI should be implemented by property trait");
	}
	
	public static BooleanProperty createInstance(String propertyURI) {
		BooleanProperty bp = new BooleanProperty()
		bp.externalURI = propertyURI
		return bp
	}

	/**
	 * creates external property criterion
	 * @param propertyURI
	 * @return
	 */
	public static VitalGraphQueryPropertyCriterion create(String propertyURI) {
		VitalGraphQueryPropertyCriterion c = new VitalGraphQueryPropertyCriterion(propertyURI);
		c.externalProperty = true
		return c;
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
