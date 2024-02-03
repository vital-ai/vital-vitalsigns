package ai.vital.vitalsigns.json

import java.io.IOException
import java.util.ArrayList
import java.util.Collection
import java.util.Date
import java.util.LinkedHashMap
import java.util.List
import java.util.Map
import java.util.Map.Entry

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.conf.VitalSignsConfig.VersionEnforcement;
import ai.vital.vitalsigns.datatype.Truth;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_GraphContainerObject;
import ai.vital.vitalsigns.model.properties.Property_types;
import ai.vital.vitalsigns.model.properties.Property_vitaltype;
import ai.vital.vitalsigns.model.property.GeoLocationProperty;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.LongProperty;
import ai.vital.vitalsigns.model.property.MultiValueProperty;
import ai.vital.vitalsigns.model.property.OtherProperty;
import ai.vital.vitalsigns.model.property.PropertyFactory;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.rdf.RDFUtils;

public class JSONSerializer {

    private final static Logger log = LoggerFactory.getLogger(JSONSerializer.class);
    
    public final static long MAX_SAFE_INTEGER = (long)Math.pow(2, 53) - 1L;
    public final static long MIN_SAFE_INTEGER = -1L * ( (long)Math.pow(2, 53) - 1L );
    
	private static ObjectMapper mapper = new ObjectMapper();
	
