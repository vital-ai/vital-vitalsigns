package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceAdminKey_PropertiesHelper extends ai.vital.vitalsigns.model.VitalAuthKey_PropertiesHelper {

	public VitalServiceAdminKey_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceAdminKey');
	}

	protected VitalServiceAdminKey_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
