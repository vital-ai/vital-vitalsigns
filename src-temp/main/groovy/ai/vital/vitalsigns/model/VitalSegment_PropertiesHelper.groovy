package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalSegment_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public VitalSegment_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalSegment');
	}

	protected VitalSegment_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getReadOnly() {
		return _implementation("readOnly");
	}


	public VitalGraphQueryPropertyCriterion getSegmentID() {
		return _implementation("segmentID");
	}

}
