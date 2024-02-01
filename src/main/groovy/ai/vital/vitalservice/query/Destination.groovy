package ai.vital.vitalservice.query

public enum Destination {

	EMPTY, 
	CURRENT,
	PARENT_SOURCE,
	PARENT_CONNECTOR,
	PARENT_DESTINATION

	public static Destination fromString(String s) {
		if(s == null) throw new NullPointerException("Destination enum input string mustn't be null")
		if(s.equals('')) return EMPTY
		return valueOf(s.toUpperCase())
	}

}

