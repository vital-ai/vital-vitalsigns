package ai.vital.vitalsigns.rdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.conf.VitalSignsConfig.VersionEnforcement;
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader.VersionedDomain;
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader.VersionedURI;
import ai.vital.vitalsigns.model.DomainOntology;
import ai.vital.vitalsigns.model.GeoLocationDataType;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.TruthDataType;
import ai.vital.vitalsigns.model.VITAL_GraphContainerObject;
import ai.vital.vitalsigns.model.property.BooleanProperty;
import ai.vital.vitalsigns.model.property.DateProperty;
import ai.vital.vitalsigns.model.property.DoubleProperty;
import ai.vital.vitalsigns.model.property.FloatProperty;
import ai.vital.vitalsigns.model.property.GeoLocationProperty;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.IntegerProperty;
import ai.vital.vitalsigns.model.property.LongProperty;
import ai.vital.vitalsigns.model.property.MultiValueProperty;
import ai.vital.vitalsigns.model.property.OtherProperty;
import ai.vital.vitalsigns.model.property.StringProperty;
import ai.vital.vitalsigns.model.property.TruthProperty;
import ai.vital.vitalsigns.datatype.Truth;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.utils.StringUtils;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl;
import com.hp.hpl.jena.sparql.util.NodeFactory;
import com.hp.hpl.jena.vocabulary.RDF;

public class RDFSerialization {

	static Set<String> typesProps = new HashSet<String>(Arrays.asList(
			VitalCoreOntology.types.getURI(), 
			VitalCoreOntology.vitaltype.getURI(),
			VitalCoreOntology.URIProp.getURI(),
			RDF.type.getURI()
	));
	
	public final static String VITAL_PROPERTY_NAME_PREFIX = "vital__";
	
	public final static String VITAL_PROPERTY_NAME_PREFIX_WITH_HASH = "#vital__";
	
	static Set<String> typesWithVersions = new HashSet<String>(typesProps);
	static {
	    
	    typesWithVersions.add(VitalCoreOntology.NS + VITAL_PROPERTY_NAME_PREFIX + VitalCoreOntology.hasOntologyIRI.getLocalName());
	    typesWithVersions.add(VitalCoreOntology.NS + VITAL_PROPERTY_NAME_PREFIX + VitalCoreOntology.hasVersionIRI.getLocalName());
	    
	    typesWithVersions.add(VitalCoreOntology.hasOntologyIRI.getURI());
	    typesWithVersions.add(VitalCoreOntology.hasVersionIRI.getURI());
	}
	
	
	private final static Logger log = LoggerFactory.getLogger(RDFSerialization.class);

    public static Resource serializeToModel(GraphObject g, Model target) {

        if (StringUtils.isEmpty(g.getURI()))
            throw new NullPointerException("Graph object URI is not set!");

        Resource r = target.createResource(g.getURI());

        Object vt = g.getProperty(VitalCoreOntology.vitaltype.getLocalName());
        if (vt == null)
            throw new RuntimeException(
                    "Graph object does not have vitaltype property, "
                            + g.getURI());

        String vitalTypeURI = (String) ((IProperty) vt).rawValue();

        Object t = g.getProperty(VitalCoreOntology.types.getLocalName());
        if (t == null)
            throw new RuntimeException(
                    "Graph object does not have types property, " + g.getURI());

        @SuppressWarnings("rawtypes")
        Collection c = (Collection) ((IProperty) t).rawValue();

        for (Object x : c) {

            r.addProperty(RDF.type, ResourceFactory
                    .createResource(oldVersionFilter((String) x)));

            if (x.equals(vitalTypeURI)) {

                r.addProperty(VitalCoreOntology.vitaltype, ResourceFactory
                        .createResource(oldVersionFilter((String) x)));

            }

        }

        // if(true) return
        DomainOntology _do = VitalSigns.get().getClassDomainOntology(
                g.getClass());
        if (_do == null)
            throw new RuntimeException("Domain ontology for class: "
                    + g.getClass().getCanonicalName() + " not found");
        g.setProperty("ontologyIRI", _do.getUri());
        g.setProperty("versionIRI", _do.toVersionString());

        for (Entry<String, IProperty> e : g.getPropertiesMap().entrySet()) {

            String fname = e.getKey();

            if (typesProps.contains(fname)) {
                continue;
            }

            IProperty unwrapped = e.getValue().unwrapped();

            if (unwrapped instanceof MultiValueProperty) {

                for (IProperty o : (MultiValueProperty<?>) unwrapped) {

                    addRDFValue(r, e.getKey(), o.unwrapped());

                }

            } else {

                addRDFValue(r, e.getKey(), unwrapped);

            }

        }

        g.setProperty("ontologyIRI", null);
        g.setProperty("versionIRI", null);

        // for(Entry String s : IPropertygraphObject.getPropertiesMap()

        return r;

    }

