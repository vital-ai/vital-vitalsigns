package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceMockConfig_PropertiesHelper extends ai.vital.vitalsigns.model.VitalServiceConfig_PropertiesHelper {

	public VitalServiceMockConfig_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceMockConfig');
	}

	protected VitalServiceMockConfig_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
