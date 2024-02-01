package ai.vital.vitalsigns.model.property;

public class FloatProperty extends NumberProperty implements IProperty {

	private static final long serialVersionUID = -2858984778559337885L;
	
	public FloatProperty() {
		super(0f)
	}
	
	public FloatProperty(Number _number) {
	    super(_number.floatValue())
	}
    
	public FloatProperty(Float _float) {
		super(_float)
	}

	
	public void setValue(Float f) {
		this._n = f
	}
	
	public String getURI() {
		if(externalURI != null) return externalURI
		throw new RuntimeException("getURI should be implemented by property trait");
	}
	
	public static FloatProperty createInstance(String propertyURI, Float val) {
		FloatProperty bp = new FloatProperty(val)
		bp.externalURI = propertyURI
		return bp
	}

    public Number asNumber() {
        return this.floatValue()
    }
}
