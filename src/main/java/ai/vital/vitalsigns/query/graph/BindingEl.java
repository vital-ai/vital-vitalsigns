package ai.vital.vitalsigns.query.graph;

import ai.vital.vitalsigns.model.GraphObject;

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;


//represents either a single node binding, or connector/endpoint binding
public class BindingEl {

	private GraphObject endpoint;
	
	private GraphObject connector;

	private Arc arc;

	public BindingEl(Arc arc, GraphObject endpoint, GraphObject connector) {
		super();
		this.arc = arc;
		setEndpoint(endpoint);
		setConnector(connector);
	}

	public GraphObject getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(GraphObject endpoint) {
		this.endpoint = endpoint;
	}

	public GraphObject getConnector() {
		return connector;
	}

	public void setConnector(GraphObject connector) {
		if(connector == null) return;
		if(connector instanceof VITAL_Edge || connector instanceof VITAL_HyperEdge) {
			this.connector = connector; 
		} else {
			throw new RuntimeException("connector may only be an instanceof edge or hyper edge, got: " + connector);
		}
	}

	public Arc getArc() {
		return arc;
	}

	public void setArc(Arc arc) {
		this.arc = arc;
	}

}
