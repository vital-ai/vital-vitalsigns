package ai.vital.vitalsigns.model.property

public class LongProperty extends NumberProperty implements IProperty {

	private static final long serialVersionUID = 6826956818508688290L;
	
	public LongProperty() {
		super(null);
	}

	public LongProperty(long arg0) {
		super(arg0);
	}

	public LongProperty(Number _number) {
	    super(_number.longValue())
	}
    
	public void setValue(Long l) {
		this._n = l
	}
    
	
	public String getURI() {
		if(externalURI != null) return externalURI
		throw new RuntimeException("getURI should be implemented by property trait");
	}
	
	public static LongProperty createInstance(String propertyURI, Long val) {
		LongProperty bp = new LongProperty(val)
		bp.externalURI = propertyURI
		return bp
	}
    
    public Number asNumber() {
        return this.longValue()
    }
}