	public static GraphObject deserialize(String URI, Model m, boolean ignoreNonVitalOrMissingObjects) {

		Resource r = m.getResource(URI);
		
		if(r == null) {
			if(ignoreNonVitalOrMissingObjects) return null;
			throw new RuntimeException("No resource with URI " + URI + " found in the model");
		}
		
		Statement ontStmt = r.getProperty(VitalCoreOntology.hasOntologyIRI);
		String ontologyIRI = null;
		String versionIRI = null;
		if(ontStmt != null) {
		    ontologyIRI = ontStmt.getResource() != null ? ontStmt.getResource().getURI() : null;
		}
		Statement verStmt = r.getProperty(VitalCoreOntology.hasVersionIRI);
		if(verStmt != null) {
		    versionIRI = verStmt.getString();
		}
		
		
		Map<String, String> oldVersions = null;
	    
		List<Statement> vitaltypeStmts = r.listProperties(VitalCoreOntology.vitaltype).toList();
        
        if(vitaltypeStmts.size() == 0 ) {
            if(!ignoreNonVitalOrMissingObjects) throw new RuntimeException("Found a subject without vital-core:vitaltype property: " + r.getURI());
            return null;
        }
        
        if(vitaltypeStmts.size() > 1) throw new RuntimeException("A graph object with more than 1 vital-core:vitaltype property found, " + r.getURI());

        RDFNode object = vitaltypeStmts.get(0).getObject();
        
        if(!object.isURIResource()) throw new RuntimeException("vital-core:vitaltype property value must be a URI resource, " + r.getURI());
        
        String vitalRDFType = object.asResource().getURI();
		
	        
        if(ontologyIRI != null && versionIRI != null) {

            DomainOntology _do = new DomainOntology(ontologyIRI, versionIRI);
            
            DomainOntology _cdo = null;
            
            
            //first check if it's not a temporary loaded older version
            for(DomainOntology d : VitalSigns.get().getDomainList()) {
                
                VersionedDomain vns = VersionedDomain.analyze(d.getUri());
                
                if(vns.versionPart != null) {
                    String ontURI = vns.domainURI;
                    if( ontURI.equals(ontologyIRI) ) {
                        if(_do.compareTo(d) == 0) {
                            _cdo = d;
                            
                            oldVersions = new HashMap<String, String>();
                            oldVersions.put(ontURI, d.getUri());
                            //collect imports tree
                            List<String> imports = VitalSigns.get().getOntologyURI2ImportsTree().get(d.getUri());
                            
                            if(imports != null) {
                                for(String i : imports) {
                                    VersionedDomain ivd = VersionedDomain.analyze(i);
                                    if(ivd.versionPart != null) {
                                        oldVersions.put(ivd.domainURI, i);
                                    }
                                }
                            }
                            
                            break;
                        }
                    }
                }
            }

            if(_cdo == null) {
                _cdo = VitalSigns.get().getDomainOntology(ontologyIRI);

                if(_cdo != null) {
                    
                    int comp = _do.compareTo(_cdo);
                    
                    if( comp != 0 ) {
                        
                        if( VitalSigns.get().getConfig().versionEnforcement == VersionEnforcement.strict ) {
                            
                            boolean backwardCompatible = false;
                            
                            String backwardMsg = "";
                            
                            //give it a try
                            if(comp < 1 && _cdo.getBackwardCompatibleVersion() != null) {
                                
                                comp = _do.compareTo( _cdo.getBackwardCompatibleVersion());
                                    
                                if(comp >= 0) {
                                    
                                    backwardCompatible = true;
                                    
                                } else {
                                    
                                    backwardMsg = " nor its backward compatible version: " + _cdo.getBackwardCompatibleVersion().toVersionString();
                                    
                                }
                                
                            }
                            
                            if(!backwardCompatible) 
                                throw new RuntimeException("Strict version mode - persisted object domain " + ontologyIRI + " version " + _do.toVersionString() + " does not match currently loaded: " + _cdo.toVersionString() + backwardMsg);
                            
                        }
                        
                    }
                }
                
                
            }
            
            
            if(_cdo != null) {
                
//	              int comp = _do.compareTo(_cdo);
//	              
//	              if( comp > 0 ) {
//	                  
//	                  log.error("Domain ontology " + ontologyIRI + " object version newer than loaded: " + _do.toVersionIRI() + " > " + _cdo.toVersionIRI() + ", object class (RDF): " + vitalRDFType + " , uri: " + uriString);
//	                  
//	              }
                
                
            } else {
            
                log.error("Domain ontology with IRI not found: " + ontologyIRI + ", object class (RDF): " + vitalRDFType + ", uri: " + URI);
                
            }
            
        }
		
        
        vitalRDFType = toOldVersion(vitalRDFType, oldVersions);
		
		ClassMetadata cmd = VitalSigns.get().getClassesRegistry().getClass(vitalRDFType);
		
		if(cmd == null) throw new RuntimeException("Class for URI: " + vitalRDFType + " not found in vitalsigns");

		GraphObject g = null;
		try {
			g = cmd.getClazz().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		g.setURI(r.getURI());
		
		List<String> otherTypes = null;
		
		for( StmtIterator typesIter = r.listProperties(RDF.type); typesIter.hasNext(); ) {
			
			RDFNode object2 = typesIter.next().getObject();
			
			if(! object2.isURIResource() ) throw new RuntimeException("All rdf:type properties values must be uri resources, " + r.getURI());
			
			String tURI = toOldVersion( object2.asResource().getURI(), oldVersions );
			
			if(vitalRDFType.equals(tURI)) {
				continue;
			}
			
			if(otherTypes == null) {
				
				otherTypes = new ArrayList<String>();
				otherTypes.add(vitalRDFType);
			}
			
			otherTypes.add(tURI);
			
		}
		
		if(otherTypes != null) {
			g.setProperty(VitalCoreOntology.types.getLocalName(), otherTypes);
		}
		
		
		//set properties now
		
		Map<String, List<Object>> multiValues = null;
		
		Map<String, List<Object>> externalProperties = null;
		
		for(StmtIterator iter = r.listProperties(); iter.hasNext(); ) {
			
			Statement next = iter.next();
			
			String pURI = next.getPredicate().getURI();
			
			if(typesWithVersions.contains(pURI)) continue;
			
            pURI = toOldVersion( pURI, oldVersions );
			
            
            RDFVitalPropertyFilter analyze = RDFVitalPropertyFilter.analyze(pURI);
            
            pURI = analyze.cleanPropertyURI;
            
			
			PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(pURI);
			
			Object rawValue = nodeToJavaType(next.getObject(), oldVersions);
			
			if(pm == null) {
			    
				//external property !

			    if(analyze.vitalProperty) {
			        
			        if( VitalSigns.get().getConfig().versionEnforcement != VersionEnforcement.lenient ) {
			            
//                    throw new RuntimeException("Strict version mode - persisted object version " + _do.toVersionString() + " does not match currently loaded: " + _cdo.toVersionString());
			            throw new RuntimeException("Property not found: " + pURI + " " + g.getClass().getCanonicalName());
			            
			        } else {
			            
			            log.warn("Property not found: " + pURI + " " + g.getClass().getCanonicalName());

			            continue;
			            
			        }
			    }
                
			    
			    if( !(g instanceof VITAL_GraphContainerObject) &&  ! VitalSigns.get().getConfig().externalProperties ) {
			        throw new RuntimeException("Cannot deserialize an object with external properties - they are disabled, property: " + pURI);
			    }
			    
				if(externalProperties == null) {
					externalProperties = new HashMap<String, List<Object>>();
				}
				
				//XXX if milliseconds are at stake we can make vals either a list or an object ( promote object to list if more than 1 value)
				List<Object> vals = externalProperties.get(pURI);
				if(vals == null) {
					vals = new ArrayList<Object>();
					externalProperties.put(pURI, vals);
				}
				
				vals.add(rawValue);
				
				//throw new RuntimeException("Property not found in VitalSigns: " + pURI);
				
			} else {
				
				String pname = RDFUtils.getPropertyShortName(pURI);
				
				
				if(pm.isMultipleValues()) {
					
					List<Object> list = null;
					
					if(multiValues == null) {
						multiValues = new HashMap<String, List<Object>>();
					} else {
						list = multiValues.get(pname);						
					}
					
					if(list == null) {
						list = new ArrayList<Object>();
						multiValues.put(pname, list);
					}
					
					list.add(rawValue);
					
				} else {
					
					//is it an exception ??
					if(g.getPropertiesMap().containsKey(pURI)) {
//						throw new RuntimeException("More than 1 value of a single value property found, URI: " + g.getURI() + " property: " + pURI);
						log.warn("More than 1 value of a single value property found, URI: " + g.getURI() + " property: " + pURI);
					}
					
					g.setProperty(pname, rawValue);
					
				}
				
			}
			
			
		}
		
		if(multiValues != null) {
			
			for(Entry<String, List<Object>> e : multiValues.entrySet()) {
				
				g.setProperty(e.getKey(), e.getValue());
				
			}
			
		}
		
		if(externalProperties != null) {
			
			for(Entry<String, List<Object>> e : externalProperties.entrySet()) {
				
				List<Object> v = e.getValue();
				
				g.setProperty(e.getKey(), v.size() == 1 ? v.get(0) : v);
				
			}
			
		}
		
		return g;
		
	}
	
	private static Object nodeToJavaType(RDFNode node, Map<String, String> oldVersions) {

		if(node.isURIResource()) {

			return new URIProperty( toOldVersion( node.asResource().getURI(), oldVersions ) );
			
		} else if(node.isLiteral()) {
			
			Literal lit = ((Literal)node);
			
			RDFDatatype dt = lit.getDatatype();
			
//			Literal lit = node.asLiteral();
//			RDFDatatype dt = lit.getDatatype();
			
			if(XSDDatatype.XSDboolean.equals(dt)) {
				return lit.getBoolean();
			} else if(XSDDatatype.XSDdateTime.equals(dt)) {
				return RDFDate.fromXSDString(lit.getLexicalForm());
			} else if(XSDDatatype.XSDdouble.equals(dt)) {
				return lit.getDouble();
			} else if(GeoLocationDataType.theGeoLocationDataType.equals(dt)) {
				return GeoLocationProperty.fromRDFString(lit.getLexicalForm());
			} else if(TruthDataType.theTruthDataType.equals(dt)) {
			    return Truth.fromString(lit.getLexicalForm());
			} else if(XSDDatatype.XSDfloat.equals(dt)) {
				return lit.getFloat();
			} else if(XSDDatatype.XSDint.equals(dt)) {
				return lit.getInt();
			} else if(XSDDatatype.XSDlong.equals(dt)) {
				return lit.getLong();
			} else if(StringUtils.isEmpty(lit.getLanguage()) && ( dt == null || XSDDatatype.XSDstring.equals(dt) ) ) {
				return toOldVersion( lit.getString(), oldVersions);
			} else {
//				throw new RuntimeException("Unknown literal datatype: " + dt.getURI());
				return new OtherProperty(lit.getLexicalForm(), lit.getDatatypeURI(), lit.getLanguage());
			}
			
		} else {
			throw new RuntimeException("Unhandled rdfnode type: " + node.getClass().getCanonicalName() + " - " + node);
		}
	}

	public static List<GraphObject> deserializeFromModel(Model m, boolean ignoreNonVitalObjects) {
		
		List<GraphObject> l = new ArrayList<GraphObject>();
		
		for( ResIterator resIter = m.listSubjects(); resIter.hasNext(); ) {
			
			Resource r = resIter.next();
		
			
			if(!r.isURIResource() && !ignoreNonVitalObjects) throw new RuntimeException("Non-URI subject found in model");
			
			GraphObject g = deserialize(r.getURI(), m, ignoreNonVitalObjects);
			
			if(g != null) l.add(g);
			
		}
		
		return l;
		
	}
	
	public static void addRDFValue(Resource r, String pURI, IProperty o) {

		RDFNode n = toRDFNode(o);
		
		r.addProperty(ResourceFactory.createProperty( RDFVitalPropertyFilter.vitalPropertyPrefixInjector( oldVersionFilter(pURI) ) ), n);
		
	}
	
	
	public static class RDFVitalPropertyFilter {
	    
	    public String cleanPropertyURI;
	    
	    boolean vitalProperty = false;
	    
//	    static Pattern pattern = Pattern.compile("([^#]+#)" + VITAL_PROPERTY_NAME_PREFIX + "(.*)");
	    
	    /**
	     * 
	     * @param propertyURI
	     */
	    public static String vitalPropertyPrefixInjector(String propertyURI) {
	        
	        int indexOfHash = propertyURI.indexOf('#');
	        
	        if(indexOfHash < 0) return propertyURI;
	        
	        PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(propertyURI);
	        
	        if(pm == null) return propertyURI;
	        
	        if(propertyURI.length() <= indexOfHash + 1 + VITAL_PROPERTY_NAME_PREFIX.length()) return propertyURI;
	        
	        if(propertyURI.substring(indexOfHash + 1, indexOfHash + 1 + VITAL_PROPERTY_NAME_PREFIX.length()).equals(VITAL_PROPERTY_NAME_PREFIX)) {
	            //ok
	            return propertyURI;
	        } else {
	            return propertyURI.substring(0, indexOfHash + 1) + VITAL_PROPERTY_NAME_PREFIX + propertyURI.substring(indexOfHash + 1);
	        }
	    }

        public static RDFVitalPropertyFilter analyze(String pURI) {

            RDFVitalPropertyFilter f = new RDFVitalPropertyFilter();
            
            int indexOfVitalPrefix = pURI.indexOf(VITAL_PROPERTY_NAME_PREFIX_WITH_HASH);
            
            if(indexOfVitalPrefix < 0) {
                f.cleanPropertyURI = pURI;
                return f;
            }
            
            f.vitalProperty = true;
            f.cleanPropertyURI = pURI.substring(0, indexOfVitalPrefix) + '#' + pURI.substring(indexOfVitalPrefix + VITAL_PROPERTY_NAME_PREFIX_WITH_HASH.length());
            
            return f;
            
        }
        
	    
	}
	
		
	public static RDFNode toRDFNode(IProperty o) {
		
		if(o instanceof URIProperty) {
			
			return ResourceFactory.createResource( oldVersionFilter(((URIProperty)o).get()) );
		}
		
		
		Literal l = null;
		
		if(o instanceof BooleanProperty) {
			
			BooleanProperty bp = (BooleanProperty) o;
			
			l = ResourceFactory.createTypedLiteral("" + bp.booleanValue(), XSDDatatype.XSDboolean);
			
		} else if(o instanceof DateProperty ) {
			
			DateProperty dp = (DateProperty) o;
			
			l = ResourceFactory.createTypedLiteral(RDFDate.toXSDString(dp.getDate()), XSDDatatype.XSDdateTime);
			
		} else if(o instanceof DoubleProperty) {
			
			DoubleProperty dp = (DoubleProperty) o;
			
			l = ResourceFactory.createTypedLiteral(DatatypeConverter.printDouble(dp.doubleValue()), XSDDatatype.XSDdouble);
			
		} else if(o instanceof FloatProperty) {
			
			FloatProperty fp = (FloatProperty) o;
			
			l = ResourceFactory.createTypedLiteral(DatatypeConverter.printFloat(fp.floatValue()), XSDDatatype.XSDfloat);
			
		} else if(o instanceof GeoLocationProperty) {
			
			GeoLocationProperty gp = (GeoLocationProperty) o;
			
			l = ResourceFactory.createTypedLiteral(gp.toRDFValue(), GeoLocationDataType.theGeoLocationDataType);
			
		} else if(o instanceof IntegerProperty ) {
			
			IntegerProperty ip = (IntegerProperty) o;
			
			l = ResourceFactory.createTypedLiteral(DatatypeConverter.printInt(ip.intValue()), XSDDatatype.XSDint);
			
		} else if(o instanceof LongProperty) {
			
			LongProperty lp = (LongProperty) o;
			
			l = ResourceFactory.createTypedLiteral(DatatypeConverter.printLong(lp.longValue()), XSDDatatype.XSDlong);
			
		} else if(o instanceof OtherProperty) {
			
			OtherProperty op = (OtherProperty) o;
			
			l = new LiteralImpl(NodeFactory.createLiteralNode(op.getLexicalForm(), op.getLangTag(), op.getDatatypeURI()), null);
			
		} else if(o instanceof StringProperty) {
			
			StringProperty sp = (StringProperty) o;
			
			l = ResourceFactory.createTypedLiteral( oldVersionFilter( sp.toString() ), XSDDatatype.XSDstring);
			
		} else if(o instanceof TruthProperty) {
		    
		    TruthProperty tp = (TruthProperty) o;
		    
		    l = ResourceFactory.createTypedLiteral(tp.toString(), TruthDataType.theTruthDataType);
			
		} else {
			
			throw new RuntimeException("Unhandled property type: " + o.getClass().getCanonicalName());
			
		}
		
		return l;
		
	}

	public static String oldVersionFilter(String input) {
       
       VersionedURI vu = VersionedURI.analyze(input);
       return vu.versionlessURI;
       
    }
   
	public static String toOldVersion(String input,
            Map<String, String> oldVersions) {

	    if(input == null) return null;
	     
        if (oldVersions == null)
            return input;

        for (Entry<String, String> e : oldVersions.entrySet()) {

            input = input.replace(e.getKey() + '#', e.getValue() + '#');

        }

        return input;
    }
}
