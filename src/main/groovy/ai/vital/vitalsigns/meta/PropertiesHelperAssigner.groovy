package ai.vital.vitalsigns.meta

import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.query.Provides;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion.Comparator;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.properties.PropertyTrait;

class PropertiesHelperAssigner {

	public static void assignPropertiesHelper() {
		
		GraphObject.metaClass.static."props" = { ->
			
			def del = delegate
			
			return VitalSigns.get().getPropertiesHelper(del);
			
		}

		IProperty.metaClass.static.'instance' = { String name ->
			IProperty p = VitalSigns.get().getProperty(name)
			if(p == null) throw new RuntimeException("Property with name not found or ambiguous: ${name}")
			return new VitalGraphQueryPropertyCriterion(null, p, null)
		}
		
		PropertyTrait.metaClass.static.'instance' = { String name ->
			IProperty p = VitalSigns.get().getProperty(name)
			if(p == null) throw new RuntimeException("Property with name not found or ambiguous: ${name}")
			return new VitalGraphQueryPropertyCriterion(null, p, null)
		}
		
		/*
		PropertyTrait.metaClass.static.'provides' = { String alias ->
			def del = delegate
			IProperty p = VitalSigns.get().getPropertyByTrait(del)
			if(p == null) throw new RuntimeException("Property not found in VitalSigns, trait class: " + del);
			Provides provides = new Provides()
			provides.alias = alias
			provides.property = p
			return provides
		}
		*/

		/* this collides with IProperty.* object methods!
		PropertyTrait.metaClass.static.'equalTo' = { Object val ->
			return PropertiesHelperImplemenation.constraintImpl(delegate, val, Comparator.EQ)
		}
		
		PropertyTrait.metaClass.static.'notEqualTo' = { Object val ->
			return PropertiesHelperImplemenation.constraintImpl(delegate, val, Comparator.NE)
		}
		
		PropertyTrait.metaClass.static.'greaterThan' = { Object val ->
			return PropertiesHelperImplemenation.constraintImpl(delegate, val, Comparator.GT)
		}
		
		PropertyTrait.metaClass.static.'greaterThanEqualTo' = { Object val ->
			return PropertiesHelperImplemenation.constraintImpl(delegate, val, Comparator.GE)
		}
		
		PropertyTrait.metaClass.static.'lessThan' = { Object val ->
			return PropertiesHelperImplemenation.constraintImpl(delegate, val, Comparator.LT)
		}
		
		PropertyTrait.metaClass.static.'lessThanEqualTo' = { Object val ->
			return PropertiesHelperImplemenation.constraintImpl(delegate, val, Comparator.LE)
		}
		
		*/
		
		
	}
	

	
}
