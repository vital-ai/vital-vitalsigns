package ai.vital.lucene.model


import java.util.Map.Entry

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalsigns.java.VitalJavaSerializationUtils;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.conf.VitalSignsConfig.VersionEnforcement;
import ai.vital.vitalsigns.datatype.Truth;
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader.VersionedDomain;
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader.VersionedURI;
import ai.vital.vitalsigns.model.DomainOntology;
import ai.vital.vitalsigns.model.GraphObject;

import ai.vital.vitalsigns.model.VITAL_GraphContainerObject;

import ai.vital.vitalsigns.model.properties.Property_hasOntologyIRI;
import ai.vital.vitalsigns.model.properties.Property_hasVersionIRI;
import ai.vital.vitalsigns.model.properties.Property_types;
import ai.vital.vitalsigns.model.property.BooleanProperty;
import ai.vital.vitalsigns.model.property.DateProperty;
import ai.vital.vitalsigns.model.property.DoubleProperty;
import ai.vital.vitalsigns.model.property.FloatProperty;
import ai.vital.vitalsigns.model.property.GeoLocationProperty;
import ai.vital.vitalsigns.model.property.IntegerProperty;
import ai.vital.vitalsigns.model.property.LongProperty;
import ai.vital.vitalsigns.model.property.MultiValueProperty;
import ai.vital.vitalsigns.model.property.StringProperty;
import ai.vital.vitalsigns.model.property.TruthProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.rdf.RDFUtils;
import ai.vital.vitalsigns.utils.StringUtils;

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;


public class VitalSignsLuceneBridge {

	private static final String EXTERNAL_FIELD_PREFIX = "external__";

	static final int MAX_EQ_STRING_LENGTH = 2048;
	
	public static final String URI_FIELD = "uri";
	
	public static final String VITAL_TYPE_FIELD = "vitaltype";
	
	//special field that contains the list of all properties, URI, vitaltype, rdf:type are skipped
	public static final String PROPERTIES_FIELD = "properties";
	
	public static final String TYPE_FIELD = "type";
	
	public static final String EDGE_SRC_URI_FIELD = VitalCoreOntology.hasEdgeSource.getURI();
	
	public static final String EDGE_DEST_URI_FIELD = VitalCoreOntology.hasEdgeDestination.getURI();
	
	public static final String HYPER_EDGE_SRC_URI_FIELD = VitalCoreOntology.hasHyperEdgeSource.getURI();
	
	public static final String HYPER_EDGE_DEST_URI_FIELD = VitalCoreOntology.hasHyperEdgeDestination.getURI();
	
//	public static final String COMPACT_STRING_FIELD = "compact_string";
	
	private static VitalSignsLuceneBridge singleton;

	private final Logger log = LoggerFactory.getLogger(VitalSignsLuceneBridge.class);
	
	public static final String LOWER_CASE_SUFFIX = "_LC";
	public static final String EQ_LOWER_CASE_SUFFIX = "_EQ_LC";
	public static final String EQ_SUFFIX = "_EQ";
	
	private Set<String> staticFieldsSet = new HashSet<String>();
	
	private Set<String> propertiesToSkip = new HashSet<String>(Arrays.asList(
		VitalCoreOntology.vitaltype.getURI(), 
		VitalCoreOntology.types.getURI(),
		VitalCoreOntology.hasEdgeSource.getURI(),
		VitalCoreOntology.hasEdgeDestination.getURI(),
		VitalCoreOntology.hasHyperEdgeSource.getURI(),
		VitalCoreOntology.hasHyperEdgeDestination.getURI()
	));
	
	private Set<String> skippedButIndexed = new HashSet<String>(Arrays.asList(
		VitalCoreOntology.hasEdgeSource.getURI(),
		VitalCoreOntology.hasEdgeDestination.getURI(),
		VitalCoreOntology.hasHyperEdgeSource.getURI(),
		VitalCoreOntology.hasHyperEdgeDestination.getURI()
	));
	
	public static VitalSignsLuceneBridge get() {
		
		if(singleton == null) {
			
			synchronized (VitalSignsLuceneBridge.class) {
				
				if(singleton == null) {
					
					singleton = new VitalSignsLuceneBridge();
					
				}
				
				
			}
		}
		
		return singleton;
		
	}
	