	public static String toJSONString(GraphObject go) {
		try {
			return mapper.writeValueAsString(toJSONMap(go));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static LinkedHashMap<String, Object> toJSONMap(GraphObject go) {
		
		LinkedHashMap<String, Object> o = new LinkedHashMap<String, Object>();

		URIProperty vitalType = (URIProperty) ((IProperty)go.get(Property_vitaltype.class)).unwrapped();

		// TODO figure out why this is string property ands not multi-value prop
		def types_object = ((IProperty)go.get(Property_types.class)).unwrapped();

		// MultiValueProperty types = (MultiValueProperty) ((IProperty)go.get(Property_types.class)).unwrapped();

		MultiValueProperty types = null

		if(types_object instanceof MultiValueProperty) {

			types = types_object
		}
		else {

			List<IProperty> props = new ArrayList<IProperty>();

			props.add(types_object)

			MultiValueProperty<URIProperty> p = new MultiValueProperty(props)

			types = p
		}

		o.put("type", vitalType.get());
		List<String> typesList = new ArrayList<String>();
		for(Object t : types) {
			typesList.add(((URIProperty)t).get());
		}
		o.put("types", typesList);
		o.put("URI", go.getURI());
		
		
		for(Entry<String, IProperty> propEntry :  go.getPropertiesMap().entrySet()) {
				
			Object prop = propEntry.getValue().rawValue();
			
			Object jsonValue = toJsonValue(prop);
			
			if(jsonValue == null) continue;
			
			String pURI = propEntry.getKey();
			if(VitalCoreOntology.vitaltype.getURI().equals(pURI)
				|| VitalCoreOntology.types.getURI().equals(pURI)
				|| VitalCoreOntology.URIProp.getURI().equals(pURI)) {
				continue;
			}
			
			PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(pURI);
			
			if(pm != null) {
			    
			    //write as a short name ? 
				o.put(pm.getURI(), jsonValue);
				
			} else {
				
				String cls = null;
				
				IProperty unwrapped = propEntry.getValue().unwrapped();
				
				if(unwrapped instanceof MultiValueProperty) {
					cls = ((MultiValueProperty)unwrapped).getFirst().getClass().getCanonicalName();
				} else {
					cls = unwrapped.getClass().getCanonicalName();
				}
						
				
				LinkedHashMap<String, Object> wrapped = new LinkedHashMap<String, Object>();
				wrapped.put("_type", cls);
				wrapped.put("value", jsonValue);
				o.put(pURI, wrapped);
			}
			
		}
		
		return o;
		
	}
	
	static Object toJsonValue(Object value) {
		
		if(value == null) return null;

		Object jsonValue = null;
		
		
		if(value instanceof String || value instanceof Number || value instanceof Boolean) {
			
		    if(value instanceof Long) {
		        
		        //check range
		        long l = (Long)value;
		        
		        if(l > MAX_SAFE_INTEGER || l < MIN_SAFE_INTEGER) {
		            
		            jsonValue = "" + l;
		            
		        } else {
		            
		            jsonValue = value;
		            
		        }
		        
		    } else {
		        
		        jsonValue = value;
		    }
		    
			
		} else if(value instanceof URIProperty) {
			
			jsonValue = ((URIProperty)value).get();
			
		} else if(value instanceof Date) {
		
			jsonValue = ((Date)value).getTime();
			
		} else if(value instanceof GeoLocationProperty) {
		
			jsonValue = ((GeoLocationProperty)value).toJSONMap();
			
		} else if(value instanceof Truth) {
		    
		    jsonValue = ((Truth)value).toString();
		    
		} else if(value instanceof OtherProperty) {
			
			jsonValue = ((OtherProperty)value).toJSONMap();
			
		} else if( value instanceof Collection) {
		
			List<Object> arr = new ArrayList<Object>();
			
			//as array list
			for(Object v : ((Collection)value)) {
				
				Object jv = toJsonValue(v);
				
				arr.add(jv);
				
			}
			
			jsonValue = arr;
			
			if(arr.size() < 1) return null;
		
		} else {
		    throw new RuntimeException("Unexpected value: " + value);
		}
		
		return jsonValue;

				
	}
	
	public static GraphObject fromJSONMap(LinkedHashMap<String, Object> map) {
		
		String vitaltype = (String) map.get("type");
		
		if(vitaltype == null || vitaltype.isEmpty()) return null;
		
		ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(vitaltype);
		
		if(cm == null) return null;
		
		Class<? extends GraphObject> clazz = cm.getClazz();
		
		GraphObject go;
		try {
			go = clazz.newInstance();
		} catch (Exception e2) {
			return null;
		}
		
		go.setURI((String) map.get("URI"));
		
		
		List<String> types = (List<String>) map.get("types");
		if(types != null) {
			go.setProperty(VitalCoreOntology.types.getLocalName(), types);
		}
		
		/*
		MultiValueProperty typesP = (MultiValueProperty) ((IProperty) go.get(Property_types.class)).unwrapped();

		List<Class<? extends GraphObject>> classes = new ArrayList<Class<? extends GraphObject>>();
		
		for(Object t : typesP) {
			
			ClassMetadata cx = VitalSigns.get().getClassesRegistry().getClass(((URIProperty)t).get());
			
			if(cx == null) continue;
			
			classes.add(cx.getClazz());
			
		}
		*/
		
		
		for(Entry<String, Object> e : map.entrySet()) {
			
			String key = e.getKey();
			
			if(key.equals("type") || key.equals("URI") || key.equals("types") ) continue;
			
			Object value = e.getValue();
			
			/*
			if( go instanceof VITAL_GraphContainerObject) {
				
				//all values are wrapped!
				if(!(value instanceof Map)) throw new RuntimeException("All graph container object")
				
				go.setProperty(key, value);
				continue;
				
			}
			
			*/
			
			PropertyMetadata pm = null;
			
			if(go instanceof VITAL_GraphContainerObject) {
			    
			} else {
			    
			        if(key.contains(":")) {
			            
			            //either a property or external property
		                pm = VitalSigns.get().getPropertiesRegistry().getProperty(key);
		                
			        } else {
			            
			            throw new RuntimeException("Short property names in json objects are no longer supported since 0.2.301");
			            /*

			            try {
			                pm = VitalSigns.get().getPropertiesRegistry().getPropertyByShortName(clazz, key);
			            } catch (NoSuchFieldException e1) {}
			            
			            if(pm == null && types.size() > 1) {
			                
			                for(String tp : types) {
			                    
			                    ClassMetadata class1 = VitalSigns.get().getClassesRegistry().getClass( tp );
			                    
			                    if(class1 != null) {
			                        
			                        try {
			                            pm = VitalSigns.get().getPropertiesRegistry().getPropertyByShortName(class1.getClazz(), key); 
			                        } catch(NoSuchFieldException e2) {}
			                        
			                        
			                        if(pm != null) break;
			                        
			                    }
			                    
			                }
			                
			            }
			            
			            if(pm == null) {
			                
                            if( VitalSigns.get().getConfig().versionEnforcement != VersionEnforcement.lenient ) {
                                
                                throw new RuntimeException("Property not found : " + key + " " + go.getClass().getCanonicalName());
                                
                            } else {
                                
                                log.warn("Property not found: " + key + " " + go.getClass().getCanonicalName());
                                
                            }
//                          throw new RuntimeException("External or container dynamic property " + propertyURI + " must define its type (name|type=\"...\")");
                         
                            continue;
			            }
			            
			            */
			            
			        }
			        
			    
			}
			
			
			if(pm == null) {
				
			    
                //determine if it's a normal property but not found
                
                boolean normalValue = false;
                
                if(value instanceof Map) {
                   
                    Map mv = (Map) value;
                    
                    if( ! mv.containsKey("_type")) {
                        normalValue = true;
                    }
                    
                    //external properties have type
                    
                } else {
                    normalValue = true;
                }
                
                if(normalValue) {
                    
                    if( VitalSigns.get().getConfig().versionEnforcement != VersionEnforcement.lenient ) {
                        
                        throw new RuntimeException("Property not found : " + key + " " + go.getClass().getCanonicalName());
                        
                    } else {
                        
                        log.warn("Property not found: " + key + " " + go.getClass().getCanonicalName());
                        
                    }
//                  throw new RuntimeException("External or container dynamic property " + propertyURI + " must define its type (name|type=\"...\")");
                 
                    continue;
                    
                } else {
                    
                    
                    if(!(go instanceof VITAL_GraphContainerObject) && !VitalSigns.get().getConfig().externalProperties) {
                        throw new RuntimeException("Cannot deserialize an object with external properties - they are disabled, property: " + key);
                    }
                    
                    if(!(value instanceof Map)) throw new RuntimeException("External or container properties values must be wrapped: " + key);
                    
                    Map v = (Map) value;
                    
                    String type = (String) v.get("_type");
                    if(type == null) throw new RuntimeException("External or container property wrapper type not set, key: " + key);
                    
                    Class<? extends IProperty> bType = null;
                    
                    try {
                        bType = (Class<? extends IProperty>) Class.forName(type);
                    } catch(Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    
                    
                    value = v.get("value");
                    
                    if( value == null) new RuntimeException("External or container property wrapper value not set, key: " + key);
                    
                    //long properties are special
                    if(bType == LongProperty.class && value instanceof String) {
                        value = Long.parseLong((String) value);
                    }
                    
                    value = PropertyFactory.createBasePropertyFromRawValue(bType, value);
                    
                    //external property
                    go.setProperty(key, value);
                    
                }
                
				
			} else {
			
				if(value instanceof Map) {
				
					Map m = (Map) value;
					if(m.get("lexicalForm") != null) {
						//other propety
						value = OtherProperty.fromJSONMap((LinkedHashMap<String, Object>) m);
//					} else if(m.get("") != null) {
//					    value = GeoLocationProperty.fromJSONMap((LinkedHashMap<String, Object>) m);
					}
				}
				
				if(pm.getBaseClass() == LongProperty.class && value instanceof String) {
				    value = Long.parseLong((String) value);
				}
				
				/*
				PropertyMetadata pm = null;
				
				for(Class<? extends GraphObject> g : classes) {
					try {
						pm = VitalSigns.get().getPropertiesRegistry().getPropertyByShortName(g, shortName);
					} catch (NoSuchFieldException e1) {}
					if(pm != null) break;
				}
				
				if(pm == null) continue;
				 */
				
				go.setProperty(RDFUtils.getPropertyShortName(key), value);
				
			}
			
			
		}
		
		return go;
		
	}
	
	
	public static GraphObject fromJSONString(String jsonString) throws IOException {
		
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, Object> m = mapper.readValue(jsonString, LinkedHashMap.class);
		
		return fromJSONMap(m);
	}

	
	public static List<GraphObject> fromJSONArrayString(String jsonString) throws IOException {
		
		@SuppressWarnings("unchecked")
		List<LinkedHashMap<String, Object>> objects = mapper.readValue(jsonString, List.class);
		
		List<GraphObject> l = new ArrayList<GraphObject>();
		
		for(LinkedHashMap<String, Object> o : objects) {
			
			GraphObject g = fromJSONMap(o);
			
			if(g != null) l.add(g);
			
		}
		
		return l;
		
	}

	public static List<LinkedHashMap<String, Object>> toJSONList(Collection<GraphObject> objects) {
		
		List<LinkedHashMap<String, Object>> l = new ArrayList<LinkedHashMap<String, Object>>();
		
		for(GraphObject g : objects) {
			
			l.add(toJSONMap(g));
			
		}
		
		return l;
		
	}
	
	public static String toJSONString(Collection<GraphObject> objects) throws IOException {
		return mapper.writeValueAsString(toJSONList(objects));
	}
			
}
