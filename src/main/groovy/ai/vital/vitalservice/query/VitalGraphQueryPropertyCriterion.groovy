package ai.vital.vitalservice.query

import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.query.Provides;
import ai.vital.vitalservice.query.VitalGraphValueCriterion.Comparator;
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.meta.AnnotationsImplementation;
import ai.vital.vitalsigns.meta.DomainPropertyAnnotation;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.properties.PropertyMetadata

//copied from vitalpropertyconstraint
public class VitalGraphQueryPropertyCriterion implements VitalGraphQueryElement {

	private static final long serialVersionUID = 2012L;
	
	public static enum Comparator {
		EQ,
		EQ_CASE_INSENSITIVE,
		NE,
		GT,
		GE,
		LT,
		LE,
		CONTAINS_CASE_INSENSITIVE,
		CONTAINS_CASE_SENSITIVE,
		//case insensitive
		REGEXP,
		REGEXP_CASE_SENSITIVE,
		
		//additional comparators/operators that are translated into boolean containers
		//also the value must be a list of values
		ONE_OF,
		NONE_OF,
		
		EXISTS,
		NOT_EXISTS,
		
		//for multivalue 
		CONTAINS,
		NOT_CONTAINS;
		
		public static Comparator fromString( String s ) {

			if(s.equalsIgnoreCase("EQ") || s.equals("==") || s.equals("=")){
				return EQ
			} else if(s.equalsIgnoreCase("EQ_CASE_INSENSITIVE") || s.equalsIgnoreCase("EQ-CASE-INSENSITIVE") || s.equalsIgnoreCase("EQCASEINSENSITIVE")  || s.equalsIgnoreCase("eq_i") || s.equalsIgnoreCase("eqi")) {
				return EQ_CASE_INSENSITIVE
			} else if(s.equalsIgnoreCase("contains") || s.equalsIgnoreCase('CONTAINS_CASE_SENSITIVE') || s.equalsIgnoreCase('CONTAINS-CASE-SENSITIVE') || s.equalsIgnoreCase('CONTAINSCASESENSITIVE')) {
				return CONTAINS_CASE_SENSITIVE
			} else if(s.equalsIgnoreCase("contains_i") || s.equalsIgnoreCase("containsi") || s.equalsIgnoreCase('CONTAINS_CASE_INSENSITIVE') || s.equalsIgnoreCase('CONTAINS-CASE-INSENSITIVE') || s.equalsIgnoreCase('CONTAINSCASEINSENSITIVE')) {
				return CONTAINS_CASE_INSENSITIVE
			} else if(s.equalsIgnoreCase("NE") || s.equals("!=")) {
				return NE
			} else if(s.equalsIgnoreCase("GT") || s.equals(">")) {
				return GT
			} else if(s.equalsIgnoreCase("GE") || s.equals(">=")) {
				return GE
			} else if(s.equalsIgnoreCase("LT") || s.equals("<")) {
				return LT
			} else if(s.equalsIgnoreCase("LE") || s.equals("<=")) {
				return LE
			} else if(s.equalsIgnoreCase("oneof") || s.equalsIgnoreCase("ONE-OF") || s.equalsIgnoreCase("ONE_OF")) {
				return ONE_OF
			} else if(s.equalsIgnoreCase("noneof") || s.equalsIgnoreCase("NONE-OF") || s.equalsIgnoreCase("NONE_OF")) {
				return NONE_OF
			} else if(s.equalsIgnoreCase("exists")) {
				return EXISTS
			} else if(s.equalsIgnoreCase("not_exists") || s.equalsIgnoreCase("not-exists") || s.equalsIgnoreCase("notexists")) {
				return NOT_EXISTS
			} else if(s.equalsIgnoreCase("contains-value") || s.equalsIgnoreCase("contains_value") || s.equalsIgnoreCase("containsvalue")) {
				return CONTAINS
			} else if(s.equalsIgnoreCase("not-contains") || s.equalsIgnoreCase("not_contains") || s.equalsIgnoreCase("notcontains") ||
                s.equalsIgnoreCase("not-contains-value") || s.equalsIgnoreCase("not_contains_value") || s.equalsIgnoreCase("notcontainsvalue")
            ) {
				return NOT_CONTAINS
			} else {
				throw new RuntimeException("Unknown comparator: ${s}")
			}
		}
	}

	
	String propertyURI;
	
	//keep unwrapped property for external property type check 
	private IProperty unwrappedProperty;
	
	private transient IProperty propertyWithTrait
	
	//raw value or base vital type ?
	Object value;
	
	boolean negative = false;
	
	Comparator comparator = Comparator.EQ;
	
	GraphElement symbol;
	
	boolean expandProperty = false
	
	boolean externalProperty = false
	
	public VitalGraphQueryPropertyCriterion(GraphElement symbol, IProperty property, Object value) {
		this(symbol, property, value, Comparator.EQ, false);
	}
	
	public VitalGraphQueryPropertyCriterion(GraphElement symbol, IProperty property, Object value, Comparator comparator) {
		this(symbol, property, value, comparator, false);
	}
	
	public final static String URI = 'URI'
	
    public VitalGraphQueryPropertyCriterion() {
        
    }
    
	public VitalGraphQueryPropertyCriterion(String propertyURI) {
		this.propertyURI = propertyURI 
	}

