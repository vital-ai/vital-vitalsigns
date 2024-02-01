package ai.vital.vitalservice.query

/**
 * "provides" criterion
 *
 */
class VitalGraphValueCriterion implements Serializable {

	private static final long serialVersionUID = 2000L;
	
	//mutually exclusive
	private String name1
	
	private VitalGraphValue value1
	
	
	Comparator comparator
	
	
	//mutually exclusive
	private String name2
	
	private VitalGraphValue value2
	
	public static enum Comparator {
		EQ,
		NE,
		GT,
		GE,
		LT,
		LE
		
		
		public static Comparator fromString( String s ) {
			
			if(s.equalsIgnoreCase("EQ") || s.equals("==")){
				return EQ
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
			} else {
				throw new RuntimeException("Unparseable comparator string: " + s)
			}
			
		}
		
		
	}

    VitalGraphValueCriterion() { }
    	
	VitalGraphValueCriterion(String name1, Comparator comparator, String name2) {
		if(name1 == null) throw new NullPointerException("provides name1 cannot be null")
		if(comparator == null) throw new NullPointerException("comparator cannot be null")
		if(name2 == null) throw new NullPointerException("provides name2 cannot be null")

		this.name1 = name1	
		this.comparator = comparator
		this.name2 = name2	
	}
	
	VitalGraphValueCriterion(String name1, Comparator comparator, VitalGraphValue value2) {
		if(name1 == null) throw new NullPointerException("provides name1 cannot be null")
		if(comparator == null) throw new NullPointerException("comparator cannot be null")
		if(value2 == null) throw new NullPointerException("value2 cannot be null")
		
		this.name1 = name1	
		this.comparator = comparator
		this.value2 = value2	
	}
	
	VitalGraphValueCriterion(VitalGraphValue value1, Comparator comparator, VitalGraphValue value2) {
		if(value1 == null) throw new NullPointerException("value1 cannot be null")
		if(comparator == null) throw new NullPointerException("comparator cannot be null")
		if(value2 == null) throw new NullPointerException("value2 cannot be null")
		
		this.value1 = value1	
		this.comparator = comparator
		this.value2 = value2	
	}
	
	VitalGraphValueCriterion(VitalGraphValue value1, Comparator comparator, String name2) {
		
		if(value1 == null) throw new NullPointerException("value1 cannot be null")
		if(comparator == null) throw new NullPointerException("comparator cannot be null")
		if(name2 == null) throw new NullPointerException("provides name2 cannot be null")
		
		this.value1 = value1	
		this.comparator = comparator
		this.name2 = name2	
	}
	
	public String getName1() {
		return name1;
	}

	public VitalGraphValue getValue1() {
		return value1;
	}

	public Comparator getComparator() {
		return comparator;
	}

	public String getName2() {
		return name2;
	}

	public VitalGraphValue getValue2() {
		return value2;
	}
	
	
}
