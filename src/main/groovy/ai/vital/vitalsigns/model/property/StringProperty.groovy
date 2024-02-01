package ai.vital.vitalsigns.model.property;

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

public class StringProperty extends GString implements IProperty {

	private static final long serialVersionUID = 1L;
	
	static {
//		GStringEqualsFix.fixGStringEquals()
	}
	
	private String[] inner;
	
	
	public void setValue(String s) {
		this.inner = [s] as String[]
	}
	
	/*
	public Object asType(Class cls) {
		if(cls == String.class) {
			return this.toString();
		}
		return this
	}
	*/
	
	public StringProperty() {
		super(new Object[0]);
		this.inner = new String[0];
	}

	public StringProperty(String s) {
		super(new Object[0]);
		inner = [s] as String[];
	}

	@Override
	public String[] getStrings() {
		return inner;
	}
	
	public boolean equals(Object that) {
		
		if(that instanceof String) {
			return this.toString().equals(that)
		}
		
		return super.equals(that);
	}

	@Override
	public Object rawValue() {
		return this.toString();
	}

	@Override
	public IProperty unwrapped() {
		return this;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}
	
	
	private String externalURI
	
	public String getURI() {
		if(externalURI != null) return externalURI
		throw new RuntimeException("getURI should be implemented by property trait");
	}
	
	
	public static StringProperty createInstance(String propertyURI, String value) {
		StringProperty sp = new StringProperty(value)
		sp.externalURI = propertyURI
		return sp
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
	public String toString() {
		return super.toString();
	}
    
    public String asString() {
        return super.toString()
    }
	
	/******* methods from java.lang.String *********/
	public String substring(int beginIndex) {
		return this.toString().substring(beginIndex)
	}
	
	public String substring(int beginIndex, int endIndex) {
		return this.toString().subSequence(beginIndex, endIndex)
	}
	
	public String[] split(String regex) {
		return this.toString().split(regex)
	}
	
	public String[] split(String regex, int limit) {
		return this.toString().split(regex, limit)
	}
	

    @Override
    public void setExternalPropertyURI(String externalPropertyURI) {
        this.externalURI = externalPropertyURI;
    }
}
