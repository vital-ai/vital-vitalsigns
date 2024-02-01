package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalOrganization_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VitalOrganization_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalOrganization');
	}

	protected VitalOrganization_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getOrganizationID() {
		return _implementation("organizationID");
	}

}
