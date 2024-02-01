package ai.vital.vitalservice.json

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier;
import java.util.Map.Entry

import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalservice.query.VitalGraphBooleanContainer;
import ai.vital.vitalservice.query.VitalGraphCriteriaContainer;
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.datatype.Truth;
import ai.vital.vitalsigns.java.SerializedProperty;
import ai.vital.vitalsigns.json.JSONSerializer
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.property.BooleanProperty;
import ai.vital.vitalsigns.model.property.DateProperty;
import ai.vital.vitalsigns.model.property.DoubleProperty;
import ai.vital.vitalsigns.model.property.FloatProperty;
import ai.vital.vitalsigns.model.property.GeoLocationProperty;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.IntegerProperty;
import ai.vital.vitalsigns.model.property.LongProperty;
import ai.vital.vitalsigns.model.property.OtherProperty
import ai.vital.vitalsigns.model.property.PropertyFactory;
import ai.vital.vitalsigns.model.property.StringProperty;
import ai.vital.vitalsigns.model.property.TruthProperty
import ai.vital.vitalsigns.model.property.URIProperty
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.properties.PropertyTrait;;


//improved mapper that avoid the types conversion, it has a few exceptions like classes extending ArrayList or IProperty
public class VitalServiceJSONMapper {

	public static Object toJSON(Object o) {
		
		if(o == null) return null;
		
        
//        if(o instanceof PropertyTrait) {
//            SerializedProperty sp = new SerializedProperty()
//            IProperty p = o
//            p = (IProperty) p.unwrapped()
//            sp.setBaseClass(p.getClass())
//            sp.setPropertyURI(o.getURI())
//            sp.setRawValue(p.rawValue())   
//            
//            o = sp
//        }
        
        
        
		if(o instanceof GraphObject) {
			
			return JSONSerializer.toJSONMap((GraphObject) o);
			
		} else if(o instanceof Collection && !(o instanceof VitalGraphArcContainer || o instanceof VitalGraphCriteriaContainer || o instanceof VitalGraphBooleanContainer) ) {
			
			//convert to array
			List<Object> a = new ArrayList<Object>();
			for(Object x : (Collection)o) {
				a.add(toJSON(x));
			}
			return a;
			
		} else if(o instanceof Map) {
			
			//just convert entries
			Map<String, Object> out = new HashMap<String, Object>();
			
			Set<Entry> entries = ((Map) o).entrySet();
			
			for(Entry e : entries) {
				
				Object key = e.getKey();
				if(key instanceof String) {
					out.put((String) key, toJSON(e.getValue()));
				} else {
					throw new RuntimeException("Map key must be a string, got" + key.getClass().getCanonicalName());
				}
				
			}
			
			return out;
		} else if(o instanceof Number || o instanceof Boolean || o instanceof String) {
			return o;
		} else if(o instanceof GString) {
			return ((GString)o).toString()
		} else if(o instanceof VitalServiceException) {
			return [_type: VitalServiceException.class.canonicalName, message: o.getLocalizedMessage(), sessionNotFound: o.sessionNotFound]
		} else if(o instanceof Class) {
			return [_type: 'class', name: o.getCanonicalName()]
		} else if(o instanceof IProperty) {
        
            Map r = null
        
            String propertyURI = null
            
            if(o instanceof PropertyTrait) {
                IProperty p = o
                propertyURI = o.getURI()
                o = (IProperty) p.unwrapped()
            }            
            
			if(o instanceof BooleanProperty) {
				r = [_type: BooleanProperty.class.canonicalName, value: o.booleanValue()]
			} else if(o instanceof DateProperty) {
				r = [_type: DateProperty.class.canonicalName, value: o.getTime()]
			} else if(o instanceof DoubleProperty) {
				r = [_type: DoubleProperty.class.canonicalName, value: o.doubleValue()]
			} else if(o instanceof FloatProperty) {
				r = [_type: FloatProperty.class.canonicalName, value: o.floatValue()]
			} else if(o instanceof GeoLocationProperty) {
				r = [_type: GeoLocationProperty.class.canonicalName, value: o.toJSONMap()]
			} else if(o instanceof IntegerProperty) {
				r = [_type: IntegerProperty.class.canonicalName, value: o.intValue()]
			} else if(o instanceof LongProperty) {
				r = [_type: LongProperty.class.canonicalName, value: o.longValue()]
			} else if(o instanceof OtherProperty) {
				r = [_type: OtherProperty.class.canonicalName, value: o.toJSONMap()]
			} else if(o instanceof StringProperty) {
				r = [_type: StringProperty.class.canonicalName, value: o.toString()]
			} else if(o instanceof TruthProperty) {
                r = [_type: TruthProperty.class.canonicalName, value: o.toString()]
			} else if(o instanceof URIProperty) {
				r = [_type: URIProperty.class.canonicalName, value: o.get()]
			} else {
				throw new RuntimeException("Unhandled serialization case: " + o)
			} 
            
            if(propertyURI) {
                r.put('propertyURI', propertyURI)
            }
            
            return r
            
		} else if(o.getClass().isEnum()) {
            return [_type: o.getClass().canonicalName, name: ((Enum)o).name()]
		} else {
		
			Map<String, Object> out = new HashMap<String, Object>();	
		
			out.put("_type", o.getClass().getCanonicalName())
			
			Map<String, Object> fields = listFields(o)
			
			for(Entry p : fields.entrySet()) {
				
				String k = p.key
				
//				if(k.equals('metaClass')
//					|| k.equals('class') 
//					|| k.equals("theClass") ) continue;
				
				Object v = p.value
				
				out.put(k, toJSON(v))
				
			}
			
			if(o instanceof List || o instanceof Set) {
				
				List _contents = [];
				//graph container objects
				for(Object x : o) {
					_contents.add(toJSON(x))
				}
				
				out.put("_contents", _contents)
				
			}
			
			return out;
			
		}
		
	}

	
	private static Map<String, Object> listFields(Object o) {
	
		Map<String, Object> m = [:]
			
		for(Class c : listClasses(o)) {
			
			for(Field f : c.getDeclaredFields()) {
				
				if( Modifier.isStatic(f.getModifiers()) ) continue
				if( Modifier.isVolatile(f.getModifiers()) ) continue
				
				if( !Modifier.isPublic(f.getModifiers()) ) continue
				
				Object val = f.get(o)
				if(val != null) m.put(f.getName(), val);
				
			}
			
			Set<String> setters = new HashSet<String>()
			for(Method x : c.getDeclaredMethods() ) {
				if(x.getName().startsWith("set") && x.getParameterTypes().length == 1) {
					setters.add(x.getName())
				}
			}
			
			
			for(Method x : c.getDeclaredMethods() ) {
				
				if( x.getParameterTypes().length > 0 ) continue;
				
				if( Modifier.isStatic(x.getModifiers()) ) continue
				if( !Modifier.isPublic(x.getModifiers()) ) continue
				
				if( x.getName().startsWith("get") || x.getName().startsWith("is")) {
					
					int l = 3;
					
					if( x.getName().startsWith("get") ) {
					
						if(x.getName().length() == 3) continue
							
					} else {
						if(x.getName().length() == 2) continue
						l = 2
					}
					
					
					String n = x.getName().substring(l)
					
					String setter = "set" + n;
					
					if(!setters.contains(setter)) continue
					
					String nn = n.substring(0, 1).toLowerCase();
					
					if(n.length() > 1) {
						nn += n.substring(1) 
					}
					
					if('metaClass'.equals(nn)) continue
					
					Object val = x.invoke(o)
					
					if(val != null) {
						m.put(nn, val);
					}
					
					
				}
				
				
			}
			
		}
		
		return m
		
	}
	 
