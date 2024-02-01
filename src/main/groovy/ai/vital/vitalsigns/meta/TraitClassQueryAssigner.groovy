package ai.vital.vitalsigns.meta

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.vitalsigns.properties.PropertyTrait

class TraitClassQueryAssigner {

    public static void assignQueryObjectGetter(Class<? extends PropertyTrait> traitClass) {
        
        traitClass.metaClass.static.'getQuery' = {
            
            IProperty p = VitalSigns.get().getPropertyByTrait( delegate )

            if(p == null) throw new RuntimeException("Property object for trait class: ${traitClass.canonicalName} not found")
                            
            return new VitalGraphQueryPropertyCriterion(null, p, null);
        }
            
    }
    
}
