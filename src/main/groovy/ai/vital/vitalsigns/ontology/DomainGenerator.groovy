package ai.vital.vitalsigns.ontology

import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalsigns.VitalManifest;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.groovy.VitalGroovyClassLoader;
import ai.vital.vitalsigns.groovy.VitalSignsTransformation;
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.model.ClassPropertiesHelper;
import ai.vital.vitalsigns.model.DomainOntology;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_Container;

import ai.vital.vitalsigns.model.property.MultiValueProperty;
import ai.vital.vitalsigns.ontology.OntologyProcessor.OntPropertyOrigin;
import ai.vital.vitalsigns.ontology.OntologyProcessor.PropertyDetails;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.properties.PropertyTrait;
import ai.vital.vitalsigns.rdf.RDFUtils;
import ai.vital.vitalsigns.utils.NIOUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import ai.vital.vitalsigns.model.VITAL_Node;



public class DomainGenerator {
	
    private final static Logger log = LoggerFactory.getLogger(DomainGenerator.class);
    
	public static final String PROPERTIES_HELPER = "_PropertiesHelper";

	static Set<String> rootGraphObjectClasses = new HashSet<String>();

	public static Set<String> nonGraphObjectClasses = new HashSet<String>();
	
	static {
		rootGraphObjectClasses.add(VitalCoreOntology.VITAL_Edge.getURI());
		rootGraphObjectClasses.add(VitalCoreOntology.VITAL_HyperEdge.getURI());
		rootGraphObjectClasses.add(VitalCoreOntology.VITAL_HyperNode.getURI());
		rootGraphObjectClasses.add(VitalCoreOntology.VITAL_Node.getURI());
		rootGraphObjectClasses.add(VitalCoreOntology.VITAL_GraphContainerObject.getURI());
		
		rootGraphObjectClasses = Collections.unmodifiableSet(rootGraphObjectClasses);
		
		
		nonGraphObjectClasses.add(VitalCoreOntology.RestrictionAnnotationValue.getURI());
		
		nonGraphObjectClasses = Collections.unmodifiableSet(nonGraphObjectClasses);
		
		
	}
	
	
	private static List<String> directions = Arrays.asList("", "In", "Out");
	
	private static List<String> listVariants = Arrays.asList(
		":",
		"serviceName: '" + String.class.getCanonicalName() + "'",
		"graphContext: '" + GraphContext.class.getCanonicalName() + "'",
		"graphContext: '" + GraphContext.class.getCanonicalName() + "', 'containers...': '" + VITAL_Container.class.getCanonicalName() + "'"
	);
	
	
	private static List<String> edgeVariants = Arrays.asList(
		":", 
		"node2: '" + VITAL_Node.class.getCanonicalName() + "'",
		"graphContext: '" + GraphContext.class.getCanonicalName() + "'",
		"graphContext: '" + GraphContext.class.getCanonicalName() + "', node2: '" + VITAL_Node.class.getCanonicalName() + "'",
		"graphContext: '" + GraphContext.class.getCanonicalName() + "', containers: '" + List.class.getCanonicalName() + "<" + VITAL_Container.class.getCanonicalName() + ">'",
		"graphContext: '" + GraphContext.class.getCanonicalName() + "', containers: '" + List.class.getCanonicalName() + "<" + VITAL_Container.class.getCanonicalName() + 
			">', node2: '" + VITAL_Node.class.getCanonicalName() + "'"
	);
	
	
	private static List<String> hyperVariants = Arrays.asList(
		":", 
		"object2: '" + GraphObject.class.getCanonicalName() + "'",
		"graphContext: '" + GraphContext.class.getCanonicalName() + "'",
		"graphContext: '" + GraphContext.class.getCanonicalName() + "', object2: '" + GraphObject.class.getCanonicalName() + "'",
		"graphContext: '" + GraphContext.class.getCanonicalName() + "', containers: '" + List.class.getCanonicalName() + "<" + VITAL_Container.class.getCanonicalName() + ">'",
		"graphContext: '" + GraphContext.class.getCanonicalName() + "', containers: '" + List.class.getCanonicalName() + "<" + VITAL_Container.class.getCanonicalName() + 
			">', object2: '" + VITAL_Node.class.getCanonicalName() + "'"
	);

	public final static Pattern ontVersionPattern = Pattern.compile("\\d+\\.\\d+\\,\\d+");

	public static final String GROOVY = "GROOVY"; 
	
	OntModel model;
	
	String ontologyURI;
	
	String _package;

	private Path targetSrcLocation;
	
	Map<String, String> thisClasses = new HashMap<String, String>();
	Set<String> thisNodes = new HashSet<String>();
	
	Map<String, List<String>> thisClass2NewProperties = new HashMap<String, List<String>>();
	
	List<OntClass> edgeClasses = new ArrayList<OntClass>();
	
	List<OntClass> hyperEdgeClasses = new ArrayList<OntClass>();
	
	public String ontologyFilename = null;

	public boolean generateOntologyDescriptor = true;
	
	private byte[] ontologyBytes;


	private String ontologyClassCname;

	private String version;
	
	private String backwardCompatibilityVersion;

	private Path basePackagePath; 
	
	private List<OntClass> classesList = new ArrayList<OntClass>();

    public boolean skipDsld = false;

	
	public DomainGenerator(OntModel model, byte[] ontologyBytes, String ontologyURI, String _package, Path targetLocation) throws Exception {
		super();
		this.model = model;
		this.ontologyBytes = ontologyBytes;
		this.ontologyURI = ontologyURI;
		this._package = _package;
		this.targetSrcLocation = targetLocation;
		if( ! Files.isDirectory(targetLocation) ) throw new Exception("target location does not exist or is not a directory: " + targetLocation);
		
		//check domains extension statement
		checkDomainsExtensionStatement();
		
	}
	