	public static Set<String> allStaticFieldsToLoad = new HashSet<String>(Arrays.asList(
		VitalSignsLuceneBridge.URI_FIELD,
		VitalSignsLuceneBridge.VITAL_TYPE_FIELD,
		VitalSignsLuceneBridge.TYPE_FIELD,
		VitalSignsLuceneBridge.EDGE_DEST_URI_FIELD,
		VitalSignsLuceneBridge.EDGE_SRC_URI_FIELD,
		VitalSignsLuceneBridge.HYPER_EDGE_DEST_URI_FIELD,
		VitalSignsLuceneBridge.HYPER_EDGE_SRC_URI_FIELD
	));
	
	private VitalSignsLuceneBridge() {
		staticFieldsSet.add(URI_FIELD);
		staticFieldsSet.add(VITAL_TYPE_FIELD);
		staticFieldsSet.add(TYPE_FIELD);
		staticFieldsSet.add(EDGE_DEST_URI_FIELD);
		staticFieldsSet.add(EDGE_SRC_URI_FIELD);
		staticFieldsSet.add(HYPER_EDGE_DEST_URI_FIELD);
		staticFieldsSet.add(HYPER_EDGE_SRC_URI_FIELD);
		
		//reverse transient fields - ignore them when deserializing
		staticFieldsSet.add(VitalCoreOntology.hasOntologyIRI.getURI());
		staticFieldsSet.add(VitalCoreOntology.hasVersionIRI.getURI());
	}
	
	
	/**
	 * Convert vitalsigns object into Lucene Document. storeFields is master setting, store numeric fields is used if storefields=no and
	 * is necessary when aggregation functions are to be used.
	 * @param graphObject
	 * @param storeFields
	 * @param storeNumericFields
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public Document graphObjectToDocument(GraphObject graphObject, boolean storeFields, boolean storeNumericFieldsB) throws IOException { 
		
		
		String uri = graphObject.getURI();
		
		if(uri == null) {
			throw new IOException("Null URI in graph object.");
		}
		
		if(uri.isEmpty()) {
			throw new IOException("Empty URI in graph object.");
		}
		

		Set<String> propertiesList = new HashSet<String>();
		
//		Map<String, Field> fields = new HashMap<String, Field>();
		
		Store storeDynamicFields = storeFields ? Store.YES : Store.NO;
		
		Store storeNumericFields = storeFields || storeNumericFieldsB ? Store.YES : Store.NO;
		
		//the document is stored as a compact string now
//		storeDynamicFields = Store.NO
		
		DomainOntology _do = VitalSigns.get().getClassDomainOntology(graphObject.getClass());
		if(_do == null) throw new IOException("No domain ontology found for class: " + graphObject.getClass().getCanonicalName());
		
		//reverse transient properties
//		fields.put(VitalCoreOntology.hasOntologyIRI.getURI(), _do.uri)
//		fields.put(VitalCoreOntology.hasVersionIRI.getURI(), _do.toVersionString())
		
		graphObject.set( Property_hasOntologyIRI.class, oldVersionFilter( _do.getUri() ) );
		graphObject.set( Property_hasVersionIRI.class, _do.toVersionString() );
		
		
		
		Document d = new Document();
		
		d.add(new Field(URI_FIELD, uri, Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		
		String rdfClass = VitalSigns.get().getClassesRegistry().getClassURI(graphObject.getClass());
		
		d.add(new Field(VITAL_TYPE_FIELD, oldVersionFilter( rdfClass ), Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		
		IProperty types = (IProperty) graphObject.get( Property_types.class );
		
		if( types != null) {
			
			Collection typesC = (Collection) types.rawValue();
			
			for( Object typeURI : typesC ) {
				
				d.add(new Field(TYPE_FIELD, oldVersionFilter( (String)typeURI ), Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				
			}
			
		}
		
		if(graphObject instanceof VITAL_Edge) {
			
			VITAL_Edge e = (VITAL_Edge) graphObject;
			d.add(new Field(EDGE_SRC_URI_FIELD, (String)e.getProperty("sourceURI"), Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			d.add(new Field(EDGE_DEST_URI_FIELD, (String)e.getProperty("destinationURI"), Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			
			
		} else if(graphObject instanceof VITAL_HyperEdge) {
			
			VITAL_HyperEdge e = (VITAL_HyperEdge) graphObject;
			d.add(new Field(HYPER_EDGE_SRC_URI_FIELD, (String)e.getProperty("sourceURI"), Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			d.add(new Field(HYPER_EDGE_DEST_URI_FIELD, (String)e.getProperty("destinationURI"), Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			
			
		}
		
		//first iteration to determine type, there are some types we don't want to index, like Content
		
		for( Entry<String, IProperty> entry : graphObject.getPropertiesMap().entrySet() ) {
			
			String propertyURI = entry.getKey();
			
			PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(propertyURI);
			if(pm == null) {
//				throw new RuntimeException("Property not found: " + propertyURI);
			    //external properties
			}
			
			IProperty property = entry.getValue();

			if(propertiesToSkip.contains(propertyURI)) {
				if(skippedButIndexed.contains(propertyURI)) {
					propertiesList.add(propertyURI);
				}
				continue;
			}
			

			propertyURI = oldVersionFilter(propertyURI);
			
			
			propertiesList.add(propertyURI);
			
			
			//don't index transient props
			if ( pm != null && pm.isTransientProperty() ) continue; 			

			if( pm != null && pm.isDontIndex() ) {
				
				if( ! storeFields ) continue;
				
				if(pm.isMultipleValues()) {
					
					Collection mvp = (Collection) property.unwrapped();
					
					for(Object o : mvp) {
						
						IProperty p = (IProperty) o;
						
						String value = oldVersionFilter( toStringValue(property.unwrapped()) );
//					fields.put(propertyURI, new Field(propertyURI, value, Store.YES, Field.Index.NO));
						d.add( new Field(propertyURI, value, Store.YES, Field.Index.NO) );
						
					}

					
				} else {
					
					String value = oldVersionFilter ( toStringValue(property.unwrapped()) );
//				fields.put(propertyURI, new Field(propertyURI, value, Store.YES, Field.Index.NO));
					d.add( new Field(propertyURI, value, Store.YES, Field.Index.NO) );
				}
				
				
				continue;
				
			}
			
			//IProperty
			Collection vals = null;
			
			if(pm != null && pm.isMultipleValues()) {
			
				vals = (Collection) property.unwrapped();
					
			} else {
			
			    
			    //multivalue external properties
			    if(pm == null && property instanceof MultiValueProperty) {
			        
			        MultiValueProperty mvp = (MultiValueProperty)property;
			        
			        vals = new ArrayList(mvp.size());
			        
			        for(Iterator<IProperty> iterator = mvp.iterator(); iterator.hasNext(); ) {
			            
			            IProperty p = iterator.next();
			            vals.add(p.unwrapped());
			            
			        }
			        
			    } else {
			        
			        vals = Arrays.asList(property.unwrapped());
			        
			    }
			    
			
			}
			
			for(Object object : vals) {
				
				if(object instanceof StringProperty) {
					
					String v = oldVersionFilter( ((StringProperty) object).toString() );
							
//					fields.put(propertyURI, new Field(propertyURI, v, storeDynamicFields, Field.Index.ANALYZED_NO_NORMS));
					d.add( new Field(propertyURI, v, storeDynamicFields, Field.Index.ANALYZED_NO_NORMS) );
					
//					fields.put(propertyURI + LOWER_CASE_SUFFIX, new Field(propertyURI + LOWER_CASE_SUFFIX, v.toLowerCase(), Store.NO, Field.Index.ANALYZED_NO_NORMS));
					d.add( new Field(propertyURI + LOWER_CASE_SUFFIX, v.toLowerCase(), Store.NO, Field.Index.ANALYZED_NO_NORMS) );
					
					if( (pm == null || !pm.isAnalyzedNoEquals()) && v.length() < MAX_EQ_STRING_LENGTH) {
//						fields.put(propertyURI + EQ_SUFFIX, new Field(propertyURI + EQ_SUFFIX, v, Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
//						fields.put(propertyURI + EQ_LOWER_CASE_SUFFIX, new Field(propertyURI + EQ_LOWER_CASE_SUFFIX, v.toLowerCase(), Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
						d.add( new Field(propertyURI + EQ_SUFFIX, v, Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS) );
						d.add( new Field(propertyURI + EQ_LOWER_CASE_SUFFIX, v.toLowerCase(), Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS) );
					}
					
				} else if(object instanceof BooleanProperty) {
					
					Boolean v = ((BooleanProperty) object).booleanValue();
							
//					fields.put(propertyURI, new Field(propertyURI, "" + v.toString(), storeNumericFields, Field.Index.NOT_ANALYZED_NO_NORMS));
					d.add( new Field(propertyURI, "" + v.toString(), storeNumericFields, Field.Index.NOT_ANALYZED_NO_NORMS) );
					
				} else if(object instanceof DateProperty || object instanceof DoubleProperty || object instanceof FloatProperty || object instanceof IntegerProperty || object instanceof LongProperty) {
					
					Field numericField = null;
							
					String s = null;		
					
					if(object instanceof DateProperty) {
						numericField = new LongField(propertyURI, ((DateProperty)object).getTime(), storeNumericFields);
						if(storeFields) s = "" + ((DateProperty)object).getTime();
					} else if(object instanceof DoubleProperty) {
						numericField = new DoubleField(propertyURI, ((DoubleProperty) object).doubleValue(), storeNumericFields);
						if(storeFields) s = "" + object.toString();
					} else if(object instanceof FloatProperty) {
						numericField = new FloatField(propertyURI, ((FloatProperty)object).floatValue(), storeNumericFields);
						if(storeFields) s = "" + object.toString();
					} else if(object instanceof LongProperty) {
						numericField = new LongField(propertyURI, ((LongProperty)object).longValue(), storeNumericFields);
						if(storeFields) s = "" + object.toString();
					} else if(object instanceof IntegerProperty) {
						numericField = new IntField(propertyURI, ((IntegerProperty)object).intValue(), storeNumericFields);
						if(storeFields) s = "" + object.toString();
					}
					
//					fields.put(propertyURI, numericField);
					d.add( numericField );
					
					if(s != null) {
						
//						fields.put(propertyURI + EQ_SUFFIX, new Field(propertyURI + EQ_SUFFIX, s, Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
						d.add( new Field(propertyURI + EQ_SUFFIX, s, Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS) );
					}
					
					
					
				} else if(object instanceof URIProperty) {
					
					String oURI = oldVersionFilter( ((URIProperty)object).get() );
					
//					fields.put(propertyURI, new Field(propertyURI, oURI, storeDynamicFields, Field.Index.NOT_ANALYZED_NO_NORMS));
					d.add( new Field(propertyURI, oURI, storeDynamicFields, Field.Index.NOT_ANALYZED_NO_NORMS) );
					
				} else if(object instanceof GeoLocationProperty) {
				    
				    GeoLocationProperty geoP = (GeoLocationProperty) object;
				    
				    d.add(new Field(propertyURI, geoP.toRDFValue(), storeDynamicFields, Field.Index.NOT_ANALYZED_NO_NORMS));
					
				} else if(object instanceof TruthProperty) {
				    
				    TruthProperty truthP = (TruthProperty) object;
				    
				    d.add(new Field(propertyURI, truthP.toString(), storeDynamicFields, Field.Index.NOT_ANALYZED_NO_NORMS));
				    
				} else {
					throw new IOException("Unhandled object type: " + object.getClass() + " - " + object);
				}
				
				//store external properties in another field - type required!
				if(storeFields && pm == null) {
					byte[] f = SerializationUtils.serialize((Serializable) object);
					String fs = Base64.encodeBase64String(f);
					d.add( new Field(EXTERNAL_FIELD_PREFIX + propertyURI, fs, Store.YES, Index.NO, TermVector.NO));
					
				}
				
			}
			
		}
		
		//keep URI just in case
		StringBuilder propertiesListField = new StringBuilder("URI");
		for(String propertyURI : propertiesList) {
			propertiesListField.append(' ').append(propertyURI);
		}
		
		d.add( new Field(PROPERTIES_FIELD, propertiesListField.toString(), Store.NO, Index.ANALYZED_NO_NORMS));

		
		/**
		 * no longer necessary!
		if(graphObject instanceof VITAL_Edge) {
			VITAL_Edge edge = (VITAL_Edge)graphObject
			if(edge.getSourceURI() == null) throw new IOException("Edge source URI cannot be null!");
			if(edge.getDestinationURI() == null) throw new IOException("Edge destination URI cannot be null!");
			d.add(new Field(EDGE_SRC_URI_FIELD, edge.getSourceURI(), Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			d.add(new Field(EDGE_DEST_URI_FIELD, edge.getDestinationURI(), Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		} else if(graphObject instanceof VITAL_HyperEdge) {
			VITAL_HyperEdge hyperEdge = (VITAL_Edge)graphObject
			if(hyperEdge.getSourceURI() == null) throw new IOException("HyperEdge source URI cannot be null!");
			if(hyperEdge.getDestinationURI() == null) throw new IOException("HyperEdge destination URI cannot be null!");
			d.add(new Field(HYPER_EDGE_SRC_URI_FIELD, hyperEdge.getSourceURI(), Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			d.add(new Field(HYPER_EDGE_DEST_URI_FIELD, hyperEdge.getDestinationURI(), Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		}
		 */
		
