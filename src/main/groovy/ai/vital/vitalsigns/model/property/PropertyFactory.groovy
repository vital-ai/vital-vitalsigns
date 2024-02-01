package ai.vital.vitalsigns.model.property


import java.lang.reflect.Constructor

import org.slf4j.Logger;
import org.slf4j.LoggerFactory
import ai.vital.vitalsigns.model.properties.Property_types;
import ai.vital.vitalsigns.model.properties.Property_vitaltype;
import ai.vital.vitalsigns.datatype.Truth;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.properties.PropertyTrait;


public class PropertyFactory {

    public static Class vitaltype = (Class<? extends PropertyTrait>) Property_vitaltype.class;
    public static Class typesP = (Class<? extends PropertyTrait>) Property_types.class;

    private final static Logger log = LoggerFactory.getLogger(PropertyFactory.class)

    //typed sets for faster access?
    static Map<Class<? extends PropertyTrait>, Constructor> cachedConstructors = Collections.synchronizedMap([:])

    public static IProperty createInstance(Class<? extends IProperty> btype, Class<? extends PropertyTrait> ttype){

        if(btype == BooleanProperty.class) {
            return createBooleanProperty(ttype, true)
        } else if(btype == DateProperty.class) {
            return createDateProperty(ttype, new Date())
        } else if(btype == DoubleProperty.class) {
            return createDoubleProperty(ttype, 0D)
        } else if(btype == FloatProperty.class) {
            return createFloatProperty(ttype, 1F)
        } else if(btype == GeoLocationProperty.class) {
            return createGeoLocationProperty(ttype, new GeoLocationProperty(0d, 0d))
        } else if(btype == IntegerProperty.class) {
            return createIntegerProperty(ttype, 1)
        } else if(btype == LongProperty.class) {
            return createLongProperty(ttype, 1L)
        } else if(btype == OtherProperty.class) {
            throw new RuntimeException("OtherProperty instances don't have traits!")
        } else if(btype == StringProperty.class) {
            return createStringProperty(ttype, "")
        } else if(btype == TruthProperty.class) {
            return createTruthProperty(ttype, Truth.UNKNOWN)
        } else if(btype == URIProperty.class) {
            return createURIProperty(ttype, "urn:urn")
        } else {
            throw new RuntimeException("unhandled property type: ${btype}");
        }
        
    }

    public static IProperty createBooleanProperty(Class<? extends PropertyTrait> traitClass, boolean val) {
//        return new BooleanProperty(val).withTraits(traitClass)
        
        Constructor constr = cachedConstructors.get(traitClass)
        
        IProperty p = null
        
        if(constr == null) {
            
            p = new BooleanProperty(val).withTraits(traitClass);
            //          templates.put(traitClass, p)

            Class c = p.getClass();
            Constructor[] constrs = c.getConstructors();

            for(Constructor x : constrs) {

                if(x.getParameterTypes().length == 2) {

                    constr = x;
                    
                }

            }
            
            if(constr == null) throw new RuntimeException("No two-arg constructor for property trait class: " + c)

            cachedConstructors.put(traitClass, constr)
            
        } else {
        
            p = constr.newInstance(Collections.emptyMap(), new BooleanProperty(val))
        
        }
        
        return p
    }

    public static IProperty createDateProperty(Class<? extends PropertyTrait> traitClass, Date date) {
        return createDateProperty(traitClass, date.getTime())
    }
    
    public static IProperty createDateProperty(Class<? extends PropertyTrait> traitClass, long millis) {
//        return new DateProperty(millis).withTraits(traitClass);
        
        Constructor constr = cachedConstructors.get(traitClass)       

        IProperty p = null
        
        if(constr == null) {
            
            p = new DateProperty(millis).withTraits(traitClass);
            //          templates.put(traitClass, p)

            Class c = p.getClass();
            Constructor[] constrs = c.getConstructors();

            for(Constructor x : constrs) {

                if(x.getParameterTypes().length == 2) {

                    constr = x;
                    
                }

            }
            
            if(constr == null) throw new RuntimeException("No two-arg constructor for property trait class: " + c)

            cachedConstructors.put(traitClass, constr)
            
        } else {
        
            p = constr.newInstance(Collections.emptyMap(), new DateProperty(millis))
        
        }
        
        return p
    }