	public VitalGraphQueryPropertyCriterion(GraphElement symbol, IProperty property, Object value, Comparator comparator, boolean negative) {
		this.unwrappedProperty = property.unwrapped();
		this.propertyURI = property.getURI();
		if(propertyURI == null) throw new RuntimeException("Property object does not provide URI");
		this.propertyWithTrait = property
		
		this.value = value;
		if(comparator == Comparator.NE) {
			comparator = Comparator.EQ
			negative = !negative
		}
		this.comparator = comparator;
		this.negative = negative;
		this.symbol = symbol;
	
	}
	
	protected Object uriFilter(Object val) {
		
		if(val instanceof String || val instanceof GString) {
			String s = val.toString()
			if( s.startsWith("<") && s.endsWith(">") && s.split("\\s+").length == 1 && s.contains(":") ) {
				val = new URIProperty(s.substring(1, s.length() - 1))
			}
		}
		return val
		
	}
	
	public VitalGraphQueryPropertyCriterion equalTo(Object val) {
		this.value = uriFilter(val)
		this.comparator = Comparator.EQ
		return this
	}
	
	public VitalGraphQueryPropertyCriterion equalTo_i(Object val) {
		this.value = uriFilter(val)
		this.comparator = Comparator.EQ_CASE_INSENSITIVE
		return this
	}
	
	public VitalGraphQueryPropertyCriterion notEqualTo(Object val) {
		this.value = uriFilter(val)
		this.comparator = Comparator.EQ
		this.negative = true
		return this
	}
	
	public VitalGraphQueryPropertyCriterion notEqualTo_i(Object val) {
		this.value = uriFilter(val)
		this.comparator = Comparator.EQ_CASE_INSENSITIVE
		this.negative = true
		return this
	}

	public VitalGraphQueryPropertyCriterion greaterThan(Object val) {
		this.value = val
		this.comparator = Comparator.GT
		return this
	}

	public VitalGraphQueryPropertyCriterion greaterThanEqualTo(Object val) {
		this.value = val
		this.comparator = Comparator.GE
		return this
	}

	public VitalGraphQueryPropertyCriterion lessThan(Object val) {
		this.value = val
		this.comparator = Comparator.LT
		return this
	}

	public VitalGraphQueryPropertyCriterion lessThanEqualTo(Object val) {
		this.value = val
		this.comparator = Comparator.LE
		return this
	}
	
	public VitalGraphQueryPropertyCriterion contains(Object val) {
		this.value = val
		this.comparator = Comparator.CONTAINS_CASE_SENSITIVE
		return this
	}
	
	public VitalGraphQueryPropertyCriterion contains_i(Object val) {
		this.value = val
		this.comparator = Comparator.CONTAINS_CASE_INSENSITIVE
		return this
	}
	
	public VitalGraphQueryPropertyCriterion regexp(Object val) {
		this.value = val
		this.comparator = Comparator.REGEXP_CASE_SENSITIVE
		return this
	}
	
	public VitalGraphQueryPropertyCriterion regexp_i(Object val) {
		this.value = val
		this.comparator = Comparator.REGEXP
		return this
	}
	
	public VitalGraphQueryPropertyCriterion expandSubproperties(boolean flag) {
		this.expandProperty = flag
		return this
	}
	
	public VitalGraphQueryPropertyCriterion oneOf(List values) {
		List filtered = []
		for(Object v : values) {
			filtered.add(uriFilter(v))
		}
		this.value = filtered
		this.comparator = Comparator.ONE_OF
		return this
	}
	
	public VitalGraphQueryPropertyCriterion noneOf(List values) {
		List filtered = []
		for(Object v : values) {
			filtered.add(uriFilter(v))
		}
		this.value = filtered
		this.comparator = Comparator.NONE_OF
		return this

	}
	
	
	public VitalGraphQueryPropertyCriterion exists() {
		this.comparator = Comparator.EXISTS
		return this
	}
	
	public VitalGraphQueryPropertyCriterion notExists() {
		this.comparator = Comparator.NOT_EXISTS
		return this
	}
	
	public VitalGraphQueryPropertyCriterion value(Object val) {
		this.value = val
		return this
	}
	
	public VitalGraphQueryPropertyCriterion containsValue(Object val) {
		this.value = val;
		this.comparator = Comparator.CONTAINS
		return this;
	}
	
	public VitalGraphQueryPropertyCriterion notContainsValue(Object val) {
		this.value = val;
		this.comparator = Comparator.NOT_CONTAINS
		return this;
	}
	
	public Provides provides(String alias) {
		Provides provides = new Provides()
		provides.property = this.propertyWithTrait
		provides.alias = alias
		return provides
	}
	
	public String getPropertyURI() {
		return propertyURI;
	}

	public VitalGraphQueryElement negated() {
		this.negative = true;
		return this;
	}
	
	public VitalGraphQueryElement nonNegated() {
		this.negative = false;
		return this;
	}
    
    @Override
    public String toString() {
        return "${this.class.simpleName} ${this.comparator} negative? ${negative} symbol: ${symbol} expand ? ${this.expandProperty} propertyURI: ${propertyURI} external: ${externalProperty} value: ${this.value}"
    }

    
    public Map<String, List<DomainPropertyAnnotation>> getAnnotations(boolean subproperties) {
        if(this.propertyWithTrait == null) throw new Exception("propertyWithTrait not set, to be used only with properties helper")
        PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(this.propertyWithTrait.getURI())
        if(pm == null) throw new Exception("Property not found: " + this.propertyWithTrait.getURI());
        return AnnotationsImplementation.getPropertyAnnotations(pm.getTraitClass(), subproperties)
    }
    
    public Map<String, List<DomainPropertyAnnotation>> getAnnotations() {
        return getAnnotations(true)
    }
    
}
