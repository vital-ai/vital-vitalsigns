package ai.vital.vitalsigns.ontology


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.classes.ClassesRegistry;
import ai.vital.vitalsigns.meta.EdgeAccessAssigner;
import ai.vital.vitalsigns.meta.HyperEdgeAccessAssigner;
import ai.vital.vitalsigns.meta.PropertyDefinitionProcessor;
import ai.vital.vitalsigns.meta.TraitClassQueryAssigner;
import ai.vital.vitalsigns.model.DomainOntology;
import ai.vital.vitalsigns.model.GraphObject;

import ai.vital.vitalsigns.model.property.BooleanProperty;
import ai.vital.vitalsigns.model.property.DateProperty;
import ai.vital.vitalsigns.model.property.DoubleProperty;
import ai.vital.vitalsigns.model.property.FloatProperty;
import ai.vital.vitalsigns.model.property.GeoLocationProperty;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.IntegerProperty;
import ai.vital.vitalsigns.model.property.LongProperty
import ai.vital.vitalsigns.model.property.StringProperty;
import ai.vital.vitalsigns.model.property.TruthProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.properties.PropertiesRegistry;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.properties.PropertyTrait;
import ai.vital.vitalsigns.rdf.RDFUtils;
import ai.vital.vitalsigns.utils.StringUtils;

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;


public class OntologyProcessor {
    
    private final static Logger log = LoggerFactory.getLogger(OntologyProcessor.class);

	private ClassesRegistry classesRegistry;
	
	private PropertiesRegistry propertiesRegistry;

	
	private List<ClassMetadata> classesList = null;

	public static Set<String> coreEdgeTypes = Collections.synchronizedSet(new HashSet<String>(Arrays.asList(
		VitalCoreOntology.VITAL_Edge.getURI(),
		VitalCoreOntology.VITAL_PeerEdge.getURI(),
		VitalCoreOntology.VITAL_TaxonomyEdge.getURI()
	)));
	
	public static Set<String> coreHyperEdgeTypes = Collections.synchronizedSet(new HashSet<String>(Arrays.asList(
		VitalCoreOntology.VITAL_HyperEdge.getURI()
	)));
	
	
	public OntologyProcessor(ClassesRegistry classesRegistry,
			PropertiesRegistry propertiesRegistry) {
		super();
		this.classesRegistry = classesRegistry;
		this.propertiesRegistry = propertiesRegistry;
		
	}

	/**
	 * Processes the 
	 * @param bareModel
	 * @param ontologyURI optional
	 * @param _package optional
	 * @return
	 * @throws Exception
	 */
	public DomainOntology processOntology(Model bareModel, OntModel targetEmptyModel, String ontologyURI, String _package) throws Exception {

	    long t = time();

		DomainOntology m = processOntologyMetaData(bareModel, targetEmptyModel, ontologyURI);
		
		log.debug("processor - metadata time {}ms", time() - t);
		
		t = time();

		processGraphObjects(targetEmptyModel, m.getUri(), _package);
		log.debug("processor - graph objects time {}ms", time() - t);
		
		t = time();
		processProperties(targetEmptyModel, m.getUri(), _package);
		log.debug("processor - properties time {}ms", time() - t);
		
		t = time();
		processEdgeAccess(targetEmptyModel);
		log.debug("processor - edge access time {}ms", time() - t);
		
		t = time();
		processHyperEdgeAccess(targetEmptyModel);
		log.debug("processor - hyperedge access time {}ms", time() - t);
		
		return m;
	}
	
	static long time() { return System.currentTimeMillis(); }
	
	public static String[] getEdgeLocalNameAndPlural(OntClass edgeClass) {
		
		String localNamePlural = edgeClass.getLocalName();
		
		if(localNamePlural.startsWith("Edge_")) {
			
			localNamePlural = localNamePlural.substring(5);
		}
		
		if(localNamePlural.startsWith("has")) {
			localNamePlural = localNamePlural.substring(3);
//			localName = localName.substring(0, 1).toLowerCase() + localName.substring(1);
		}
		
		//upper case !
		
		localNamePlural = localNamePlural.substring(0, 1).toUpperCase() + localNamePlural.substring(1);
		
		String localNameSingle = localNamePlural;
		
		if( localNamePlural.endsWith("s") ) {
			localNamePlural += "es";
		} else if(localNamePlural.endsWith("y")) {
			localNamePlural = localNamePlural.substring(0, localNamePlural.length() - 1) + "ies";
		} else {
			localNamePlural += "s";
		}
		
		return new String[]{localNameSingle, localNamePlural};
	}
	
	public static String[] getHyperEdgeLocalNameAndPlural(OntClass hyperEdgeClass) {
		
		String localNamePlural = hyperEdgeClass.getLocalName();
		
		if(localNamePlural.startsWith("HyperEdge_")) {
			
			localNamePlural = localNamePlural.substring(10);
		}
		
		if(localNamePlural.startsWith("has")) {
			localNamePlural = localNamePlural.substring(3);
//			localName = localName.substring(0, 1).toLowerCase() + localName.substring(1);
		}
		
		//upper case !
		
		localNamePlural = localNamePlural.substring(0, 1).toUpperCase() + localNamePlural.substring(1);
		
		String localNameSingle = localNamePlural;
		
		if( localNamePlural.endsWith("s") ) {
			localNamePlural += "es";
		} else if(localNamePlural.endsWith("y")) {
			localNamePlural = localNamePlural.substring(0, localNamePlural.length() - 1) + "ies";
		} else {
			localNamePlural += "s";
		}
		
		return new String[]{localNameSingle, localNamePlural};
	}
		
