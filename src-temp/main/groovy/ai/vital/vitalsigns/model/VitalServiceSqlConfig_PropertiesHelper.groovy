package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceSqlConfig_PropertiesHelper extends ai.vital.vitalsigns.model.VitalServiceConfig_PropertiesHelper {

	public VitalServiceSqlConfig_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceSqlConfig');
	}

	protected VitalServiceSqlConfig_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getDbType() {
		return _implementation("dbType");
	}


	public VitalGraphQueryPropertyCriterion getEndpointURL() {
		return _implementation("endpointURL");
	}


	public VitalGraphQueryPropertyCriterion getPassword() {
		return _implementation("password");
	}


	public VitalGraphQueryPropertyCriterion getPoolInitialSize() {
		return _implementation("poolInitialSize");
	}


	public VitalGraphQueryPropertyCriterion getPoolMaxTotal() {
		return _implementation("poolMaxTotal");
	}


	public VitalGraphQueryPropertyCriterion getTablesPrefix() {
		return _implementation("tablesPrefix");
	}


	public VitalGraphQueryPropertyCriterion getUsername() {
		return _implementation("username");
	}

}
