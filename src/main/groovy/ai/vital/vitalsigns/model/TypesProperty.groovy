package ai.vital.vitalsigns.model

import ai.vital.vitalsigns.ontology.VitalCoreOntology
import ai.vital.vitalsigns.properties.PropertyTrait

trait TypesProperty implements PropertyTrait {

	String getURI() {
		return VitalCoreOntology.types.getLocalName()
	}
	
}
