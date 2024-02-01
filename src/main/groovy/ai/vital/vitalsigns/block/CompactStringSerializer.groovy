package ai.vital.vitalsigns.block;

import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.Comparator
import java.util.Date
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.Map.Entry
import java.util.Set
import org.apache.commons.lang3.StringEscapeUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.classes.ClassMetadata
import ai.vital.vitalsigns.conf.VitalSignsConfig.VersionEnforcement
import ai.vital.vitalsigns.datatype.Truth
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader.VersionedURI
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_GraphContainerObject
import ai.vital.vitalsigns.model.property.BooleanProperty
import ai.vital.vitalsigns.model.property.DateProperty
import ai.vital.vitalsigns.model.property.DoubleProperty
import ai.vital.vitalsigns.model.property.FloatProperty
import ai.vital.vitalsigns.model.property.GeoLocationProperty
import ai.vital.vitalsigns.model.property.IntegerProperty
import ai.vital.vitalsigns.model.property.LongProperty
import ai.vital.vitalsigns.model.property.MultiValueProperty
import ai.vital.vitalsigns.model.property.OtherProperty
import ai.vital.vitalsigns.model.property.StringProperty
import ai.vital.vitalsigns.model.property.TruthProperty
import ai.vital.vitalsigns.model.property.URIProperty
import ai.vital.vitalsigns.ontology.VitalCoreOntology
import ai.vital.vitalsigns.properties.PropertyMetadata
import ai.vital.vitalsigns.rdf.RDFUtils

public class CompactStringSerializer {

    private final static Logger log = LoggerFactory.getLogger(CompactStringSerializer.class)
    
	public static String toCompactString(GraphObject go) {

		StringBuilder sb = new StringBuilder()
		
		toCompactStringBuilder(go, sb)
		
		return sb.toString()
				
	}
	
	public static String oldVersionFilter(String input, boolean suppressed) {
	    
	    if(suppressed) return input
	    
	    VersionedURI vu = VersionedURI.analyze(input)
	    
	    return vu.versionlessURI
	    
	    /*
	    input = input.replace(DifferentDomainVersionLoader._OLDVERSION_NS_SUFFIX + "#", "#")
	    
	    if(input.endsWith(DifferentDomainVersionLoader._OLDVERSION_NS_SUFFIX)) {
	        input = input.substring(0, input.length() - DifferentDomainVersionLoader._OLDVERSION_NS_SUFFIX.length())
	    }
	    
	    return input
	    */
	    
	}
	
	
	public static StringBuilder toCompactStringBuilder(GraphObject go, StringBuilder sb) {
	    return toCompactStringBuilder(go, sb, false)
	}
	/**
	 * 
	 * @param go
	 * @param StringBuilder
	 * @return
	 */
	public static StringBuilder toCompactStringBuilder(GraphObject go, StringBuilder sb, boolean suppressOldVersionFilter) {
		
		if(go.getURI() == null) throw new RuntimeException("No URI set!");
		
		String rdfType = VitalSigns.get().getClassesRegistry().getClassURI(go.getClass());
		
		if(rdfType == null) throw new RuntimeException("No rdf type found object of class: " + go.getClass().getCanonicalName());
		
		sb.append("type=\"").append(oldVersionFilter(rdfType, suppressOldVersionFilter)).append('"').append("\tURI=\"").append(es(go.getURI(), suppressOldVersionFilter)).append('"');
		
		// if(false && go instanceof VITAL_GraphContainerObject) {

			/*
			Map<String, IProperty> propertiesMap = go.getPropertiesMap();
			
			//sort properties to output types first?
			//			List<String> keys = new ArrayList<String>(propertiesMap.keySet());
			//			
			//			Collections.so
			
			for(Entry<String, IProperty> propEntry : propertiesMap.entrySet()) {
				
				if(VitalCoreOntology.vitaltype.getURI().equals( propEntry.getKey() )) {
					continue;
				}
					
				IProperty value = propEntry.getValue();
				
				if(value == null) continue;
				
				
				//skip vital type
				
				//type is encoded along with

				sb.append('\t')
				.append(propEntry.getKey() + "|" + value.getClass().getCanonicalName())
				.append("=\"")
				.append(es(value.rawValue()))
				.append('"');
				
			}
			
			for( Entry<String, Object> propEntry : ((VITAL_GraphContainerObject) go).getOverriddenMap().entrySet() ) {
				
				Object value = propEntry.getValue();
				
				if(value == null) continue;

				sb.append('\t')
				.append(propEntry.getKey() + "|" + value.getClass().getCanonicalName())
				.append("=\"")
				.append(es(value))
				.append('"');
				
			}
			*/
			
		// } else {
			
			for(Entry<String, IProperty> propEntry : go.getPropertiesMap().entrySet()) {
				
				if(VitalCoreOntology.vitaltype.getURI().equals( propEntry.getKey() )) {
					continue;
				} else if(VitalCoreOntology.URIProp.getURI().equals( propEntry.getKey() )) {
					continue;
				}
				
				IProperty prop = propEntry.getValue().unwrapped();
				
				if(prop == null) continue;
				
				IProperty value = prop.unwrapped();
				
				if(value == null) continue;
		
				PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(propEntry.getKey());
				
				if(value instanceof MultiValueProperty) {
					
					for(Object o : ((MultiValueProperty)value) ) {
						
						IProperty v = (IProperty) o;
						
						sb.append('\t')
						.append(oldVersionFilter(propEntry.getKey(), suppressOldVersionFilter));
						if(pm == null) {
							sb.append('|').append(v.getClass().getCanonicalName());
						}
						sb.append("=\"")
						.append(es(v.rawValue(), suppressOldVersionFilter))
						.append('"');
						
					}
					
				} else {
					
					sb.append('\t')
					.append(oldVersionFilter(propEntry.getKey(), suppressOldVersionFilter));
					if(pm == null) {
						sb.append('|').append(prop.getClass().getCanonicalName());
					}
					sb.append("=\"")
					.append(es(value.rawValue(), suppressOldVersionFilter))
					.append('"');
					
				}
				
				
			}
		// }
		
		
		return sb;
		
	}
	
