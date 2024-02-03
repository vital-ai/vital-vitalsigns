package ai.vital.vitalsigns.model;

import groovy.lang.GString;
import groovy.lang.GeneratedGroovyProxy;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.vital.query.ops.Ref;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.block.CompactStringSerializer;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.csv.ToCSVHandler;
import ai.vital.vitalsigns.datatype.Truth;
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils;
import ai.vital.vitalsigns.json.JSONSerializer;
import ai.vital.vitalsigns.meta.EdgesResolver;
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.meta.HyperEdgeAccessImplementation;
import ai.vital.vitalsigns.meta.PropertyDefinitionProcessor;
import ai.vital.vitalsigns.meta.PropertyDefinitionProcessor.PropertyValidationException;
import ai.vital.vitalsigns.model.properties.Property_types;
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
import ai.vital.vitalsigns.model.property.PropertyFactory;
import ai.vital.vitalsigns.model.property.StringProperty;
import ai.vital.vitalsigns.model.property.TruthProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.properties.PropertiesValidator;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.properties.PropertyUtils;
import ai.vital.vitalsigns.rdf.RDFFormat;
import ai.vital.vitalsigns.rdf.RDFSerialization;
import ai.vital.vitalsigns.rdf.RDFUtils;
import ai.vital.vitalsigns.sql.ToSQLRowsHandler;
import ai.vital.vitalsigns.uri.URIGenerator;
import ai.vital.vitalsigns.uri.URIGenerator.URIResponse;
import ai.vital.vitalsigns.xml.XMLUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import java.io.Serializable;
import groovy.lang.GroovyObjectSupport;

public class GraphObject<T extends GraphObject<T>> extends GroovyObjectSupport implements Serializable, Cloneable {

    private static final long serialVersionUID = 62375945152915902L;

    // this one is not serialized
    transient protected Map<String, IProperty> _properties = null;

    // this one is for serialization only!
    private Map<String, Object> _serialized = null;

    // only used when more than 1 type - optimization
    transient private List<Class<? extends GraphObject>> types = new ArrayList<Class<? extends GraphObject>>();

    private final static Logger log = LoggerFactory.getLogger(GraphObject.class);

    public GraphObject(Map<String, Object> propertiesMap) {

        this();

        if(propertiesMap == null) return;

        for(Entry<String, Object> e : propertiesMap.entrySet() )  {

            String k = e.getKey();

            Object value = e.getValue();

            if(value == null) continue;

            setProperty(k, value);
        }
    }

    @SuppressWarnings("unchecked")
    public GraphObject() {

        // this forces vitalsigns to initialize

        String classURI = VitalSigns.get().getClassesRegistry().getClassURI(this.getClass());

        if(classURI == null) throw new RuntimeException("Class URI not found: " + this.getClass().getCanonicalName());

        initPropertiesMap();

        types.add(this.getClass());

        // this added for the vitalprime deserialization problem

        synchronized(_properties) {

            _properties.put(VitalCoreOntology.vitaltype.getURI(), PropertyFactory.createURIProperty(PropertyFactory.vitaltype, classURI));

            IProperty typesProp = PropertyFactory.createMultiValueProperty(URIProperty.class, PropertyFactory.typesP, new URIProperty(classURI));

            _properties.put(VitalCoreOntology.types.getURI(), typesProp);
        }

        // try {
        //	setProperty(VitalCoreOntology.vitaltype.getLocalName(), classURI);
        //	setProperty(VitalCoreOntology.types.getLocalName(), Arrays.asList(classURI));
        // } catch(Exception e) {}

    }

