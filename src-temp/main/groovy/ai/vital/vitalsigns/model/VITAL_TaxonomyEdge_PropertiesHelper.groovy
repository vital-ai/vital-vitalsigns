package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_TaxonomyEdge_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Edge_PropertiesHelper {

	public VITAL_TaxonomyEdge_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_TaxonomyEdge');
	}

	protected VITAL_TaxonomyEdge_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