	public List<GraphObject> listFromString(String inputString) throws IOException {
		
		BufferedReader reader = new BufferedReader(new StringReader(inputString));
		
		List<GraphObject> objects = new ArrayList<GraphObject>(); 
		
		for(String line = reader.readLine(); line != null; line = reader.readLine()) {
			
			GraphObject go = fromString(line);
			
			if(go != null) {
				objects.add(go);
			}
			
		}
		
		return objects;
		
	}
	
	public static GraphObject fromString(String serializedForm) {
		return fromString(serializedForm, null);
	}
	
	/**
	 * The rdf types selector may be used - only the classes contained in that set will be deserialized
	 */
	public static GraphObject fromString(String serializedForm, Set<String> rdfTypesSelector) {
		
		int indexOfType = serializedForm.indexOf("type=\"");
		
		if(indexOfType < 0) return null;
		
		int indexOfFirstTab = serializedForm.indexOf('\t', indexOfType);
		
		if(indexOfFirstTab <= 0) return null;
		
		String type = (String) de(serializedForm.substring(indexOfType + 6, indexOfFirstTab - 1), String.class);
		
		if(rdfTypesSelector != null && !rdfTypesSelector.contains(type)) {
			return null;
		}
		
		ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(type);
		
		if(cm == null) throw new RuntimeException("Class not found in VitalSigns: " + type);
		
		Class<? extends GraphObject> clazz = cm.getClazz(); 
		
		Object o = null;
		
		try {
			o = clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if(!(o instanceof GraphObject)) return null;
		
		GraphObject go = (GraphObject) o;
		
		int indexOfURIPart = serializedForm.indexOf("URI=\"", indexOfFirstTab);
		
		if(indexOfURIPart < 0) return null;
		
		int indexOfSecondTab = serializedForm.indexOf('\t', indexOfURIPart);
		
		if(indexOfSecondTab < 0) indexOfSecondTab = serializedForm.length();
		
		String uri = (String) de(serializedForm.substring(indexOfURIPart + 5, indexOfSecondTab - 1), String.class);
		
		go.setURI(uri);
		
		// deserialize properties now
		
		if(indexOfSecondTab >= serializedForm.length()) return go;
		
		int startTabIndex = indexOfSecondTab;
		
		int nextTabIndex = serializedForm.indexOf('\t', startTabIndex + 1);
		
		if(nextTabIndex < 0) nextTabIndex = serializedForm.length();
		
		if(startTabIndex == nextTabIndex) return go;

		Map<String, Object> valuesMap = null;
		
		Map<String, List<Object>> externalPropertiesMap = null;
		
		// keep values in a map first
		
		while(nextTabIndex > 0 && nextTabIndex <= serializedForm.length()) {
			
			int firstEqualsSignIndex = serializedForm.indexOf("=\"", startTabIndex);
			
			if(firstEqualsSignIndex > 0) {
				
				String propertyURI = serializedForm.substring(startTabIndex+1, firstEqualsSignIndex);
				
				String serializedValue = serializedForm.substring(firstEqualsSignIndex + 2, nextTabIndex - 1);
				
				PropertyMetadata prop = VitalSigns.get().getPropertiesRegistry().getProperty(propertyURI);
				
				/*
				if(false && go instanceof VITAL_GraphContainerObject) {

//					int typeSep = propertyURI.indexOf('|');
//					if(typeSep <= 0) throw new RuntimeException("Intanceof of " + VITAL_GraphContainerObject.class.getCanonicalName() + " must define dynamic properties types (name|type=\"...\")");
//					
//					String name = propertyURI.substring(0, typeSep);
//					String cls = propertyURI.substring(typeSep + 1);
//					Class valueClass = null;
//					try {
//						valueClass = Class.forName(cls);
//					} catch (ClassNotFoundException e) {
//						throw new RuntimeException(e);
//					}
//					
//					go.setProperty(name, de(serializedValue, valueClass));
					
				} else {
				*/
				
					
				if(prop != null) {
						
					String shortName = RDFUtils.getPropertyShortName(propertyURI);
						
					if(valuesMap == null) valuesMap = new HashMap<String, Object>();
						
					// base type!
					Object desVal = de(serializedValue, prop.getBaseClass());
						
					if( ! prop.isMultipleValues()) {
							
						valuesMap.put(shortName, desVal);
							
					} else {
							
						List<Object> v = (List<Object>) valuesMap.get(shortName);
							
						if(v == null) {
							v = new ArrayList();
							valuesMap.put(shortName, v);
						}
							
						v.add(desVal);
							
					}
						
				} else {
						
					boolean skip = false;
					    
					// external property
					int typeSep = propertyURI.indexOf('|');
					
					if(typeSep <= 0) {

					    if(go instanceof VITAL_GraphContainerObject) throw new RuntimeException("Graph container object properties must provide type: " + propertyURI);
						    
		                if( VitalSigns.get().getConfig().versionEnforcement != VersionEnforcement.lenient ) {
		                        
		                	throw new RuntimeException("Property not found : " + propertyURI + " " + go.getClass().getCanonicalName());
		                        
		                } else {
		                        
		                     log.warn("Property not found: " + propertyURI + " " + go.getClass().getCanonicalName());
		                        
		                     skip = true;
		                }
		                	
		                // throw new RuntimeException("External or container dynamic property " + propertyURI + " must define its type (name|type=\"...\")");
						 
					}
						
						
					if(!skip) {
						    
						String newPropURI = propertyURI.substring(0, typeSep);
						    
						if(!(go instanceof VITAL_GraphContainerObject) && !VitalSigns.get().getConfig().externalProperties) {
							throw new RuntimeException("Cannot deserialize an object with external properties - they are disabled, property: " + newPropURI);
						}
						    
						String cls = propertyURI.substring(typeSep + 1);
						    
						Class valueClass = null;
						    
						try {
							valueClass = Class.forName(cls);
						} catch (ClassNotFoundException e) {
						    throw new RuntimeException(e);
						}
						    
						if(externalPropertiesMap == null) {
							externalPropertiesMap = new HashMap<String, List<Object>>();
						}
						    
						List<Object> v = externalPropertiesMap.get(newPropURI);
						    
						if(v == null) {
							v = new ArrayList<Object>();
						    externalPropertiesMap.put(newPropURI, v);
						}
						    
						v.add(de(serializedValue, valueClass));
						    
					}		
				}
					
				// }
				
			}
			
			if(nextTabIndex >= serializedForm.length()) {
				nextTabIndex = -1;
				// break
			} else {
			
				startTabIndex = nextTabIndex;
				
				nextTabIndex = serializedForm.indexOf('\t', startTabIndex + 1);
				
				if(nextTabIndex < 0) nextTabIndex = serializedForm.length();
			}
		}
		
		if(valuesMap != null) {
		
			List<String> keys = new ArrayList<String>(valuesMap.keySet());
			
			// types property first!
			Collections.sort(keys, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {

					// types property first
					Integer s1 = VitalCoreOntology.types.getLocalName().equals(o1) ? 0 : 1;
					Integer s2 = VitalCoreOntology.types.getLocalName().equals(o2) ? 0 : 1;
					
					return s1.compareTo(s2);
				}
			});
			
			for( String key : keys ) {
				
				go.setProperty(key, valuesMap.get(key));
				
			}
		}
		
		if(externalPropertiesMap != null) {
			
			for( Entry<String, List<Object>> e : externalPropertiesMap.entrySet()) {
				
				go.setProperty(e.getKey(), e.getValue().size() > 1 ? e.getValue() : e.getValue().get(0));
				
			}
		}
		
		return go;
	}
	
