package ai.vital.vitalservice

enum EndpointType {
	
	MOCK("Mock"),
	ALLEGROGRAPH("Allegrograph"),
	LUCENEDISK("LuceneDisk"),
	LUCENEMEMORY("LuceneMemory"),
	INDEXDB("IndexDB"),
	VITALPRIME("VitalPrime"),
    SQL("SQL")
	
	
	private final String name 
	
	EndpointType(String name) { this.name = name }
	
	public getName() { return name }
	
	public static EndpointType fromString(String val) {
		
		for(EndpointType et : values()) {
			
			if(et.name.equalsIgnoreCase(val)) return et
			
		}
		
		throw new RuntimeException("Unknown endpoint type: ${val}")
		
	}
	
}