    public static IProperty createDoubleProperty(
            Class<? extends PropertyTrait> traitClass, Double d) {
//        return new DoubleProperty(d).withTraits(traitClass);
            
        Constructor constr = cachedConstructors.get(traitClass)       

        IProperty p = null
        
        if(constr == null) {
            
            p = new DoubleProperty(d).withTraits(traitClass);
            //          templates.put(traitClass, p)

            Class c = p.getClass();
            Constructor[] constrs = c.getConstructors();

            for(Constructor x : constrs) {

                if(x.getParameterTypes().length == 2) {

                    constr = x;
                    
                }

            }
            
            if(constr == null) throw new RuntimeException("No two-arg constructor for property trait class: " + c)

            cachedConstructors.put(traitClass, constr)
            
        } else {
        
            p = constr.newInstance(Collections.emptyMap(), new DoubleProperty(d))
        
        }
        
        return p
    }

    public static IProperty createFloatProperty(
            Class<? extends PropertyTrait> traitClass, Float f) {
//        return new FloatProperty(f).withTraits(traitClass);
            
        Constructor constr = cachedConstructors.get(traitClass)       

        IProperty p = null
        
        if(constr == null) {
            
            p = new FloatProperty(f).withTraits(traitClass);
            //          templates.put(traitClass, p)

            Class c = p.getClass();
            Constructor[] constrs = c.getConstructors();

            for(Constructor x : constrs) {

                if(x.getParameterTypes().length == 2) {

                    constr = x;
                    
                }

            }
            
            if(constr == null) throw new RuntimeException("No two-arg constructor for property trait class: " + c)

            cachedConstructors.put(traitClass, constr)
            
        } else {
        
            p = constr.newInstance(Collections.emptyMap(), new FloatProperty(f))
        
        }
        
        return p
    }

    public static IProperty createGeoLocationProperty(
            Class<? extends PropertyTrait> traitClass, GeoLocationProperty val) {
//        return new GeoLocationProperty(val.longitude, val.latitude).withTraits(traitClass);
            
        Constructor constr = cachedConstructors.get(traitClass)       

        IProperty p = null
        
        if(constr == null) {
            
            p = val.withTraits(traitClass);
            //          templates.put(traitClass, p)

            Class c = p.getClass();
            Constructor[] constrs = c.getConstructors();

            for(Constructor x : constrs) {

                if(x.getParameterTypes().length == 2) {

                    constr = x;
                    
                }

            }
            
            if(constr == null) throw new RuntimeException("No two-arg constructor for property trait class: " + c)

            cachedConstructors.put(traitClass, constr)
            
        } else {
        
            p = constr.newInstance(Collections.emptyMap(), val)
        
        }
        
        return p
    }

    public static IProperty createIntegerProperty(
            Class<? extends PropertyTrait> traitClass, Integer i) {

        //		return new IntegerProperty(i).withTraits(traitClass);

        Constructor constr = cachedConstructors.get(traitClass)       

        IProperty p = null
        
        if(constr == null) {
            
            p = new IntegerProperty(i).withTraits(traitClass);
            //          templates.put(traitClass, p)

            Class c = p.getClass();
            Constructor[] constrs = c.getConstructors();

            for(Constructor x : constrs) {

                if(x.getParameterTypes().length == 2) {

                    constr = x;
                    
                }

            }
            
            if(constr == null) throw new RuntimeException("No two-arg constructor for property trait class: " + c)

            cachedConstructors.put(traitClass, constr)
            
        } else {
        
            p = constr.newInstance(Collections.emptyMap(), new IntegerProperty(i))
        
        }
        
        return p

    }