	// deserialize the value from string
	public static Object de(String input, Class<?> clazz) {
	
		if(clazz == String.class || clazz == StringProperty.class) {
			return StringEscapeUtils.unescapeJava(input);
		} else if( clazz == URIProperty.class ) {
			return new URIProperty(StringEscapeUtils.unescapeJava(input));
		} else if(clazz == Boolean.class || clazz == BooleanProperty.class) {
			return new Boolean( Boolean.parseBoolean(input));
		} else if(clazz == Truth.class || clazz == TruthProperty.class) {
		    return Truth.fromString(input);
		} else if(clazz == Integer.class || clazz == IntegerProperty.class) {
			return new Integer(Integer.parseInt(input));
		} else if(clazz == Long.class || clazz == LongProperty.class) {
			return new Long(Long.parseLong(input));
		} else if(clazz == Double.class || clazz == DoubleProperty.class) {
			return new Double(Double.parseDouble(input));
		} else if(clazz == Float.class || clazz == FloatProperty.class) {
			return new Float(Float.parseFloat(input));
		} else if(clazz == Date.class || clazz == DateProperty.class) {
			return new Date(Long.parseLong(input));
		} else if(clazz == GeoLocationProperty.class) {
			return GeoLocationProperty.fromRDFString(input);
		} else if(clazz == OtherProperty.class) {
			return OtherProperty.fromRDFString(StringEscapeUtils.unescapeJava(input));
		} else {
			throw new RuntimeException("Unsupported data type: " + clazz.getCanonicalName());
		}	
	}
	
