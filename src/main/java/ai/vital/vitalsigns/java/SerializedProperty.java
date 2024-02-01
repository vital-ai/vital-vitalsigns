package ai.vital.vitalsigns.java;

import java.io.Serializable;

import ai.vital.vitalsigns.model.property.IProperty;

public class SerializedProperty implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private Class<? extends IProperty> baseClass;
    
    private Object rawValue;

    private String propertyURI;
    
    public Class<? extends IProperty> getBaseClass() {
        return baseClass;
    }

    public void setBaseClass(Class<? extends IProperty> baseClass) {
        this.baseClass = baseClass;
    }

    public Object getRawValue() {
        return rawValue;
    }

    public void setRawValue(Object rawValue) {
        this.rawValue = rawValue;
    }

    public String getPropertyURI() {
        return propertyURI;
    }

    public void setPropertyURI(String propertyURI) {
        this.propertyURI = propertyURI;
    }

    
}
