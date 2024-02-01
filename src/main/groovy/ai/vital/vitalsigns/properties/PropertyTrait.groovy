package ai.vital.vitalsigns.properties

import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation

import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.vitalsigns.model.property.NumberProperty;
import ai.vital.vitalsigns.model.property.IProperty
import groovy.lang.GeneratedGroovyProxy
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.properties.PropertiesRegistry

// known groovy deadlock, we cannot use comparable not to break == operator
trait PropertyTrait {

	abstract String getURI();
	
	public int compareTo(Object n) {
//		println "PropertyTrait ${this} compareto to ${n}"
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
	public Object clone() throws CloneNotSupportedException {
		println "PROPERTY TRAIN CLONE!"
//		Object cloned = this.clone()
		return super.clone()
	}
	*/

	public String toString() {
		
		if(this instanceof GeneratedGroovyProxy) {
			return ((GeneratedGroovyProxy)this).getProxyTarget().toString();
		}
		
		return super.toString()
		
	}
	
	/*
	public int compareTo(Object n) {
//		println "${this} compareto to ${n}"
//		if(n instanceof GeneratedGroovyProxy) {
//			n = ((GeneratedGroovyProxy)n).getProxyTarget();
//		}
		if(n instanceof IProperty) {
			n = n.rawValue()
		}
		return this.rawValue().compareTo(n)
//		return super.compareTo(n)
	}
	*/
	
	/*
	public boolean equalTo(Object n) {

//		if(n instanceof GeneratedGroovyProxy) {
//			n = ((GeneratedGroovyProxy)n).getProxyTarget();
//		}
		
		if(n instanceof IProperty) {
			n = n.rawValue()
		}
		
		return this.rawValue().equals(n)
		
	}
	
	public boolean greaterThan(Object n) {
		return this.compareTo(n) > 0
	}
	
	public boolean greaterEqualThan(Object n) {
		return this.compareTo(n) >= 0
	}
	
	public boolean lessThan(Object n) {
		return this.compareTo(n) < 0
	}
	
	public boolean lessEqualThan(Object n) {
		return this.compareTo(n) <= 0
	}
	
	*/
	
	
	
//	public int compareTo(Number n) {
//		println "${this} compareto N to ${n}"
//		return super.compareTo(n)
//	}

	
	/*
	public boolean equals(Object obj) {
		
//		println "PropertyTrait ${this} equals ${obj}"
		
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

    public Boolean equalsSemantically(Object other) {
        
        String thisPropertyURI = null
        
        try { thisPropertyURI = getURI() } catch(Exception e){}
        
        //semantic comparison only works for property+trait class
        if( thisPropertyURI != null && other instanceof PropertyTrait ) {
            
            //static methods but require inference
//            boolean subProperty1 = this.getClass().isSubPropertyOf(other.getClass())
//            boolean subProperty2 = other.getClass().isSubPropertyOf(this.getClass())

            String otherPropertyURI = ((PropertyTrait)other).getURI()
            
            
            if(thisPropertyURI.equals(otherPropertyURI)) {
                
                //fallback to default comparison
                return null
                
            } else {
            
                PropertiesRegistry pr = VitalSigns.get().getPropertiesRegistry()
                PropertyMetadata pm1 = pr.getProperty(thisPropertyURI)
                if(pm1 == null) throw new RuntimeException("Property with URI not found: " + thisPropertyURI + " in VitalSigns")
                PropertyMetadata pm2 = pr.getProperty(otherPropertyURI)
                if(pm2 == null) throw new RuntimeException("Property with URI not found: " + thisPropertyURI + " in VitalSigns")
            
                List<PropertyMetadata> h1 = pr.getParentProperties(pm1, true)
                
                List<PropertyMetadata> h2 = pr.getParentProperties(pm2, true)
                
                if(h1.contains(pm2) || h2.contains(pm1)) {
                    
                    //fallback to default comparison
                    return null
                    
                }

                //properties are incompatible
                return false
                
            }
            
        } else {
        
            //fallback to default comparison
            return null
            
        }
        
    };

    /*
    public Object xor(Object other) {
        
        if(other instanceof PropertyTrait) {
            
            return ((IProperty)this).rawValue() == ((IProperty)other).rawValue()
            
        }// else if(other instanceof NumberProperty) {
        
        
        throw new RuntimeException("XOR only allowed")
        
        
    }
    */
    
}
