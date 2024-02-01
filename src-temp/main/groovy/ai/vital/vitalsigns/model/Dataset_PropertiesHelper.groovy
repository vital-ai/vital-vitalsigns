package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class Dataset_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public Dataset_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#Dataset');
	}

	protected Dataset_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getDateRetrieved() {
		return _implementation("dateRetrieved");
	}


	public VitalGraphQueryPropertyCriterion getSourceName() {
		return _implementation("sourceName");
	}


	public VitalGraphQueryPropertyCriterion getSourceUrl() {
		return _implementation("sourceUrl");
	}

}
