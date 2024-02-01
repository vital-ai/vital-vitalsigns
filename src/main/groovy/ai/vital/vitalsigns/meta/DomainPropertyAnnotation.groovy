package ai.vital.vitalsigns.meta

import ai.vital.vitalsigns.properties.PropertyMetadata;

class DomainPropertyAnnotation extends DomainAnnotation {

	public PropertyMetadata propertyInterface

	@Override
	public String toString() {
		return "${value}"
	}
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof DomainPropertyAnnotation)) { 
            return false;
        }
        
        DomainPropertyAnnotation o2 = (DomainPropertyAnnotation)obj;
        
        boolean v = o2.propertyInterface == this.propertyInterface && o2.URI == this.URI && o2.value == this.value && o2.lang == this.lang
            
        return v
        
    }
}
