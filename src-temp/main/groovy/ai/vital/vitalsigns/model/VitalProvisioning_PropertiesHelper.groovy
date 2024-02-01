package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalProvisioning_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VitalProvisioning_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalProvisioning');
	}

	protected VitalProvisioning_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
