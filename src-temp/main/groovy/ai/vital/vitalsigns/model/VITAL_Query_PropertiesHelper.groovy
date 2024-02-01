package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_Query_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VITAL_Query_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_Query');
	}

	protected VITAL_Query_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getQueryString() {
		return _implementation("queryString");
	}

}
