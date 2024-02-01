package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_PayloadNode_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VITAL_PayloadNode_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_PayloadNode');
	}

	protected VITAL_PayloadNode_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getSerializedJSON() {
		return _implementation("serializedJSON");
	}


	public VitalGraphQueryPropertyCriterion getSerializedRDF() {
		return _implementation("serializedRDF");
	}

}
