package ai.vital.query.querybuilder

import java.util.regex.Matcher
import java.util.regex.Pattern;

import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.query.BaseConstraint;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion.Comparator;
import ai.vital.vitalservice.query.VitalGraphQueryTypeCriterion
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.property.BooleanProperty;
import ai.vital.vitalsigns.model.property.DateProperty;
import ai.vital.vitalsigns.model.property.DoubleProperty;
import ai.vital.vitalsigns.model.property.FloatProperty;
import ai.vital.vitalsigns.model.property.GeoLocationProperty;
import ai.vital.vitalsigns.model.property.IntegerProperty;
import ai.vital.vitalsigns.model.property.LongProperty;
import ai.vital.vitalsigns.model.property.StringProperty
import ai.vital.vitalsigns.model.property.TruthProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

abstract class BaseConstraintFactory extends AbstractFactory {

	static JsonSlurper jsonSlurper = new JsonSlurper()
	
	public boolean isHandlesNodeChildren() {
		return true
	}

	public boolean isLeaf() {
		return false
	}

	public void onNodeCompleted(FactoryBuilderSupport builder,
			Object parent, Object c) {
		//invoice.save()
	}
			
	abstract protected Class<? extends GraphObject> getClazz();

	abstract protected String getName();
	
	static Pattern pattern = Pattern.compile("\\s+")
	
	static Map<Class, List> propertyType2Comparators = [:]
	
	
	static Pattern uriValuePattern = Pattern.compile("^<[^<^>]+>\$")
	
	static Pattern uriValuePattern2 = Pattern.compile("^[^\\s]+\$")
	
	
	static {
		propertyType2Comparators.put(BooleanProperty.class, [Comparator.EQ, Comparator.NE])
		propertyType2Comparators.put(DateProperty.class, [Comparator.EQ, Comparator.NE, Comparator.GE, Comparator.GT, Comparator.LE, Comparator.LT])
		propertyType2Comparators.put(DoubleProperty.class, [Comparator.EQ, Comparator.NE, Comparator.GE, Comparator.GT, Comparator.LE, Comparator.LT])
		propertyType2Comparators.put(FloatProperty.class, [Comparator.EQ, Comparator.NE, Comparator.GE, Comparator.GT, Comparator.LE, Comparator.LT])
		propertyType2Comparators.put(GeoLocationProperty.class, [Comparator.EQ, Comparator.NE, Comparator.GE, Comparator.GT, Comparator.LE, Comparator.LT])
		propertyType2Comparators.put(IntegerProperty.class, [Comparator.EQ, Comparator.NE, Comparator.GE, Comparator.GT, Comparator.LE, Comparator.LT])
		propertyType2Comparators.put(LongProperty.class, [Comparator.EQ, Comparator.NE, Comparator.GE, Comparator.GT, Comparator.LE, Comparator.LT])
		propertyType2Comparators.put(StringProperty.class, [Comparator.CONTAINS_CASE_INSENSITIVE, Comparator.CONTAINS_CASE_SENSITIVE, Comparator.EQ, Comparator.EQ_CASE_INSENSITIVE,
			 Comparator.REGEXP, Comparator.REGEXP_CASE_SENSITIVE, Comparator.NE, Comparator.ONE_OF, Comparator.NONE_OF])
		propertyType2Comparators.put(TruthProperty.class, [Comparator.EQ, Comparator.NE])
		propertyType2Comparators.put(URIProperty.class, [Comparator.EQ, Comparator.NE, Comparator.ONE_OF, Comparator.NONE_OF])
	} 
	
