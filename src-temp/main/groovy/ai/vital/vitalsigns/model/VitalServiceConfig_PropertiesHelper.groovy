package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceConfig_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VitalServiceConfig_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceConfig');
	}

	protected VitalServiceConfig_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getAppID() {
		return _implementation("appID");
	}


	public VitalGraphQueryPropertyCriterion getConfigString() {
		return _implementation("configString");
	}


	public VitalGraphQueryPropertyCriterion getConnectionError() {
		return _implementation("connectionError");
	}


	public VitalGraphQueryPropertyCriterion getConnectionState() {
		return _implementation("connectionState");
	}


	public VitalGraphQueryPropertyCriterion getDefaultSegmentName() {
		return _implementation("defaultSegmentName");
	}


	public VitalGraphQueryPropertyCriterion getKey() {
		return _implementation("key");
	}


	public VitalGraphQueryPropertyCriterion getOrganizationID() {
		return _implementation("organizationID");
	}


	public VitalGraphQueryPropertyCriterion getPrimary() {
		return _implementation("primary");
	}


	public VitalGraphQueryPropertyCriterion getTargetAppID() {
		return _implementation("targetAppID");
	}


	public VitalGraphQueryPropertyCriterion getTargetOrganizationID() {
		return _implementation("targetOrganizationID");
	}


	public VitalGraphQueryPropertyCriterion getUriGenerationStrategy() {
		return _implementation("uriGenerationStrategy");
	}

}
