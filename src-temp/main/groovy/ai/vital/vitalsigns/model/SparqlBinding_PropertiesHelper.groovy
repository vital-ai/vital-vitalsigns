package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class SparqlBinding_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_GraphContainerObject_PropertiesHelper {

	public SparqlBinding_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#SparqlBinding');
	}

	protected SparqlBinding_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
