package ai.vital.vitalsigns.model

import ai.vital.vitalsigns.model.GraphObject

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.block.CompactStringSerializer;
import ai.vital.vitalsigns.model.property.PropertyFactory;
import ai.vital.vitalsigns.model.property.StringProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;

/**
 * A base class for schema-less graph objects, such as sparql results binding etc.
 * The property values are unwrapped, it is user responsibility to make sure all objects are serializable
 *
 */

class VITAL_GraphContainerObject extends GraphObject<VITAL_GraphContainerObject> {

    private static final long serialVersionUID = 1L;

    static Set<String> forwardedProperties = new HashSet<String>(Arrays.asList(
            VitalCoreOntology.vitaltype.getLocalName(),
            VitalCoreOntology.types.getLocalName(),
            VitalCoreOntology.URIProp.getLocalName(),
            "URI",
            "class"
    ));

    //overridden properties map

    public VITAL_GraphContainerObject() {
        super();
    }

    public VITAL_GraphContainerObject(Map<String, Object> propertiesMap) {
        super(propertiesMap);
    }

    @Override
    public Object getProperty(String property) {

        if(forwardedProperties.contains(property)) {
            return super.getProperty(property);
        }

        if(this instanceof GraphMatch) {
            IProperty p = _properties.get(property);

            if(p != null && p instanceof StringProperty) {
                GraphObject g = CompactStringSerializer.fromString(p.toString());
                if(g != null) return g;
            }

            return p;

        }

        return _properties.get(property);

    }

    @Override
    public void setProperty(String property, Object newValue) {

        if(forwardedProperties.contains(property)) {
            super.setProperty(property, newValue);
            return;
        }

        //direct map access?
        if(newValue == null) {
            _properties.remove(property);
            return;
        }

        newValue = PropertyFactory.createBaseProperty(newValue);

        _properties.put(property, (IProperty) newValue);

    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName() + "-URI:" + getURI() + "-" + _properties;
    }

    public Set<String> getDynamicPropertyNames() {
        return new HashSet<String>(_properties.keySet());
    }

}