    private void writeObject(ObjectOutputStream out) throws IOException {

        if(_properties != null) {

            // this added for the vitalprime serialization/deserialization problem

            // test serialization update???:

            _serialized = Collections.synchronizedMap( new HashMap<String, Object>() );

            // new HashMap<String, Object>();

            synchronized ( _serialized ) {  //?????

                for(Entry<String, IProperty> e : _properties.entrySet() ) {

                    // unwrapped objects ?

                    // use short names ?

                    _serialized.put(e.getKey(), e.getValue().unwrapped());

                    // _serialized.put(e.getKey(), e.getValue().rawValue());
                }
            }
        }

        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {

        // System.out.println("In GraphObject ReadObject");

        // System.out.println("Class: " + getClass().getName());

        // our "pseudo-constructor"

        input.defaultReadObject();

        // now we are a "live" object again, so let's run rebuild and start
        if(_serialized != null) {

            synchronized (_serialized) {

                // first set types
                Object typesP = _serialized.remove(VitalCoreOntology.types.getURI());

                if(typesP == null) throw new IOException("No types property found in java serialized object");

                Object vt = _serialized.remove(VitalCoreOntology.vitaltype.getURI());

                if(vt == null) throw new IOException("No vitaltype property found in java serialized object");

                // first types!
                this.setProperty(VitalCoreOntology.types.getLocalName(), typesP);

                this.setProperty(VitalCoreOntology.vitaltype.getLocalName(), vt);

                for(Entry<String, Object> e : _serialized.entrySet()) {

                    String key = e.getKey();

                    if( VitalSigns.get().getPropertiesRegistry().getProperty(key) != null ) {
                        key = RDFUtils.getPropertyShortName(key);
                    }

                    this.setProperty(key, e.getValue());
                }

                _serialized.clear();
            }

            // moved this outside the synchronized block
            _serialized = null;

            // System.out.println("JSON: " + toJSON());
        }
    }

    @Override
    public void setProperty(String pname, Object newValue) {

        // wrap values etc

        if(pname == null) throw new NullPointerException("Null property key");

        if(newValue instanceof GeneratedGroovyProxy) {
            newValue = ((GeneratedGroovyProxy)newValue).getProxyTarget();
        } else if(newValue instanceof Ref) {
            newValue = ((Ref)newValue).getReferencedURI();
        }


        if(newValue instanceof String) {
            if( ((String)newValue).isEmpty() ) newValue = null;
        } else if(newValue instanceof GString) {
            if( ((GString) newValue).toString().isEmpty() ) {
                newValue = null;
            }
        }

        if(pname.equals("URI")) {
            this.setURI(newValue);
            return;
        }

        initPropertiesMap();

        // check if it's an external property
        int indexOfColon = pname.indexOf(':');

        String annotationPropertyURI = VitalSigns.get().getPropertiesRegistry().getAnnotationPropertyURIByShortName(this.getClass(), pname);

        if(indexOfColon >= 0 || annotationPropertyURI != null) {

            if( !(this instanceof VITAL_GraphContainerObject) && ! VitalSigns.get().getConfig().externalProperties ) throw new RuntimeException("Cannot set an external property " + pname + " - they are disabled");

            if( VitalSigns.get().getPropertiesRegistry().getProperty(pname) != null ) throw new RuntimeException("Cannot use full property URI to set vital property value - " + pname);

            if(RDF.type.getURI().equals(pname)) throw new RuntimeException("rdf:type property may only be set via 'types' vital property only");

            if(annotationPropertyURI != null) {

                pname = annotationPropertyURI;

            } else {

                if( pname.lastIndexOf(':') == pname.length() -1 ) throw new RuntimeException("External property name must not end with colon: " + pname);

                String prefix = pname.substring(0,  indexOfColon);

                String ns = VitalSigns.get().listExternalNamespaces().get(prefix);

                if(ns == null){
                    if(prefix.isEmpty())throw new RuntimeException("Empty prefix not set - cannot set external property: " + pname);
                } else {
                    String shortname = pname.substring(indexOfColon + 1);
                    pname = ns + '#' + shortname;
                }
            }


            // clear

            if( newValue == null || ( newValue instanceof Collection && ((Collection)newValue).size() == 0 ) ) {

                synchronized(_properties) {
                    _properties.remove(pname);
                }

                return;
            }

            if(newValue instanceof MultiValueProperty &&  ((MultiValueProperty)newValue).size() == 0) {

                synchronized(_properties) {
                    _properties.remove(pname);
                }
            }

            synchronized(_properties) {
                _properties.put(pname, toVitalExternalProperty(pname, newValue));
            }
            return;
        }

        if( _properties.containsKey(VitalCoreOntology.vitaltype.getURI())  && VitalCoreOntology.vitaltype.getLocalName().equals( pname ) ) {
            throw new RuntimeException(pname + " property is immutable");
        }

        if(newValue instanceof OtherProperty) throw new RuntimeException("Cannot set generic rdf property as vital property (short name): " + pname);

        // validate property

        // access the property

        IProperty currentP = null;

        synchronized (_properties) {

            String pkey  = null;

            //XXX optimize it!
            for(String key : _properties.keySet()) {

                if(pname.equals( RDFUtils.getPropertyShortName(key) ) ) {
                    pkey = key;
                    break;

                }
            }

            if(pkey != null) {
                currentP = newValue != null ? _properties.get(pkey) : _properties.remove(pkey);
            }
        }

        // we're done
        if(newValue == null) return;

        PropertyMetadata pm = null;

        if(currentP == null) {

            if(types != null && types.size() > 1) {

                for(Class<? extends GraphObject> c : types) {

                    PropertyMetadata pmx = null;
                    //check if property is valid
                    try {
                        pmx = VitalSigns.get().getPropertiesRegistry().getPropertyByShortName(c, pname);
                    } catch (NoSuchFieldException e) {
                    }

                    if(pmx != null) {

                        if(pm != null && !pm.getURI().equals(pmx.getURI()))
                            throw new RuntimeException("Cannot use property " + pname + " two or more properties short names are mapped to types " + types +
                                    ": " + pm.getURI() + " and " + pmx.getURI());

                        pm = pmx;

                    }
                }

                if(pm == null) throw new RuntimeException("Property not found: " + pname + ", types: " + types);

            } else {

                // check if property is valid
                try {
                    pm = VitalSigns.get().getPropertiesRegistry().getPropertyByShortName(this.getClass(), pname);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }

        } else {

            String pURI = currentP.getURI();

            pm = VitalSigns.get().getPropertiesRegistry().getProperty(pURI);

            if(pm == null) throw new RuntimeException("no property definition found for URI: " + pURI + ", the property might have been removed from this class");

        }

        // set value

        if(pm.isMultipleValues()) {

            if(!(newValue instanceof Collection)) throw new RuntimeException("Multivalue property value must be a Collection: " + newValue);

            List vals = new ArrayList();

            for(Object v : (Collection) newValue) {

                v = validateAndTransformProperty(pm, v, false);

                vals.add(v);

                // try {
                // mv.getClass().getMethod("add", Object.class).invoke(mv, v);
                // } catch (Exception e) {
                // throw new RuntimeException(e);
                // }

                // ((Collection)mv).add(v);
            }

            IProperty mv = (IProperty) PropertyFactory.createMultiValueProperty(pm, vals);

            Integer size = null;

            try {
                size = (Integer) mv.getClass().getMethod("size").invoke(mv);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if(size == 0) {


                if(pname.equals(VitalCoreOntology.types.getLocalName())) {
                    throw new RuntimeException("Cannot clear types property!");
                }

                synchronized(_properties) {
                    _properties.remove(pname);
                }

                return;

            }

            if(pname.equals(VitalCoreOntology.types.getLocalName())) {

                boolean constainsType = false;

                String classURI = VitalSigns.get().getClassesRegistry().getClassURI(this.getClass());

                Iterator i = null;

                try {
                    i = (Iterator) mv.getClass().getMethod("iterator").invoke(mv);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                List<String> newTypesURIs = new ArrayList<String>();

                List<Class<? extends GraphObject>> newTypes = new ArrayList<Class<? extends GraphObject>>();

                for(; i.hasNext(); ) {

                    Object o = i.next();

                    String u = null;
                    try {
                        u = (String) o.getClass().getMethod("get").invoke(o);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    if(u.equals(classURI)) {
                        constainsType = true;
                        newTypes.add(this.getClass());
                    } else {

                        ClassMetadata c = VitalSigns.get().getClassesRegistry().getClass(u);

                        if(c != null) {
                            newTypes.add(c.getClazz());
                        } else {
//							if(c == null) throw new RuntimeException("Cannot set type: " + u + " - no vitalsigns class found");
                        }

                    }

                    newTypesURIs.add(u);

                }

                if(!constainsType) throw new RuntimeException("Cannot set types property without base type property: " + classURI + ", class URI: " + this.getClass().getCanonicalName());


                //validate properties list after updates
//				validateTypes(newTypesURIs, )

                PropertiesValidator.validateProperties(newTypes, _properties, false);


                if(newTypes != null) {
                    types = newTypes;
                } else {
                    types = null;
                }



            }

            newValue = mv;

        } else {

            newValue = validateAndTransformProperty(pm, newValue, true);

        }

        synchronized(_properties) {
            _properties.put(pm.getURI(), (IProperty) newValue);
        }

        //_properties.put(property, newValue);
    }

    private IProperty toVitalExternalProperty(String propertyURI, Object newValue) {

        if(newValue instanceof IProperty) {
            //clone it ?

            if( newValue instanceof MultiValueProperty ) {

                Collection<Object> collection = new ArrayList<Object>();

                MultiValueProperty mvp = (MultiValueProperty) newValue;

                for(Object p : mvp) {
                    if(p instanceof IProperty) {
                        collection.add(((IProperty)p).unwrapped());
                    } else {
                        collection.add(p);
                    }
                }

                // Collection<Object> collection = (Collection<Object>) ((IProperty) newValue).rawValue();

                return toVitalExternalProperty(propertyURI, collection);

            }

            IProperty p = (IProperty) newValue;
            p = p.unwrapped();
            IProperty v = PropertyFactory.createBasePropertyFromRawValue(p.getClass(), p.rawValue());
            v.setExternalPropertyURI(propertyURI);
            return v;
        }

        if(newValue instanceof Collection) {


            Collection c = (Collection) newValue;

            List<IProperty> props = new ArrayList<IProperty>();

            for(Object o : c) {
                IProperty p = toVitalExternalProperty(propertyURI, o);
                if(p instanceof MultiValueProperty) {
                    throw new RuntimeException("Multivalue property cannot be nested inside another multivalue property");
                }
                // mv.add(p);
                props.add(p);
            }


            //when setting a single element collection value it should be treated as a single value property
            if(props.size() == 1) {
                return props.get(0);
            }

            MultiValueProperty mv = new MultiValueProperty(props);
            mv.setExternalPropertyURI(propertyURI);
            // mv.setEx

            return mv;


        }

        IProperty p = null;

        if(newValue instanceof BigDecimal) {
            double v = ((BigDecimal) newValue).doubleValue();
            if(v == Double.POSITIVE_INFINITY) throw new RuntimeException("Big decimal value too high - converted to positive infinity " + newValue);
            if(v == Double.NEGATIVE_INFINITY) throw new RuntimeException("Big decimal value too low - converted to negative infinity " + newValue);
            p = new DoubleProperty(v);
        } else if(newValue instanceof Boolean) {
            p = new BooleanProperty((Boolean)newValue);
        } else if(newValue instanceof Date) {
            p = new DateProperty(((Date)newValue).getTime());
        } else if(newValue instanceof Double) {
            p = new DoubleProperty(((Double) newValue).doubleValue());
        } else if(newValue instanceof Float) {
            p = new FloatProperty(((Float)newValue).floatValue());
        } else if(newValue instanceof Integer) {
            p = new IntegerProperty(((Integer)newValue).intValue());
        } else if(newValue instanceof Long) {
            p = new LongProperty(((Long)newValue).longValue());
        } else if(newValue instanceof String) {
            p = new StringProperty((String) newValue);
        } else if(newValue instanceof Truth) {
            p = new TruthProperty((Truth) newValue);
        } else if(newValue instanceof URI) {
            p = new URIProperty(((URI)newValue).toString());
        } else {
            throw new RuntimeException("Unexpected external property value type: " + newValue.getClass().getCanonicalName() + " - " + newValue);
        }

        p.setExternalPropertyURI(propertyURI);
        return p;

    }

    private IProperty validateAndTransformProperty(PropertyMetadata pm,
                                                   Object newValue, boolean wrapWithTrait) {

        // System.out.println("validateAndTransformProperty: " + pm.getShortName());
        // System.out.println("validateAndTransformProperty Value: " + newValue);
        // System.out.println("validateAndTransformProperty isMultiValue: " + pm.isMultipleValues());


        IProperty pattern = pm.getPattern();

        IProperty out = null;

        Class<? extends IProperty> base = pm.getBaseClass();

        if( base == BooleanProperty.class ) {

            boolean val = false;

            if(newValue instanceof BooleanProperty) {
                BooleanProperty bp = (BooleanProperty) newValue;
                val = bp.booleanValue();
            } else if(newValue instanceof String) {
                val = Boolean.parseBoolean((String) newValue);
            } else if(newValue instanceof Boolean) {
                val = ((Boolean)newValue).booleanValue();
            } else {
                unhandledType(pattern, newValue);
            }

            out = wrapWithTrait ? PropertyFactory.createBooleanProperty(pm.getTraitClass(), val) : new BooleanProperty(val);

        } else if(base == DateProperty.class ) {

            Long d = null;

            if(newValue instanceof DateProperty) {

                d = ((DateProperty)newValue).getTime();

            } else if(newValue instanceof Date) {

                d = ((Date) newValue).getTime();

            } else if(newValue instanceof Long) {

                d = (Long) newValue;

            } else if(newValue instanceof Integer) {

                d = ((Integer) newValue).longValue();

            } else if(newValue instanceof Calendar) {

                d = ((Calendar) newValue).getTimeInMillis();

            } else {
                unhandledType(pattern, newValue);
            }

            out = wrapWithTrait ? PropertyFactory.createDateProperty(pm.getTraitClass(), d) : new DateProperty(d);

        } else if(base == DoubleProperty.class) {

            Double d = null;

            if(newValue instanceof DoubleProperty) {
                d = ((DoubleProperty)newValue).doubleValue();
            } else if(newValue instanceof Number) {
                d = ((Number) newValue).doubleValue();
            } else if(newValue instanceof String) {
                // TODO
                throw new RuntimeException("TODO string->number conversion");
            } else {
                unhandledType(pattern, newValue);
            }

            out = wrapWithTrait ? PropertyFactory.createDoubleProperty(pm.getTraitClass(), d) : new DoubleProperty(d);

        } else if(base == FloatProperty.class) {

            Float f = null;

            if(newValue instanceof FloatProperty) {
                f = ((FloatProperty)newValue).floatValue();
            } else if(newValue instanceof Number) {
                f = ((Number) newValue).floatValue();
            } else if(newValue instanceof String) {
                // TODO
                throw new RuntimeException("TODO string->number conversion");
            } else {
                unhandledType(pattern, newValue);
            }

            out = wrapWithTrait ? PropertyFactory.createFloatProperty(pm.getTraitClass(), f) : new FloatProperty(f);

        } else if(base == GeoLocationProperty.class) {

            GeoLocationProperty val = null;

            if(newValue instanceof GeoLocationProperty) {
                val = (GeoLocationProperty) newValue;
            } else if(newValue instanceof String) {
                val = GeoLocationProperty.fromRDFString((String) newValue);
            } else if(newValue instanceof Map) {
                val = GeoLocationProperty.fromJSONMap((Map<String, Object>) newValue);
            } else {
                unhandledType(pattern, newValue);
            }

            out = wrapWithTrait ? PropertyFactory.createGeoLocationProperty(pm.getTraitClass(), val) : val;

        } else if(base == IntegerProperty.class) {

            Integer i = null;

            if(newValue instanceof IntegerProperty) {
                i = ((IntegerProperty)newValue).intValue();
            } else if(newValue instanceof Number) {
                i = ((Number) newValue).intValue();
            } else if(newValue instanceof String) {
                // TODO
                throw new RuntimeException("TODO string->number conversion");
            } else {
                unhandledType(pattern, newValue);
            }

            out = wrapWithTrait ? PropertyFactory.createIntegerProperty(pm.getTraitClass(), i) : new IntegerProperty(i);

        } else if(base == LongProperty.class) {

            Long l = null;

            if(newValue instanceof LongProperty) {
                l = ((LongProperty) newValue).longValue();
            } else if(newValue instanceof Number) {
                l = ((Number) newValue).longValue();
            } else if(newValue instanceof String) {
                l = Long.parseLong((String) newValue);
            } else {
                unhandledType(pattern, newValue);
            }

            out = wrapWithTrait ? PropertyFactory.createLongProperty(pm.getTraitClass(), l) : new LongProperty(l);

        } else if(base == StringProperty.class) {

            String s = null;

            if(newValue instanceof StringProperty) {
                s = ((StringProperty)newValue).toString();
            } else if(newValue instanceof GString) {
                s = ((GString)newValue).toString();
            } else if(newValue instanceof String) {
                s = (String) newValue;
            } else {
                unhandledType(pattern, newValue);
            }

            out = wrapWithTrait ? PropertyFactory.createStringProperty(pm.getTraitClass(), xmlStringFilter(pm.getShortName(), s)) : new StringProperty(s);

        } else if(base == TruthProperty.class) {

            Truth t = null;

            if(newValue instanceof StringProperty) {

                t = Truth.fromString( ((StringProperty)newValue).asString() );

            } else if(newValue instanceof String || newValue instanceof GString) {

                t = Truth.fromString( newValue.toString() );

            } else if(newValue instanceof TruthProperty) {

                t = ((TruthProperty)newValue).getTruth();

            } else if(newValue instanceof Truth) {

                t = (Truth) newValue;

            } else if(newValue instanceof BooleanProperty) {

                //yes/no only
                t = Truth.fromBoolean( ((BooleanProperty)newValue).asBoolean() );

            } else if(newValue instanceof Boolean) {

                t = Truth.fromBoolean( (Boolean)newValue );

            } else {
                unhandledType(pattern, newValue);
            }

            out = wrapWithTrait ? PropertyFactory.createTruthProperty(pm.getTraitClass(), t) : new TruthProperty(t);

        } else if(base == URIProperty.class) {

            String s = null;

            if(newValue instanceof URIProperty) {
                s = ((URIProperty)newValue).get();
            } else if(newValue instanceof GString) {
                s = ((GString)newValue).toString();
            } else if(newValue instanceof String) {
                s = (String) newValue;
            } else {
                unhandledType(pattern, newValue);
            }

            out = wrapWithTrait ? PropertyFactory.createURIProperty(pm.getTraitClass(), xmlStringFilter(pm.getShortName(), s)) : new URIProperty(s);

        } else {
            throw new RuntimeException("Unhandled property type: " + pattern.getClass().getCanonicalName() + ", URI: "  + pattern.getURI());
        }


        if( VitalSigns.get().getConfig().enforceConstraints ) {

            try {
                PropertyDefinitionProcessor.validateProperty(this.getClass(), pm, out.rawValue());
            } catch (PropertyValidationException e) {
                throw new RuntimeException("Property validation failed: " + e.getLocalizedMessage());
            }
        }

        return out;
    }

    private void unhandledType(IProperty pattern, Object newValue) {
        throw new RuntimeException("Unexpected property " + pattern.getURI() + " value class: " + newValue.getClass().getCanonicalName());
    }

    private void initPropertiesMap() {

        if(_properties == null){
            _properties = Collections.synchronizedMap( new HashMap<String, IProperty>() );
        }

    }

    @Override
    public Object getProperty(String pname) {

        if(pname == null) throw new NullPointerException("Null property key");

        if(pname.equals("URI")) {
            if(_properties == null) return null;
            IProperty iProperty = _properties.get(VitalCoreOntology.URIProp.getURI());
            if(iProperty == null) return null;
            return iProperty.rawValue();
        }

        if(pname.equals("class")) return this.getClass();

        // List<String> longNames = VitalSigns.get().getPropertiesRegistry().getPropertyLongName(shortName);

        initPropertiesMap();

        int indexOfColon = pname.indexOf(':');

        if(indexOfColon >= 0) {

            if(RDF.type.getURI().equals(pname)) throw new RuntimeException("Cannot access external rdf:type properties, use 'types' getter instead");

            if( pname.lastIndexOf(':') == pname.length() -1 ) throw new RuntimeException("External property name must not end with colon: " + pname);

            String prefix = pname.substring(0,  indexOfColon);

            String ns = VitalSigns.get().listExternalNamespaces().get(prefix);

            if(ns == null){
                if(prefix.isEmpty())throw new RuntimeException("Empty prefix not set - cannot set external property: " + pname);
            } else {
                String shortname = pname.substring(indexOfColon + 1);
                pname = ns + '#' + shortname;
            }

            return _properties.get(pname);

        }

        String pKey = null;

        synchronized (_properties) {

            // XXX optimize it!
            for(String key : _properties.keySet()) {

                if(pname.equals( RDFUtils.getPropertyShortName(key) ) ) {

                    pKey = key;

                    break;
                }
            }
        }

        if(pKey == null) {

            NoSuchFieldException ex = null;

            try {
                VitalSigns.get().getPropertiesRegistry().getPropertyByShortName(this.getClass(), pname);
            } catch (NoSuchFieldException e) {
                ex = e;
            }

            if(ex != null) {
                // check if annotation property
                String pURI = VitalSigns.get().getPropertiesRegistry().getAnnotationPropertyURIByShortName(this.getClass(), pname);
                if(pURI != null) {
                    pKey = pURI;
                } else {
                    throw new RuntimeException(ex);
                }
            }

            if(pKey == null) {
                return null;
            }
        }

        IProperty v = _properties.get(pKey);

        return v;

    }

    public String getURI() {
        return (String) getProperty("URI");
    }


    public void setURI(Object _URI) {
		/*
		if(_URI == null) {
			this.URI = null;
		} else if(_URI instanceof String) {
			this.URI = (String)_URI;
		} else if(_URI instanceof GString) {
			this.URI = ((GString)_URI).toString();
		} else if(_URI instanceof StringProperty) {
			this.URI = ((StringProperty)_URI).toString();
		} else if(_URI instanceof URIProperty) {
			this.URI = ((URIProperty)_URI).get();
		} else {
			throw new RuntimeException("Invalid URI argument type, only String/Gstring/StringProperty/URIProperty allowed");
		}
		*/

        setProperty(VitalCoreOntology.URIProp.getLocalName(), _URI);
    }

    public Map<String, IProperty> getPropertiesMap() {
        initPropertiesMap();
        return _properties;
    }

    public String toCompactString() {
        return CompactStringSerializer.toCompactString(this);
    }

    public String toJSON() {
        return JSONSerializer.toJSONString(this);
    }

    /**
     * Alias for toRDF(RDFFormat.N_TRIPLE)
     * @return n-triples string
     */
    public String toRDF() {
        return this.toRDF(RDFFormat.N_TRIPLE);
    }

    /**
     * Returns the RDF string representation of this graph object in given format
     * @return rdfstring rdf string representation in given format
     */
    public String toRDF(RDFFormat format) {

        //internally it gets converted
        if(getURI() == null) throw new RuntimeException("No URI set of a graph object!");
        Model model = ModelFactory.createDefaultModel();

        RDFSerialization.serializeToModel(this, model);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        model.write(os, format.toJenaTypeString());

        model.close();

        try {
            return os.toString("UTF-8").trim();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Appends the graph object to jena model and returns its resource
     * @param model jena model
     * @return resource this graph object resource in the model
     */
    public Resource toRDF(Model model) {
        return RDFSerialization.serializeToModel(this, model);
    }

    /**
     * Generates and assigns URI for given app, returns this object
     * @param app
     * @return this graph object
     */
    public T generateURI(VitalApp app) {
        this.setURI(URIGenerator.generateURI(app, this.getClass()));
        return (T) this;
    }

    public T generateURI(VitalApp app, String randomPart) {
        this.setURI(URIGenerator.generateURI(app, this.getClass(), randomPart));
        return (T) this;
    }

    /**
     * Generates and assigns a random URI for current app (must be set in VitalSigns), returns this object
     * @return this graph object
     */
    public T generateURI() {
        if ( VitalSigns.get().getCurrentApp() == null ) throw new RuntimeException("Current application not set - cannot generate URI");
        this.setURI( URIGenerator.generateURI(VitalSigns.get().getCurrentApp(), this.getClass()) );
        return (T) this;
    }

    /**
     * Generates and assigns a fixed URI for current app (must be set in VitalSigns), returns this object
     * @param randomPart
     * @return
     */
    public T generateURI(String randomPart) {
        if ( VitalSigns.get().getCurrentApp() == null ) throw new RuntimeException("Current application not set - cannot generate URI");
        this.setURI(URIGenerator.generateURI(VitalSigns.get().getCurrentApp(), this.getClass(), randomPart));
        return (T) this;
    }

    /**
     * Adds this object to cache and returns this object
     * @return this graph object
     */
    public T addToCache() {
        if(this.getURI() == null) throw new RuntimeException("Cannot add to cache, graph object's URI mustn't be null");
        VitalSigns.get().addToCache(this);
        return (T) this;
    }


    protected String xmlStringFilter(String pname, String value) {

        if(value == null) return null;

        if( VitalSigns.get().getConfig().xmlStringFilter ) {

            String s = (String)value;

            String newValue = XMLUtils.cleanupString(s);

            if(newValue.length() != s.length()) {
                log.info("Filtered out some xml characters, URI: " + this.getURI() + ", property: " + pname +
                        ", input string: " + s + " (" + s.length() +
                        "), output: " + newValue + " (" + newValue.length() + ")");
            }

            value = newValue;

        }

        return value;

    }

    /**
     * shortcut method to set property by trait class
     * @param prop
     * @param val
     */
    public void set(Class<?> prop, Object val) {
        this.setProperty(PropertyUtils.getShortName(prop), val);
    }

    /**
     * shortcut method to get property by trait class
     * @param prop
     * @return
     */
    public Object get(Class<?> prop) {
        return this.getProperty( PropertyUtils.getShortName(prop) );
    }

    /**
     * Returns raw value of property by trait class
     * @param prop
     * @return
     */
    public Object getRaw(Class<?> prop) {
        Object v = this.getProperty(PropertyUtils.getShortName(prop));
        if(v == null) return null;
        if(v instanceof IProperty) {
            return ((IProperty)v).rawValue();
        } else {
            return v;
        }
    }

    /**
     * Validates graph object
     * @return
     */
    public ValidationStatus validate() {

        ValidationStatus status = new ValidationStatus();

        if(this.getURI() == null) {
            status.putError("URI", "null_uri_field");
        } else if(this.getURI().isEmpty()) {
            status.putError("URI", "empty_uri_field");
        } else {

            URIResponse ur = URIGenerator.validateURI(this.getURI());

            status.setUriResponse(ur);

            if(!ur.valid) {

                if( VitalSigns.get().getConfig().enforceConstraints ) {
                    status.putError("URI", "invalid_uri_field");
                } else {
                    log.warn("invalid uri: " + this.getURI() + " allowed - no constraints enforced");
                }

            }

        }


        for(Entry<String, IProperty> e : this.getPropertiesMap().entrySet()) {

            String f = e.getKey();


            PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(f);

            if(pm != null && pm.isInternalStore()) {
                status.putError(f, "cannot_set_internal_store_field");
                continue;
            }

            if(pm != null) {

                try {
                    PropertyDefinitionProcessor.validateProperty(this.getClass(), pm, e.getValue().rawValue());
                } catch (PropertyValidationException e1) {
                    status.putError(f, "property_validation " + e1.getLocalizedMessage());
                }

            }
        }

        return status;

    }

    public boolean isSuperTypeOf(Class<? extends GraphObject> other) {
        return _typeImpl(other, false);
    }

    public boolean isSubTypeOf(Class<? extends GraphObject> other) {
        return _typeImpl(other, true);
    }

    private boolean _typeImpl(Class<? extends GraphObject> other, boolean subNotSuper) {

        @SuppressWarnings("rawtypes")
        MultiValueProperty types = (MultiValueProperty) ((IProperty) this.get(Property_types.class)).unwrapped();

        if(types.size() == 1) {
            if(subNotSuper) {
                return other.isAssignableFrom(this.getClass());
            } else {
                return this.getClass().isAssignableFrom(other);
            }
        }

        for(Object o : types) {

            URIProperty u = (URIProperty)o;

            Class<? extends GraphObject> c = VitalSigns.get().getClass(u);

            if(c == null) throw new RuntimeException("Class not found for URI: " + u.get());

            if(subNotSuper) {
                if(other.isAssignableFrom( c ) ) {
                    return true;
                }
            } else {
                if(c.isAssignableFrom( other ) ) {
                    return true;
                }

            }

        }

        return false;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {

        // safest deep clone is to use utility
        return VitalJavaSerializationUtils.clone(this);
    }

    /**
     * Compares graph object to another object
     * Two graph objects are equal only and if only they are of the same class, have same URIs and properties set.
     * @param obj
     * @return equals
     */
    @Override
    public boolean equals(Object obj) {

        if(obj == null) return false;

        if(!(this.getClass().equals(obj.getClass()))) return false;

        GraphObject g = (GraphObject) obj;

        String u = this.getURI();
        if( u != null) {
            if( ! u.equals( g.getURI() ) ) return false;
        } else {
            if(g.getURI() != null) return false;
        }

        Map<String, IProperty> m = g.getPropertiesMap();

        Map<String, IProperty> thisProperties = this.getPropertiesMap();

        int size1 = m.size();
        int thisSize = thisProperties.size();
        if(size1 != thisSize) return false;

        // compare properties
        for(Entry<String, IProperty> entry : thisProperties.entrySet()) {

            IProperty pi = m.get(entry.getKey());

            if(pi == null) return false;

            IProperty unwrappedThis = entry.getValue().unwrapped();
            IProperty unwrapped1 = pi.unwrapped();

            if(! unwrappedThis.getClass().equals(unwrapped1.getClass() ) ) return false;

            //			if(unwrappedThis instanceof MultiValueProperty) {
            //
            //				for(Object p : (MultiValueProperty)unwrappedThis) {
            //
            //					IProperty p :
            //
            //				}
            //
            //			} else {
            //
            //			}

            Object v1 = pi.rawValue();
            Object thisRaw = entry.getValue().rawValue();

            if(!v1.equals(thisRaw)) return false;


        }

        return true;
    }

    @Override
    public String toString() {
        return this.toCompactString();
    }

    private void cleanHierarchyList(List<Class<? extends GraphObject>> otherTopTypes) {

        //get rid of super classes from the list
        for(int i = otherTopTypes.size() -1; i >= 0; i--) {
            Class<? extends GraphObject> c1 = otherTopTypes.get(i);
            boolean deleteIt = false;
            for(int j = otherTopTypes.size() -1; j >= 0; j--) {
                if(i == j) continue;
                Class<? extends GraphObject> c2 = otherTopTypes.get(j);
                //same or super
                if(c1.isAssignableFrom(c2)) {
                    deleteIt = true;
                    break;
                }
            }

            if(deleteIt) {
                otherTopTypes.remove(i);
            }
        }

    }
    /**
     * tests if one graph object instance is the same type (trait) as another (or at least one of the most specific traits in the hierarchy matches).
     * @param other
     * @return
     */
    public boolean isaType(GraphObject other) {

        List<Class<? extends GraphObject>> otherTopTypes = new ArrayList<Class<? extends GraphObject>>(other.types);
        cleanHierarchyList(otherTopTypes);
        return _isaTypeImpl(otherTopTypes);
    }

    public boolean isaType(Class<? extends GraphObject> clazz) {
        List<?>  l = Arrays.asList(clazz);
        return _isaTypeImpl((List<Class<? extends GraphObject>>) l);
    }

    private boolean _isaTypeImpl(List<Class<? extends GraphObject>> otherTopTypes) {

        List<Class<? extends GraphObject>> thisTopTypes = new ArrayList<Class<? extends GraphObject>>(this.types);
        cleanHierarchyList(thisTopTypes);

        for(Class<? extends GraphObject> otherClass : otherTopTypes) {
            if(thisTopTypes.contains(otherClass)) {
                return true;
            }
        }

        return false;

    }

    public boolean addType(String classURI) {
        if(classURI == null) throw new NullPointerException("Null classURI to add");
        ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(classURI);
        if(cm == null) throw new RuntimeException("Class with URI not found: " + classURI);
        return addType(cm.getClazz());
    }

    public boolean addType(Class<? extends GraphObject> type) {

        if(type == null) throw new NullPointerException("Null type to add");
        String typeURI = VitalSigns.get().getClassesRegistry().getClassURI(type);
        if(typeURI == null) throw new RuntimeException("URI for class: " + type.getCanonicalName() + " not found.");

        if(this.types.contains(type)) return false;

        // use set type method
        List<String> newSet = new ArrayList<String>();
        for(Class<? extends GraphObject> c : this.types) {
            String u = VitalSigns.get().getClassesRegistry().getClassURI(c);
            if(u == null) throw new RuntimeException("URI for class: " + c.getCanonicalName() + " not found.");
            newSet.add(u);
        }
        newSet.add(typeURI);

        setProperty(VitalCoreOntology.types.getLocalName(), newSet);

        return true;

    }

    public boolean removeType(String classURI) {

        if(classURI == null) throw new NullPointerException("Null classURI to add");

        ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(classURI);

        if(cm == null) throw new RuntimeException("Class with URI not found: " + classURI);

        return removeType(cm.getClazz());
    }

    public boolean removeType(Class<? extends GraphObject> type) {

        if(this.getClass().equals(type)) throw new RuntimeException("Cannot remove self type");

        if(type == null) throw new NullPointerException("Null type to add");
        String typeURI = VitalSigns.get().getClassesRegistry().getClassURI(type);
        if(typeURI == null) throw new RuntimeException("URI for class: " + type.getCanonicalName() + " not found.");

        List<Class<? extends GraphObject>> newTypes = new ArrayList<Class<? extends GraphObject>>();

        boolean f = false;

        for( Iterator<Class<? extends GraphObject>> iterator = this.types.iterator(); iterator.hasNext(); ) {
            Class<? extends GraphObject> next = iterator.next();
            if(type.equals(next)) {
                f = true;
            } else {
                newTypes.add(next);
            }
        }

        if(!f) return f;

        PropertiesValidator.validateProperties(newTypes, _properties, true);


        //use set type method
        List<String> newSet = new ArrayList<String>();
        for(Class<? extends GraphObject> c : this.types) {
            String u = VitalSigns.get().getClassesRegistry().getClassURI(c);
            if(u == null) throw new RuntimeException("URI for class: " + c.getCanonicalName() + " not found.");
            newSet.add(u);
        }
        if(!newSet.remove(typeURI)) throw new RuntimeException("URI type not found in source list: " + typeURI);

        setProperty(VitalCoreOntology.types.getLocalName(), newSet);

        return f;

    }


    protected VITAL_Edge _addEdgeImplementation(ClassMetadata edgeClass, VITAL_Node otherNode, boolean forwardNotReverse) {

        //validate other endpoint
        List<ClassMetadata> otherDomains = null;
        if(forwardNotReverse) {
            otherDomains = edgeClass.getEdgeDestinationDomains();
        } else {
            otherDomains = edgeClass.getEdgeSourceDomains();
        }

        boolean valid = false;
        for(ClassMetadata od : otherDomains) {
            if( od.getClazz().isAssignableFrom(otherNode.getClass()) ) {
                valid = true;
                break;
            }
        }

        if(!valid) throw new RuntimeException("Edge other endpoint is not " + ( forwardNotReverse ? "destination" : "source" ) + " domain of edge " + edgeClass.getClazz().getSimpleName() + ": " + otherNode.getClass().getSimpleName());

        VITAL_Edge edge = null;
        try {
            edge = (VITAL_Edge) edgeClass.getClazz().newInstance();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        if(forwardNotReverse) {
            edge.setSourceURI(this.getURI());
            edge.setDestinationURI(otherNode.getURI());
        } else {
            edge.setSourceURI(otherNode.getURI());
            edge.setDestinationURI(this.getURI());
        }

        edge.generateURI();

        return edge;


    }

    protected VITAL_HyperEdge _addHyperEdgeImplementation(ClassMetadata hyperEdgeClass, GraphObject otherObject, boolean forwardNotReverse) {

        //validate other endpoint
        List<ClassMetadata> otherDomains = null;
        if(forwardNotReverse) {
            otherDomains = hyperEdgeClass.getHyperEdgeDestinationDomains();
        } else {
            otherDomains = hyperEdgeClass.getHyperEdgeSourceDomains();
        }

        boolean valid = false;
        for(ClassMetadata od : otherDomains) {
            if( od.getClazz().isAssignableFrom(otherObject.getClass()) ) {
                valid = true;
                break;
            }
        }

        if(!valid) throw new RuntimeException("HyperEdge other endpoint is not in " + (forwardNotReverse ? "destination" : "source" ) + " domain of hyperedge " + hyperEdgeClass.getClazz().getSimpleName() + ": " + otherObject.getClass().getSimpleName());

        VITAL_HyperEdge hyperEdge = null;
        try {
            hyperEdge = (VITAL_HyperEdge) hyperEdgeClass.getClazz().newInstance();
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        if(forwardNotReverse) {
            hyperEdge.setSourceURI(this.getURI());
            hyperEdge.setDestinationURI(otherObject.getURI());
        } else {
            hyperEdge.setSourceURI(otherObject.getURI());
            hyperEdge.setDestinationURI(this.getURI());
        }

        hyperEdge.generateURI();

        return hyperEdge;

    }

    @Override
    public Object invokeMethod(String name, Object args) {

        try {

            return super.invokeMethod(name, args);

        } catch(MissingMethodException e ) {

            if(name.equals("addEdge") || name.equals("addReverseEdge")) {

                boolean forwardNotReverse = name.equals("addEdge");

                if(!(args instanceof Object[])) throw new RuntimeException("Excepted arguments as an array of objects, length 2, method: " + name);

                Object[] argsA = (Object[]) args;

                if(argsA.length != 2) throw new RuntimeException("Excepted arguments as an array of objects, length 2, method: " + name);

                Class<? extends VITAL_Edge> edgeClass = null;

                Object arg0 = argsA[0];
                Object arg1 = argsA[1];

                if(arg0 instanceof Class && VITAL_Edge.class.isAssignableFrom((Class<?>) arg0)) {
                    edgeClass = (Class<? extends VITAL_Edge>) arg0;
                } else {
                    throw new RuntimeException("Excepted arguments as an array of objects, length 2, first argument: Class<? extends VITAL_Edge>, method: " + name);
                }


                ClassMetadata ecm = VitalSigns.get().getClassesRegistry().getClassMetadata(edgeClass);
                if(ecm == null) throw new RuntimeException("Edge class not found in VitalSigns: " + edgeClass.getCanonicalName());

                if(!( arg1 instanceof VITAL_Node) ) throw new RuntimeException("Excepted arguments as an array of objects, length 2, second argument: VITAL_Node, method: " + name);

                VITAL_Node otherNode = (VITAL_Node) arg1;

                return _addEdgeImplementation(ecm, otherNode, forwardNotReverse);

            } else if(name.equals("addHyperEdge") || name.equals("addReverseHyperEdge")) {

                boolean forwardNotReverse = name.equals("addHyperEdge");

                if(!(args instanceof Object[])) throw new RuntimeException("Excepted arguments as an array of objects, length 2, method: " + name);

                Object[] argsA = (Object[]) args;

                if(argsA.length != 2) throw new RuntimeException("Excepted arguments as an array of objects, length 2, method: " + name);

                Class<? extends VITAL_HyperEdge> hyperEdgeClass = null;

                Object arg0 = argsA[0];
                Object arg1 = argsA[1];

                if(arg0 instanceof Class && VITAL_HyperEdge.class.isAssignableFrom((Class<?>) arg0)) {
                    hyperEdgeClass = (Class<? extends VITAL_HyperEdge>) arg0;
                } else {
                    throw new RuntimeException("Excepted arguments as an array of objects, length 2, first argument: Class<? extends VITAL_HyperEdge>, method: " + name);
                }

                ClassMetadata ecm = VitalSigns.get().getClassesRegistry().getClassMetadata(hyperEdgeClass);
                if(ecm == null) throw new RuntimeException("Edge class not found in VitalSigns: " + hyperEdgeClass.getCanonicalName());

                if(!( arg1 instanceof GraphObject) ) throw new RuntimeException("Excepted arguments as an array of objects, length 2, second argument: GraphObject, method: " + name);

                GraphObject otherObject = (GraphObject) arg1;

                return _addHyperEdgeImplementation(ecm, otherObject, forwardNotReverse);

            } else if(name.startsWith("addEdge_") || name.startsWith("addReverseEdge_")) {

                if(!(args instanceof Object[])) throw new RuntimeException("Excepted arguments as an array of objects, length 1, method: " + name);

                Object[] argsA = (Object[]) args;

                if(argsA.length != 1) throw new RuntimeException("Excepted arguments as an array of objects, length 1, method: " + name);

                Object arg = argsA[0];

                if(!(arg instanceof VITAL_Node)) throw new RuntimeException("The only method " + name + " argument must be an instanceof VITAL_Node");

                VITAL_Node otherNode = (VITAL_Node) arg;


                if(!VITAL_Node.class.isAssignableFrom(this.getClass())) {
                    throw new RuntimeException("Cannot invoke " + name + " method on non VITAL_Node object");
                }

                if(this.getURI() == null) throw new RuntimeException("No URI set in this node");
                if(otherNode.getURI() == null) throw new RuntimeException("No URI set in argument node");

                boolean forwardNotReverse = true;

                String edgeName = null;

                if( name.startsWith("addEdge_") ) {

                    edgeName = name.substring("addEdge_".length());

                } else {

                    edgeName = name.substring("addReverseEdge_".length());
                    forwardNotReverse = false;

                }

                String edgeSingleName = edgeName;

                if(edgeSingleName.startsWith("has")) {
                    edgeSingleName = edgeSingleName.substring(3);
                    edgeSingleName = edgeSingleName.substring(0, 1).toLowerCase() + edgeSingleName.substring(1);
                }


                ClassMetadata edgeClass = null;

                List<ClassMetadata> c = VitalSigns.get().getClassesRegistry().getEdgeClassesWithSourceOrDestNodeClass((Class<? extends VITAL_Node>) this.getClass(), forwardNotReverse);

                for(ClassMetadata ec : c) {

                    if(ec.getEdgeSingleName().equals(edgeSingleName)) {
                        if(edgeClass != null) throw new RuntimeException("More than 1 matching edge class found for name: " + edgeName);
                        edgeClass = ec;
                    }

                }

                if(edgeClass == null) throw new RuntimeException("No matching edge found for class: " + this.getClass() + " edge name: " + edgeName);

                return _addEdgeImplementation(edgeClass, otherNode, forwardNotReverse);

            } else if(name.startsWith("addHyperEdge_") || name.startsWith("addReverseHyperEdge_")){

                if(!(args instanceof Object[])) throw new RuntimeException("Excepted arguments as an array of objects, length 1, method: " + name);

                Object[] argsA = (Object[]) args;

                if(argsA.length != 1) throw new RuntimeException("Excepted arguments as an array of objects, length 1, method: " + name);

                Object arg = argsA[0];

                if(!(arg instanceof GraphObject)) throw new RuntimeException("The only method " + name + " argument must be an instanceof GraphObject");

                GraphObject otherObject = (GraphObject) arg;

                if(this.getURI() == null) throw new RuntimeException("No URI set in this graph object");

                if(otherObject.getURI() == null) throw new RuntimeException("No URI set in argument graph object");

                boolean forwardNotReverse = true;

                String hyperEdgeName = null;

                if( name.startsWith("addHyperEdge_") ) {

                    hyperEdgeName = name.substring("addHyperEdge_".length());

                } else {

                    hyperEdgeName = name.substring("addReverseHyperEdge_".length());
                    forwardNotReverse = false;

                }

                String hyperEdgeSingleName = hyperEdgeName;

                if(hyperEdgeSingleName.startsWith("has")) {
                    hyperEdgeSingleName = hyperEdgeSingleName.substring(3);
                    hyperEdgeSingleName = hyperEdgeSingleName.substring(0, 1).toLowerCase() + hyperEdgeSingleName.substring(1);
                }


                ClassMetadata hyperEdgeClass = null;

                List<ClassMetadata> c = VitalSigns.get().getClassesRegistry().getHyperEdgeClassesWithSourceOrDestGraphClass(this.getClass(), forwardNotReverse);

                for(ClassMetadata ec : c) {

                    if(ec.getEdgeSingleName().equals(hyperEdgeSingleName)) {
                        if(hyperEdgeClass != null) throw new RuntimeException("More than 1 matching hyper edge class found for name: " + hyperEdgeName);
                        hyperEdgeClass = ec;
                    }

                }

                if(hyperEdgeClass == null) throw new RuntimeException("No matching hyper edge found for class: " + this.getClass() + " edge name: " + hyperEdgeName);

                return _addHyperEdgeImplementation(hyperEdgeClass, otherObject, forwardNotReverse);

            }


            throw e;
        }

    }

	/*
	public GraphObject save(Object... args) {
		throw new RuntimeException("save() not implemented by upper layer");
	}
	*/

    // hyper edge methods
    public List<VITAL_HyperEdge> getOutgoingHyperEdges() {
        return getOutgoingHyperEdges(null, null);
    }

    public List<VITAL_HyperEdge> getOutgoingHyperEdges(GraphContext graphContext, VITAL_Container container) {

        if(graphContext == null) graphContext = GraphContext.Local;

        if(graphContext != GraphContext.Container && container != null) {
            throw new RuntimeException("GraphContext: " + graphContext + " cannot be used with containers." );
        }

        String uri = this.getURI();

        EdgesResolver resolver = VitalSigns.get().getEdgesResolver(graphContext);

        if( resolver == null ) {
            throw new RuntimeException("No edges resolver set - vital signs requires it!");
        }

        List<VITAL_HyperEdge> res = resolver.getHyperEdgesForSrcURI(uri, container);

        return res;
    }

    // six variants
    // 1

    public List<VITAL_HyperEdge> getHyperEdges() {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, true);
    }

    public List<VITAL_HyperEdge> getHyperEdgesOut() {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, false);
    }

    public List<VITAL_HyperEdge> getHyperEdgesIn() {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, false, true);
    }

    // 2
    public List<VITAL_HyperEdge> getHyperEdges(GraphObject node2) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, true, node2);
    }

    public List<VITAL_HyperEdge> getHyperEdgesOut(GraphObject node2) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, false, node2);
    }

    public List<VITAL_HyperEdge> getHyperEdgesIn(GraphObject node2) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, false, true, node2);
    }


    // 3
    public List<VITAL_HyperEdge> getHyperEdges(GraphContext graphContext) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, true, graphContext);
    }

    public List<VITAL_HyperEdge> getHyperEdgesOut(GraphContext graphContext) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, false, graphContext);
    }

    public List<VITAL_HyperEdge> getHyperEdgesIn(GraphContext graphContext) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, false, true, graphContext);
    }


    // 4
    public List<VITAL_HyperEdge> getHyperEdges(GraphContext graphContext, GraphObject node2) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, true, graphContext, node2);
    }

    public List<VITAL_HyperEdge> getHyperEdgesOut(GraphContext graphContext, GraphObject node2) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, false, graphContext, node2);
    }

    public List<VITAL_HyperEdge> getHyperEdgesIn(GraphContext graphContext, GraphObject node2) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, false, true, graphContext, node2);
    }


    // 5
    public List<VITAL_HyperEdge> getHyperEdges(GraphContext graphContext, List<VITAL_Container> containers) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, true, graphContext, containers);
    }

    public List<VITAL_HyperEdge> getHyperEdgesOut(GraphContext graphContext, List<VITAL_Container> containers) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, false, graphContext, containers);
    }

    public List<VITAL_HyperEdge> getHyperEdgesIn(GraphContext graphContext, List<VITAL_Container> containers) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, false, true, graphContext, containers);
    }


    // 6
    public List<VITAL_HyperEdge> getHyperEdges(GraphContext graphContext, List<VITAL_Container> containers, GraphObject node2) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, true, graphContext, containers, node2);
    }

    public List<VITAL_HyperEdge> getHyperEdgesOut(GraphContext graphContext, List<VITAL_Container> containers, GraphObject node2) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, true, false, graphContext, containers, node2);
    }

    public List<VITAL_HyperEdge> getEdgesIn(GraphContext graphContext, List<VITAL_Container> containers, GraphObject node2) {
        return (List<VITAL_HyperEdge>) HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(this, null, false, true, graphContext, containers, node2);
    }


    public List<VITAL_HyperEdge> getIncomingHyperEdges() {
        return getIncomingHyperEdges(null, null);
    }

    public List<VITAL_HyperEdge> getIncomingHyperEdges(GraphContext graphContext, VITAL_Container container) {

        if(graphContext == null) graphContext = GraphContext.Local;

        if(graphContext != GraphContext.Container && container != null) {

            throw new RuntimeException("GraphContext: " + graphContext + " cannot be used with containers." );
        }

        String uri = this.getURI();

        EdgesResolver resolver = VitalSigns.get().getEdgesResolver(graphContext);

        if( resolver == null ) {

            throw new RuntimeException("No edges resolver set - vital signs requires it!");
        }

        List<VITAL_HyperEdge> res = resolver.getHyperEdgesForDestURI(uri, container);

        return res;
    }


    // java hack?

    public List<GraphObject> getHyperCollection(String collectionName) {
        return getHyperCollection(collectionName, GraphContext.Local);
    }

    public List<GraphObject> getHyperCollection(String collectionName, GraphContext graphContext) {
        return getHyperCollection(collectionName, graphContext, new VITAL_Container[0]);
    }

    public List<GraphObject> getHyperCollection(String collectionName, GraphContext graphContext, VITAL_Container... containers) {

        // look for
        Class<? extends VITAL_HyperEdge> hyperEdgeClass = null;

        List<ClassMetadata> edges = VitalSigns.get().getClassesRegistry().getHyperEdgeClassesWithSourceOrDestGraphClass(this.getClass(), true);

        for(ClassMetadata cm : edges) {

            if(cm.getEdgePluralName() != null && cm.getEdgePluralName().equals(collectionName)) {
                hyperEdgeClass = (Class<? extends VITAL_HyperEdge>) cm.getClazz();
                break;
            }

        }

        if(hyperEdgeClass == null) throw new RuntimeException("Collection " + collectionName + " not found for class: " + this.getClass());

        return (List<GraphObject>) HyperEdgeAccessImplementation.hyperCollectionImplementation(this, hyperEdgeClass, graphContext, containers, true);
    }

    /**
     *
     * @param includeHeader
     * @return
     */

    public List<String> toCSV(boolean includeHeader) {

        List<String> r = new ArrayList<String>();

        if(includeHeader) {
            r.add(ToCSVHandler.getHeaders());
        }

        ToCSVHandler.toCSV(this, r);

        return r;

    }

    /**
     * Returns a list of SQL records that may be imported via multi-row insert
     * Each string is a complete row: (field1, field2, field3 ...)
     * @return
     */

    public List<String> toSQLRows() {
        return ToSQLRowsHandler.toSqlRows(this);
    }
}