    public static IProperty createLongProperty(
            Class<? extends PropertyTrait> traitClass, Long l) {
//        return new LongProperty(l).withTraits(traitClass);
            
        Constructor constr = cachedConstructors.get(traitClass)       

        IProperty p = null
        
        if(constr == null) {
            
            p = new LongProperty(l).withTraits(traitClass);
            //          templates.put(traitClass, p)

            Class c = p.getClass();
            Constructor[] constrs = c.getConstructors();

            for(Constructor x : constrs) {

                if(x.getParameterTypes().length == 2) {

                    constr = x;
                    
                }

            }
            
            if(constr == null) throw new RuntimeException("No two-arg constructor for property trait class: " + c)

            cachedConstructors.put(traitClass, constr)
            
        } else {
        
            p = constr.newInstance(Collections.emptyMap(), new LongProperty(l))
        
        }

        return p
    }

    public static IProperty createStringProperty(
            Class<? extends PropertyTrait> traitClass, String s) {

        //		return new StringProperty(s).withTraits(traitClass);

        Constructor constr = cachedConstructors.get(traitClass)       

        IProperty p = null
        
        if(constr == null) {
            
            p = new StringProperty(s).withTraits(traitClass);
            //          templates.put(traitClass, p)

            Class c = p.getClass();
            Constructor[] constrs = c.getConstructors();

            for(Constructor x : constrs) {

                if(x.getParameterTypes().length == 2) {

                    constr = x;
                    
                }

            }
            
            if(constr == null) throw new RuntimeException("No two-arg constructor for property trait class: " + c)

            cachedConstructors.put(traitClass, constr)
            
        } else {
        
            p = constr.newInstance(Collections.emptyMap(), new StringProperty(s))
        
        }
        
        return p

    }
            
    public static IProperty createTruthProperty(Class<? extends PropertyTrait> traitClass, Truth t) {
        
        Constructor constr = cachedConstructors.get(traitClass)
        
        IProperty p = null
        
        if(constr == null) {
            
            p = new TruthProperty(t).withTraits(traitClass)
            
            Class c = p.getClass()
            
            Constructor[] constrs = c.getConstructors();
            
            for(Constructor x : constrs) {
                
                if(x.getParameterTypes().length == 2) {
                    
                    constr = x;
                    
                }
                
            }
            
            if(constr == null) throw new RuntimeException("No two-arg constructor for property trait class: " + c)
            
            cachedConstructors.put(traitClass, constr)
             
        } else {
        
            p = constr.newInstance(Collections.emptyMap(), new TruthProperty(t))
        
        }
        
        return p
        
    }
            
            

    public static IProperty createURIProperty(
            Class<? extends PropertyTrait> traitClass, String s) {

        //		return new URIProperty(s).withTraits(traitClass);

        Constructor constr = cachedConstructors.get(traitClass)       

        IProperty p = null
        
        if(constr == null) {
            
            p = new URIProperty(s).withTraits(traitClass);
            //			templates.put(traitClass, p)

            Class c = p.getClass();
            Constructor[] constrs = c.getConstructors();

            for(Constructor x : constrs) {

                if(x.getParameterTypes().length == 2) {

                    constr = x;
                    
                }

            }
            
            if(constr == null) throw new RuntimeException("No two-arg constructor for property trait class: " + c)

            cachedConstructors.put(traitClass, constr)
            
        } else {
        
            p = constr.newInstance(Collections.emptyMap(), new URIProperty(s))
        
        }

        return p

    }

    public static IProperty createMultiValueProperty(PropertyMetadata pm, List vals) {

        return createMultiValueProperty(pm.getBaseClass(), pm.getTraitClass(), vals)

    }

    public static IProperty createMultiValueProperty(Class<? extends IProperty> baseClazz, Class<? extends PropertyTrait> traitClass) {
        return createMultiValueProperty(baseClazz, traitClass, null)
    }

