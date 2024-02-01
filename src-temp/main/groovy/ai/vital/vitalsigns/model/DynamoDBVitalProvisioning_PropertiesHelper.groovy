package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class DynamoDBVitalProvisioning_PropertiesHelper extends ai.vital.vitalsigns.model.VitalProvisioning_PropertiesHelper {

	public DynamoDBVitalProvisioning_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#DynamoDBVitalProvisioning');
	}

	protected DynamoDBVitalProvisioning_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getEdge_indexed() {
		return _implementation("edge_indexed");
	}


	public VitalGraphQueryPropertyCriterion getEdge_read() {
		return _implementation("edge_read");
	}


	public VitalGraphQueryPropertyCriterion getEdge_stored() {
		return _implementation("edge_stored");
	}


	public VitalGraphQueryPropertyCriterion getEdge_write() {
		return _implementation("edge_write");
	}


	public VitalGraphQueryPropertyCriterion getHyperEdge_indexed() {
		return _implementation("hyperEdge_indexed");
	}


	public VitalGraphQueryPropertyCriterion getHyperEdge_read() {
		return _implementation("hyperEdge_read");
	}


	public VitalGraphQueryPropertyCriterion getHyperEdge_stored() {
		return _implementation("hyperEdge_stored");
	}


	public VitalGraphQueryPropertyCriterion getHyperEdge_write() {
		return _implementation("hyperEdge_write");
	}


	public VitalGraphQueryPropertyCriterion getHyperNode_indexed() {
		return _implementation("hyperNode_indexed");
	}


	public VitalGraphQueryPropertyCriterion getHyperNode_read() {
		return _implementation("hyperNode_read");
	}


	public VitalGraphQueryPropertyCriterion getHyperNode_stored() {
		return _implementation("hyperNode_stored");
	}


	public VitalGraphQueryPropertyCriterion getHyperNode_write() {
		return _implementation("hyperNode_write");
	}


	public VitalGraphQueryPropertyCriterion getNode_indexed() {
		return _implementation("node_indexed");
	}


	public VitalGraphQueryPropertyCriterion getNode_read() {
		return _implementation("node_read");
	}


	public VitalGraphQueryPropertyCriterion getNode_stored() {
		return _implementation("node_stored");
	}


	public VitalGraphQueryPropertyCriterion getNode_write() {
		return _implementation("node_write");
	}


	public VitalGraphQueryPropertyCriterion getProperties_number_index_read() {
		return _implementation("properties_number_index_read");
	}


	public VitalGraphQueryPropertyCriterion getProperties_number_index_write() {
		return _implementation("properties_number_index_write");
	}


	public VitalGraphQueryPropertyCriterion getProperties_read() {
		return _implementation("properties_read");
	}


	public VitalGraphQueryPropertyCriterion getProperties_string_index_read() {
		return _implementation("properties_string_index_read");
	}


	public VitalGraphQueryPropertyCriterion getProperties_string_index_write() {
		return _implementation("properties_string_index_write");
	}


	public VitalGraphQueryPropertyCriterion getProperties_write() {
		return _implementation("properties_write");
	}

}