	private void checkDomainsExtensionStatement() throws Exception {

	    Model rawModel = ModelFactory.createDefaultModel();
        rawModel.read(new ByteArrayInputStream(ontologyBytes), null);
        
//
	    //remove imports
	    rawModel.removeAll(null, OWL.imports, null);
	    
	    OntModelSpec spec = OntModelSpec.OWL_MEM;
//	      OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);
//	    spec.setDocumentManager(manager);

	    OntModel m = ModelFactory.createOntologyModel(spec, null);
	    m.add(rawModel);
	    
	    for( Resource r : m.listSubjectsWithProperty(RDFS.domain).toList() ) {
	        if(r.getURI() == null) continue;
	        if( RDFUtils.getOntologyPart(r.getURI()).equals(ontologyURI) ) continue;

	        OntProperty ontProperty = m.getOntProperty(r.getURI());
	        
	        if(ontProperty == null) {
	            m.add(r, RDF.type, OWL.DatatypeProperty);
	            ontProperty = m.getOntProperty(r.getURI());
	        }
	        
	        List<String> domains = OntologyProcessor.getPropertyDomains(ontProperty);

	        for(String domain : domains) {
	            
	            if(!RDFUtils.getOntologyPart(domain).equals(ontologyURI)) {
	                throw new Exception("Cannot extend parent ontology property domain to class not from this ontology, property: " + r.getURI() + ", domain class: " + domain);
	            }
	            
	        }
	        
	    }
	    
    }