		/*
		List<Field> fieldsList = new ArrayList<Field>(fields.values());
		
		Collections.sort(fieldsList, fieldComparator);
		
		for(Field f : fieldsList) {
			
			d.add(f);
			
		}
		*/
		
		
		/*
		if(storeFields) {
			
			d.add(new Field(COMPACT_STRING_FIELD, graphObject.toCompactString(), Store.YES, Field.Index.NO))
			
		}
		*/
		
		graphObject.set( Property_hasOntologyIRI.class, null );
		graphObject.set( Property_hasVersionIRI.class, null );
		
		return d;
	}
	
	
	
	public GraphObject documentToGraphObject(Document document) throws IOException {
		
		long start = 0;
		if(log.isDebugEnabled()) {
			log.debug("Converting document into graph object...");
			start = System.currentTimeMillis();
		}
		
		// just read the compact string field
//		if(true) {
//			return CompactStringSerializer.fromString(document.get(COMPACT_STRING_FIELD))
//		}
		
		String uriString = document.get(URI_FIELD);
		if(uriString == null) throw new IOException("No " + URI_FIELD + " field");

//		Class<? extends GraphObject> cls = 
		
//		String rdfClazz = document.get(CLAZZ_FIELD)
//		if(rdfClazz == null) throw new IOException("No ${CLAZZ_FIELD} field")
		
		String ontologyIRI = document.get(VitalCoreOntology.hasOntologyIRI.getURI());
		String versionIRI = document.get(VitalCoreOntology.hasVersionIRI.getURI());
		
        
        String vitalRDFType = document.get(VITAL_TYPE_FIELD);
        
        if(StringUtils.isEmpty(vitalRDFType)) throw new IOException("No " + VITAL_TYPE_FIELD + " field");
        
        
        Map<String, String> oldVersions = null;
        
		
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
				

				
				
			} else {
			
				log.error("Domain ontology with IRI not found: " + ontologyIRI + ", object class (RDF): " + vitalRDFType + ", uri: " + uriString);
				
			}
			
		}
		
		
	    vitalRDFType = toOldVersion(vitalRDFType, oldVersions);
		
      
        String[] rdfTypes = document.getValues(TYPE_FIELD);
        
        if(rdfTypes == null || rdfTypes.length < 1) {
            throw new IOException("No " + TYPE_FIELD + " field(s)");
        }
        
        //deserialize in the same manner as in rdf
        List<String> types = null;
        
        //do the inferencing part, types hierarchy is cached, should it be
        
        for(String rdfType : rdfTypes) {

            rdfType = toOldVersion(rdfType, oldVersions);
            
            if(rdfType.equals(vitalRDFType)) continue;
            
            if(types == null) {
                types = new ArrayList<String>();
                types.add(vitalRDFType);
            }
            
            types.add(rdfType);
            
        }
        
        ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(vitalRDFType);
        if(cm == null) throw new IOException("Class not found in VitalSigns: " + vitalRDFType);
        
		
		
