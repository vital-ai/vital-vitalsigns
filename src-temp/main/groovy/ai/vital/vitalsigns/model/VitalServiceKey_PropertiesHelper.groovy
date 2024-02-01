package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceKey_PropertiesHelper extends ai.vital.vitalsigns.model.VitalAuthKey_PropertiesHelper {

	public VitalServiceKey_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceKey');
	}

	protected VitalServiceKey_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
