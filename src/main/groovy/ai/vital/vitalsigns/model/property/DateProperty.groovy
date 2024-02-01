package ai.vital.vitalsigns.model.property


import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

public class DateProperty implements IProperty {

	private Date inner;
	
	static {
//		GNumbersEqualsFix.fixGNumbersEquals();
	}
	
	private static final long serialVersionUID = -2302492432388821418L;

	public DateProperty() {
		super()
		inner = null
	}

	public DateProperty(long arg0) {
		super
		inner = new Date(arg0)
	}
	
	public DateProperty(Date arg0) {
		super
		inner = arg0
	}

	
	
	public void setValue(Long millis) {
		this.inner = new Date(millis)
	}
	
	public void setValue(Date date) {
		this.inner = date
	}

	@Override
	public Object rawValue() {
		return inner;
	}
	
	@Override
	public IProperty unwrapped() {
		return this;
	}

	public Long getTime() {
		return inner.getTime();
	}

	public Date getDate() {
		return inner;
	}
	
	
	@Override
	public String toString() {
		return inner;
	}
	
	
	private String externalURI
	
	public String getURI() {
		if(externalURI != null) return externalURI
		throw new RuntimeException("getURI should be implemented by property trait");
	}
	
	public static DateProperty createInstance(String propertyURI, Date val) {
		DateProperty bp = new DateProperty(val)
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
	
    public Date asDate() {
        return inner
    }
    
    public Date plus(int days) {
        return inner + days
    }
	
    public Date minus(int days) {
        return inner - days
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
