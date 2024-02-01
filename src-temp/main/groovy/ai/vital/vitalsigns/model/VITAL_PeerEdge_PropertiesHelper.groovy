package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_PeerEdge_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Edge_PropertiesHelper {

	public VITAL_PeerEdge_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_PeerEdge');
	}

	protected VITAL_PeerEdge_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}

}
