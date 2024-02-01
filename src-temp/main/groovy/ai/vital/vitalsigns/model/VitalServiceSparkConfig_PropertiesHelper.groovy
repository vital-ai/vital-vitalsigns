package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceSparkConfig_PropertiesHelper extends ai.vital.vitalsigns.model.VitalServiceConfig_PropertiesHelper {

	public VitalServiceSparkConfig_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceSparkConfig');
	}

	protected VitalServiceSparkConfig_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getEndpointURL() {
		return _implementation("endpointURL");
	}

}
