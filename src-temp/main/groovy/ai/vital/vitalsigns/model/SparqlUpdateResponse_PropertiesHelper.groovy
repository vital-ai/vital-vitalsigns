package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class SparqlUpdateResponse_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public SparqlUpdateResponse_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#SparqlUpdateResponse');
	}

	protected SparqlUpdateResponse_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getUpdatedTriplesCount() {
		return _implementation("updatedTriplesCount");
	}

}
