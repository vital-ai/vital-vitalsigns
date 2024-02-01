package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServicePrimeConfig_PropertiesHelper extends ai.vital.vitalsigns.model.VitalServiceConfig_PropertiesHelper {

	public VitalServicePrimeConfig_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServicePrimeConfig');
	}

	protected VitalServicePrimeConfig_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getEndpointURL() {
		return _implementation("endpointURL");
	}

}
