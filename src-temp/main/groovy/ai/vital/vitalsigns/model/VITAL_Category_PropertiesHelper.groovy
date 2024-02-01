package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_Category_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VITAL_Category_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_Category');
	}

	protected VITAL_Category_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
