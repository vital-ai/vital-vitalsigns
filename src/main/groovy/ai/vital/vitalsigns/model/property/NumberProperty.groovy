package ai.vital.vitalsigns.model.property


import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

abstract class NumberProperty extends Number implements IProperty {

	private static final long serialVersionUID = 1L;
	
	static {
//		GNumbersEqualsFix.fixGNumbersEquals();
	}
	
	protected Number _n;
	
    protected String externalURI
    
	public NumberProperty(Number n) {
		this._n = n;
	}

	@Override
	public double doubleValue() {
		return _n.doubleValue();
	}

	@Override
	public float floatValue() {
		return _n.floatValue();
	}

	@Override
	public int intValue() {
		return _n.intValue();
	}

	@Override
	public long longValue() {
		return _n.longValue();
	}

	@Override
	public String toString() {
		return _n.toString();
	}

	@Override
	public Object rawValue() {
		return _n;
	}

	@Override
	public IProperty unwrapped() {
		return this;
	}
	
	
	public Number plus(IProperty other) {
//		println "PLUS!"
		return this.rawValue() + ((IProperty)other).rawValue()
	}
	
	public Number plus(Number other) {
//		println "plus number!"
		return this.rawValue() + other
	}
	
	
	public Number minus(IProperty other) {
		return this.rawValue() - ((IProperty)other).rawValue()
	}
	
	public Number minus(Number other) {
		return this.rawValue() - other
	}
	
	
	public Number multiply(IProperty other) {
		return this.rawValue() * ((IProperty)other).rawValue()
	}
	
	public Number multiply(Number other) {
		return this.rawValue() * other
	}
	
	public Number div(IProperty other) {
		return this.rawValue() / ((IProperty)other).rawValue()
	}
	
	public Number div(Number other) {
		return this.rawValue() / other
	}
//	
//	public int compareTo(IProperty other) {
//		println "Compareto to Prop!"
//		return _n.compareTo(other.rawValue())
//	}
//	public int compareTo(Number n) {
//		println "Compareto to N!"
//		return _n.compareTo(n)
//	}
//
//		
//	public int compareTo(Object n) {
//		println "Compareto to O!"
//		return _n.compareTo(n)
//	}

//    def asType(Class clazz) {
//        return DefaultGroovyMethods.asType(this, clazz)
//    }
    
	/**
	 * creates external property critertion
	 * @param propertyURI
	 * @return
	 */
	public static VitalGraphQueryPropertyCriterion create(String propertyURI) {
		VitalGraphQueryPropertyCriterion c = new VitalGraphQueryPropertyCriterion(propertyURI);
		c.externalProperty = true
		return c;
	}
    
    
    public boolean equals(Object obj) {
        
        //      println "IProperty ${this} equals ${obj}"
        
                boolean unwrapped = false
        
                def target = null;
                if(this instanceof GeneratedGroovyProxy) {
                    target = this.getProxyTarget();
                    unwrapped = true
                } else {
                    target = this
                }
        
        
                if(obj instanceof GeneratedGroovyProxy) {
                    obj = ((GeneratedGroovyProxy)obj).getProxyTarget();
                }
        
                if(unwrapped) {
                    return target.equals(obj)
                } else {
        
                    if(target instanceof IProperty && obj instanceof IProperty) {
                        return ((IProperty)target).rawValue().equals(((IProperty)obj).rawValue());
                    } else if(target instanceof IProperty){
                        return ((IProperty)target).rawValue().equals(obj);
                    } else {
                        return target.equals(obj)
                    }
                    //both values are equal
                }
        
        
            }
    
    abstract public Number asNumber()

    @Override
    public void setExternalPropertyURI(String externalPropertyURI) {
        this.externalURI = externalPropertyURI;
    }
}
