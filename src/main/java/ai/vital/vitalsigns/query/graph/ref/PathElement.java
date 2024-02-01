package ai.vital.vitalsigns.query.graph.ref;

import ai.vital.vitalservice.query.Connector;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.GraphObject;

class PathElement {

	public Arc arc;
	
	public ResultList connectorResults;
	
	public ResultList endpointResults;

	public PathElement(Arc arc) {
		super();
		this.arc = arc;
	}
	
	public boolean isHyperArc() {
		return Connector.HYPEREDGE == arc.arcContainer.getArc().connector;
	}

	public boolean isOptional() {
		return arc.isOptional();
	}

	public boolean isForwardNotReverse() {
		return arc.isForwardNotReverse();
	}

	public GraphObject getEndpoint(String otherEndpoint) {

		for(GraphObject g : endpointResults) {
			if(g.getURI().equals(otherEndpoint)) return g;
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		if( arc != null ) {
			return ( arc.getLabel() != null && arc.getLabel().length() > 0) ? ("PE:" + arc.getLabel()) : ("PE:(no_label)");
		} else {
			return "PathElement: no arc set!";
		}
	}
	
}