	private void processEdgeAccess(OntModel targetEmptyModel) throws Exception {

		for(ClassMetadata cm : classesList) {
			
			if(!cm.isEdge()) continue;
			
			if(coreEdgeTypes.contains(cm.getURI())) {
				cm.setEdgeDestinationDomains(new ArrayList<ClassMetadata>());
				cm.setEdgeSourceDomains(new ArrayList<ClassMetadata>());
				continue;
			}

	         Class<? extends GraphObject> edgeClazz = cm.getClazz();
	         
			OntClass edgeClass = targetEmptyModel.getOntClass(cm.getURI());
			
			if(edgeClass == null) throw new Exception("Edge class not found");
			
			String[] singleAndPlural = getEdgeLocalNameAndPlural(edgeClass);
			
			List<Statement> srcStmts = edgeClass.listProperties(VitalCoreOntology.hasEdgeSrcDomain).toList();
			List<Statement> destStmts = edgeClass.listProperties(VitalCoreOntology.hasEdgeDestDomain).toList();
			
			if(srcStmts.size() < 1) throw new Exception("Edge class " + edgeClass.getURI() + " has no source domain properties");
			if(destStmts.size() < 1) throw new Exception("Edge class " + edgeClass.getURI() + " has no dest domain properties");
			
			List<ClassMetadata> sourceDomains = new ArrayList<ClassMetadata>();
			List<ClassMetadata> destDomains = new ArrayList<ClassMetadata>();
			
			
			String singleLCStart = singleAndPlural[0];
			singleLCStart = singleLCStart.substring(0, 1).toLowerCase() + singleLCStart.substring(1);
			String pluralLCStart = singleAndPlural[1];
			pluralLCStart = pluralLCStart.substring(0, 1).toLowerCase() + pluralLCStart.substring(1);
			
			
			//collect types that may be both source or destination
			Set<String> sourceTypeURI = new HashSet<String>();
			Set<String> destTypeURI = new HashSet<String>();
			
			for(Statement srcStmt : srcStmts) {
				
				RDFNode srcObject = srcStmt.getObject();
				
				if(!srcObject.isURIResource()) throw new Exception("Edge " + edgeClass.getURI() + " source domain must be a URI resource!");
				
				String srcURI = srcObject.asResource().getURI();
				
				sourceTypeURI.add(srcURI);
			}
			
			for(Statement destStmt : destStmts) {
				
				RDFNode destObject = destStmt.getObject();
					
				if(!destObject.isURIResource()) throw new Exception("Edge " + edgeClass.getURI() + " destination domain must be a URI resource!");
					
				String destURI = destObject.asResource().getURI();
				
				destTypeURI.add(destURI);
				
			}
			
			
			for(Statement srcStmt : srcStmts) {
				
				RDFNode srcObject = srcStmt.getObject();
				
				if(!srcObject.isURIResource()) throw new Exception("Edge " + edgeClass.getURI() + " source domain must be a URI resource!");
				
				String srcURI = srcObject.asResource().getURI();
				
				ClassMetadata srcClass = classesRegistry.getClass(srcURI);
				
				if(srcClass == null) throw new Exception("Edge " + edgeClass.getURI() + " source domain class not found in VitalSigns " + srcURI);
				
				if(! VITAL_Node.class.isAssignableFrom( srcClass.getClazz() ) ) throw new Exception("Edge " + edgeClass.getURI() + " source domain class is not a subclass of VITAL_Node: " + srcClass.getClazz().getCanonicalName());

				boolean bothSrcAndDest = sourceTypeURI.contains(srcURI) && destTypeURI.contains(srcURI);
				
				sourceDomains.add(srcClass);
				
				EdgeAccessAssigner.assignDynamicEdgeAccess((Class<? extends VITAL_Edge>)edgeClazz, (Class<? extends VITAL_Node>)srcClass.getClazz(), true, bothSrcAndDest, singleAndPlural[0], singleAndPlural[1]);
				
				//check if a property already exists?
				for( ClassMetadata current : classesRegistry.getEdgeAndHyperEdgeClassesWithSourceOrDestGraphClass((Class<? extends VITAL_Node>)srcClass.getClazz(), true)) {
					
					if(singleLCStart.equals(current.getEdgeSingleName()) || singleLCStart.equals(current.getEdgePluralName()) || pluralLCStart.equals(current.getEdgeSingleName()) || pluralLCStart.equals(current.getEdgePluralName())) {
						
						throw new Exception("Cannot register edge type: " + cm.getURI() + " for source domain: " + srcURI + " conflict with " + current.getURI());
						
					}
					
				}
				
			}
				
			for(Statement destStmt : destStmts) {
					
				RDFNode destObject = destStmt.getObject();
					
				if(!destObject.isURIResource()) throw new Exception("Edge " + edgeClass.getURI() + " destination domain must be a URI resource!");
					
				String destURI = destObject.asResource().getURI();
					
				ClassMetadata destClass = classesRegistry.getClass(destURI);
				
				if(destClass == null) throw new Exception("Edge " + edgeClass.getURI() + " destination domain class not found in VitalSigns " + destURI);
				
				if(! VITAL_Node.class.isAssignableFrom( destClass.getClazz() ) ) throw new Exception("Edge " + edgeClass.getURI() + " destination domain class is not a subclass of VITAL_Node: " + destClass.getClazz().getCanonicalName());

				boolean bothSrcAndDest = sourceTypeURI.contains(destURI) && destTypeURI.contains(destURI);
				
				destDomains.add(destClass);
				
				EdgeAccessAssigner.assignDynamicEdgeAccess((Class<? extends VITAL_Edge>)edgeClazz, (Class<? extends VITAL_Node>)destClass.getClazz(), false, bothSrcAndDest, singleAndPlural[0], singleAndPlural[1]);
				
				//check if a property already exists?
				for( ClassMetadata current : classesRegistry.getEdgeAndHyperEdgeClassesWithSourceOrDestGraphClass((Class<? extends VITAL_Node>)destClass.getClazz(), false)) {
					
					if(singleLCStart.equals(current.getEdgeSingleName()) || singleLCStart.equals(current.getEdgePluralName()) || pluralLCStart.equals(current.getEdgeSingleName()) || pluralLCStart.equals(current.getEdgePluralName())) {
						
						throw new Exception("Cannot register edge type: " + cm.getURI() + " for destination domain: " + destURI + " conflict with " + current.getURI());
						
					}
					
				}
				
			}

			cm.setEdgeSingleName(singleLCStart);
			cm.setEdgePluralName(pluralLCStart);
			
			cm.setEdgeSourceDomains(sourceDomains);
			
			cm.setEdgeDestinationDomains(destDomains);
	
			
		}
		
		
	}
	
