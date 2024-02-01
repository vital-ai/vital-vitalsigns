package ai.vital.vitalsigns.java;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.VitalSignsSingleton;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.PropertyFactory;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.properties.PropertyTrait;

/**
 * Special map for serialization of vital properties+traits
 *
 */
public class VitalMap implements Map<String, Object>, Serializable {

    private static final long serialVersionUID = 1L;
    
    private Map<String, Object> innerMap = null;
    
    public VitalMap() {
        super();
        innerMap = new HashMap<String, Object>();
    }
    
    public VitalMap(Map<String, Object> m) {
        super();
        innerMap = new HashMap<String, Object>(m);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
     
        Map<String, Object> toUpdate = null;
        
        for(Entry<String, Object> e : this.entrySet()) {
            
            Object v = e.getValue();
            
            if(v instanceof PropertyTrait) {
                
                PropertyTrait trait = (PropertyTrait) v;
                
                IProperty prop = (IProperty) v;
                
                IProperty unwrapped = prop.unwrapped();
                
                SerializedProperty sp = new SerializedProperty();
                
                sp.setBaseClass(prop.unwrapped().getClass());
                sp.setPropertyURI(trait.getURI());
                sp.setRawValue(unwrapped.rawValue());
                
                if(toUpdate == null) toUpdate = new HashMap<String, Object>();
                
                toUpdate.put(e.getKey(), sp);
                
            }
        }
        

        if(toUpdate != null) {
            this.putAll(toUpdate);
        }
        
        out.defaultWriteObject();
        
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        
        in.defaultReadObject();
        
        Map<String, Object> toUpdate = null;
        
        for(Entry<String, Object> e : this.entrySet()) {

            Object v = e.getValue();
            if(v instanceof SerializedProperty) {
                
                SerializedProperty sp = (SerializedProperty) v;
                
                if(toUpdate == null) toUpdate = new HashMap<String, Object>();
                
                PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(sp.getPropertyURI());

                if(pm == null) throw new RuntimeException("Property not found: " + sp.getPropertyURI());
                
                IProperty propWTrait = PropertyFactory.createPropertyWithTraitFromRawValue(sp.getBaseClass(), pm.getTraitClass(), sp.getRawValue());
                
                toUpdate.put(e.getKey(), propWTrait);
            }
            
        }
        
        if(toUpdate != null) {
            this.putAll(toUpdate);
        }
        
    }

    @Override
    public void clear() {
        innerMap.clear();
    }

    @Override
    public boolean containsKey(Object arg0) {
        return innerMap.containsKey(arg0);
    }

    @Override
    public boolean containsValue(Object arg0) {
        return innerMap.containsKey(arg0);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return innerMap.entrySet();
    }

    @Override
    public Object get(Object arg0) {
        return innerMap.get(arg0);
    }

    @Override
    public boolean isEmpty() {
        return innerMap.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return innerMap.keySet();
    }

    @Override
    public Object put(String arg0, Object arg1) {
        return innerMap.put(arg0, arg1);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> arg0) {
        innerMap.putAll(arg0);
    }

    @Override
    public Object remove(Object arg0) {
        return innerMap.remove(arg0);
    }

    @Override
    public int size() {
        return innerMap.size();
    }

    @Override
    public Collection<Object> values() {
        return innerMap.values();
    }

    @Override
    public String toString() {
        return innerMap.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return innerMap.equals(obj);
    }

    @Override
    public int hashCode() {
        return innerMap.hashCode();
    }
    
}
