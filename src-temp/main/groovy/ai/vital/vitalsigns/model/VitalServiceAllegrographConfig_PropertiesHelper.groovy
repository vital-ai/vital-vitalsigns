package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceAllegrographConfig_PropertiesHelper extends ai.vital.vitalsigns.model.VitalServiceConfig_PropertiesHelper {

	public VitalServiceAllegrographConfig_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceAllegrographConfig');
	}

	protected VitalServiceAllegrographConfig_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getCatalogName() {
		return _implementation("catalogName");
	}


	public VitalGraphQueryPropertyCriterion getPassword() {
		return _implementation("password");
	}


	public VitalGraphQueryPropertyCriterion getPoolMaxTotal() {
		return _implementation("poolMaxTotal");
	}


	public VitalGraphQueryPropertyCriterion getRepositoryName() {
		return _implementation("repositoryName");
	}


	public VitalGraphQueryPropertyCriterion getServerURL() {
		return _implementation("serverURL");
	}


	public VitalGraphQueryPropertyCriterion getUsername() {
		return _implementation("username");
	}

}