	private void processHyperEdgeAccess(OntModel targetEmptyModel) throws Exception {
		
		for(ClassMetadata cm : classesList) {
			
			if(!cm.isHyperEdge()) continue;
			
			if(coreHyperEdgeTypes.contains(cm.getURI())) {
				cm.setHyperEdgeDestinationDomains(new ArrayList<ClassMetadata>());
				cm.setHyperEdgeSourceDomains(new ArrayList<ClassMetadata>());
				continue;
			}

			Class<? extends GraphObject> hyperEdgeClazz = cm.getClazz();
	         
			OntClass hyperEdgeClass = targetEmptyModel.getOntClass(cm.getURI());
			
			if(hyperEdgeClass == null) throw new Exception("HyperEdge class not found");
			
			String[] singleAndPlural = getHyperEdgeLocalNameAndPlural(hyperEdgeClass);
			
			List<Statement> srcStmts = hyperEdgeClass.listProperties(VitalCoreOntology.hasHyperEdgeSrcDomain).toList();
			List<Statement> destStmts = hyperEdgeClass.listProperties(VitalCoreOntology.hasHyperEdgeDestDomain).toList();
			
			if(srcStmts.size() < 1) throw new Exception("HyperEdge class " + hyperEdgeClass.getURI() + " has no source domain properties");
			if(destStmts.size() < 1) throw new Exception("HyperEdge class " + hyperEdgeClass.getURI() + " has no dest domain properties");
			
			List<ClassMetadata> sourceDomains = new ArrayList<ClassMetadata>();
			List<ClassMetadata> destDomains = new ArrayList<ClassMetadata>();
			
			
			String singleLCStart = singleAndPlural[0];
			singleLCStart = singleLCStart.substring(0, 1).toLowerCase() + singleLCStart.substring(1);
			String pluralLCStart = singleAndPlural[1];
			pluralLCStart = pluralLCStart.substring(0, 1).toLowerCase() + pluralLCStart.substring(1);
			
			
			//collect types that may be both source or destination
			Set<String> sourceTypeURI = new HashSet<String>();
			Set<String> destTypeURI = new HashSet<String>();
			
			for(Statement srcStmt : srcStmts) {
				
				RDFNode srcObject = srcStmt.getObject();
				
				if(!srcObject.isURIResource()) throw new Exception("HyperEdge " + hyperEdgeClass.getURI() + " source domain must be a URI resource!");
				
				String srcURI = srcObject.asResource().getURI();
				
				sourceTypeURI.add(srcURI);
			}
			
			for(Statement destStmt : destStmts) {
				
				RDFNode destObject = destStmt.getObject();
				
				if(!destObject.isURIResource()) throw new Exception("HyperEdge " + hyperEdgeClass.getURI() + " destination domain must be a URI resource!");
				
				String destURI = destObject.asResource().getURI();
				
				destTypeURI.add(destURI);
				
			}
			
			
			for(Statement srcStmt : srcStmts) {
				
				RDFNode srcObject = srcStmt.getObject();
				
				if(!srcObject.isURIResource()) throw new Exception("HyperEdge " + hyperEdgeClass.getURI() + " source domain must be a URI resource!");
				
				String srcURI = srcObject.asResource().getURI();
				
				ClassMetadata srcClass = classesRegistry.getClass(srcURI);
				
				if(srcClass == null) throw new Exception("HyperEdge " + hyperEdgeClass.getURI() + " source domain class not found in VitalSigns " + srcURI);
				
//				if(! VITAL_Node.class.isAssignableFrom( srcClass.getClazz() ) ) throw new Exception("Edge " + hyperEdgeClass.getURI() + " source domain class is not a subclass of VITAL_Node: " + srcClass.getClazz().getCanonicalName());
				
				boolean bothSrcAndDest = sourceTypeURI.contains(srcURI) && destTypeURI.contains(srcURI);
				
				sourceDomains.add(srcClass);
				
				HyperEdgeAccessAssigner.assignDynamicHyperEdgeAccess((Class<? extends VITAL_HyperEdge>)hyperEdgeClazz, srcClass.getClazz(), true, bothSrcAndDest, singleAndPlural[0], singleAndPlural[1]);
				
				//check if a property already exists?
				for( ClassMetadata current : classesRegistry.getEdgeAndHyperEdgeClassesWithSourceOrDestGraphClass(srcClass.getClazz(), true)) {
					
					if(singleLCStart.equals(current.getEdgeSingleName()) || singleLCStart.equals(current.getEdgePluralName()) || pluralLCStart.equals(current.getEdgeSingleName()) || pluralLCStart.equals(current.getEdgePluralName())) {
						
						throw new Exception("Cannot register Hyper edge type: " + cm.getURI() + " for source domain: " + srcURI + " conflict with " + current.getURI());
						
					}
					
				}
				
			}
			
			for(Statement destStmt : destStmts) {
				
				RDFNode destObject = destStmt.getObject();
				
				if(!destObject.isURIResource()) throw new Exception("HyperEdge " + hyperEdgeClass.getURI() + " destination domain must be a URI resource!");
				
				String destURI = destObject.asResource().getURI();
				
				ClassMetadata destClass = classesRegistry.getClass(destURI);
				
				if(destClass == null) throw new Exception("HyperEdge " + hyperEdgeClass.getURI() + " destination domain class not found in VitalSigns " + destURI);
				
//				if(! VITAL_Node.class.isAssignableFrom( destClass.getClazz() ) ) throw new Exception("Edge " + hyperEdgeClass.getURI() + " destination domain class is not a subclass of VITAL_Node: " + destClass.getClazz().getCanonicalName());
				
				boolean bothSrcAndDest = sourceTypeURI.contains(destURI) && destTypeURI.contains(destURI);
				
				destDomains.add(destClass);
				
				HyperEdgeAccessAssigner.assignDynamicHyperEdgeAccess((Class<? extends VITAL_HyperEdge>)hyperEdgeClazz, destClass.getClazz(), false, bothSrcAndDest, singleAndPlural[0], singleAndPlural[1]);
				
				//check if a property already exists?
				for( ClassMetadata current : classesRegistry.getEdgeAndHyperEdgeClassesWithSourceOrDestGraphClass(destClass.getClazz(), false)) {
					
					if(singleLCStart.equals(current.getEdgeSingleName()) || singleLCStart.equals(current.getEdgePluralName()) || pluralLCStart.equals(current.getEdgeSingleName()) || pluralLCStart.equals(current.getEdgePluralName())) {
						
						throw new Exception("Cannot register Hyper edge type: " + cm.getURI() + " for destination domain: " + destURI + " conflict with " + current.getURI());
						
					}
					
				}
				
			}
			
			cm.setEdgeSingleName(singleLCStart);
			cm.setEdgePluralName(pluralLCStart);
			
			cm.setHyperEdgeSourceDomains(sourceDomains);
			
			cm.setHyperEdgeDestinationDomains(destDomains);
			
			
		}
		
		
	}
	
