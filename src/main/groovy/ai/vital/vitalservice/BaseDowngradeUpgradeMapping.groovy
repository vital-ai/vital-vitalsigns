package ai.vital.vitalservice

import ai.vital.vitalsigns.model.GraphObject
import groovy.lang.Closure;

abstract class BaseDowngradeUpgradeMapping implements Serializable {

    private static final long serialVersionUID = 1L;
    
    Class<? extends GraphObject> oldClass
    
    Class<? extends GraphObject> newClass
    
    Closure closure
    
}
