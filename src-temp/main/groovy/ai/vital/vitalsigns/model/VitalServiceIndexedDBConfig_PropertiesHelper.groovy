package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceIndexedDBConfig_PropertiesHelper extends ai.vital.vitalsigns.model.VitalServiceConfig_PropertiesHelper {

	public VitalServiceIndexedDBConfig_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceIndexedDBConfig');
	}

	protected VitalServiceIndexedDBConfig_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getGraphQueries() {
		return _implementation("graphQueries");
	}


	public VitalGraphQueryPropertyCriterion getSelectQueries() {
		return _implementation("selectQueries");
	}

}