	public static Set<String> getDirectImports(Model bareModel, String ontologyURI) {
	    
       Resource ontologyRes = null;
        
        for( ResIterator resIter = bareModel.listSubjectsWithProperty(RDF.type, OWL.Ontology); resIter.hasNext(); ) {
            
            if(ontologyRes != null) throw new RuntimeException("More than 1 ontology found in source model!");
            
            Resource o = resIter.next();
            
            if(!o.isURIResource()) throw new RuntimeException("Ontology resource is not a URI resource: " + o);
            
            if(ontologyURI != null && !o.getURI().equals(ontologyURI)) throw new RuntimeException("Ontology in model and target URIs don't match: " + o.getURI() + " vs. " + ontologyURI);
            
            ontologyRes = o;
            
        }
	    
        return getDirectImports(ontologyRes);
	    
	}
	
	public static Set<String> getDirectImports(Resource ontologyRes) {
	    
	    String ontologyURI = ontologyRes.getURI();
	    
        Set<String> imports = new HashSet<String>();
        
        for( StmtIterator importsIter = ontologyRes.listProperties(OWL.imports); importsIter.hasNext(); ) {
               
            Resource r = importsIter.next().getResource();
               
            if(r == null) throw new RuntimeException("owl:imports statement object must be a URI resource");
            
            if(r.getURI() == null) throw new RuntimeException("owl:imports statement must be a URI resource");
            
            if(r.getURI().equals(ontologyURI)) throw new RuntimeException("An ontology must not import self: " + ontologyURI);
            
            imports.add(r.getURI());
               
        }
        
        return imports;
	    
	}

	private DomainOntology processOntologyMetaData(Model bareModel, OntModel model,
			String ontologyURI) throws Exception {

		
		Resource ontologyRes = null;
		
		for( ResIterator resIter = bareModel.listSubjectsWithProperty(RDF.type, OWL.Ontology); resIter.hasNext(); ) {
			
			if(ontologyRes != null) throw new Exception("More than 1 ontology found in source model!");
			
			Resource o = resIter.next();
			
			if(!o.isURIResource()) throw new Exception("Ontology resource is not a URI resource: " + o);
			
			if(ontologyURI != null && !o.getURI().equals(ontologyURI)) throw new Exception("Ontology in model and target URIs don't match: " + o.getURI() + " vs. " + ontologyURI);
			
			ontologyRes = o;
			
		}
		
		if(ontologyRes == null) throw new Exception("No ontology found in the source model: " + model.size());
		
		String version = RDFUtils.getStringPropertySingleValue(ontologyRes, OWL.versionInfo);
		
		if(StringUtils.isEmpty(version)) throw new Exception("Ontology " + ontologyRes.getURI() + " does not have owl:versionInfo annotation");
		
		String backwardCompatibleVersion = RDFUtils.getStringPropertySingleValue(ontologyRes, VitalCoreOntology.hasBackwardCompatibilityVersion);
		
		List<String> imports = new ArrayList<String>();
			
		Set<String> imports2Check = new HashSet<String>();
			
		Set<String> checked = new HashSet<String>();
		
		
		for( StmtIterator importsIter = ontologyRes.listProperties(OWL.imports); importsIter.hasNext(); ) {
			
			Resource r = importsIter.next().getResource();
			
			if(r == null) throw new RuntimeException("owl:imports statement object must be a URI resource");
			
			imports2Check.add(r.getURI());
			
		}
		
		Set<String> parentOntologies = new HashSet<String>(imports2Check);
		
		Map<String, List<String>> ontologyURI2ImportsTree = VitalSigns.get().getOntologyURI2ImportsTree();
		
		while(imports2Check.size() > 0) {
				
			Set<String> newSet = new HashSet<String>();
				
			for(String i : imports2Check) {
					
				checked.add(i);
					
				List<String> s = ontologyURI2ImportsTree.get(i);

				if(!imports.contains(i)) {
					imports.add(i);
				}
					
				if(s != null) {
						
					for(String sub : s) {
							
						if(!checked.contains(sub)) {
							newSet.add(sub);
						}
							
					}
						
				} else {
					
					throw new Exception("Ontology referenced in " + ontologyRes.getURI() + " - " + i + " not found"); 
					
				}
			}
				
			imports2Check = newSet;
				
		}
			
		ontologyURI2ImportsTree.put(ontologyRes.getURI(), imports);
			
		
		//construct the ont model now
		
		for(int i = imports.size() - 1; i >= 0; i--) {
			
			String importURI = imports.get(i);
			Model importModel = VitalSigns.get().getOntologyBaseModel(importURI);
			
			if(importModel == null) throw new Exception("Model for referenced ontology in " + ontologyRes.getURI() + " - " + importURI + " not found");
			
			model.add(importModel);
			
		}
		
		//add this model as last
		model.add(bareModel);
		
		DomainOntology m = new DomainOntology(ontologyRes.getURI(), version, backwardCompatibleVersion);
		m.setDirectParentOntologies(parentOntologies);
		m.setPreferredImportVersions(RDFUtils.getStringPropertyValues(ontologyRes, VitalCoreOntology.hasPreferredImportVersion));
		return m;
	}

