package ai.vital.vitalsigns.ontology

import java.io.InputStream;
import java.util.List;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.GraphObject;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Resource;

class VitalCoreOntology implements OntologyDescriptor {

	public final static String PACKAGE = "ai.vital.vitalsigns.model";
	
	public final static String getFileName() { return "vital-core-" + VitalSigns.VERSION + ".owl" };
	
	public final static String ONTOLOGY_IRI = "http://vital.ai/ontology/vital-core";
	
	public final static String NS = ONTOLOGY_IRI + '#';
	
	
	public final static Resource VITAL_Edge = ResourceFactory.createResource(NS + "VITAL_Edge");
	
	public final static Resource VITAL_HyperEdge = ResourceFactory.createResource(NS + "VITAL_HyperEdge");
	
	public final static Resource VITAL_HyperNode = ResourceFactory.createResource(NS + "VITAL_HyperNode");
	
	public final static Resource VITAL_Node = ResourceFactory.createResource(NS + "VITAL_Node");
	
	public final static Resource VITAL_Event = ResourceFactory.createResource(NS + "VITAL_Event");
	
	public final static Resource VITAL_PayloadNode = ResourceFactory.createResource(NS + "VITAL_PayloadNode");
	
	public final static Resource VITAL_URIReference = ResourceFactory.createResource(NS + "VITAL_URIReference");
	
	public final static Resource VITAL_Container = ResourceFactory.createResource(NS + "VITAL_Container");

	
	
	public final static Resource VITAL_PeerEdge = ResourceFactory.createResource(NS + "VITAL_PeerEdge");
	
	public final static Resource VITAL_TaxonomyEdge = ResourceFactory.createResource(NS + "VITAL_TaxonomyEdge");
		
	
	
	public final static Resource VITAL_GraphContainerObject = ResourceFactory.createResource(NS + "VITAL_GraphContainerObject");
	
	//container subclasses
	public final static Resource Message = ResourceFactory.createResource(NS + "Message");
	
	public final static Resource Payload = ResourceFactory.createResource(NS + "Payload");
	
	public final static Resource ProcessFlow = ResourceFactory.createResource(NS + "ProcessFlow");
	
	public final static Resource ProcessFlowStep = ResourceFactory.createResource(NS + "ProcessFlowStep");
	
	public final static Resource ResultSet = ResourceFactory.createResource(NS + "ResultSet");
	
	public final static Resource Result = ResourceFactory.createResource(NS + "Result");
	
	public final static Resource Workflow = ResourceFactory.createResource(NS + "Workflow");
	
	public final static Resource WorkflowStep = ResourceFactory.createResource(NS + "WorkflowStep");
	
	public final static Resource RestrictionAnnotationValue = ResourceFactory.createResource(NS + "RestrictionAnnotationValue");
	
	
	public final static Property hasTimestamp = ResourceFactory.createProperty(NS + "hasTimestamp");
	
	public final static Property hasNocacheFlag = ResourceFactory.createProperty(NS + "hasNocacheFlag");
	
	public final static Property hasReferringURI = ResourceFactory.createProperty(NS + "hasReferringURI");
	
	
	//ontology annotation
	public final static Property hasDefaultPackage = ResourceFactory.createProperty(NS + "hasDefaultPackage");
    
    	
	//ontology annotation
	public final static Property hasDefaultArtifactId = ResourceFactory.createProperty(NS + "hasDefaultArtifactId");
    	
	//ontology annotation
	public final static Property hasDefaultGroupId = ResourceFactory.createProperty(NS + "hasDefaultGroupId");	
	
    
    //ontology annotation
    public final static Property hasBackwardCompatibilityVersion = ResourceFactory.createProperty(NS + "hasBackwardCompatibilityVersion");
	
    public final static Property hasPreferredImportVersion = ResourceFactory.createProperty(NS + "hasPreferredImportVersion");
    
	
	//class annotation
	public final static Property hasEdgeSrcDomain = ResourceFactory.createProperty(NS + "hasEdgeSrcDomain");

