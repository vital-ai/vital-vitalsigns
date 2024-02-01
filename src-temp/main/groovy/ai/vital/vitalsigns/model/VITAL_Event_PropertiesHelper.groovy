package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_Event_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VITAL_Event_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_Event');
	}

	protected VITAL_Event_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
