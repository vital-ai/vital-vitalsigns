package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceRootKey_PropertiesHelper extends ai.vital.vitalsigns.model.VitalAuthKey_PropertiesHelper {

	public VitalServiceRootKey_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceRootKey');
	}

	protected VitalServiceRootKey_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
