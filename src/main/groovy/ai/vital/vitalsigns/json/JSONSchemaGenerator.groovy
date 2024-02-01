package ai.vital.vitalsigns.json

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.impl.Indenter;
import org.codehaus.jackson.util.DefaultPrettyPrinter.FixedSpaceIndenter;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.ontology.OntologyProcessor;
import ai.vital.vitalsigns.ontology.OntologyProcessor.PropertyDetails;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.rdf.RDFUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * This generates json schema for all classes, current VitalSigns singleton is used
 *
 */

public class JSONSchemaGenerator {

	private final static Logger log = LoggerFactory.getLogger(JSONSchemaGenerator.class);
	
	static ObjectMapper mapper = new ObjectMapper();
	
	
	private OntModel model = null;
	
	private String ontologyURI = null;
	
	//should be set for validation
	private String domainOWLHash = null;
	private String version = null;
	
	//list of schemas for all classes
	private List<LinkedHashMap<String, Object>> schemas;
	
	private List<LinkedHashMap<String, Object>> properties;
	
	public JSONSchemaGenerator(OntModel model, String ontURI) {
		this.model = model;
		this.ontologyURI = ontURI;
	}

    static Set<String> unwanted = new HashSet<String>(
            Arrays.asList(
            /*
        "defaultLabel",
        "dontIndexFlag",
        "inferenceProcessflowStatus",
        "inferenceProcessflowTimestamp",
        "integratorProcessflowStatus",
        "integratorProcessflowTimestamp",
        "loggerProcessflowStatus",
        "loggerProcessflowTimestamp",
        "nlpProcessflowStatus",
        "nlpProcessflowTimestamp",
        "multipleValues",
        "nocacheFlag",
        "predictProcessflowStatus",
        "predictProcessflowTimestamp",
        "processflowStatus",
        "processflowTimestamp",
        "singleValue",
        "skipExpansionFlag"
    */
        "vitaltype",
        "types",
        "URIProp"
                    )
    );
	
