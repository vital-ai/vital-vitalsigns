package ai.vital.vitalsigns.meta

import java.io.Serializable
import ai.vital.vitalsigns.model.GraphObject

public class PathElement implements Serializable {

	private static final long serialVersionUID = -2283676484207247474L

	private String edgeTypeURI
	
	private String destNodeTypeURI

	private Class<? extends GraphObject> edgeClass
	
	// if reversed, use dest node type as source
	private boolean reversed = false
	
	private boolean hyperedge = false

	public String getEdgeTypeURI() {
		return edgeTypeURI
	}

	public void setEdgeTypeURI(String edgeTypeURI) {
		this.edgeTypeURI = edgeTypeURI
	}

	public String getDestNodeTypeURI() {
		return destNodeTypeURI
	}

	public void setDestNodeTypeURI(String destNodeTypeURI) {
		this.destNodeTypeURI = destNodeTypeURI
	}

	public Class<? extends GraphObject> getEdgeClass() {
		return edgeClass
	}

	public void setEdgeClass(Class<? extends GraphObject> edgeClass) {
		this.edgeClass = edgeClass
	}

	public boolean isReversed() {
		return reversed
	}

	public void setReversed(boolean reversed) {
		this.reversed = reversed
	}

	public boolean isHyperedge() {
		return hyperedge
	}

	public void setHyperedge(boolean hyperedge) {
		this.hyperedge = hyperedge
	}

}