    public void generateDomainSourceClasses() throws Exception {
		
		Ontology ontology = model.getOntology(ontologyURI);
		
		if(ontology == null) throw new Exception("Ontology not found in the model: " + ontologyURI);

		String versionInfo = RDFUtils.getStringPropertySingleValue(ontology, OWL.versionInfo);
		this.version = versionInfo;
		if(versionInfo == null || versionInfo.isEmpty()) throw new Exception("Ontology owl:versionInfo property expected to be a plain literal or an xsd:string");
		if(!DomainOntology.getVersionPattern().matcher(this.version).matches()) throw new Exception("Ontology owl:versionInfo property must match pattern: " + DomainOntology.getVersionPattern().pattern() + " - " + this.version);
		
		
		this.backwardCompatibilityVersion = RDFUtils.getStringPropertySingleValue(ontology, VitalCoreOntology.hasBackwardCompatibilityVersion);
		if(this.backwardCompatibilityVersion != null) {
		    if(!DomainOntology.getVersionPattern().matcher(this.backwardCompatibilityVersion).matches()) throw new Exception("Ontology vital-core:" + VitalCoreOntology.hasBackwardCompatibilityVersion.getLocalName() + " property must match pattern: " + DomainOntology.getVersionPattern().pattern() + " - " + this.backwardCompatibilityVersion);
		}
		

		String defaultPackage = RDFUtils.getStringPropertySingleValue(ontology, VitalCoreOntology.hasDefaultPackage);
		
		if(_package == null || _package.isEmpty()) {
			if(defaultPackage == null) throw new Exception("Ontology does not provide default package, cannot generate source without a package: " + ontologyURI);
			_package = defaultPackage;
		}
		
		String baseDir = _package.replace('.', '/');
		
		basePackagePath = targetSrcLocation.resolve(baseDir);
		
		Files.createDirectories(basePackagePath);
		
		//TODO package name validator
		
		List<OntClass> classes = OntologyProcessor.listOntologyClassesSorted(model, ontologyURI);
		
		for( OntClass cls : classes ) {
			
			
			if(nonGraphObjectClasses.contains(cls.getURI())) continue;
			
			Path filePath = basePackagePath.resolve(cls.getLocalName() + ".groovy"); 
			
			if(cls.listSuperClasses(true).toList().size() > 1 ) throw new Exception("Class " + cls.getURI() + " cannot have more than 1 direct parent");
			
			List<OntClass> parentClasses = RDFUtils.getClassParents(cls, false);
			
			boolean validGraph = false;
			
			boolean isEdge = false;
			boolean isHyperEdge = false;
			boolean isNode = false;
			
			for(OntClass c : parentClasses) {
				
				if(!c.isURIResource()) throw new Exception("All parent classes must be URI resources: " + cls.getURI());
				
				if(rootGraphObjectClasses.contains(c.getURI())) {
					validGraph = true;
					
					if(VitalCoreOntology.VITAL_Edge.getURI().equals(c.getURI())) {
						isEdge = true;
					}
					
					if(VitalCoreOntology.VITAL_Node.getURI().equals(c.getURI())) {
						isNode = true;
					}
					
					if(VitalCoreOntology.VITAL_HyperEdge.getURI().equals(c.getURI())) {
						isHyperEdge = true;
					}
					
					break;
				}
				
			}
			
			
			if(!validGraph) {
				
				if(rootGraphObjectClasses.contains(cls.getURI())) {
					validGraph = true;
					
//					if(VitalCoreOntology.VITAL_Edge.getURI().equals(c.getURI())) {
//						isEdge = true;
//					}
					
					if(VitalCoreOntology.VITAL_Node.getURI().equals(cls.getURI())) {
						isNode = true;
					}				
					
				}
				
			}
			
			if(!validGraph) throw new Exception("Class " + cls.getURI() + " is not a subclass of one of: " + rootGraphObjectClasses.toString());
			
			classesList.add(cls);

			if(isEdge) {
				edgeClasses.add(cls);
			}
			
			if(isHyperEdge) {
				hyperEdgeClasses.add(cls);
			}
			
			//imports ? 
			OntClass parentCls = cls.getSuperClass();
			
			String parentCName = null;
			
			if(rootGraphObjectClasses.contains(cls.getURI())) {
				
				//core case
				parentCName = GraphObject.class.getCanonicalName();
				
			} else {
				
				
				if(thisClasses.containsKey(parentCls.getURI())) {
					
					parentCName = parentCls.getLocalName();
					
				} else {
					
					ClassMetadata class1 = VitalSigns.get().getClassesRegistry().getClass(parentCls.getURI());
					
					if(class1 == null) throw new Exception("Parent class with URI not found (not registered in VitalSigns): " + parentCls.getURI() + ", class: " + cls.getURI());
					
					parentCName = class1.getClazz().getCanonicalName();
					
				}
				
			}
			
			thisClasses.put(cls.getURI(), _package + '.' + cls.getLocalName());
			
			if(isNode) thisNodes.add(cls.getURI());
			
			
			//another exception, don't generate 
			
			if( VitalCoreOntology.VITAL_GraphContainerObject.getURI().equals( cls.getURI() ) ) {
				continue;
			}
			
			String c = "package " + _package + "\n\n";
			
			c += ( "class " + cls.getLocalName() + (parentCName != null ? ( " extends " + parentCName ) : "" ) + " {\n\n" );
			
			c += ( "\tprivate static final long serialVersionUID = 1L;\n\n" );
			
			c += ( "\n" );
			
			c += ( "\tpublic " + cls.getLocalName() + "() {\n" );
			c += ( "\t\tsuper()\n" );
			c += ( "\t}\n" );
			
			c += ( "\n" );

			c += ( "\tpublic " + cls.getLocalName() + "(Map<String, Object> props) {\n" );
			c += ( "\t\tsuper(props)\n" );
			c += ( "\t}\n" );
			
			c += ( "\n" );
			
			c += "}\n";
			
			Files.write(filePath, c.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
			
		}

		
		if(generateOntologyDescriptor) {
			
			
			
			//write ontology class
			Path ontPackagePath = basePackagePath.resolve(RDFUtils.ONTOLOGY_PACKAGE);
			
			Files.createDirectories(ontPackagePath);
			
			String ontFName = ontologyFilename != null ? ontologyFilename : "ontology.owl";
			
			Path ontFile = ontPackagePath.resolve(ontFName);
			
			Files.write(ontFile, ontologyBytes, StandardOpenOption.CREATE);
			
			
			String ontClassName = "ai.vital.domain".equals(_package) ? "VitalOntology" : "Ontology";
			
			this.ontologyClassCname = _package + "." + RDFUtils.ONTOLOGY_PACKAGE + '.' + ontClassName;
			//create descriptor class
			Path descFile = ontPackagePath.resolve(ontClassName + ".groovy");
			
			String c = ( "package " + _package + "." + RDFUtils.ONTOLOGY_PACKAGE + "\n\n" );
			
			c += ( "import " + InputStream.class.getCanonicalName() + "\n" );
			
			c += ( "import " + ExtendedOntologyDescriptor.class.getCanonicalName() + "\n" );
			
			c += "\n";
			
			c += ( "class " + ontClassName + " implements " + ExtendedOntologyDescriptor.class.getSimpleName() + " {\n" );
	
			c += "\n";
			
			c += ( "\tpublic final static String ONTOLOGY_IRI = '" + ontologyURI + "'\n" );
			
			c += "\n";
			
			c += ( "\tpublic final static String NS = ONTOLOGY_IRI + '#'\n" );
			
			c += "\n";
			
			c += ( "\tpublic final static String PACKAGE = '" + _package + "'\n" );
				
			c += "\n";
			
			c += ( "\t@Override\n" );
			c += ( "\tpublic String getVitalSignsVersion() {\n");
			c += ( "\t\treturn \"" + VitalSigns.VERSION + "\";\n");
			c += ( "\t}\n" );
			        
			c += "\n";
			
			c += ( "\t@Override\n" );
			
			c += ( "\tpublic InputStream getOwlInputStream() {\n" );
			
			c += ( "\t\treturn this.getClass().getResourceAsStream('" + ontFName + "')\n" );
			
			c += ( "\t}" );
	
			c += "\n";
			
			c += ( "\t@Override\n" );
			
			c += ( "\tpublic String getOntologyIRI() {\n");
			
			c += ( "\t\treturn ONTOLOGY_IRI\n" );
			
			c += ( "\t}\n" );
	
			c += "\n";
	
			c += ( "\t@Override\n" );
			
			c += ("\tpublic String getPackage() {\n" );
			
			c += ( "\t\treturn PACKAGE\n" );
			
			c += ("\t}" );
	
			c += "\n";
	
			c += "}\n";
	
			Files.write(descFile, c.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
			
		}	
		
	}
	
	
	static class EdgeAccessData {
		
		public String singleNameUC;
		
		public String pluralNameUC;
		
		public boolean sourceNotDestination;

		private boolean bothSrcAndDest;

		public EdgeAccessData(String singleNameUC, String pluralNameUC,
				boolean sourceNotDestination, boolean bothSrcAndDest) {
			super();
			this.singleNameUC = singleNameUC;
			this.pluralNameUC = pluralNameUC;
			this.sourceNotDestination = sourceNotDestination;
			this.bothSrcAndDest = bothSrcAndDest; 
		}
		
	}
	
	static class HyperEdgeAccessData {
		
		public String singleNameUC;
		
		public String pluralNameUC;
		
		public boolean sourceNotDestination;

		private boolean bothSrcAndDest;

		public HyperEdgeAccessData(String singleNameUC, String pluralNameUC,
				boolean sourceNotDestination, boolean bothSrcAndDest) {
			super();
			this.singleNameUC = singleNameUC;
			this.pluralNameUC = pluralNameUC;
			this.sourceNotDestination = sourceNotDestination;
			this.bothSrcAndDest = bothSrcAndDest; 
		}
		
	}
	
	public void generateDomainSourceProperties() throws Exception {
		
		//now properties set
	    log.info("Listing properties...");
		List<OntPropertyOrigin> properties = OntologyProcessor.listOntologyPropertiesSorted(model, ontologyURI, false);
		log.info("Properties count: {}", properties.size());
		Map<String, OntProperty> thisProperties = new HashMap<String, OntProperty>();
		
		//class to 
		Map<String, List<PropertyDetails>> className2Properties = new HashMap<String, List<PropertyDetails>>();
		
		//classes augmented with new properties
		Set<String> otherClassNames = new HashSet<String>();
		
		List<String> propertyTraitsList = new ArrayList<String>();
		
		for(OntPropertyOrigin propertyO : properties ) {
			
		    OntProperty property = propertyO.property;
		    
		    //implement DSLD for property domain extension
		    if(! propertyO.ontologyURI.equals(ontologyURI)) {

		        PropertyDetails details = OntologyProcessor.getPropertyDetails(property, null);

		        for(String domanClassURI : details.domainClassesURIs) {
	             
		            String clsName =  thisClasses.get(domanClassURI);
		            
		            if(clsName == null) continue;
		            
		            List<PropertyDetails> list = className2Properties.get(clsName);
		            if(list == null) {
		                list = new ArrayList<PropertyDetails>();
		                className2Properties.put(clsName, list);
		            }
		            list.add(details);
		            
		        }
		        
		        continue;
		        
		    }
		    
			if(VitalCoreOntology.internalProperties.contains(property.getURI())) continue;
			
			List<? extends OntProperty> parents = property.listSuperProperties(true).toList();
			if(parents.size() > 1) {
				throw new Exception("Property " + property.getURI() + " must have 0 or 1 parent properties, got: " + parents.size() + " - " + parents);
			}
			
			PropertyDetails details = OntologyProcessor.getPropertyDetails(property, null);
			
			thisProperties.put(property.getURI(), property);
			
			String pPackage = _package + '.' + RDFUtils.PROPERTIES_PACKAGE;  
			
			String traitName = "Property_" + property.getLocalName();

			Path propsPath = targetSrcLocation.resolve(pPackage.replace('.', '/'));
			Files.createDirectories(propsPath);
			
			Path filePath = propsPath.resolve( traitName + ".groovy" );
			
			String c = "package " + pPackage + "\n\n";
			
			
			OntProperty superProperty = property.getSuperProperty();
			
			String parentName = null;
			
			if(superProperty != null) {
				
				if(!superProperty.isURIResource()) throw new Exception("parent property must be a uri resource: " + superProperty + ", " + property.getURI());
				
				if(thisProperties.containsKey(superProperty.getURI())) {
					
					parentName = "Property_" + superProperty.getLocalName();
					
				} else {
					
					PropertyMetadata pmd = VitalSigns.get().getPropertiesRegistry().getProperty(superProperty.getURI());
					if(pmd == null) throw new Exception("Parent property not found (not registered in VitalSigns): " + superProperty.getURI() + ", " + property.getURI());
					
					parentName = pmd.getTraitClass().getCanonicalName();
					
				}
				
			} else {
			
				parentName = PropertyTrait.class.getCanonicalName();
				
			}
			
			c += ( "trait " + traitName + " extends " + parentName + " {\n\n" );
			
			c += ("\tpublic String getURI() {\n");
			
			c += ("\t\treturn '" + property.getURI() + "'\n");
			
			c += ("\t}\n");
			
			
			c += "\n}";
			
			
			Files.write(filePath, c.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
			
			
			propertyTraitsList.add(pPackage + '.' + traitName);
			
			
			for(String domainURI : details.domainClassesURIs) {
				
				String clsName = null;
				
				if(thisClasses.containsKey(domainURI)) {
					
					clsName = thisClasses.get(domainURI);
					
				} else {
					
					ClassMetadata cls = VitalSigns.get().getClassesRegistry().getClass(domainURI);
					
					if(cls == null) throw new Exception("Class not found for URI: " + domainURI + " (domain of " + property.getURI() + ")");

					clsName = cls.getClazz().getCanonicalName();
					
					otherClassNames.add(clsName);
					
					
				}
				
				List<PropertyDetails> list = className2Properties.get(clsName);
				if(list == null) {
					list = new ArrayList<PropertyDetails>();
					className2Properties.put(clsName, list);
				}
				list.add(details);

			}
			
//			trait Property_hasName extends PropertyTrait {
//
//				public String getURI(){
//					return TestOntology.NS + 'hasName'
//				}
//				
////				public boolean equals(Object obj) {
////					return super.equals(obj);
////				}
//				
//				
//			}
			
		}
		
		Map<String, List<EdgeAccessData>> edgesAccess = new HashMap<String, List<EdgeAccessData>>();
		
		
		
		
		
		for(OntClass edgeClass : edgeClasses) {
			
			if( OntologyProcessor.coreEdgeTypes.contains(edgeClass.getURI()) ) continue;
			
			//check source and domain
			String[] singleAndPlural = OntologyProcessor.getEdgeLocalNameAndPlural(edgeClass);
			
			List<Statement> srcStmts = edgeClass.listProperties(VitalCoreOntology.hasEdgeSrcDomain).toList();
			List<Statement> destStmts = edgeClass.listProperties(VitalCoreOntology.hasEdgeDestDomain).toList();
			
			if(srcStmts.size() < 1) throw new Exception("Edge class " + edgeClass.getURI() + " has no source domain properties");
			if(destStmts.size() < 1) throw new Exception("Edge class " + edgeClass.getURI() + " has no dest domain properties");

			
			String singleUC = singleAndPlural[0];
			String pluralUC = singleAndPlural[1];

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
				
				String srcClassName = null;
						
				ClassMetadata srcClass = VitalSigns.get().getClassesRegistry().getClass(srcURI);
				
				if(srcClass != null) {
					srcClassName = srcClass.getClazz().getCanonicalName();
					if(! VITAL_Node.class.isAssignableFrom( srcClass.getClazz() ) ) throw new Exception("Edge " + edgeClass.getURI() + " source domain class is not a subclass of VITAL_Node: " + srcClass.getClazz().getCanonicalName());
				} else {
					srcClassName = thisClasses.get(srcURI);
					if(srcClassName != null) {
						if(!thisNodes.contains(srcURI)) throw new Exception("Edge " + edgeClass.getURI() + " source domain is not a VITAL_Node: " + srcURI);
					}
				}

				if(srcClassName == null) throw new Exception("Edge " + edgeClass.getURI() + " source domain class not found in VitalSigns nor this ontology " + srcURI);
				

				List<EdgeAccessData> list = edgesAccess.get(srcClassName);
				if(list == null) {
					list = new ArrayList<EdgeAccessData>();
					edgesAccess.put(srcClassName, list);
				}
				
				boolean bothSrcAndDest = sourceTypeURI.contains(srcURI) && destTypeURI.contains(srcURI);
				
				list.add(new EdgeAccessData(singleUC, pluralUC, true, bothSrcAndDest));
				
			}
				
			for(Statement destStmt : destStmts) {
					
				RDFNode destObject = destStmt.getObject();
					
				if(!destObject.isURIResource()) throw new Exception("Edge " + edgeClass.getURI() + " destination domain must be a URI resource!");
					
				String destURI = destObject.asResource().getURI();

				String destClassName = null;
				
				ClassMetadata destClass = VitalSigns.get().getClassesRegistry().getClass(destURI);
				
				if(destClass != null) {
					destClassName = destClass.getClazz().getCanonicalName();
					if(! VITAL_Node.class.isAssignableFrom( destClass.getClazz() ) ) throw new Exception("Edge " + edgeClass.getURI() + " destination domain class is not a subclass of VITAL_Node: " + destClass.getClazz().getCanonicalName());
				} else {
					destClassName = thisClasses.get(destURI);
					if(destClassName != null) {
						if(!thisNodes.contains(destURI)) throw new Exception("Edge " + edgeClass.getURI() + " destination domain is not a VITAL_Node: " + destURI); 
					}
				}
				
				if(destClassName == null) throw new Exception("Edge " + edgeClass.getURI() + " destination domain class not found in VitalSigns not this ontology " + destURI);
			
				
				List<EdgeAccessData> list = edgesAccess.get(destClassName);
				if(list == null) {
					list = new ArrayList<EdgeAccessData>();
					edgesAccess.put(destClassName, list);
				}
				
				boolean bothSrcAndDest = sourceTypeURI.contains(destURI) && destTypeURI.contains(destURI);
				
				list.add(new EdgeAccessData(singleUC, pluralUC, false, bothSrcAndDest));

			}
			
		}
		
		
		
		Map<String, List<HyperEdgeAccessData>> hyperEdgesAccess = new HashMap<String, List<HyperEdgeAccessData>>();
		
		for(OntClass hyperEdgeClass : hyperEdgeClasses) {
			
			if( OntologyProcessor.coreHyperEdgeTypes.contains(hyperEdgeClass.getURI()) ) continue;
			
			//check source and domain
			String[] singleAndPlural = OntologyProcessor.getHyperEdgeLocalNameAndPlural(hyperEdgeClass);
			
			List<Statement> srcStmts = hyperEdgeClass.listProperties(VitalCoreOntology.hasHyperEdgeSrcDomain).toList();
			List<Statement> destStmts = hyperEdgeClass.listProperties(VitalCoreOntology.hasHyperEdgeDestDomain).toList();
			
			if(srcStmts.size() < 1) throw new Exception("HyperEdge class " + hyperEdgeClass.getURI() + " has no source domain properties");
			if(destStmts.size() < 1) throw new Exception("HyperEdge class " + hyperEdgeClass.getURI() + " has no dest domain properties");

			
			String singleUC = singleAndPlural[0];
			String pluralUC = singleAndPlural[1];

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
				
				String srcClassName = null;
						
				ClassMetadata srcClass = VitalSigns.get().getClassesRegistry().getClass(srcURI);
				
				if(srcClass != null) {
					srcClassName = srcClass.getClazz().getCanonicalName();
				} else {
					srcClassName = thisClasses.get(srcURI);
				}

				if(srcClassName == null) throw new Exception("HyperEdge " + hyperEdgeClass.getURI() + " source domain class not found in VitalSigns nor this ontology " + srcURI);
				

				List<HyperEdgeAccessData> list = hyperEdgesAccess.get(srcClassName);
				if(list == null) {
					list = new ArrayList<HyperEdgeAccessData>();
					hyperEdgesAccess.put(srcClassName, list);
				}
				
				boolean bothSrcAndDest = sourceTypeURI.contains(srcURI) && destTypeURI.contains(srcURI);
				
				list.add(new HyperEdgeAccessData(singleUC, pluralUC, true, bothSrcAndDest));
				
			}
				
			for(Statement destStmt : destStmts) {
					
				RDFNode destObject = destStmt.getObject();
					
				if(!destObject.isURIResource()) throw new Exception("HyperEdge " + hyperEdgeClass.getURI() + " destination domain must be a URI resource!");
					
				String destURI = destObject.asResource().getURI();

				String destClassName = null;
				
				ClassMetadata destClass = VitalSigns.get().getClassesRegistry().getClass(destURI);
				
				if(destClass != null) {
					destClassName = destClass.getClazz().getCanonicalName();
				} else {
					destClassName = thisClasses.get(destURI);
				}
				
				if(destClassName == null) throw new Exception("HyperEdge " + hyperEdgeClass.getURI() + " destination domain class not found in VitalSigns not this ontology " + destURI);
			
				
				List<HyperEdgeAccessData> list = hyperEdgesAccess.get(destClassName);
				if(list == null) {
					list = new ArrayList<HyperEdgeAccessData>();
					hyperEdgesAccess.put(destClassName, list);
				}
				
				boolean bothSrcAndDest = sourceTypeURI.contains(destURI) && destTypeURI.contains(destURI);
				
				list.add(new HyperEdgeAccessData(singleUC, pluralUC, false, bothSrcAndDest));

			}
			
		}


		//generate source files for helpers
		
		
		//dsld optional
		//always generate the dsld now as it will contain properties helper class
		if(!skipDsld/* || classProperties.size() > 0 || edgesAccess.size() > 0 || hyperEdgesAccess.size() > 0*/)  {
			
		    log.info("Generating DSLD file");
		    
		    StringBuilder cb = new StringBuilder("package dsld;\n");
//			String c = "package dsld;\n";

			Path dsldPackage = targetSrcLocation.resolve("dsld");
			
			Files.createDirectories(dsldPackage);

			Path dsldFile = dsldPackage.resolve(_package + ".dsld");
			
			//sort keys
			Set<String> allClassNames = new HashSet<String>(thisClasses.values());
			allClassNames.addAll(className2Properties.keySet());
			allClassNames.addAll(edgesAccess.keySet());
			allClassNames.addAll(hyperEdgesAccess.keySet());
			
			List<String> classes = new ArrayList<String>(allClassNames);
			Collections.sort(classes, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return o1.compareToIgnoreCase(o2);
				}
			});
			
			for(String cls : classes) {
				
				cb.append("\n");

				cb.append("contribute(currentType(subType(\"" + cls + "\"))) {\n");
					
				cb.append("\n");
	
				cb.append("\tmethod name:\"props\", isStatic:true, type: \"" + cls + PROPERTIES_HELPER + "\", params : [:]\n");
				
				cb.append("\n");
				
				List<PropertyDetails> propertiesList = className2Properties.get(cls);
				if(propertiesList == null) propertiesList = new ArrayList<PropertyDetails>();
				
				Collections.sort(propertiesList, new Comparator<PropertyDetails>() {

					@Override
					public int compare(PropertyDetails o1, PropertyDetails o2) {
						return o1.shortName.compareToIgnoreCase(o2.shortName);
					}
				});
				
				for(PropertyDetails d : propertiesList) {

					String ptype = null;
					
					if(d.multipleValues) { 
						
						ptype = MultiValueProperty.class.getCanonicalName()+"<" + d.pClass.getCanonicalName() + ">";
						
						
					} else {
						
						ptype = d.pClass.getCanonicalName();
						
					}
				
				
					
					cb.append( "\tproperty name: \"" + d.shortName + "\", type: \"" + ptype + "\"\n" );
					
					cb.append("\n");
					
//					property name: "nameInt", type: "ai.vital.vitalsigns.model.property.IntegerProperty"
//
//						property name: "date", type: "ai.vital.vitalsigns.model.property.DateProperty"

				}
				
				List<EdgeAccessData> accessList = edgesAccess.get(cls);
				if(accessList == null) accessList = new ArrayList<EdgeAccessData>();
				
				Collections.sort(accessList, new Comparator<EdgeAccessData>() {

					@Override
					public int compare(EdgeAccessData o1, EdgeAccessData o2) {
						int c = o1.singleNameUC.compareTo(o2.singleNameUC);
						if(c != 0) return c;
						return new Boolean(o1.sourceNotDestination).compareTo(new Boolean(o2.sourceNotDestination));
					}
				});

				List<HyperEdgeAccessData> hyperAccessList = hyperEdgesAccess.get(cls);
				if(hyperAccessList == null) hyperAccessList = new ArrayList<HyperEdgeAccessData>();
				
				Collections.sort(hyperAccessList, new Comparator<HyperEdgeAccessData>(){

					@Override
					public int compare(HyperEdgeAccessData o1,
							HyperEdgeAccessData o2) {
						int c = o1.singleNameUC.compareTo(o2.singleNameUC);
						if(c != 0) return c;
						return new Boolean(o1.sourceNotDestination).compareTo(new Boolean(o2.sourceNotDestination));
					}
					
				});
				
				cb.append("\n");
				
				for(EdgeAccessData e : accessList) {

					for(String variant : listVariants) {
						
					    cb.append("\tmethod name:\"get" + e.pluralNameUC + (e.sourceNotDestination ? "" : "Reverse") + "\", type: \"java.util.List\", params : [ " + variant + " ]\n");
					    cb.append("\n");
					}
					
					
					for(String direction : directions) {
						
						if(direction.equals("")) {
							
							if(!e.sourceNotDestination && e.bothSrcAndDest) {
								//skip for destination domain if it's both
								continue;
							}
							
						} else if(direction.equals("In")) {
							
							if( e.sourceNotDestination) continue;
							
						} else if(direction.equalsIgnoreCase("Out")) {
							
							if( ! e.sourceNotDestination) continue;
							
						}
						
						for(String variant : edgeVariants) {
							
						    cb.append("\tmethod name:\"get" + e.singleNameUC + "Edges" + direction +"\", type: \"java.util.List\", params : [ " + variant + " ]\n");
							
						    cb.append("\n");
							
						}
						
					}
					
				}
				
				
				for(HyperEdgeAccessData e : hyperAccessList) {
					
					for(String variant : listVariants) {
						
					    cb.append("\tmethod name:\"get" + e.pluralNameUC + (e.sourceNotDestination ? "" : "Reverse") + "\", type: \"java.util.List\", params : [ " + variant + " ]\n");
					    cb.append("\n");
					}
					
					
					for(String direction : directions) {
						
						if(direction.equals("")) {
							
							if(!e.sourceNotDestination && e.bothSrcAndDest) {
								//skip for destination domain if it's both
								continue;
							}
							
						} else if(direction.equals("In")) {
							
							if( e.sourceNotDestination) continue;
							
						} else if(direction.equalsIgnoreCase("Out")) {
							
							if( ! e.sourceNotDestination) continue;
							
						}
						
						for(String variant : hyperVariants) {
							
						    cb.append("\tmethod name:\"get" + e.singleNameUC + "HyperEdges" + direction +"\", type: \"" + List.class.getCanonicalName() + "\", params : [ " + variant + " ]\n");
							
						    cb.append("\n");
							
						}
						
					}
					
				}
				
				cb.append("}\n");
				
				
                //properties helper additional properties 
                if(otherClassNames.contains(cls)) {
                    
                    cb.append("\n");
                    
                    cb.append("contribute(currentType(subType(\"" + cls + PROPERTIES_HELPER + "\"))) {\n");
                    
                    for(PropertyDetails d : propertiesList) {

                        cb.append("\n");
                        
                        cb.append( "\tproperty name: \"" + d.shortName + "\", type: \"" + VitalGraphQueryPropertyCriterion.class.getCanonicalName() + "\"\n" );
                        
                        cb.append("\n");

                    }
                    
                    cb.append("}\n");
                    
                }
				
			}
			
			cb.append("\n");
			
			for(String propertyTraitClass : propertyTraitsList) {
			    
			    cb.append("\n");
			    
			    cb.append("contribute(currentType(subType(\"" + propertyTraitClass + "\"))) {\n");

			    cb.append("\n");
			    
			    cb.append("\tproperty name:\"query\", isStatic:true, type: \"" + VitalGraphQueryPropertyCriterion.class.getCanonicalName() + "\"\n");

			    cb.append("\n");
			    
			    cb.append("}\n");
			    
			}
			
			Files.write(dsldFile, cb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
			
		}
		
		if(basePackagePath == null) throw new Exception("No base package path set!");
		
		log.info("Generating source classes files");
		
		for(OntClass cls : classesList) {
			
			Path helperPath = basePackagePath.resolve(cls.getLocalName() + PROPERTIES_HELPER + ".groovy");
			
			String clsCName = thisClasses.get(cls.getURI());
			
			
			//collect all properties shortnames
			List<PropertyDetails> list = className2Properties.get(clsCName);
			
			Set<String> shortNames = new HashSet<String>();
			
			if(list != null) {
				for(PropertyDetails d : list) {
					if( ! shortNames.add(d.shortName) ) throw new RuntimeException("Property with short name used more than once for class: " + clsCName + " property " + d.shortName);
				}
			}

			
			//from now on only register direct properties
            List<OntClass> superClasses = new ArrayList<OntClass>();
            RDFUtils.collectAllSuperClasses(cls, superClasses, null);
            
            
            String parentHelperClass = null;
            
            if(superClasses.size() > 0) {
                OntClass superClass = superClasses.get(0);
                String parentURI = superClass.getURI();
                String parentCName = thisClasses.get(parentURI);
                if(parentCName == null) {
                    //look for existing class
                    ClassMetadata class1 = VitalSigns.get().getClassesRegistry().getClass(parentURI);
                    if(class1 != null) {
                        ClassPropertiesHelper helper = VitalSigns.get().getPropertiesHelper(class1.getClazz());
                        parentHelperClass = helper.getClass().getCanonicalName();
                    } else {
                        parentHelperClass = ClassPropertiesHelper.class.getCanonicalName();
                    }
                    
                } else {
                    parentHelperClass = parentCName + PROPERTIES_HELPER;
                }
                
            } else {
                parentHelperClass = ClassPropertiesHelper.class.getCanonicalName();
            }
            
			/*
			//list all subclasses
			List<OntClass> superClasses = new ArrayList<OntClass>();
			RDFUtils.collectAllSuperClasses(cls, superClasses, null);
			
			Class<? extends GraphObject> firstRegisteredParent = null;
			
			
			//we are only interested in direct properties
			
			for(OntClass sc : superClasses) {
				
				if(thisClasses.containsKey(sc.getURI())) {
					String parentClsCName = thisClasses.get(sc.getURI());
					List<PropertyDetails> parentProps = className2Properties.get(parentClsCName);
					if(parentProps != null) {
						for(PropertyDetails d : parentProps) {
							if( ! shortNames.add(d.shortName) ) throw new RuntimeException("Property with short name used more than once for class: " + clsCName + " property " + d.shortName);
						}
					}
					
				} else {
					
					ClassMetadata class1 = VitalSigns.get().getClassesRegistry().getClass(sc.getURI());
					if(class1 == null) throw new RuntimeException("Parent class of " + clsCName + " not registered in VitalSigns - URI: " + sc.getURI());
					firstRegisteredParent = class1.getClazz();
					break;
				}
				
			}
			
			//vital-core may not have registe
			if(firstRegisteredParent != null) {
				
				List<PropertyMetadata> pms = VitalSigns.get().getPropertiesRegistry().getClassProperties(firstRegisteredParent);
				if(pms == null) throw new Exception("No class properties found: " + firstRegisteredParent.getCanonicalName());
				
				for(PropertyMetadata pm : pms) {
					
					if( ! shortNames.add(pm.getShortName())) throw new RuntimeException("Property with short name used more than once for class: " + clsCName + " property " + pm.getShortName());
					
				}
				
			}
			*/
			
			List<String> shortNamesSorted = new ArrayList<String>(shortNames);
			Collections.sort(shortNamesSorted, new Comparator<String>(){

				@Override
				public int compare(String arg0, String arg1) {
					return arg0.compareTo(arg1);
				}
				
			});
			
			String h = "package " + _package + "\n\n";
			
			h += ("import " + VitalGraphQueryPropertyCriterion.class.getCanonicalName() + "\n\n");
			
			h += ("class " + cls.getLocalName() + PROPERTIES_HELPER + " extends " + parentHelperClass + /*ClassPropertiesHelper.class.getCanonicalName()*/ " {\n\n");
			
			//default constructor
			h += ("\tpublic " + cls.getLocalName() + PROPERTIES_HELPER + "() {\n");
			
			h += ("\t\tsuper('" + cls.getURI() + "');\n");
			
			h += "\t}\n\n";
			
			//protected constructor for inheritance
			
			h += ("\tprotected " + cls.getLocalName() + PROPERTIES_HELPER + "(String propertyURI) {\n");
			
			h += ("\t\tsuper(propertyURI);\n");
			
			h += "\t}\n\n";
			
			for(String shName : shortNamesSorted) {
				
				h += "\n";
				
				String getterName = RDFUtils.getGetterName(shName);
				
				h += ("\tpublic " + VitalGraphQueryPropertyCriterion.class.getSimpleName() + " " + getterName + "() {\n");
				h += ("\t\treturn _implementation(\"" + shName + "\");\n");
				h += ("\t}\n\n");

				
			}
			
			
			h += "}\n";
			
			Files.write(helperPath, h.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
			
		}
		
	}
	
	public void compileSource(Path targetDir) throws Exception {
		
		VitalGroovyClassLoader gcl = null;

		try {
		
		File targetClsDir = new File("IAmADummyFile");

		// Inspect
		CompilerConfiguration cc = new CompilerConfiguration();
		
		Set<String> disabledGlobalASTTransformations = cc.getDisabledGlobalASTTransformations();
		if(disabledGlobalASTTransformations == null) {
		    disabledGlobalASTTransformations = new HashSet<String>();
		} else {
		    disabledGlobalASTTransformations = new HashSet<String>(disabledGlobalASTTransformations);
		}
		
		//disable VitalSignsTransformer
		disabledGlobalASTTransformations.add(VitalSignsTransformation.class.getCanonicalName());
		cc.setDisabledGlobalASTTransformations(disabledGlobalASTTransformations);;
		
			//XXX invoke dynamic
	//		cc.getOptimizationOptions().put("indy", true);
	//		cc.getOptimizationOptions().put("int", false);
		//the target directory has to be set in order to enter output phase, the file system is not touched
		cc.setTargetDirectory(targetClsDir);
			
			
		gcl = new VitalGroovyClassLoader(Thread.currentThread().getContextClassLoader(), cc);
		gcl.setOutputPath(targetDir);
			
			
		//inspect the source files and compile them
			
		final List<Path> srcFiles = new ArrayList<Path>();
		
		log.info("Collecting source files");
			
		Files.walkFileTree(targetSrcLocation, new FileVisitor<Path>(){
				
			public java.nio.file.FileVisitResult postVisitDirectory(Path dirPath, IOException exc) {
				return java.nio.file.FileVisitResult.CONTINUE;
			}
				 
			public java.nio.file.FileVisitResult preVisitDirectory(Path dirPath, BasicFileAttributes attributes) {
				return java.nio.file.FileVisitResult.CONTINUE;
			}
				 
			public java.nio.file.FileVisitResult visitFile(Path path, BasicFileAttributes attributes) {
				if( path.toUri().toString().endsWith(".groovy") ) {
					srcFiles.add(path);
				}
				return java.nio.file.FileVisitResult.CONTINUE;
			}
				
			public java.nio.file.FileVisitResult visitFileFailed(Path path, IOException exc) {
				return java.nio.file.FileVisitResult.CONTINUE;
			}

			
		});
			
		// we need to compile the classes conditionally, in n runs, otherwise it will get ha
	
		int pass = 0;
			
		Exception lastException = null;
		
		int total = srcFiles.size();
		
		log.info("Source files count: {}", total);
		
		int c = 0;
		
		
		while(pass < 1000 && srcFiles.size() > 0) {
				
			pass++;
			
			for(Iterator<Path> fIter = srcFiles.iterator(); fIter.hasNext(); ) {
					
				Path f = fIter.next();
				
//				String fn = f.getFileName().toString();
				
				String groovyCode = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
					
					// println("Compiling " + f.getAbsolutePath());
					
				try {
						
					gcl.parseClass(groovyCode, f.toString());
						
					fIter.remove();
		
					c++;
					
					if(c % 1000 == 0) {
					   log.info("Compiled {} of {}", c, total); 
					}
					
				} catch(Exception e) {
					lastException = e;
					//println("File skipped for next pass: " + f.getAbsolutePath() + " reason: " + e.localizedMessage);
				}
				
			}
				
		}
			
		if(pass == 1000) throw new Exception("Error: 1000 passes reached - last compiler exception message: " + (lastException != null ? lastException.getLocalizedMessage() : "(null)" ));
		
		log.info("All source files compiled");
		
		String _owlFileName = ontologyFilename;
		if(_owlFileName == null ) _owlFileName = "ontology.owl";
		
		Path clsOntologyPackage = targetDir.resolve( _package.replace('.', '/') + '/' + "ontology" );
		
		Files.createDirectories(clsOntologyPackage);
		
		Files.write(clsOntologyPackage.resolve(_owlFileName), ontologyBytes, StandardOpenOption.CREATE);
		
		//copy .dsld file if exists
		Path dsldDir = targetSrcLocation.resolve("dsld");
			
		if( Files.isDirectory(dsldDir) ) {
			
			NIOUtils.copyDirectoryToPath(dsldDir, targetDir);
				
		}
			
		
		//services file moved to compilation ?
		
		//first entry is the service description one
		Path servicesDir = targetDir.resolve("META-INF/services/");
		Files.createDirectories(servicesDir);
		Path serviceFile = servicesDir.resolve(OntologyDescriptor.class.getCanonicalName());
		
		Files.write(serviceFile, this.ontologyClassCname.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
		
//		
//		jos.putNextEntry(serviceEntry);
//		
//		IOUtils.write(this.ontClsCanonicalName, jos);
		
		} finally {
			IOUtils.closeQuietly(gcl);
		}
		
	}
	
	public void generateJar(Path basePath, Path jarPath) throws IOException {
		
		
		// log.info("Ontology version: " + version);
		
		Manifest manifest = new Manifest();

		Attributes a = manifest.getMainAttributes();
		a.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		a.putValue(VitalManifest.VITAL_ONTOLOGY_URI, ontologyURI);
		a.putValue(VitalManifest.VITAL_ONTOLOGY_VERSION, version);
		a.putValue(VitalManifest.VITAL_VITALSIGNS_VERSION, VitalSigns.VERSION);
		if(this.backwardCompatibilityVersion != null) {
		    a.putValue(VitalManifest.VITAL_BACKWARD_COMPATIBLE_VERSION, this.backwardCompatibilityVersion);
		}
		JarOutputStream jos = null;
		OutputStream newOutputStream = null;
		try {

			Files.deleteIfExists(jarPath);
			
			newOutputStream = Files.newOutputStream(jarPath, StandardOpenOption.CREATE);
			jos = new JarOutputStream( newOutputStream, manifest); 			
		
			List<Path> allFiles = NIOUtils.listFilesRecursively(basePath);
			
			// Collection<File> allFiles = FileUtils.listFiles(targetClsDir, null, true);

			for(Path f : allFiles) {
				
				String path = basePath.relativize(f).toString();
				
				JarEntry jarEntry = new JarEntry(path.replace('\\', '/'));
				
				jos.putNextEntry(jarEntry);
				
				Files.copy(f, jos);
				
			}

		} finally {
			IOUtils.closeQuietly(jos);				
			IOUtils.closeQuietly(newOutputStream);				
		}
		
	}

	public String getOntologyURI() {
		return ontologyURI;
	}
	
}