	/**
	 * Generates schema 
	 * @throws Exception 
	 */
	public void generateSchema() throws Exception {

		if(schemas != null) throw new RuntimeException("Schemas already generated!");
		
		schemas = new ArrayList<LinkedHashMap<String, Object>>();
		
		properties = new ArrayList<LinkedHashMap<String, Object>>();
		
		List<OntClass> thisClasses = OntologyProcessor.listOntologyClassesSorted(model, ontologyURI);

		Collections.sort(thisClasses, new Comparator<OntClass>() {

			@Override
			public int compare(OntClass o1, OntClass o2) {
				return o1.getURI().compareTo(o2.getURI());
			}
		});
		
		Map<String, OntClass> thisClassesMap = new HashMap<String, OntClass>();
		
		for(OntClass c : thisClasses) {
			thisClassesMap.put(c.getURI(), c);
		}
		
		//get the properties that extend base schema
		
		Map<String, List<OntProperty>> parentOntProperties = new HashMap<String, List<OntProperty>>();
		
		List<PropertyDetails> propsList = new ArrayList<PropertyDetails>();
		
		//special case properties assigned to parent ontology classes
		for(ExtendedIterator<OntProperty> iter = model.listAllOntProperties(); iter.hasNext(); ) {
			OntProperty p = iter.next();
			if(!(p.isDatatypeProperty() ||p.isObjectProperty())) continue;
			if(p.getURI() == null) continue;
			String ontURI = RDFUtils.getOntologyPart(p.getURI());
			if(!ontologyURI.equals(ontURI)) continue;
			
			if(VitalCoreOntology.internalProperties.contains(p.getURI())) continue;

			PropertyDetails pDetails = OntologyProcessor.getPropertyDetails(p, null);
			
			for(String domainURI : pDetails.domainClassesURIs) {
				
				if(thisClassesMap.containsKey(domainURI)) continue;
				
				List<OntProperty> props = parentOntProperties.get(domainURI);
				
				if(props == null) {
					props = new ArrayList<OntProperty>();
					parentOntProperties.put(domainURI, props);
				}
				
				props.add(p);
				
			}
			
			propsList.add(pDetails);
			
		}
		
		Collections.sort(propsList, new Comparator<PropertyDetails>(){

            @Override
            public int compare(PropertyDetails o1, PropertyDetails o2) {
                return o1.URI.compareTo(o2.URI);
            }});
		
		
		for(PropertyDetails pd : propsList) {
		    
		    
		    if(unwanted.contains(pd.shortName)) {
		        continue;
		    }
		    //skip core properties
		    
		    LinkedHashMap<String, Object> pm = new LinkedHashMap<String, Object>();
		    
		    List<String> x = pd.domainClassesURIs;
		    Collections.sort(x);
		    
		    pm.put("URI", pd.URI);
		    pm.put("domainClassesURIs", x);
		    pm.put("shortName", pd.shortName);
		    pm.put("multipleValues", pd.multipleValues);
		    pm.put("type", pd.pClass.getSimpleName());
		    
		    OntProperty ontProperty = model.getOntProperty(pd.URI);
		    if(ontProperty == null) throw new RuntimeException("OntProperty not found: " + pd.URI);
		    
		    OntProperty superProperty = ontProperty.getSuperProperty();
		    if(superProperty != null) {
		        String superURI = superProperty.getURI();
		        if( ! "http://www.w3.org/2002/07/owl#topDataProperty".equals(superURI) ) {
		            pm.put("parent", superURI);
		        }
		    }
		    
		    properties.add(pm);
		    
		}
		
		
		List<Entry<String, List<OntProperty>>> entries = new ArrayList<Entry<String, List<OntProperty>>>(parentOntProperties.entrySet());
		
		Collections.sort(entries, new Comparator<Entry<String, List<OntProperty>>>(){

			@Override
			public int compare(Entry<String, List<OntProperty>> e1,
					Entry<String, List<OntProperty>> e2) {
				return e1.getKey().compareToIgnoreCase(e2.getKey());	
			}});
		
				
		for(Entry<String, List<OntProperty>> e : entries) {
			
			String cls = e.getKey();
			
			List<OntProperty> props = e.getValue();
			
			Collections.sort(props, new Comparator<OntProperty>() {

				@Override
				public int compare(OntProperty p1, OntProperty p2) {
					return p1.getURI().compareToIgnoreCase(p2.getURI());
				}
				
			});
			
			LinkedHashMap<String, Object> schema = new LinkedHashMap<String, Object>();
			
			String uri = cls;
			
			ClassMetadata gClass = VitalSigns.get().getClassesRegistry().getClass(uri);
			
			if(gClass == null) continue;
			
			
//			schema.put("$schema", "http://json-schema.org/draft-04/schema#");
			schema.put("extends", uri);
//			schema.put("title", "Extension to " + uri);
//			schema.put("description", "Extension to upper ontology class: " + uri);
			
			LinkedHashMap<String, Object> propsSchema = new LinkedHashMap<String, Object>();
			
			for(OntProperty ontProperty : props) {
				
//				String shortName = RDFUtils.getPropertyShortName(ontProperty.getURI());
				
				LinkedHashMap<String, Object> pVal = getPropertyRangeValue(model, ontProperty);
				
				if(pVal == null) continue;
				
				propsSchema.put(ontProperty.getURI()/*shortName*/, pVal);
				
			}
			
			schema.put("properties", propsSchema);
			
		
			schemas.add(schema);
			
		}
		
		
		
		//listDeclared proeprties does now work if there are multiple domains statements, instead list all properties with domains and assign them to classes, use a different approach, get all properties domains
		//
		
		Map<OntProperty, Set<String>> prop2Domains = new HashMap<OntProperty, Set<String>>();
		
		for(OntProperty prop : model.listAllOntProperties().toList()) {
		    
		    List<String> propertyDomains = OntologyProcessor.getPropertyDomains(prop);
		    
		    prop2Domains.put(prop, new HashSet<String>(propertyDomains));
		    
		}
		
		for(OntClass _cls : thisClasses) {

			List<OntClass> hierarchy = new ArrayList<OntClass>();
			
			hierarchy.add(_cls);
			
			RDFUtils.collectAllSuperClasses(_cls, hierarchy, null);
			
			boolean skipNonGraphObject = false;
			
			for(OntClass hc : hierarchy) {
				
				//filter out the containers
				if(VitalCoreOntology.VITAL_Container.getURI().equals(hc.getURI())) {
					skipNonGraphObject = true;
					break;
				}
				
				if(VitalCoreOntology.RestrictionAnnotationValue.getURI().equals(hc.getURI())) {
					skipNonGraphObject = true;
					break;
				}
				
				if(VitalCoreOntology.VITAL_URIReference.getURI().equals(hc.getURI())) {
					skipNonGraphObject = true;
					break;
				}
				
			}
			
			if(skipNonGraphObject) continue;
			
			Set<String> thisTree = new HashSet<String>();
            thisTree.add(_cls.getURI());
            
			//only direct properties!
//            for(OntClass c : hierarchy) {
//                thisTree.add(c.getURI());
//            }
	        
            List<OntProperty> directProperties  = new ArrayList<OntProperty>();
            
            for(Iterator<Entry<OntProperty,Set<String>>> iter = prop2Domains.entrySet().iterator(); iter.hasNext(); ) {
                
                Entry<OntProperty, Set<String>> next = iter.next();
                for(String d : next.getValue()) {
                    if(thisTree.contains(d)) {
                        directProperties.add(next.getKey());
                        break;
                    }
                }
                
            }
            
			/*
			List<OntProperty> directProperties = _cls.listDeclaredProperties(true).toList();
			
			for(OntProperty op : directProperties) {
			    System.out.println(_cls + " " + op);
			}
			
			for(OntClass cls : hierarchy) {
				
				for(OntProperty _p : cls.listDeclaredProperties(true).toList()) {
					if(!directProperties.contains(_p)) {
						directProperties.add(_p);
					}
				}
				
				for(ExtendedIterator<UnionClass> unionIterator = model.listUnionClasses(); unionIterator.hasNext();) {
					
					UnionClass uc = unionIterator.next();
					
					for( ExtendedIterator<? extends OntClass> ei =  uc.listOperands(); ei.hasNext(); ) {
						
						OntClass oc = ei.next();
						
						if(oc.equals(cls)) {
							
							for(OntProperty ontProperty : model.listAllOntProperties().toList()) {
								if( ! ( ontProperty.isDatatypeProperty() || ontProperty.isObjectProperty() ) ) continue; 
								if(ontProperty.getDomain().equals(uc)) {
									
									if(!directProperties.contains(ontProperty)){
										directProperties.add(ontProperty);
									}
									
								}
							}
							
						}
					}
					
				}
				
			}
			*/
			
			
			//filter out some annotation properties
			for(Iterator<OntProperty> iterator = directProperties.iterator(); iterator.hasNext(); ) {
				
				OntProperty p = iterator.next();
				
				String shortName = RDFUtils.getPropertyShortName(p.getURI());
				
				if(unwanted.contains(shortName)) {
					iterator.remove();
					continue;
				}
				
//				if(!p.getURI().startsWith(ontologyURI)) {
//					iterator.remove();
//					continue
//				}

			}
			
			Collections.sort(directProperties, new Comparator<OntProperty>() {

				@Override
				public int compare(OntProperty op1, OntProperty op2) {
//					return RDFUtils.getPropertyShortName(op1.getURI()).compareTo(RDFUtils.getPropertyShortName(op2.getURI()));
					return op1.getURI().compareTo(op2.getURI());
				}
			});

			LinkedHashMap<String, Object> schema = new LinkedHashMap<String, Object>();
			
			String uri = _cls.getURI();
			
			schema.put("id", uri);
			
			OntClass superClass = _cls.getSuperClass();
			if(superClass != null && superClass.getURI() != null) {
				schema.put("parent", superClass.getURI());
			}
			
			//not necessary
//			schema.put("$schema", "http://json-schema.org/draft-04/schema#");
//			schema.put("title", uri);
//			schema.put("description", "Auto-generated schema for owl class: " + uri);
//			schema.put("type", "object");
			
			
			boolean isEdge = false;
			boolean isHyperEdge = false;
			boolean isNode = false;
			boolean isHyperNode = false;
			
			for( OntClass ontCls : hierarchy ) {
				
				if( VitalCoreOntology.VITAL_Edge.getURI().equals(  ontCls.getURI() ) ) {
					
					isEdge = true;
					
				} else if( VitalCoreOntology.VITAL_HyperEdge.getURI().equals( ontCls.getURI() ) ) {
				
					isHyperEdge = true;
				
				} else if( VitalCoreOntology.VITAL_Node.getURI().equals( ontCls.getURI() ) ) {
					
					isNode = true;
					
				} else if( VitalCoreOntology.VITAL_HyperNode.getURI().equals( ontCls.getURI() ) ) {
				
					isHyperNode = true;
				
				}
				
			}
			
			if(!isEdge && !isHyperEdge && !isNode && !isHyperNode) {
				log.warn("class is not a subclass of vital node, edge, hypernode or hyperedge: " + _cls);
			}
			
			if(isEdge || isHyperEdge) {
				
				Set<String> sourceDomains = new HashSet<String>();
				
				Set<String> destinationDomains = new HashSet<String>();
				
				for(Statement stmt : _cls.listProperties( isEdge ? VitalCoreOntology.hasEdgeSrcDomain : VitalCoreOntology.hasHyperEdgeSrcDomain).toList() ) {
					
					Resource srcDomain = stmt.getResource();
					if(srcDomain == null || srcDomain.getURI() == null) continue;
					
					OntClass srcClass = model.getOntClass(srcDomain.getURI());
					
					if(srcClass == null) continue;
					
					if(srcClass.getURI() != null) {
						
						sourceDomains.add(srcClass.getURI());
						
					}
					
				}
				
				for(Statement stmt : _cls.listProperties( isEdge ? VitalCoreOntology.hasEdgeDestDomain : VitalCoreOntology.hasHyperEdgeDestDomain).toList() ) {
					
					Resource destDomain = stmt.getResource();
					if(destDomain == null || destDomain.getURI() == null) continue;
							
					OntClass destClass = model.getOntClass(destDomain.getURI());
					
					if(destClass == null) continue;
					
					if(destClass.getURI() != null) {
						
						destinationDomains.add(destClass.getURI());
						
					}
					
				}
				
				List<String> sourceDomainsList = new ArrayList<String>(sourceDomains);
				Collections.sort(sourceDomainsList);
				
				List<String> destinationDomainsList = new ArrayList<String>(destinationDomains);
				Collections.sort(destinationDomainsList);
				
				schema.put("sourceDomains", sourceDomainsList);
				schema.put("destinationDomains", destinationDomainsList);
				
			}
			
			
			LinkedHashMap<String, Object> propsSchema = new LinkedHashMap<String, Object>();
			
			
			LinkedHashMap<String, Object> uriP = new LinkedHashMap<String, Object>();
			uriP.put("type", "string");
			//array of strings
			

			/*
			propsSchema.put("URI", uriP);
			
			LinkedHashMap<String, Object> typeP = new LinkedHashMap<String, Object>();

			List<String> enumL = new ArrayList<String>();
			enumL.add(uri);
			typeP.put("enum", enumL);
			propsSchema.put("type", typeP);
			
			LinkedHashMap<String, Object> typesOpt = new LinkedHashMap<String, Object>();
			
			typesOpt.put("type", "array");
			typesOpt.put("minItems", 1);
			LinkedHashMap<String, Object> typesOptItems = new LinkedHashMap<String, Object>();
			typesOptItems.put("type", "string");
			
			typesOpt.put("items", typesOptItems);
			typesOpt.put("uniqueItems", true);
			propsSchema.put("types", typesOpt);
			*/
			
			for(OntProperty ontProperty : directProperties) {
				
//				String shortName = RDFUtils.getPropertyShortName(ontProperty.getURI());
				
				LinkedHashMap<String, Object> pVal = getPropertyRangeValue(model, ontProperty);
				
				if(pVal == null) continue;
				
				propsSchema.put(ontProperty.getURI(), pVal);
				
			}
			
			
			schema.put("properties", propsSchema);
//			schema.put("required", new ArrayList<String>(Arrays.asList("URI", "type")));
//			schema.put("additionalProperties", false);
			
			schemas.add(schema);
			
		}
		
		
	}
	
