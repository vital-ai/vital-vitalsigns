package ai.vital.vitalsigns.classes;

import java.util.ArrayList
import java.util.List
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.model.GraphObject

public class ClassMetadata {

    private String ontologyURI
    
	private String URI
	
	private String canonicalName = null
	
	private String shortName = null
	
	private Class<? extends GraphObject> clazz

	private List<ClassMetadata> children = new ArrayList<ClassMetadata>()
	
	private ClassMetadata parentClass = null

	private boolean isEdge = false
	
	private boolean isHyperEdge = false
	
	private List<ClassMetadata> edgeSourceDomains

	private List<ClassMetadata> edgeDestinationDomains
	
	private List<ClassMetadata> hyperEdgeSourceDomains
	
	private List<ClassMetadata> hyperEdgeDestinationDomains

	// both edge and hyperedge
	private String edgeSingleName

	// both edge and hyperedge
	private String edgePluralName
	
	public ClassMetadata(String URI, String ontologyURI, String shortName, String canonicalName, ClassMetadata parentClass, boolean isEdge, boolean isHyperEdge) {
		super()
		if(isEdge && isHyperEdge) throw new RuntimeException("Class cannot be both edge and hyperedge");
		this.URI = URI
		this.ontologyURI = ontologyURI
		// this.clazz = clazz
		this.shortName = shortName
		this.canonicalName = canonicalName
		this.parentClass = parentClass
		this.isEdge = isEdge
		this.isHyperEdge = isHyperEdge
	}

	// lazy load the class for performance
	public Class<? extends GraphObject> getClazz() {
	    
	    if(clazz == null) {
	        try {
                clazz = VitalSigns.get().getGraphObjectClass(ontologyURI, canonicalName)

            } catch (Exception e) {
                throw new RuntimeException(e)
            }
	    }
	    
	    if(clazz == null) throw new RuntimeException("Class not found: " + canonicalName)
	    
		return clazz
	}

	public List<ClassMetadata> getChildren() {
		return children
	}

	public void setChildren(List<ClassMetadata> children) {
		this.children = children
	}

	public String getURI() {
		return URI
	}

	public ClassMetadata getParentClass() {
		return parentClass
	}

	public void setEdgeSourceDomains(List<ClassMetadata> edgeSourceDomains) {
		this.edgeSourceDomains = edgeSourceDomains
	}

	public List<ClassMetadata> getEdgeSourceDomains() {
		return edgeSourceDomains
	}

	public void setEdgeDestinationDomains(List<ClassMetadata> destDomains) {
		this.edgeDestinationDomains = destDomains
	}

	public List<ClassMetadata> getEdgeDestinationDomains() {
		return edgeDestinationDomains
	}

	public void setEdgeSingleName(String string) {
		this.edgeSingleName = string
	}

	public String getEdgeSingleName() {
		return edgeSingleName
	}

	public void setEdgePluralName(String string) {
		this.edgePluralName = string
	}

	public String getEdgePluralName() {
		return edgePluralName
	}

	public List<ClassMetadata> getHyperEdgeSourceDomains() {
		return hyperEdgeSourceDomains
	}

	public void setHyperEdgeSourceDomains(List<ClassMetadata> hyperEdgeSourceDomains) {
		this.hyperEdgeSourceDomains = hyperEdgeSourceDomains
	}

	
	public List<ClassMetadata> getHyperEdgeDestinationDomains() {
		return hyperEdgeDestinationDomains
	}

	public void setHyperEdgeDestinationDomains(
			List<ClassMetadata> hyperEdgeDestinationDomains) {
		this.hyperEdgeDestinationDomains = hyperEdgeDestinationDomains
	}

    public String getCanonicalName() {
        return canonicalName
    }

    public String getSimpleName() {
        return shortName
    }

    public String getOntologyURI() {
        return ontologyURI
    }

    public boolean isEdge() {
        return isEdge
    }

    public boolean isHyperEdge() {
        return isHyperEdge
    }

}
