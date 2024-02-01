package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class DatabaseConnection_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public DatabaseConnection_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#DatabaseConnection');
	}

	protected DatabaseConnection_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getAppID() {
		return _implementation("appID");
	}


	public VitalGraphQueryPropertyCriterion getConfigString() {
		return _implementation("configString");
	}


	public VitalGraphQueryPropertyCriterion getEndpointType() {
		return _implementation("endpointType");
	}


	public VitalGraphQueryPropertyCriterion getEndpointURL() {
		return _implementation("endpointURL");
	}


	public VitalGraphQueryPropertyCriterion getOrganizationID() {
		return _implementation("organizationID");
	}


	public VitalGraphQueryPropertyCriterion getPassword() {
		return _implementation("password");
	}


	public VitalGraphQueryPropertyCriterion getReadOnly() {
		return _implementation("readOnly");
	}


	public VitalGraphQueryPropertyCriterion getUsername() {
		return _implementation("username");
	}

}
