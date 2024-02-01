package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceSaaSConfig_PropertiesHelper extends ai.vital.vitalsigns.model.VitalServiceConfig_PropertiesHelper {

	public VitalServiceSaaSConfig_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceSaaSConfig');
	}

	protected VitalServiceSaaSConfig_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