	//class annotation
	public final static Property hasEdgeDestDomain = ResourceFactory.createProperty(NS + "hasEdgeDestDomain");
	
	//class annotation
	public final static Property hasSkipExpansionFlag = ResourceFactory.createProperty(NS + "hasSkipExpansionFlag");
	
	//class annotation
	public final static Property hasHyperEdgeSrcDomain = ResourceFactory.createProperty(NS + "hasHyperEdgeSrcDomain");
	
	//class annotation
	public final static Property hasHyperEdgeDestDomain = ResourceFactory.createProperty(NS + "hasHyperEdgeDestDomain");
	
	
	//property annotation
	public final static Property hasDontIndexFlag = ResourceFactory.createProperty(NS + "hasDontIndexFlag");
	
	public final static Property hasDontPersistFlag = ResourceFactory.createProperty(NS + "hasDontPersistFlag");
	
	
	//property annotation
	public final static Property isInternalStore = ResourceFactory.createProperty(NS + "isInternalStore");
	
	//property annotation
	public final static Property isDefaultLabel = ResourceFactory.createProperty(NS + "isDefaultLabel");
	
	//property annotation
	public final static Property hasMultipleValues = ResourceFactory.createProperty(NS + "hasMultipleValues");
	
	//property annotation
	public final static Property hasSingleValue = ResourceFactory.createProperty(NS + "hasSingleValue");
    
    
	//property annotation
	public final static Property isEntityId = ResourceFactory.createProperty(NS + "isEntityId");
	
	//hasContainsListFlag
	public final static Property hasContainsListFlag = ResourceFactory.createProperty(NS + "hasContainsListFlag");
	
	
	//numerical properties constraints / annotations	
	public final static Property hasMinValueInclusive = ResourceFactory.createProperty(NS + "hasMinValueInclusive");
	
	public final static Property hasMinValueExclusive = ResourceFactory.createProperty(NS + "hasMinValueExclusive");
	
	public final static Property hasMaxValueInclusive = ResourceFactory.createProperty(NS + "hasMaxValueInclusive");
	
	public final static Property hasMaxValueExclusive = ResourceFactory.createProperty(NS + "hasMaxValueExclusive");
	
	public final static Property hasRestrictionClasses = ResourceFactory.createProperty(NS + "hasRestrictionClasses");
	
	
	//numerical restriction 
	public final static Property hasRestrictionValue = ResourceFactory.createProperty(NS + "hasRestrictionValue");
	
    
    
	public final static Property isVitalInternal = ResourceFactory.createProperty(NS + "isVitalInternal");
    
    
	
	
	//don't index nor persist flag
	public final static Property hasTransientPropertyFlag = ResourceFactory.createProperty(NS + "hasTransientPropertyFlag");
	
	public final static Property isPrologTokenizable = ResourceFactory.createProperty(NS + "isPrologTokenizable");
	
	/**
	 * a property that indicates a property value shouldn't be analyzed for EQ and EQ_case_insensitive comparison
	 */
	public final static Property isAnalyzedNoEquals = ResourceFactory.createProperty(NS + "isAnalyzedNoEquals");
	
	
	
	
	//datatypes
	public final static Resource geoLocation = ResourceFactory.createResource(NS + "geoLocation");
    
	public final static Resource truth = ResourceFactory.createResource(NS + "truth");
	
	
	
	public final static Property hasEdgeDestination = ResourceFactory.createProperty(NS + "hasEdgeDestination");
	
	public final static Property hasEdgeSource = ResourceFactory.createProperty(NS + "hasEdgeSource");
	
	public final static Property hasHyperEdgeDestination = ResourceFactory.createProperty(NS + "hasHyperEdgeDestination");
	
	public final static Property hasHyperEdgeSource = ResourceFactory.createProperty(NS + "hasHyperEdgeSource");
	