	void processGraphObjects(OntModel model, String ontologyURI, String _package) throws Exception {

	    long t = time();
	    
		List<OntClass> classes = listOntologyClassesSorted(model, ontologyURI);
		
		log.debug("Classes listing time: {}ms", time() - t);
		
		classesList = new ArrayList<ClassMetadata>(classes.size());
		
		for(OntClass c : classes) {
			
			if(DomainGenerator.nonGraphObjectClasses.contains(c.getURI())) continue;
			
			String localName = c.getLocalName();
			
            String canonicalName = _package + '.' + localName;
			
//			Class<? extends GraphObject> clazz = null;
//			Class<? extends GraphObject> clazz = VitalSigns.get().getGraphObjectClass(ontologyURI, x);
//			if(clazz == null) throw new Exception("Graph object class not found: " + x);
			
			OntClass sc = c.getSuperClass();
			
			ClassMetadata parentClass = null;
			
			//can only have 1 direct parent
			List<OntClass> parentsList = c.listSuperClasses(true).toList();
			if( parentsList.size() > 1 ) {
				throw new Exception("At most 1 direct parent allowed: " + c.getURI() + ", has " + parentsList.size() + ": " + parentsList.toString());
			}
			
			if(sc != null && !OWL.Thing.getURI().equals(sc.getURI())) {
				
				parentClass = classesRegistry.getClass(sc.getURI());
				
				if(parentClass == null) throw new Exception("Parent class not found: " + sc.getURI()); 
				
			}
			
			List<OntClass> superClasses = new ArrayList<OntClass>();
			superClasses.add(c);
			RDFUtils.collectAllSuperClasses(c, superClasses, null);
			
			boolean isEdge = false;
			boolean isHyperEdge = false;
			for(OntClass oc : superClasses) {
			    if(VitalCoreOntology.VITAL_Edge.getURI().equals( oc.getURI()) ) {
			        isEdge = true;
			    } else if(VitalCoreOntology.VITAL_HyperEdge.getURI().equals(oc.getURI())) {
			        isHyperEdge = true;
			    }
			}

			ClassMetadata classMD = new ClassMetadata(c.getURI(), ontologyURI, localName, canonicalName, parentClass, isEdge, isHyperEdge);
			
			classesRegistry.addClass(parentClass, classMD);
			
			propertiesRegistry.addClass(classMD);
			
			classesList.add(classMD);
			
			///XXX properties helper validation is expensive as it initializes instances
			///even looking up properties helper classes double the domain loading time
//	        ClassPropertiesHelper propertiesHelper = VitalSigns.get().getPropertiesHelper(clazz);
//	        
			//for the time being we need to check the existence of properties classes
//			VitalSigns.get().getPropertiesHelperClass(clazz);
			
//	        if(propertiesHelper == null) throw new Exception("Properties helper class not found for class: " + ontologyURI + " java class: " + clazz.getCanonicalName());
			
		}
		
		log.debug("Classes processed {}", classes.size());
		
	}

	public static List<OntClass> listOntologyClassesSorted(OntModel model,
			String ontologyURI) {
		
		List<OntClass> classes = new ArrayList<OntClass>();
		
		final Map<String, Integer> class2Depth = new HashMap<String, Integer>();
		
		//sort them first, parents go first
		for( ExtendedIterator<OntClass> iter = model.listClasses(); iter.hasNext(); ) {
			
			OntClass next = iter.next();
			
			if(!next.isURIResource()) continue;
			
			String baseURI = RDFUtils.uriFromNamespace(next.getNameSpace());
			
			if(!ontologyURI.equals(baseURI)) continue;
			
			classes.add(next);
			
			
			List<OntClass> tree = RDFUtils.getClassParents(next, false);
			
			int score = 0;
			
			for(OntClass c: tree){
				
				if(c.getNameSpace() != null) {
					
					String ontURI = RDFUtils.uriFromNamespace(c.getNameSpace());
					
					if(ontologyURI.equals(ontURI)) {
						
						score++;
						
					}
					
				}
				
			}
			
			class2Depth.put(next.getURI(), score);
			
		}
		
		Collections.sort(classes, new Comparator<OntClass>() {

			@Override
			public int compare(OntClass o1, OntClass o2) {
				Integer s1 = class2Depth.get(o1.getURI());
				Integer s2 = class2Depth.get(o2.getURI());
				return s1.compareTo(s2);
			}
		});
		
		return classes;
	}

