package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_PathQuery_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Query_PropertiesHelper {

	public VITAL_PathQuery_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_PathQuery');
	}

	protected VITAL_PathQuery_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
