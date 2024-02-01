package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalSession_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VitalSession_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalSession');
	}

	protected VitalSession_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getKey() {
		return _implementation("key");
	}


	public VitalGraphQueryPropertyCriterion getSessionID() {
		return _implementation("sessionID");
	}


	public VitalGraphQueryPropertyCriterion getSessionType() {
		return _implementation("sessionType");
	}

}