	void processProperties(OntModel model, String ontologyURI, String _package) throws Exception {
		
	    
	    long t = time();
	    
		//sort them first, parents go first
		List<OntPropertyOrigin> properties = listOntologyPropertiesSorted(model, ontologyURI, true);
		
		log.debug("Properties listing time: {}ms", time() - t);
		
		for(OntPropertyOrigin dpo : properties) {
			
		    OntProperty dp = dpo.property;
		    
            if(dp instanceof AnnotationProperty) {
                List<String> propertyDomains = getPropertyDomains(dp);
                for(String classURI : propertyDomains) {
                    if(classURI.startsWith(OWL.NS)) continue;
                    ClassMetadata cm = classesRegistry.getClass(classURI);
                    if(cm == null) {
                        //throw new RuntimeException("Class metadata not found: " + classURI + ", annotation property: " + dp.getURI());
                        log.warn("Class metadata not found: " + classURI + ", annotation property: " + dp.getURI());
                        continue;
                    }
                    String shortName = RDFUtils.getPropertyShortName(dp.getURI());
                    propertiesRegistry.addAnnotationProperty(Arrays.asList(cm), dp.getURI(), shortName);
                }
                //extending base property
                continue;
            }
			
		    if(!dpo.ontologyURI.equals(ontologyURI)) {
		        
		        //extend base property
		        PropertyMetadata parentPM = VitalSigns.get().getPropertiesRegistry().getProperty(dp.getURI());
		        if(parentPM == null) throw new RuntimeException("Property not found in parent domains: " + dp.getURI());
		        
		        //check if we can 
		        List<String> propertyDomains = getPropertyDomains(dp);
		        for(String classURI : propertyDomains) {
		            
		            String ontologyPart = RDFUtils.getOntologyPart(classURI);
		            if(ontologyPart.equals(ontologyURI)) {
		                
		                //class should be loaded already!
		                ClassMetadata cm = classesRegistry.getClass(classURI);
		                if(cm == null) throw new RuntimeException("Class metadata not found: " + classURI);
		                
		                if(!parentPM.getDomains().contains(cm)) {
		                    parentPM.getDomains().add(cm);
		                }
		                
		                propertiesRegistry.addProperty(parentPM.getParent(), parentPM);
		                
		                
		            }
		            
		        }
		        
		        continue;
		        
		    }
		    
			if(VitalCoreOntology.internalProperties.contains(dp.getURI())) continue;

			if(dp instanceof AnnotationProperty) {
			    List<String> propertyDomains = getPropertyDomains(dp);
			    continue;
			}
			
			PropertyDetails details = getPropertyDetails(dp, classesRegistry);
			
			
			int superProps = dp.listSuperProperties(true).toList().size();
			
			if(superProps > 1) throw new Exception("More than one parent property of: " + dp.getURI());
			
			//get super class?
			OntProperty superProperty = dp.getSuperProperty();
			
			PropertyMetadata parentProperty = null;
			
			if(superProperty != null) {
				
				parentProperty = propertiesRegistry.getProperty(superProperty.getURI());
				
				if(parentProperty == null) throw new Exception("Parent property of " + dp.getURI() + " not found in VitalSigns: " + superProperty.getURI());
				
			}
			
			Class<? extends PropertyTrait> propertyTrait = null;

			try {
				propertyTrait = VitalSigns.get().getPropertyClass(ontologyURI, _package + '.' + RDFUtils.PROPERTIES_PACKAGE + '.' + RDFUtils.getPropertyClassName(dp.getLocalName()));
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			
			
			//create new property
			
			// Object property = PropertyFactory.createInstance(details.pClass, propertyTrait);
			// deferred loading

			Object property = null

			String shortName = RDFUtils.getPropertyShortName(dp.getURI())

			// println "Shortname: " + shortName

			PropertyMetadata pm = new PropertyMetadata(parentProperty, dp.getURI(), (IProperty) property, details.pClass, propertyTrait, details.domainsClasses, shortName);
			pm.setMultipleValues(details.multipleValues);
			pm.setInternalStore(details.internalStore);
			pm.setDontIndex(details.dontIndex);
			pm.setTransientProperty(details.transientProperty);
			pm.setAnalyzedNoEquals(details.analyzedNoEquals);
			
			PropertyDefinitionProcessor.setPropertyDefinitionProperties(model, dp, pm);
			
			propertiesRegistry.addProperty(parentProperty, pm);
			
			TraitClassQueryAssigner.assignQueryObjectGetter(propertyTrait);
		}
		
		log.debug("Properties processed {}", properties.size());
		
	}
	
	public static class PropertyDetails {
		
		public boolean analyzedNoEquals;

		public boolean transientProperty;

		public boolean dontIndex;

		public boolean internalStore = false;

		public Class<? extends IProperty> pClass;
		
		public List<ClassMetadata> domainsClasses;
		
		public List<String> domainClassesURIs;

		public String shortName;

		public boolean multipleValues = false;

        public String URI;
		
		public PropertyDetails(String shortName, Class<? extends IProperty> pClass,
				List<ClassMetadata> domainsClasses,
				List<String> domainClassesURIs) {
			super();
			this.shortName = shortName;
			this.pClass = pClass;
			this.domainsClasses = domainsClasses;
			this.domainClassesURIs = domainClassesURIs;
		}
		
	}
	
	public static List<String> getPropertyDomains(OntProperty dp) throws Exception {
	    
	  Set<String> domains = new HashSet<String>();
	  
	  List<? extends OntResource> domainsRes = dp.listDomain().toList();
	  
      for(OntResource domain : domainsRes) {
        
            if(domain == null) throw new Exception("Property has no domain: " + dp.getURI());
            
            OntClass domainClass = domain.asClass();
            
            if(domainClass == null) throw new Exception("domain is not an ontclass: " + domain + ", " + dp.getURI());
    
            
            if( domainClass.isUnionClass() ) {
                
                UnionClass uc = domainClass.asUnionClass();
                
                RDFList operands = uc.getOperands();
                
                for(RDFNode n : operands.iterator().toList() ) {
                    
                    if(!n.isURIResource()) {throw new RuntimeException("Property domain union must consist of URI resources only");};
                    
                    String uri = n.asResource().getURI();
      
                    domains.add(uri);
                    
                }
                
//                for( ExtendedIterator<? extends OntClass> ei =  uc.listOperands(); ei.hasNext(); ) {
//                                        
//                    OntClass oc = ei.next();
//    
//                    if(!oc.isURIResource()) {
//                        throw new Exception("All union operands must be URI ont classes: " + oc + ", " + dp.getURI());
//                    }
//                    
//                    
//                    domains.add(oc.getURI());
//                        
//                }
                
            } else {
                
                //single domain?
                if( ! domainClass.isURIResource() ) {
                    throw new Exception("Property single domain class must be a URI resource: " + domainClass + ", " + dp.getURI());
                }
                
                domains.add(domainClass.getURI());
                
            }
      }
      
      return new ArrayList<String>(domains);
	    
	}
	
	public static PropertyDetails getPropertyDetails(OntProperty dp, ClassesRegistry optionalClassesRegistry) throws Exception {
		
	    List<String> domains = getPropertyDomains(dp);
	    
	    /*
		OntResource domain = dp.getDomain();
		
		if(domain == null) throw new Exception("Property has no domain: " + dp.getURI());
		
		OntClass domainClass = domain.asClass();
		
		if(domainClass == null) throw new Exception("domain is not an ontclass: " + domain + ", " + dp.getURI());

		
		if( domainClass.isUnionClass() ) {
			
			UnionClass uc = domainClass.asUnionClass();
								
			for( ExtendedIterator<? extends OntClass> ei =  uc.listOperands(); ei.hasNext(); ) {
									
				OntClass oc = ei.next();

				if(!oc.isURIResource()) {
					throw new Exception("All union operands must be URI ont classes: " + oc + ", " + dp.getURI());
				}
				
				
				domains.add(oc.getURI());
					
			}
			
		} else {
			
			//single domain?
			if( ! domainClass.isURIResource() ) {
				throw new Exception("Property single domain class must be a URI resource: " + domainClass + ", " + dp.getURI());
			}
			
			domains.add(domainClass.getURI());
			
		}
		
		*/
		
		//verify domains
		
		List<ClassMetadata> domainsClasses = new ArrayList<ClassMetadata>();
		
		if(optionalClassesRegistry != null) {
			
			
			for(String domainURI : domains) {
				
				ClassMetadata domainCMD = optionalClassesRegistry.getClass(domainURI);
				
				if(domainCMD == null) throw new Exception("Domain class not found: " + domainURI);
				
				domainsClasses.add(domainCMD);
				
			}
			
			if(domainsClasses.size() < 1) throw new Exception("No property domain classes: " + dp.getURI());
			
		}
		
		
		
		
		Class<? extends IProperty> pClass = null;
		
		OntResource range = dp.getRange();

		if(dp instanceof ObjectProperty) {
			
		} else {
			
			if(range == null) throw new Exception("No range defined for property: " + dp.getURI());
			
			if(RDFS.Datatype.equals(range.getPropertyValue(RDF.type))) {
				
				//extract range type from restriction node
				Resource baseDatatype = range.getPropertyResourceValue(OWL2.onDatatype);
				
				if(baseDatatype != null) {
					
					OntModel ontModel = (OntModel)dp.getModel();
					
					range = ontModel.getOntResource(baseDatatype);
					
				}
				
				
			}
			
		}
		
		if(dp instanceof ObjectProperty) {
			
			pClass = URIProperty.class;
			
		} else {
			
			if(XSD.xboolean.equals(range)) {
				
				pClass = BooleanProperty.class;
				
			} else if(XSD.xstring.equals(range)) {
				
				pClass = StringProperty.class;
				
			} else if(XSD.xint.equals(range)) {
				
				pClass = IntegerProperty.class;
				
			} else if(XSD.xlong.equals(range)) {
			
				pClass = LongProperty.class;
			
			} else if(XSD.xdouble.equals(range)) {
				
				pClass = DoubleProperty.class;
				
			} else if(XSD.xfloat.equals(range)) {
				
				pClass = FloatProperty.class;
				
			} else if(XSD.dateTime.equals(range)) {
			
				pClass = DateProperty.class;
				
			} else if(VitalCoreOntology.geoLocation.equals(range)) {
			
				pClass = GeoLocationProperty.class;
			
			} else if(VitalCoreOntology.truth.equals(range)) {
				
			    pClass = TruthProperty.class;
			    
			} else if(XSD.anyURI.equals(range)) {
				
				pClass = URIProperty.class; 
			
			} else {
				throw new Exception("Unsupported data type: " + range.getURI());
			}
		}
		
		Boolean multipleValues = RDFUtils.getBooleanPropertySingleValue(dp, VitalCoreOntology.hasMultipleValues);
		if(multipleValues == null) multipleValues = false;
		
		Boolean internalStore = RDFUtils.getBooleanPropertySingleValue(dp, VitalCoreOntology.isInternalStore);
		if(internalStore == null) internalStore = false;
		
		Boolean dontIndex = RDFUtils.getBooleanPropertySingleValue(dp, VitalCoreOntology.hasDontIndexFlag);
		if(dontIndex == null) dontIndex = false;
		
		Boolean transientFlag = RDFUtils.getBooleanPropertySingleValue(dp, VitalCoreOntology.hasTransientPropertyFlag);
		if(transientFlag == null) transientFlag = false;
		
		Boolean analyzedNoEquals = RDFUtils.getBooleanPropertySingleValue(dp, VitalCoreOntology.isAnalyzedNoEquals);
		if(analyzedNoEquals == null) analyzedNoEquals = false;
		
		PropertyDetails pd = new PropertyDetails(RDFUtils.getPropertyShortName(dp.getURI()), pClass, domainsClasses, domains);
		pd.multipleValues = multipleValues;
		pd.internalStore = internalStore;
		pd.dontIndex = dontIndex;
		pd.transientProperty = transientFlag;
		pd.analyzedNoEquals = analyzedNoEquals;
		pd.URI = dp.getURI();
		return pd;
		
	}

	public static class OntPropertyOrigin {
	    
	    OntProperty property;
	    
	    String ontologyURI;
	    
        public OntPropertyOrigin(OntProperty property, String ontologyURI) {
            super();
            this.property = property;
            this.ontologyURI = ontologyURI;
        }
	    
	}
	/**
	 * Returns properties directly defined in ontology as well as properties with extended domain by this ontology 
	 * @param model
	 * @param ontologyURI
	 * @return
	 * @throws Exception 
	 */
	public static List<OntPropertyOrigin> listOntologyPropertiesSorted(OntModel model,
			final String ontologyURI, boolean includeAnnotationProperties) throws Exception {
		
		List<OntPropertyOrigin> properties = new ArrayList<OntPropertyOrigin>();
		
		final Map<String, Integer> property2Depth = new HashMap<String, Integer>();
		
		for( OntProperty dp : model.listAllOntProperties().toList()) {
			
			if( ! ( dp instanceof DatatypeProperty || dp instanceof ObjectProperty || ( includeAnnotationProperties && dp instanceof AnnotationProperty) ) ) {
				continue;
			}
			
			if( ! dp.isURIResource() ) continue;
			
			String ns = dp.getNameSpace();
			
			String baseURI = RDFUtils.uriFromNamespace(ns);
			
//			if(!ontologyURI.equals(baseURI)) continue;
			
			String originURI = ontologyURI;
			
			boolean keepIt = false;
			
			if(!ontologyURI.equals(baseURI)) {
			    List<String> domainClasses = getPropertyDomains(dp);
			    for( String dc : domainClasses) {
			        String pburi = RDFUtils.uriFromNamespace(dc);
			        
			        if(pburi.equals(ontologyURI)) {
			            keepIt = true;
			            originURI = baseURI;
			            break;
			            
			        }
			        
			    }
			} else {
			    keepIt = true;
			}
			
			if(!keepIt) continue;
			
			properties.add(new OntPropertyOrigin(dp, originURI));
			
			//collect properties tree length
			List<OntProperty> tree = RDFUtils.getPropertyParents(dp, false);
			
			int score = 0;
			
			for(OntProperty p : tree) {
				
				if( p.getNameSpace() != null ) {
					
					String ouri = RDFUtils.uriFromNamespace(p.getNameSpace());
					
					if(ontologyURI.equals(ouri)) {
						score++;
					}
				}
				
			}
			
			property2Depth.put(dp.getURI(), score);
			
		}

		Collections.sort(properties, new Comparator<OntPropertyOrigin>(){

			@Override
			public int compare(OntPropertyOrigin arg0, OntPropertyOrigin arg1) {
			    
			    boolean this1 = arg0.ontologyURI.equals(ontologyURI); 
			    boolean this2 = arg0.ontologyURI.equals(ontologyURI);
			    
			    if(this1 && !this2) return -1;
			    if(!this1 && this2) return 1;
			    
			    //otherwise compare by
				Integer s1 = property2Depth.get(arg0.property.getURI());
				Integer s2 = property2Depth.get(arg1.property.getURI());
				int c = s1.compareTo(s2);
				if(c != 0 ) return c;
				return arg0.property.getURI().compareTo(arg1.property.getURI());
			}
			
		});
		
		return properties;
	}
	
//	public static List<OntProperty> listExtendedProperties(OntModel model, String ontologyURI) {
//	    
//	    
//	    
//	}

	
	public static DomainOntology getOntologyMetaData(Model bareModel) throws Exception {

        Resource ontologyRes = null;

        String ontologyURI = null;
        
        for( ResIterator resIter = bareModel.listSubjectsWithProperty(RDF.type, OWL.Ontology); resIter.hasNext(); ) {
            
            if(ontologyRes != null) throw new Exception("More than 1 ontology found in source model!");
            
            Resource o = resIter.next();
            
            if(!o.isURIResource()) throw new Exception("Ontology resource is not a URI resource: " + o);

            ontologyRes = o;
            
            ontologyURI = ontologyRes.getURI();
            
            
        }
	        
        if(ontologyRes == null) throw new Exception("No ontology found in the source model: " + bareModel.size());
	        
        String version = RDFUtils.getStringPropertySingleValue(ontologyRes, OWL.versionInfo);
	        
        if(StringUtils.isEmpty(version)) throw new Exception("Ontology " + ontologyRes.getURI() + " does not have owl:versionInfo annotation");
	        
        String defaultPackage = RDFUtils.getStringPropertySingleValue(ontologyRes, VitalCoreOntology.hasDefaultPackage);
        
        String backwardVersion = RDFUtils.getStringPropertySingleValue(ontologyRes, VitalCoreOntology.hasBackwardCompatibilityVersion);
        
        String defaultGroupId = RDFUtils.getStringPropertySingleValue(ontologyRes, VitalCoreOntology.hasDefaultGroupId);
        
        String defaultArtifactId = RDFUtils.getStringPropertySingleValue(ontologyRes, VitalCoreOntology.hasDefaultArtifactId);
	        
	    List<String> imports = new ArrayList<String>();
	            
	    Set<String> imports2Check = new HashSet<String>();
	            
	    Set<String> checked = new HashSet<String>();
	        
        
        for( StmtIterator importsIter = ontologyRes.listProperties(OWL.imports); importsIter.hasNext(); ) {
            
            Resource r = importsIter.next().getResource();
            
            if(r == null) throw new RuntimeException("owl:imports statement object must be a URI resource");
            
            imports2Check.add(r.getURI());
            
        }
	        
        Set<String> parentOntologies = new HashSet<String>(imports2Check);
        
        /*
        Map<String, List<String>> ontologyURI2ImportsTree = VitalSigns.get().getOntologyURI2ImportsTree();
        
        while(imports2Check.size() > 0) {
                
            Set<String> newSet = new HashSet<String>();
                
            for(String i : imports2Check) {
                    
                checked.add(i);
                    
                List<String> s = ontologyURI2ImportsTree.get(i);

                if(!imports.contains(i)) {
                    imports.add(i);
                }
                    
                if(s != null) {
                        
                    for(String sub : s) {
                            
                        if(!checked.contains(sub)) {
                            newSet.add(sub);
                        }
                            
                    }
                        
                } else {
                    
                    throw new Exception("Ontology referenced in " + ontologyRes.getURI() + " - " + i + " not found"); 
                    
                }
            }
                
            imports2Check = newSet;
                
        }
            
        ontologyURI2ImportsTree.put(ontologyRes.getURI(), imports);
	            
	        
        //construct the ont model now
        
        for(int i = imports.size() - 1; i >= 0; i--) {
            
            String importURI = imports.get(i);
            Model importModel = VitalSigns.get().getOntologyBaseModel(importURI);
            
            if(importModel == null) throw new Exception("Model for referenced ontology in " + ontologyRes.getURI() + " - " + importURI + " not found");
            
            model.add(importModel);
            
        }
        
        //add this model as last
        model.add(bareModel);
        */
        
        DomainOntology m = new DomainOntology(ontologyRes.getURI(), version, backwardVersion);
        m.setDirectParentOntologies(parentOntologies);
        m.setDefaultPackage(defaultPackage);
        m.setDefaultArtifactId(defaultArtifactId);
        m.setDefaultGroupId(defaultGroupId);
        m.setPreferredImportVersions(RDFUtils.getStringPropertyValues(ontologyRes, VitalCoreOntology.hasPreferredImportVersion));
        return m;
    }
}
