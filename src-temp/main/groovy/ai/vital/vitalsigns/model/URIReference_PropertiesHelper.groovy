package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class URIReference_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public URIReference_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#URIReference');
	}

	protected URIReference_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getURIRef() {
		return _implementation("uRIRef");
	}

}
