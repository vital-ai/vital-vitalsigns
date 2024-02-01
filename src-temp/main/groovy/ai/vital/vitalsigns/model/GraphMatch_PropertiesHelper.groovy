package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class GraphMatch_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_GraphContainerObject_PropertiesHelper {

	public GraphMatch_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#GraphMatch');
	}

	protected GraphMatch_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
