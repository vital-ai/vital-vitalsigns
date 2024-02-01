package ai.vital.vitalsigns.meta;

/**
 * An annotation from ontology. The value may be either literal or URIPropertyValue
 *
 */

class DomainAnnotation implements Serializable {

	public String URI
	
	public Object value
    
    public String lang
	
	@Override
	public String toString() {
		return "${value}"
	}
}