	// serialize value into string
	public static String es(Object input) {
	    return es(input, false);
	}
	
	public static String es(Object input, boolean suppressOldVersionFilter) {
		
		String s = null;
		
		// escape only string values
			
		if(input instanceof String) {
			s = StringEscapeUtils.escapeJava( oldVersionFilter( (String)input, suppressOldVersionFilter ));
		} else if(input instanceof Boolean) {
			s = "" + input;
		} else if(input instanceof Truth) {
		    s = "" + input;
		} else if(input instanceof Integer) {
			s = "" + input;
		} else if(input instanceof Long) {
			s = "" + input;
		} else if(input instanceof Double) {
			s = "" + input;
		} else if(input instanceof Float) {
			s = "" + input;
		} else if(input instanceof Date) {
			s = "" + ((Date)input).getTime();
		} else if(input instanceof GeoLocationProperty) {
			s = ((GeoLocationProperty)input).toRDFValue();
		} else if(input instanceof OtherProperty) {
			s = StringEscapeUtils.escapeJava( ((OtherProperty)input).toRDFString() );
		} else {
			throw new RuntimeException("Unsupported data type: " + input.getClass().getCanonicalName());
		}
		return s;
		
		// all literal properties
	}
	
	/**
	 * Short circuit method of generating compact string format objects
	 * @param clazz graph object type
	 * @param URI 
	 * @param values, a values map containing propertyURI=value entries, the value is either a list of string[] or a string[], where
	 * string[] is a [already encoded value, optional external property value type] 
	 * @return
	 */
	
	public static String shortCircuitCompactString(Class<? extends GraphObject> clazz, String URI, Map<String, Object> values) {
	    
	    StringBuilder sb = new StringBuilder();
	    
	    String rdfType = VitalSigns.get().getClassesRegistry().getClassURI(clazz);
	    
	    if(rdfType == null) throw new RuntimeException("No URI for class: " + clazz.getCanonicalName());
        
        sb.append("type=\"").append(CompactStringSerializer.oldVersionFilter(rdfType, false)).append("\"\tURI=\"").append(URI).append("\"");
        
        for(Entry<String, Object> e : values.entrySet()) {
            
            Object val = e.getValue();
            
            String key = e.getKey();
            
            if(val instanceof List) {
                
                for( Object x : (List<?>)val ) {
                    
                    String v = null;
                    
                    String ext = null;
                    
                    if(x instanceof String[]) {
                        String[] vs = (String[]) x;
                        if(vs.length != 2) throw new RuntimeException("value record must be of length 2: " + vs.length);
                        v = vs[0];
                        ext = vs[1];
                        
                    } else if(x instanceof String) {
                        
                        v = (String) x;
                        
                    } else {
                        
                        throw new RuntimeException("list value must be string or string[2]");
                        
                    }
                    
                    sb.append('\t')
                    .append(CompactStringSerializer.oldVersionFilter(key, false));
                    
                    if(ext != null) {
                        
                        sb.append('|').append(ext);
                        
                    }
                    
                    sb.append("=\"")
                        .append(v)
                        .append('"');
                    
                }
                
            } else {
                
                String v = null;
                
                String ext = null;
                
                if(val instanceof String[]) {
                    String[] vs = (String[]) val;
                    if(vs.length != 2) throw new RuntimeException("value record must be of length 2: " + vs.length);
                    v = vs[0];
                    ext = vs[1];
                    
                } else if(val instanceof String) {
                    
                    v = (String) val;
                    
                } else {
                    
                    throw new RuntimeException("Expected either a list or a single record of string[2] or string type");
                    
                }
                
                sb.append('\t')
                .append(CompactStringSerializer.oldVersionFilter(key, false));
                
                if(ext != null) {
                    
                    sb.append('|').append(ext);
                    
                }
                
                sb.append("=\"")
                    .append(v)
                    .append('"');
                
            }
        }
        
        return sb.toString();
	}
}
