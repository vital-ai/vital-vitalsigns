package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class SqlUpdateResponse_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public SqlUpdateResponse_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#SqlUpdateResponse');
	}

	protected SqlUpdateResponse_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getUpdatedRowsCount() {
		return _implementation("updatedRowsCount");
	}

}