    public static IProperty createMultiValueProperty(Class<? extends IProperty> baseClazz, Class<? extends PropertyTrait> traitClass, Object arg) {
        List args = null
        if(arg != null) {
            args = [arg]
        }
        return createMultiValueProperty(baseClazz, traitClass, (List)args)
    }

    public static IProperty createMultiValueProperty(Class<? extends IProperty> baseClazz, Class<? extends PropertyTrait> traitClass, List args) {

        def mv = null;// templates.get(traitClass)

        //		if(mv == null) {

        if(baseClazz == BooleanProperty) {
            mv = new MultiValueProperty<BooleanProperty>(args).withTraits(traitClass)
        } else if(baseClazz == DateProperty) {
            mv = new MultiValueProperty<DateProperty>(args).withTraits(traitClass)
        } else if(baseClazz == DoubleProperty) {
            mv = new MultiValueProperty<DoubleProperty>(args).withTraits(traitClass)
        } else if(baseClazz == FloatProperty){
            mv = new MultiValueProperty<FloatProperty>(args).withTraits(traitClass)
        } else if(baseClazz == GeoLocationProperty) {
            mv = new MultiValueProperty<GeoLocationProperty>(args).withTraits(traitClass)
        } else if(baseClazz == IntegerProperty) {
            mv = new MultiValueProperty<IntegerProperty>(args).withTraits(traitClass)
        } else if(baseClazz == LongProperty) {
            mv = new MultiValueProperty<LongProperty>(args).withTraits(traitClass)
        } else if(baseClazz == StringProperty) {
            mv = new MultiValueProperty<StringProperty>(args).withTraits(traitClass)
        } else if(baseClazz == URIProperty) {
            mv = new MultiValueProperty<URIProperty>(args).withTraits(traitClass)
        } else {
            throw new RuntimeException("Unhandled class: " + baseClazz)
        }

        //			if(vals != null) {
        //				for(Object o : vals) {
        //					mv.add(o)
        //				}
        //			}

        //			templates.put(traitClass, mv)
        return mv;

        /*
         }
         mv = mv.clone()
         if(vals != null) {
         for(Object o : vals) {
         mv.add(o)	
         }
         }
         return mv
         */

    }


    /**
     * Creates base property value (wrapped raw value), no trait
     */
    public static IProperty createBaseProperty(Object rawValue) {

        if(rawValue instanceof IProperty) return ((IProperty)rawValue).unwrapped()

        if(rawValue instanceof Collection) {

            List values = []

            for(Object v : rawValue) {
                if(v instanceof Collection) throw new RuntimeException("Nested collections forbidden!");
                values.add( createBaseProperty(v) )
            }

            MultiValueProperty mvp = new MultiValueProperty(values)
            return mvp

        }

        if(rawValue instanceof Boolean) {
            rawValue = new BooleanProperty((Boolean) rawValue);
        } else if(rawValue instanceof Date){
            rawValue = new DateProperty((Date)rawValue);
        } else if(rawValue instanceof Double) {
            rawValue = new DoubleProperty((Double)rawValue);
        } else if(rawValue instanceof Float) {
            rawValue = new FloatProperty((Float)rawValue)
        } else if(rawValue instanceof GeoLocationProperty) {
        } else if(rawValue instanceof Integer) {
            rawValue = new IntegerProperty((Integer)rawValue)
        } else if(rawValue instanceof Long) {
            rawValue = new LongProperty((Long) rawValue)
        } else if(rawValue instanceof String || rawValue instanceof GString) {
            rawValue = new StringProperty(rawValue.toString())
        } else if(rawValue instanceof Truth) {
            rawValue = new TruthProperty((Truth)rawValue)
        } else if(rawValue instanceof URI) {
            rawValue = new URIProperty(((URI)rawValue).toString())
        } else {
            throw new RuntimeException("Unsupported property value type: " + rawValue.getClass().getCanonicalName());
        }

    }