//		Class cls = VitalSigns.get().getGroovyClass(rdfClazz)
//		if(cls == null) throw new IOException("No groovy class for URI found: ${rdfClazz}");
				
		GraphObject object = null;
		try {
			object = cm.getClazz().newInstance();
		} catch (Exception e) {
			throw new IOException(e);
		}
		
		object.setURI(uriString);
		if(types != null) {
			object.set(Property_types.class, types);
		}
		
		
		if(object instanceof VITAL_Edge) {
			VITAL_Edge e = (VITAL_Edge) object;
			e.setSourceURI(document.get(EDGE_SRC_URI_FIELD));
			e.setDestinationURI(document.get(EDGE_DEST_URI_FIELD));
		} else if(object instanceof VITAL_HyperEdge) {
			VITAL_HyperEdge e = (VITAL_HyperEdge) object;
			e.setSourceURI(document.get(HYPER_EDGE_SRC_URI_FIELD));
			e.setDestinationURI(document.get(HYPER_EDGE_DEST_URI_FIELD));
			
		}
		
		Map<String, List> multiValuesMap = null;
		
		Map<String, List> externalPropertiesMap = null;
		
		Set<String> externalProperties = null;
		
		for(IndexableField field : document) {
		    
		    String fn = field.name();
            
            if(staticFieldsSet.contains(fn)) {
                continue;
            }
		    
            if(fn.startsWith(EXTERNAL_FIELD_PREFIX)) {
                
                fn = fn.substring(EXTERNAL_FIELD_PREFIX.length());
                if(externalProperties == null) externalProperties = new HashSet<String>();
                externalProperties.add(fn);
                
            }
            
		}
		
		for(IndexableField field : document) {
			
			String fn = field.name();
			
			if(staticFieldsSet.contains(fn)) {
				continue;
			}
			
			IndexableFieldType ft = field.fieldType();
			if(!ft.stored()) continue;
			
			if(fn.startsWith(EXTERNAL_FIELD_PREFIX)) {
				
			    
			    if( !(object instanceof VITAL_GraphContainerObject) && ! VitalSigns.get().getConfig().externalProperties) {
			        throw new RuntimeException("Cannot deserialize an object with external properties - they are disabled, property: " + fn);
			    }
			    
				fn = fn.substring(EXTERNAL_FIELD_PREFIX.length());
				
				//deserialize it from base64 encoded form
				
				IProperty prop = (IProperty) VitalJavaSerializationUtils.deserialize(Base64.decodeBase64(field.stringValue()));
				
				List vals = null;
				if(externalPropertiesMap == null) {
					externalPropertiesMap = new HashMap<String, List>();
				} else {
					vals = externalPropertiesMap.get(fn);
				}
				
				if(vals == null) {
					vals = new ArrayList();
					externalPropertiesMap.put(fn, vals);
				}
				
				vals.add(prop);
				
				
				
			} else {
				
			    String originalFieldName = fn;
			    
			    fn = toOldVersion(fn, oldVersions);
				
				PropertyMetadata prop = VitalSigns.get().getPropertiesRegistry().getProperty(fn);
				
				if(prop == null) {
				    
				    if ( externalProperties != null && externalProperties.contains(originalFieldName ) ) {
				        continue;
				    }
				    
				    
                    if( VitalSigns.get().getConfig().versionEnforcement != VersionEnforcement.lenient ) {
                        
//                        throw new RuntimeException("Strict version mode - persisted object version " + _do.toVersionString() + " does not match currently loaded: " + _cdo.toVersionString());
                        throw new IOException("Property not found : " + fn + " " + object.getClass().getCanonicalName());
                        
                    } else {
                        
                        log.warn("Property not found: " + fn + " " + object.getClass().getCanonicalName());
                        
                    }
                    
					//ignore such errors - assumed external properties
//					throw new IOException("Property with URI not found: " + fn);
					continue;
				}
				
				String shortName = RDFUtils.getPropertyShortName(fn);
				
				Object value = deserializeValue(prop.getBaseClass(), field, oldVersions);
				
				if(prop.isMultipleValues()) {
					
					List v = null;
					
					if(multiValuesMap == null) {
						multiValuesMap = new HashMap<String, List>();
					} else {
						v = multiValuesMap.get(shortName);
					}
					
					if(v == null) {
						v = new ArrayList();
						multiValuesMap.put(shortName, v);
					}
					
					v.add(value);
					
				} else {
					
					object.setProperty(shortName, value);
					
				}
				
			}

							
			
			
		}
		
		
		if(multiValuesMap != null) {
			
			for( Entry<String, List> entry : multiValuesMap.entrySet()) {
				object.setProperty(entry.getKey(), entry.getValue());
			}
			
		}
		
		if(externalPropertiesMap != null) {
			
			for( Entry<String, List> entry : externalPropertiesMap.entrySet()) {
				List l = entry.getValue();
				if(l.size() > 1) {
					object.setProperty(entry.getKey(), l);
				} else {
					object.setProperty(entry.getKey(), l.get(0));
				}
			}
			
		}
		
		if(log.isDebugEnabled()) {
			long stop = System.currentTimeMillis();
			log.debug("Object with uri: " + uriString + " parsed from lucene document - time: " + (stop-start) + "ms");
		}
		
		return object;
		
	}
	
	public static String toOldVersion(String input,
            Map<String, String> oldVersions) {
        
	    if(input == null) return null;
	    
	    if(oldVersions == null) return input;
	    
	    for(Entry<String, String> e : oldVersions.entrySet()) {
	        
	        input = input.replace(e.getKey() + '#', e.getValue() + '#');
	        
	    }
	    
        return input;
    }


    public static Object deserializeValue(Class<? extends IProperty> pc,
			IndexableField field, Map<String, String> oldVersions) throws IOException {
		
		Object value = null;
		
		if(StringProperty.class.equals(pc)) {
		    //process?
//		    value = toOldVersion( field.stringValue(), oldVersions );	
		    value = field.stringValue();	
		} else if(URIProperty.class.equals(pc)) {
			value = toOldVersion( field.stringValue(), oldVersions);
		} else if(BooleanProperty.class.equals(pc)) {
			value = Boolean.parseBoolean(field.stringValue());
		} else if(IntegerProperty.class.equals(pc)) {
			value = (Integer) field.numericValue();
		} else if(LongProperty.class.equals(pc)) {
			value = (Long) field.numericValue();
		} else if(FloatProperty.class.equals(pc)) {
			value = (Float) field.numericValue();
		} else if(DoubleProperty.class.equals(pc)) {
			value = (Double) field.numericValue();
		} else if(DateProperty.class.equals(pc)) {
			value = new Date((Long)field.numericValue());
		} else if(GeoLocationProperty.class.equals(pc)) {
		    value = GeoLocationProperty.fromRDFString(field.stringValue());
		} else if(TruthProperty.class.equals(pc)) {
		    value = Truth.fromString(field.stringValue());
		} else {
			throw new IOException("Unhandled property value type: " + pc);
		}
		
		return value;
		
	}


	private String toStringValue(IProperty input) {
		
		String s = null;
			
		if(input instanceof StringProperty) {
			s = ((StringProperty)input).toString();
		} else if(input instanceof URIProperty) {
			s = ((URIProperty) input).get();
		} else if(input instanceof BooleanProperty) {
			s = "" + ((BooleanProperty)input).booleanValue();
		} else if(input instanceof IntegerProperty) {
			s = "" + ((IntegerProperty)input).intValue();
		} else if(input instanceof LongProperty) {
			s = "" + ((LongProperty)input).longValue();
		} else if(input instanceof DoubleProperty) {
		s = "" + ((DoubleProperty)input).doubleValue();
		} else if(input instanceof FloatProperty) {
			s = "" + ((FloatProperty)input).floatValue();
		} else if(input instanceof DateProperty) {
			s = "" + ((DateProperty)input).getTime();
		} else if(input instanceof GeoLocationProperty) {
		    s = ((GeoLocationProperty)input).toRDFValue();
		} else if(input instanceof TruthProperty) {
		    s = ((TruthProperty)input).toString();
		} else {
			throw new RuntimeException("Unsupported data type: " + input.getClass().getCanonicalName());
		}
		
		return s;
	}
	
	static Comparator<Field> fieldComparator = new Comparator<Field>() {
		
		@Override
		public int compare(Field o1, Field o2) {
			return o1.name().compareTo(o2.name());
		}
		
	};	
	
   public static String oldVersionFilter(String input) {
       
       if(input == null) return null;
       
       VersionedURI vu = VersionedURI.analyze(input);
       return vu.versionlessURI;
       
    }
   
}
