package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class AggregationResult_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public AggregationResult_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#AggregationResult');
	}

	protected AggregationResult_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getAggregationType() {
		return _implementation("aggregationType");
	}


	public VitalGraphQueryPropertyCriterion getValue() {
		return _implementation("value");
	}

}
