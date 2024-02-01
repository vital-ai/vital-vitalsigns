package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class SqlDatabaseConnection_PropertiesHelper extends ai.vital.vitalsigns.model.DatabaseConnection_PropertiesHelper {

	public SqlDatabaseConnection_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#SqlDatabaseConnection');
	}

	protected SqlDatabaseConnection_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getDatabase() {
		return _implementation("database");
	}

}