	public static VitalGraphQueryPropertyCriterion evaluate(String e) {
		
		e = e.trim()
		
//		String[] columns = e.trim().split("\\s+")
//		if(columns.length < 3) ee("Expected at least 3 columns", e)

		Matcher matcher = pattern.matcher(e)
		
		int col = 0;
		String property = null;
		String comparator = null;
		String vsx = null;
		
		int lastEnd = 0
		
		while(matcher.find()) {
			
			if(col == 0) {
				
				property = e.substring(lastEnd, matcher.start())
				
			} else if(col == 1) {
			
				comparator = e.substring(lastEnd, matcher.start())
			
			}
			
			lastEnd = matcher.end();
			
			col++;
			
			if(col == 2) {
				break
			}
			
		}
		
		if(col < 2) ee("Expected at least 3 columns", e);		
		
		vsx = e.substring(lastEnd)

		IProperty propertyObj = null
			
		if(property.contains(":")) {
			propertyObj = VitalSigns.get().getProperty(new URIProperty(property))
			if(propertyObj == null) ee("Property with URI not found: ${property}", e)
		} else {
			if(property.equals(VitalGraphQueryPropertyCriterion.URI)) {
				propertyObj = new URIProperty('')
			} else {
				propertyObj = VitalSigns.get().getProperty(property)
				if(propertyObj == null) ee("Property with name ${property} not found or ambiguous", e)
			}
		}
		
		Comparator c = null
		try {
			c = Comparator.fromString(comparator)
		} catch(Exception ex) {
			ee(ex.localizedMessage, e)
		}
		
		List vals = null

		Object val = null
		
		List inputStrings = [vsx]
		
		IProperty unwrapped = propertyObj.unwrapped()
		
		boolean neg = false;
		if(c == Comparator.NE) {
			c = Comparator.EQ
			neg = true
		} else if(c == Comparator.ONE_OF || c == Comparator.NONE_OF) {
			vals = []
			
			if( unwrapped instanceof URIProperty) {
				//
				vsx = vsx.trim()
				if(! ( vsx.startsWith("[") && vsx.endsWith("]") ) ) ee("URIs list must be enclosed with square brackets", e)
				String[] uris = vsx.substring(1, vsx.length()-1).split(",")
				inputStrings = []
				for(String uri : uris) {
					uri = uri.trim()
					inputStrings.add(uri)
				}
			} else {
			
				Object parsed = null
				try {
					parsed = jsonSlurper.parseText(vsx)
				} catch(Exception ex) {
					ee("Error when parsing json list: ${ex.localizedMessage}", e)
				}
				if(!(parsed instanceof List)) ee("Operator ${comparator} requires a list of strings serialized as json, got object of type: ${parsed.class}", e)
				List l = parsed
				for(Object p : parsed) {
					if( ! (p instanceof String) ) ee("Operator ${comparator} requires a list of strings serialized as json, list has a value of class ${p.class}", e)
				}
				inputStrings = l
			}
			
		}
		
		for(String vs : inputStrings) {
			
			Object v = null;
			
			try {
				
				if(unwrapped instanceof BooleanProperty) {
					v = Boolean.parseBoolean(vs)	
				} else if(unwrapped instanceof DateProperty){
					throw new Exception("TODO: date property string conversion unsupported")
				} else if(unwrapped instanceof DoubleProperty) {
					v = Double.parseDouble(vs)
				} else if(unwrapped instanceof FloatProperty) {
					v = Float.parseFloat(vs)
				} else if(unwrapped instanceof GeoLocationProperty) {
					v = GeoLocationProperty.fromRDFString(vs)
				} else if(unwrapped instanceof IntegerProperty) {
					v = Integer.parseInt(vs)
				} else if(unwrapped instanceof LongProperty) {
					v = Long.parseLong(vs)
				} else if(unwrapped instanceof StringProperty) {
					v = vs
				} else if(unwrapped instanceof TruthProperty) {
                    v = TruthProperty.Truth.fromString(vs)
				} else if(unwrapped instanceof URIProperty) {
					if(uriValuePattern.matcher(vs).matches()) {
						vs = vs.substring(1, vs.length()-1)
					} else if(uriValuePattern2.matcher(vs).matches()) { 
					} else {
						throw new Exception("URIProperty value must match pattern: ${uriValuePattern.pattern()} or ${uriValuePattern2.pattern()} - ${vs}")
					}
					v = new URIProperty(vs)
				} else {
					throw new Exception("unhandled property type: ${unwrapped.class.canonicalName}")
				}
				
				if( vals != null ) {
					vals.add(v)
				} else {
					val = v
				}
				
			} catch(Exception ex) {
				ee("conversion error: " + ex.getLocalizedMessage(), e);
			}
		}
		

		List comps = propertyType2Comparators.get(unwrapped.getClass())
		if(!comps.contains(c)) throw new RuntimeException("Comparator: ${c} cannot be applied to property of type: ${unwrapped.class.simpleName}")
				
		//parse from string?
		if(property.equals(VitalGraphQueryPropertyCriterion.URI)) {
			VitalGraphQueryPropertyCriterion cr = new VitalGraphQueryPropertyCriterion(VitalGraphQueryPropertyCriterion.URI)
			cr.value = vals != null ? vals : val
			cr.comparator = c
			cr.negative = neg
			
			if(cr.value instanceof String || cr.value instanceof GString || cr.value instanceof URIProperty) {
				String sv = cr.value
				if(URIProperty.MATCH_ANY_URI.equals(sv)) {
					cr.setComparator(Comparator.EXISTS)
				}
			}
			
			return cr
		} else {
			return new VitalGraphQueryPropertyCriterion(null, propertyObj, vals != null ? vals : val, c, neg)
		}
		
	}
	
	public static void ee(String ex, String s) {
		throw new RuntimeException("${this.getName()} evaluation exception: ${ex} - source string: ${s}")
	}
	
	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node, Closure childContent) {

		BaseConstraint c = (BaseConstraint) node;

		Object o = childContent.call()

		if(o instanceof java.lang.Class) {

			if(!getClazz().isAssignableFrom(o)) {
				throw new RuntimeException("Expected a subclass of ${getClazz().canonicalName} in ${this.getName()} - ${o.canonicalName}")
			}

			c.criterion = new VitalGraphQueryTypeCriterion(null, o);
			
		} else if(o instanceof GString || o instanceof java.lang.String) {

			//evaluate property now
			c.criterion = evaluate(o.toString())

			//also Type Criterion
		} else if(o instanceof VitalGraphQueryPropertyCriterion ) {

			c.criterion = o;
			
		} else {
		
			throw new RuntimeException("Unexpected ${getName()} value: ${o}")
			
		}
	}
}
