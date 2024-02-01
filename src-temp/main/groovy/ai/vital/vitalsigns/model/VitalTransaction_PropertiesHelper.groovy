package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalTransaction_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VitalTransaction_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalTransaction');
	}

	protected VitalTransaction_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getTransactionID() {
		return _implementation("transactionID");
	}


	public VitalGraphQueryPropertyCriterion getTransactionState() {
		return _implementation("transactionState");
	}

}
