package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class Edge_SameAs_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_PeerEdge_PropertiesHelper {

	public Edge_SameAs_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#Edge_SameAs');
	}

	protected Edge_SameAs_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
