package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_GraphQuery_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Query_PropertiesHelper {

	public VITAL_GraphQuery_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_GraphQuery');
	}

	protected VITAL_GraphQuery_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
