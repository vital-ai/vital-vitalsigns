package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class SparqlDatabaseConnection_PropertiesHelper extends ai.vital.vitalsigns.model.DatabaseConnection_PropertiesHelper {

	public SparqlDatabaseConnection_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#SparqlDatabaseConnection');
	}

	protected SparqlDatabaseConnection_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getCatalogName() {
		return _implementation("catalogName");
	}


	public VitalGraphQueryPropertyCriterion getRepositoryName() {
		return _implementation("repositoryName");
	}

}
