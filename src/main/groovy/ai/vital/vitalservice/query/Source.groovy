package ai.vital.vitalservice.query

public enum Source {

	EMPTY, 
	CURRENT,
	PARENT_SOURCE,
	PARENT_CONNECTOR,
	PARENT_DESTINATION
	
	
	public static Source fromString(String s) {
		if(s == null) throw new NullPointerException("Source enum input string mustn't be null")
		if(s.equals('')) return EMPTY
		return valueOf(s.toUpperCase())
	}
	
}

