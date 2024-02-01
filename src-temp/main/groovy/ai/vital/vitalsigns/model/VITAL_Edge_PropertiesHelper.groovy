package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VITAL_Edge_PropertiesHelper extends ai.vital.vitalsigns.model.ClassPropertiesHelper {

	public VITAL_Edge_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VITAL_Edge');
	}

	protected VITAL_Edge_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getURIProp() {
		return _implementation("URIProp");
	}


	public VitalGraphQueryPropertyCriterion getActive() {
		return _implementation("active");
	}


	public VitalGraphQueryPropertyCriterion getEdgeDestination() {
		return _implementation("edgeDestination");
	}


	public VitalGraphQueryPropertyCriterion getEdgeSource() {
		return _implementation("edgeSource");
	}


	public VitalGraphQueryPropertyCriterion getListIndex() {
		return _implementation("listIndex");
	}


	public VitalGraphQueryPropertyCriterion getOntologyIRI() {
		return _implementation("ontologyIRI");
	}


	public VitalGraphQueryPropertyCriterion getProvenance() {
		return _implementation("provenance");
	}


	public VitalGraphQueryPropertyCriterion getTimestamp() {
		return _implementation("timestamp");
	}


	public VitalGraphQueryPropertyCriterion getTypes() {
		return _implementation("types");
	}


	public VitalGraphQueryPropertyCriterion getUpdateTime() {
		return _implementation("updateTime");
	}


	public VitalGraphQueryPropertyCriterion getVersionIRI() {
		return _implementation("versionIRI");
	}


	public VitalGraphQueryPropertyCriterion getVitaltype() {
		return _implementation("vitaltype");
	}

}
