package ai.vital.query.ops

import ai.vital.vitalsigns.model.GraphObject;
import groovy.lang.Closure;

class UpgradeDowngradeDefBase {

    Class<? extends GraphObject> oldClass
    
    Class<? extends GraphObject> newClass
    
    Closure closure
    
}