    public static IProperty createBasePropertyFromRawValue(Class<? extends IProperty> btype, Object val) {

        if(val instanceof Collection) {
            List values = []
            for(Object o : val) {

                if(o instanceof Collection) throw new RuntimeException("Nested collections forbidden as properties values")

                IProperty m = createBasePropertyFromRawValue(btype, o)
                values.add(m)

            }

            MultiValueProperty mvp = new MultiValueProperty<IProperty>(values)
            return mvp
        }

        if(btype == BooleanProperty.class) {
            return new BooleanProperty(val)
        } else if(btype == DateProperty.class) {
            return new DateProperty(val)
        } else if(btype == DoubleProperty.class) {
            return new DoubleProperty(val)
        } else if(btype == FloatProperty.class) {
            return new FloatProperty(val)
        } else if(btype == GeoLocationProperty.class) {
            if(val instanceof GeoLocationProperty) {
                return val
            } else if(val instanceof Map) {
                return GeoLocationProperty.fromJSONMap(val)
            } else if(val instanceof String) {
                return GeoLocationProperty.fromRDFString(val)
            } else {
                throw new RuntimeException("Unsupported raw value of ${GeoLocationProperty.class.canonicalName}: ${val}")
            }
        } else if(btype == IntegerProperty.class) {
            return new IntegerProperty(val)
        } else if(btype == LongProperty.class) {
            return new LongProperty(val)
        } else if(btype == OtherProperty.class) {
            if(val instanceof OtherProperty) {
                //immutable
                return val
            } else if(val instanceof Map) {
                return OtherProperty.fromJSONMap(val)
            } else if(val instanceof String || val instanceof GString){
                return OtherProperty.fromRDFString(val)
            } else {
                throw new RuntimeException("Unsupported raw value of ${OtherProperty.class.canonicalName}: ${val}")
            }

        } else if(btype == StringProperty.class) {
            return new StringProperty(val)
        } else if(btype == TruthProperty.class) {
            if(val instanceof Truth) {
                return new TruthProperty((Truth)val)
            } else if(val instanceof String || val instanceof GString) {
                return new TruthProperty(Truth.fromString(val))
            } else {
                throw new RuntimeException("Unsupported raw value of ${TruthProperty.class.canonicalName}: ${val}")
            }
        } else if(btype == URIProperty.class) {
            return new URIProperty(val)
        }

        throw new RuntimeException("unhandled property type: ${btype}");

    }
    
    public static IProperty createPropertyWithTraitFromRawValue(Class<? extends IProperty> btype, Class<? extends PropertyTrait> ttype, Object val) {
        
        if(btype == MultiValueProperty) {
            return createMultiValueProperty(btype, ttype, val)
        }        
         
        if(btype == BooleanProperty.class) {
            return createBooleanProperty(ttype, val)
        } else if(btype == DateProperty.class) {
            return createDateProperty(ttype, val)
        } else if(btype == DoubleProperty.class) {
            return createDoubleProperty(ttype, val)
        } else if(btype == FloatProperty.class) {
            return createFloatProperty(ttype, val)
        } else if(btype == GeoLocationProperty.class) {
            return createGeoLocationProperty(ttype, val)
        } else if(btype == IntegerProperty.class) {
            return createIntegerProperty(ttype, val)
        } else if(btype == LongProperty.class) {
            return createLongProperty(ttype, val)
        } else if(btype == OtherProperty.class) {
            throw new RuntimeException("OtherProperty instances don't have traits!")
        } else if(btype == StringProperty.class) {
            return createStringProperty(ttype, val)
        } else if(btype == URIProperty.class) {
            return createURIProperty(ttype, val)
        } else {
            throw new RuntimeException("unhandled property type: ${btype}");
        }
        
    }

    public static void clearProperty(Class<? extends PropertyTrait> traitClass) {

        cachedConstructors.remove(traitClass)
        
    }

}