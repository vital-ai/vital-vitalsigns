package ai.vital.vitalsigns.model.property


import org.codehaus.groovy.runtime.DefaultGroovyMethods


public trait IProperty implements Serializable , Comparable, Cloneable {

	public String getURI() {
		throw new RuntimeException("getURI should be implemented by property trait");
	}

	/**
	 * Returns raw value ( base java object )
	 * @return
	 */
	public abstract Object rawValue();


	/**
	 * Returns the unwrapped instance of the property (no traits nor proxy class)
	 * @return
	 */
	public abstract IProperty unwrapped();
    
    /**
     * This method may only be used by base properties
     */
    public abstract void setExternalPropertyURI(String externalPropertyURI);

	/*	
	public Object clone() throws CloneNotSupportedException {
		println "CLONE!"
		return super.clone()
	}
	*/

	public int compareTo(Object n) {
//		println "IProperty ${this} compareto to ${n}"
		//		if(n instanceof GeneratedGroovyProxy) {
		//			n = ((GeneratedGroovyProxy)n).getProxyTarget();
		//		}
		if(n instanceof IProperty) {
			n = n.rawValue()
		}
		return this.rawValue().compareTo(n)
		//		return super.compareTo(n)
	}

	
	/*
	
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
    
	*/
	
	
	
	
	public boolean equalTo(Object n) {

		//		if(n instanceof GeneratedGroovyProxy) {
		//			n = ((GeneratedGroovyProxy)n).getProxyTarget();
		//		}

		if(n instanceof IProperty) {
			n = n.rawValue()
		}

		return this.rawValue().equals(n)

	}
	
	public boolean notEqualTo(Object n) {
		return ! equalTo(n);
	}

	public boolean greaterThan(Object n) {
		return this.compareTo(n) > 0
	}

	public boolean greaterThanEqualTo(Object n) {
		return this.compareTo(n) >= 0
	}

	public boolean lessThan(Object n) {
		return this.compareTo(n) < 0
	}

	public boolean lessThanEqualTo(Object n) {
		return this.compareTo(n) <= 0
	}
	
    /*
	public boolean equals(Object obj) {

//		println "IProperty ${this} equals ${obj}"

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
	*/

    def asType(Class clazz) {
        return DefaultGroovyMethods.asType(this, clazz)
    }
    
}
