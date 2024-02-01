package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalAuthKey_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VitalAuthKey_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalAuthKey');
	}

	protected VitalAuthKey_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getKey() {
		return _implementation("key");
	}

}
