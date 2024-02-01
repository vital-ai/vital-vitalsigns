package ai.vital.vitalservice.query

public enum Connector {
	EMPTY, 
	EDGE,
	HYPEREDGE
	
	public static Connector fromString(String s) {
		
		if(s == null) throw new NullPointerException("Connector enum input string mustn't be null")
		if(s.equals('')) return EMPTY
		return valueOf(s.toUpperCase())
		
	}
}

