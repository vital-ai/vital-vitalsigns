package ai.vital.vitalsigns.model.property;

public class IntegerProperty extends NumberProperty implements IProperty {

	private static final long serialVersionUID = -3942039184369128233L;
	
	public IntegerProperty() {
		super(null);
	}

	public IntegerProperty(int arg0) {
		super(arg0)
	}
    
	public IntegerProperty(Number _number) {
	    super(_number.intValue())
	}
	
	public void setValue(Integer i) {
		this._n = i
	}
	
	/*
	public int compareTo(Object n) {
		println "INT ${this} compareto to ${n}"
		return super.compareTo(n)
	}

	
	public int compareTo(Number n) {
		println "INT ${this} compareto N to ${n}"
		return super.compareTo(n)
	}
	*/
	
	public String getURI() {
		if(externalURI != null) return externalURI
		throw new RuntimeException("getURI should be implemented by property trait");
	}
	
	public static IntegerProperty createInstance(String propertyURI, Integer val) {
		IntegerProperty bp = new IntegerProperty(val)
		bp.externalURI = propertyURI
		return bp
	}
    
    public Number asNumber() {
        return this.intValue()
    }
    
}
