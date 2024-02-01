package ai.vital.vitalsigns.meta

import ai.vital.vitalsigns.model.GraphObject

class DomainClassAnnotation extends DomainAnnotation {

	public Class<? extends GraphObject> clazz

	@Override
	public String toString() {
		return "DomainClassAnnotation-class:${clazz}-URI:${this.URI}-value:${value}"
	}
	
}