	private static void _listClasses(Class c, List<Class> target) {
	
		if(c.equals(Object.class)) return
			
		target.add(c)
		
		_listClasses(c.getSuperclass(), target)
		
		
	}
	
	private static List<Class> listClasses(o) {
		List<Class> c = []
		_listClasses(o.getClass(), c)
		return c
	}
	
	public static Object fromJSON(Object o) {

		if(o instanceof List) {

			List out = new ArrayList();
			
			for(Object x : o) {
				out.add(fromJSON(x))
			}
			
			return out
						
		} else if(o instanceof Map) {
		
			//if it's a typed map the deserialize
		
			String _type = o.get("_type")
			
			if(_type) {
				
				if(_type.equals(VitalServiceException.class.getCanonicalName())) {
					VitalServiceException vse = new VitalServiceException(o.get("message"))
                    vse.sessionNotFound = o.get('sessionNotFound') ? o.get('sessionNotFound') : false
                    return vse;
				} else if(_type.equals("class")) {
				
					String n = o.get("name")
					if(!n) throw new RuntimeException("No class 'name'")
				
					Class<?> clz = null
					try {
						clz = Class.forName(n);
					} catch(Exception e) {
					}
		
					if(clz == null) {
						
						//try loading it from vitalsigns...
						for(ClassLoader cl : VitalSigns.get().ontologiesClassLoaders) {

							try {
								clz = cl.loadClass(n);
							} catch(ClassNotFoundException e) {}
							
							if(clz != null) break;
							
						}
							
					}
					
					if(clz == null) throw new RuntimeException("Class not found: " + n)

					return clz;
					 
				}
				
				Class cls = null
                
                ClassNotFoundException origEx = null
                
                try {
                    cls = Class.forName(_type)
                } catch(ClassNotFoundException ex) {
                    origEx = ex
                }
                
				//fix for inner enums
                if(cls == null) {
                    
                    int lastDot =  _type.lastIndexOf('.')
                    
                    if( lastDot > 0 ) {
                        
                        String innerEnumClass = _type.substring(0, lastDot) + '$' + _type.substring(lastDot + 1)
                        
                        try {
                            cls = Class.forName(innerEnumClass)
                        } catch(ClassNotFoundException ex) {
                        }
                        
                    }
                    
                }
                
                if(cls == null) throw origEx
                
//                if(_type == VitalStatus.Status.class.canonicalName) {
//                    cls = VitalStatus.Status.class
//                } else {
//                    cls = Class.forName(_type)
//                }
				
				if(IProperty.class.isAssignableFrom(cls) ) {
				
					Object v = o.get('value')
					if(v == null) throw new RuntimeException("IProperty value must not be null")
                    
                    String propertyURI = o.get('propertyURI')
                    
                    if(propertyURI != null) {
                        
                        PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(propertyURI)
                        if(pm == null) throw new RuntimeException("Property with URI ${propertyURI} not found")
                        
                        Class<? extends PropertyTrait> ttype = pm.getTraitClass()
                        
                        if(BooleanProperty.class.equals(cls)) {
                            return PropertyFactory.createBooleanProperty(ttype, (boolean)v)
                        } else if(DateProperty.class.equals(cls)) {
                            return PropertyFactory.createDateProperty(ttype, (long)v)
                        } else if(DoubleProperty.class.equals(cls)) {
                            return PropertyFactory.createDoubleProperty(ttype, (double)v)
                        } else if(FloatProperty.class.equals(cls)) {
                            return PropertyFactory.createFloatProperty(ttype, (float)v)
                        } else if(GeoLocationProperty.class.equals(cls)) {
                            return PropertyFactory.createGeoLocationProperty(GeoLocationProperty.fromJSONMap((Map)v))
                        } else if(IntegerProperty.class.equals(cls)) {
                            return PropertyFactory.createIntegerProperty(ttype, (int)v)
                        } else if(LongProperty.class.equals(cls)) {
                            return PropertyFactory.createLongProperty(ttype, (long)v)
                        } else if(OtherProperty.class.equals(cls)) {
                            throw new RuntimeException("OtherProperty does not have trait")
                        } else if(StringProperty.class.equals(cls)) {
                            return PropertyFactory.createStringProperty(ttype, (String)v)
                        } else if(TruthProperty.class.equals(cls)) {
                            return PropertyFactory.createTruthProperty(ttype, (String)v)
                        } else if(URIProperty.class.equals(cls)) {
                            return PropertyFactory.createURIProperty(ttype, (String)v)
                        } else {
                            throw new RuntimeException("Unhandled property+trait type: "  + cls)
                        }
                        
                    } else {
                    
                        if(BooleanProperty.class.equals(cls)) {
                            return new BooleanProperty((boolean)v)
                        } else if(DateProperty.class.equals(cls)) {
                            return new DateProperty((long)v)
                        } else if(DoubleProperty.class.equals(cls)) {
                            return new DoubleProperty((double)v)
                        } else if(FloatProperty.class.equals(cls)) {
                            return new FloatProperty((float)v)
                        } else if(GeoLocationProperty.class.equals(cls)) {
                            return GeoLocationProperty.fromJSONMap((Map)v)
                        } else if(IntegerProperty.class.equals(cls)) {
                            return new IntegerProperty((int)v)
                        } else if(LongProperty.class.equals(cls)) {
                            return new LongProperty((long)v)
                        } else if(OtherProperty.class.equals(cls)) {
                            return OtherProperty.fromJSONMap((Map)v)
                        } else if(StringProperty.class.equals(cls)) {
                            return new StringProperty((String)v)
                        } else if(TruthProperty.class.equals(cls)) {
                            return new TruthProperty(Truth.fromString((String)v))
                        } else if(URIProperty.class.equals(cls)) {
                            return new URIProperty((String)v)
                        } else {
                            throw new RuntimeException("Unhandled property type: "  + cls)
                        }
                        
                    }
					
				} else if(cls.isEnum()) {
                
                    String name = o.get("name")
                
                    if(!name) throw new RuntimeException("No 'name' property in enum: ${cls}")
                    
                    return Enum.valueOf(cls, name)
                
				}
				
				Object r = cls.newInstance()
				
				for(Entry e : o.entrySet() ) {
					
					String k = e.key
					
					if(k.equals('_type') || k.equals('_contents')) continue;

					Object v = fromJSON(e.value)
					
					try {
						r[k] = v
					} catch(Exception ex) {
					
						String k2 = k.substring(0, 1).toUpperCase() + k.substring(1)
						try {
							r[k2] = v
						} catch(Exception e1) {
							throw new RuntimeException("Couldn't set property " + k + " / " + k2 + " of " + r.getClass().getCanonicalName() + " " +ex.getLocalizedMessage() + " / " + e1.getLocalizedMessage())
						}
					}
					
				}
				
				if(r instanceof List || r instanceof Set) {
					
					List c = o.get("_contents")
					
					if(c != null) {
						
						for(Object x : c) {
							r.add(fromJSON(x))
						}
						
					}
					
				}
				
				return r
			} 
			
			String type = o.get("type")
			
			if(type) {
				
				return JSONSerializer.fromJSONMap(o)
				
			}
			
			Map r = [:]
			
			for(Entry e : o.entrySet() ) {
				
				String k = e.key
				
				Object v = fromJSON(e.value)
				try {
					r[k] = v
				} catch(Exception ex) {
					String k2 = k.substring(0, 1).toUpperCase() + k.substring(1)
					try {
						r[k2] = v
					} catch(Exception e1) {
						throw new RuntimeException("Couldn't set property " + k + " / " + k2 + " of " + r.getClass().getCanonicalName() + " " + ex.getLocalizedMessage() + " / " + e1.getLocalizedMessage() )
					}
				}
				
				
			}
			
			return r
		
		} else {
		
			return o
		
		}
		
	}
	
}
