package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class SparqlAskResponse_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public SparqlAskResponse_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#SparqlAskResponse');
	}

	protected SparqlAskResponse_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getPositiveResponse() {
		return _implementation("positiveResponse");
	}

}