	public final static Property hasListIndex = ResourceFactory.createProperty(NS + "hasListIndex");
	
	public final static Property hasResultIndex = ResourceFactory.createProperty(NS + "hasResultIndex");
	
	public final static Property hasResultScore = ResourceFactory.createProperty(NS + "hasResultScore");

	
		

	public final static Property hasSerializedPayload  = ResourceFactory.createProperty(NS + "hasSerializedPayload");
	
	
	public final static Property isPayloadCompressed = ResourceFactory.createProperty(NS + "isPayloadCompressed");
	
	public final static Property hasPayloadFormat = ResourceFactory.createProperty(NS + "hasPayloadFormat");
	
	public final static Property contains = ResourceFactory.createProperty(NS + "contains");
	
	
	public final static Property hasProcessflowTimestamp = ResourceFactory.createProperty(NS + "hasProcessflowTimestamp");
	
	public final static Property hasNlpProcessflowTimestamp = ResourceFactory.createProperty(NS + "hasNlpProcessflowTimestamp");
	
	public final static Property hasIntegratorProcessflowTimestamp = ResourceFactory.createProperty(NS + "hasIntegratorProcessflowTimestamp");
	
	public final static Property hasInferenceProcessflowTimestamp = ResourceFactory.createProperty(NS + "hasInferenceProcessflowTimestamp");
	
	public final static Property hasLoggerProcessflowTimestamp = ResourceFactory.createProperty(NS + "hasLoggerProcessflowTimestamp");
	
	public final static Property hasPredictProcessflowTimestamp = ResourceFactory.createProperty(NS + "hasPredictProcessflowTimestamp");
	
	
	public final static Property hasProcessflowStatus = ResourceFactory.createProperty(NS + "hasProcessflowStatus");
	
	public final static Property hasNlpProcessflowStatus = ResourceFactory.createProperty(NS + "hasNlpProcessflowStatus");
	
	public final static Property hasInferenceProcessflowStatus = ResourceFactory.createProperty(NS + "hasInferenceProcessflowStatus");
	
	public final static Property hasIntegratorProcessflowStatus = ResourceFactory.createProperty(NS + "hasIntegratorProcessflowStatus");
	
	public final static Property hasLoggerProcessflowStatus = ResourceFactory.createProperty(NS + "hasLoggerProcessflowStatus");
	
	public final static Property hasPredictProcessflowStatus = ResourceFactory.createProperty(NS + "hasPredictProcessflowStatus");
	
	public final static Property hasSerializedJSON = ResourceFactory.createProperty(NS + "hasSerializedJSON");
	
	public final static Property hasSerializedRDF = ResourceFactory.createProperty(NS + "hasSerializedRDF");

	
	public final static Property hasExternalTypes = ResourceFactory.createProperty(NS + "hasExternalTypes");	
	
	// reverse transient properties
	
	
	public final static Property hasOntologyIRI = ResourceFactory.createProperty(NS + "hasOntologyIRI");
	
	public final static Property hasVersionIRI = ResourceFactory.createProperty(NS + "hasVersionIRI");
	
    
    //URI property
    public final static Property hasProvenance = ResourceFactory.createProperty(NS + "hasProvenance");
    
	
	public final static Property vitaltype = ResourceFactory.createProperty(NS + "vitaltype");
	
	public final static Property types = ResourceFactory.createProperty(NS + "types");
	
	public final static Property URIProp = ResourceFactory.createProperty(NS + "URIProp");

	
	
	public final static Set<String> internalProperties = new HashSet<String>(Arrays.asList(
		hasRestrictionValue.getURI()
	));
	
	
	@Override
	public InputStream getOwlInputStream() {
		return VitalSigns.get().getCoreModelInputStream();
	}

	@Override
	public String getOntologyIRI() {
		return ONTOLOGY_IRI;
	}

	@Override
	public String getPackage() {
		return PACKAGE;
	}
}