	public void writeSchemaToOutputStream(boolean core, String outputFileName, OutputStream outputStream) throws IOException {
		
		if(schemas == null) throw new RuntimeException("Schemas not generated yet");
		
		if(!outputFileName.endsWith(".js")) throw new IOException("Output file must end with .js");
		
		String n = outputFileName.substring(0, outputFileName.length() - 3);
		
		String varName = outputFileName.substring(0, outputFileName.length() - 3).replaceAll("[^a-z0-9_]", "_") + "_schema";
		
		
		List<String> directImports = new ArrayList<String>(OntologyProcessor.getDirectImports(model.getResource(ontologyURI)));
		
		if(core) {
		    outputStream.write("var VITAL_JSON_SCHEMAS = [];\n\n".getBytes(StandardCharsets.UTF_8));
		} else {
		    
		    String script = 
"if(typeof(VITAL_JSON_SCHEMAS) == 'undefined') {\n" +
        " throw (\"No VITAL_JSON_SCHEMAS list defined - vital-core domain unavailable\");\n" +
"}\n\n";
		    outputStream.write(script.getBytes(StandardCharsets.UTF_8));
		}
		
		outputStream.write(("var " + varName + " = ").getBytes(StandardCharsets.UTF_8));
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		Map<String, Object> output = new LinkedHashMap<String, Object>();
		output.put("domainURI", ontologyURI);
		output.put("name", n);
		if(version != null) {
		    output.put("version", version);
		}
		if(domainOWLHash != null) {
		    output.put("domainOWLHash", domainOWLHash);
		}
		output.put("vitalsignsVersion", VitalSigns.VERSION);
		output.put("parents", directImports);
		output.put("schemas", schemas);
		output.put("properties", properties);
		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
		prettyPrinter.indentArraysWith(new FixedSpaceIndenter());
		prettyPrinter.indentObjectsWith(new UnixLf2SpacesIndenter());
		mapper.writer(prettyPrinter).writeValue(bos, output);
		
		outputStream.write(bos.toByteArray());
		
		outputStream.write((";\n\nVITAL_JSON_SCHEMAS.push(" + varName + ");").getBytes(StandardCharsets.UTF_8));
		
		//upgrade for nodejs
		outputStream.write("\n\n".getBytes(StandardCharsets.UTF_8));
		
		outputStream.write("if(typeof(module) !== 'undefined') {\n\n".getBytes(StandardCharsets.UTF_8));
		
		outputStream.write(("  module.exports = " + varName + ";\n\n").getBytes(StandardCharsets.UTF_8));
		
		outputStream.write("}".getBytes(StandardCharsets.UTF_8));
		
		
	}
	
	
	public static LinkedHashMap<String, Object> getPropertyRangeValue(OntModel m, OntProperty ontProperty) throws Exception {
	
		Boolean multipleValues = RDFUtils.getBooleanPropertySingleValue(ontProperty, VitalCoreOntology.hasMultipleValues);
		
		if(multipleValues == null) multipleValues = false;
		
		OntResource range  = ontProperty.getRange();
	
		if( range != null && RDFS.Datatype.equals(range.getPropertyValue(RDF.type)) ) {
			
			//extract range type from restriction node
			Resource baseDatatype = range.getPropertyResourceValue(OWL2.onDatatype);

			if(baseDatatype != null) {
				range = m.getOntResource(baseDatatype);
			}
							
		}
		
		if(range == null && ! ontProperty.isObjectProperty()) return null;
		
		String t = "";
		
		boolean geo = false;
		
		if(ontProperty.isObjectProperty() || XSD.xstring.equals(range) || VitalCoreOntology.truth.equals(range)) {

			t = "string";
								
		} else if(XSD.xboolean.equals(range)) {
			
			t = "boolean";
			
		} else if(XSD.xint.equals(range) || XSD.integer.equals(range) || XSD.xlong.equals(range) || XSD.xdouble.equals(range) || XSD.xfloat.equals(range) || XSD.dateTime.equals(range)) {
			
			t = "number";
			
		} else if(VitalCoreOntology.geoLocation.equals(range)) {
		
			t = "object";
			
			geo = true;
		
		} else {
			throw new Exception("Unsupported data type: " + range.getURI());
		}
		
		LinkedHashMap<String, Object> pVal = new LinkedHashMap<String, Object>();
		pVal.put("type", t);
		
		if( geo ) {
			
			LinkedHashMap<String, Object> gProps = new LinkedHashMap<String, Object>();
			
			LinkedHashMap<String, Object> longProps = new LinkedHashMap<String, Object>();
			longProps.put("type", "number");
			gProps.put("longitude", longProps);
			
			LinkedHashMap<String, Object> latProps = new LinkedHashMap<String, Object>();
			latProps.put("type", "number");
			gProps.put("latitude", latProps);
			
			//add properties def
			pVal.put("properties", gProps);
			pVal.put("required", new ArrayList<String>(Arrays.asList("longitude", "latitude")));
			pVal.put("additionalProperties", false);

		}
		
		if(multipleValues) {
			
			LinkedHashMap<String, Object> parentVal = new LinkedHashMap<String, Object>();
			
			parentVal.put("type", "array");
			
			parentVal.put("items", pVal);
			
			return parentVal;
			
		} else {
		
			return pVal;
			
		}
		
			
	}

    public String getDomainOWLHash() {
        return domainOWLHash;
    }

    public void setDomainOWLHash(String domainOWLHash) {
        this.domainOWLHash = domainOWLHash;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * Default linefeed-based indenter uses system-specific linefeeds and
     * 2 spaces for indentation per level.
     */
    public static class UnixLf2SpacesIndenter
        implements Indenter
    {
        final static String LINE_SEPARATOR = "\n";
        final static int SPACE_COUNT = 64;
        final static char[] SPACES = new char[SPACE_COUNT];
        static {
            Arrays.fill(SPACES, ' ');
        }

        public UnixLf2SpacesIndenter() { }

//      @Override
        public boolean isInline() { return false; }

//      @Override
        public void writeIndentation(JsonGenerator jg, int level)
            throws IOException, JsonGenerationException
        {
            jg.writeRaw(LINE_SEPARATOR);
            level += level; // 2 spaces per level
            while (level > SPACE_COUNT) { // should never happen but...
                jg.writeRaw(SPACES, 0, SPACE_COUNT); 
                level -= SPACES.length;
            }
            jg.writeRaw(SPACES, 0, level);
        }
    }

}
