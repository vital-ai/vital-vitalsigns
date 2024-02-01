package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class RDFStatement_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public RDFStatement_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#RDFStatement');
	}

	protected RDFStatement_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getRdfContext() {
		return _implementation("rdfContext");
	}


	public VitalGraphQueryPropertyCriterion getRdfObject() {
		return _implementation("rdfObject");
	}


	public VitalGraphQueryPropertyCriterion getRdfPredicate() {
		return _implementation("rdfPredicate");
	}


	public VitalGraphQueryPropertyCriterion getRdfSubject() {
		return _implementation("rdfSubject");
	}

}
