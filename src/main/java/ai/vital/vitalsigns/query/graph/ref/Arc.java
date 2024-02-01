package ai.vital.vitalsigns.query.graph.ref;

import java.util.ArrayList;
import java.util.List;

import ai.vital.vitalservice.query.Connector;
import ai.vital.vitalservice.query.Source;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalsigns.query.graph.ref.QueryAnalysis.WrappedContainer;
import ai.vital.vitalsigns.utils.StringUtils;

class Arc {

	boolean topArc = false;
	
	//the exact arc container
	VitalGraphArcContainer arcContainer;
	
	List<Arc> children = new ArrayList<Arc>();

	WrappedContainer endpointContainer;

	WrappedContainer connectorContainer;

	public String defaultSource;

	public String defaultConnector;

	public String defaultDestination;
	
	public String getSourceBind() {
		
		if(StringUtils.isEmpty(arcContainer.getSourceBind())) {
			return defaultSource;
		} else {
			return arcContainer.getSourceBind();
		}
	}
	
	public String getConnectorBind() {
		
		if(StringUtils.isEmpty(arcContainer.getConnectorBind())) {
			return defaultConnector;
		} else {
			return arcContainer.getConnectorBind();
		}
	}
	
	public String getDestinationBind() {
		
		if(StringUtils.isEmpty(arcContainer.getTargetBind())) {
			return defaultDestination;
		} else {
			return arcContainer.getTargetBind();
		}
	}
	
	
	public VitalGraphArcContainer getArcContainer() {
		return arcContainer;
	}

	public void setArcContainer(VitalGraphArcContainer arcContainer) {
		this.arcContainer = arcContainer;
	}

	public List<Arc> getChildren() {
		return children;
	}

	public void setChildren(List<Arc> children) {
		this.children = children;
	}

	public boolean isTopArc() {
		return topArc;
	}

	public void setTopArc(boolean topArc) {
		this.topArc = topArc;
	}


	public boolean isOptional() {
		return arcContainer.isOptional();
	}

	public boolean isForwardNotReverse() {
		return arcContainer.getArc().source != Source.CURRENT;
	}

	public String getLabel() {
		return arcContainer.getLabel();
	}

	public boolean isHyperArc() {
		return Connector.HYPEREDGE == arcContainer.getArc().connector;
	}
}
