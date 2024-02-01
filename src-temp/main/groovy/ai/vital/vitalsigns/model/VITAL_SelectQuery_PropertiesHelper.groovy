package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_SelectQuery_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Query_PropertiesHelper {

	public VITAL_SelectQuery_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_SelectQuery');
	}

	protected VITAL_SelectQuery_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
