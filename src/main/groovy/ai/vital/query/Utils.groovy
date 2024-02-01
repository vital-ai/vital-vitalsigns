package ai.vital.query

import ai.vital.query.querybuilder.BaseConstraintFactory;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.properties.PropertyTrait

class Utils {

	
	public static VitalGraphQueryPropertyCriterion eval(String s) {
		
		return BaseConstraintFactory.evaluate(s);
		
	}
	
	public static VitalGraphQueryPropertyCriterion PropertyConstraint(String propertyName) {
		
		
		if(propertyName.contains(":")) {
			
			return PropertyConstraint(URIProperty.withString(propertyName))
			
		} else {
		
			IProperty p = VitalSigns.get().getProperty(propertyName);
			
			if(p == null) throw new RuntimeException("Property with name not found or ambiguous: ${propertyName}")
			
			return new VitalGraphQueryPropertyCriterion(null, p, null)
			
		}
		
		
	}
	
	public static VitalGraphQueryPropertyCriterion PropertyConstraint(URIProperty propertyURI) {
		
		IProperty p = VitalSigns.get().getProperty(propertyURI)
		
		if(p == null) throw new RuntimeException("Property with URI ${propertyURI?.get()} not found")
		
		return new VitalGraphQueryPropertyCriterion(null, p, null)
		
	}
	
	public static VitalGraphQueryPropertyCriterion PropertyConstraint(Class<? extends PropertyTrait> cls) {
		
		IProperty p = VitalSigns.get().getPropertyByTrait(cls)
		
		if(p == null) throw new RuntimeException("Property for trait class not found: ${cls?.canonicalName}")
		
		return new VitalGraphQueryPropertyCriterion(null, p, null)
		
	}
	
    
    public static Provides provides(String providedName, String propertyURI) {
        
        if(!providedName) throw new NullPointerException("Null or empty providedName")
        if(!propertyURI) throw new Exception("Null or empty propretyURI")
        
        Provides provides = new Provides()
        provides.alias = providedName
        provides.propertyURI = propertyURI
        
        return provides
        
    }
	

	
}
