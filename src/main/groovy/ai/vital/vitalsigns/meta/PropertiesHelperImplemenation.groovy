package ai.vital.vitalsigns.meta;

import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion.Comparator;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.properties.PropertyTrait;

public class PropertiesHelperImplemenation {

	public static VitalGraphQueryPropertyCriterion constraintImpl(Class<? extends PropertyTrait> del, Object val, Comparator c) {
		
		IProperty property = VitalSigns.get().getPropertyByTrait(del);
		
		if(property == null) throw new RuntimeException("Property not found in VitalSigns, trait class: " + del);
		
		return new VitalGraphQueryPropertyCriterion(null, property, val, c);
		
	}
	
}
