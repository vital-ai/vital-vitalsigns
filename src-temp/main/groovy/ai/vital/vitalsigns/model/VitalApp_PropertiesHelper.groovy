package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalApp_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VitalApp_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalApp');
	}

	protected VitalApp_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getAppID() {
		return _implementation("appID");
	}

}
