package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_GraphContainerObject_PropertiesHelper extends ai.vital.vitalsigns.model.ClassPropertiesHelper {

	public VITAL_GraphContainerObject_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_GraphContainerObject');
	}

	protected VITAL_GraphContainerObject_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getURIProp() {
		return _implementation("URIProp");
	}


	public VitalGraphQueryPropertyCriterion getTypes() {
		return _implementation("types");
	}


	public VitalGraphQueryPropertyCriterion getVitaltype() {
		return _implementation("vitaltype");
	}

}
