package ai.vital.vitalservice.query

import java.util.List;

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion.Comparator;
import ai.vital.vitalsigns.model.property.URIProperty;

/**
 * GraphObject URI property helper class
 *
 */
class URI {

	protected static Object uriFilter(Object val) {
		
		if(val instanceof String || val instanceof GString) {
			String s = val.toString()
			if( s.startsWith("<") && s.endsWith(">") && s.split("\\s+").length == 1 && s.contains(":") ) {
				val = new URIProperty(s.substring(1, s.length() - 1))
			}
			val = new URIProperty(val)
		}
		return val
		
	}
	
	public static VitalGraphQueryPropertyCriterion equalTo(Object val) {
		VitalGraphQueryPropertyCriterion c = new VitalGraphQueryPropertyCriterion(VitalGraphQueryPropertyCriterion.URI) 
		c.value = uriFilter(val)
		c.comparator = Comparator.EQ
		return c
	}
	
	public static VitalGraphQueryPropertyCriterion notEqualTo(Object val) {
		VitalGraphQueryPropertyCriterion c = new VitalGraphQueryPropertyCriterion(VitalGraphQueryPropertyCriterion.URI)
		c.value = uriFilter(val)
		c.comparator = Comparator.EQ
		c.negative = true
		return c
	}

	public static VitalGraphQueryPropertyCriterion oneOf(List values) {
		VitalGraphQueryPropertyCriterion c = new VitalGraphQueryPropertyCriterion(VitalGraphQueryPropertyCriterion.URI)
		List filtered = []
		for(Object v : values) {
			filtered.add(uriFilter(v))
		}
		c.value = filtered
		c.comparator = Comparator.ONE_OF
		return c
	}
	
	public static VitalGraphQueryPropertyCriterion noneOf(List values) {
		VitalGraphQueryPropertyCriterion c = new VitalGraphQueryPropertyCriterion(VitalGraphQueryPropertyCriterion.URI)
		List filtered = []
		for(Object v : values) {
			filtered.add(uriFilter(v))
		}
		c.value = filtered
		c.comparator = Comparator.NONE_OF
		return c

	}
	
}
