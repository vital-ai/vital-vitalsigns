package ai.vital.vitalsigns.model;

import java.io.Serializable;
import groovy.lang.GroovyObjectSupport;

public class GraphObject extends GroovyObjectSupport implements Serializable, Cloneable {

    @Override
    public void setProperty(String propertyName, Object newValue) {


        System.out.println("PropertyName: " + propertyName);

        System.out.println("PropertyValue: " + newValue);



    }

    @Override
    public Object getProperty(String propertyName) {


        return null;

    }

        @Override
    public GraphObject clone() {
        try {
            return (GraphObject) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
