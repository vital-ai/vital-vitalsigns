package ai.vital.vitalsigns.model.property;

public class DoubleProperty extends NumberProperty implements IProperty {

	private static final long serialVersionUID = 8377868782264587003L;
	
	public DoubleProperty() {
		super(0d);
	}
	
	public DoubleProperty(double _double) {
		super(_double);
	}
    
	public DoubleProperty(Number _number) {
	    super(_number.doubleValue());
	}
	
	public void setValue(Double d) {
		this._n = d
	}
	
	public String getURI() {
		if(externalURI != null) return externalURI
		throw new RuntimeException("getURI should be implemented by property trait");
	}
	
	public static DoubleProperty createInstance(String propertyURI, Double val) {
		DoubleProperty bp = new DoubleProperty()
		bp.externalURI = propertyURI
		return bp
	}
    
    public Number asNumber() {
        return this.doubleValue()
    }
}
