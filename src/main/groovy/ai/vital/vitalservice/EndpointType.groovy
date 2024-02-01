package ai.vital.vitalservice

enum EndpointType {
	
	MOCK("Mock"),
	VITALSAAS("VitalSaaS"),
	ALLEGROGRAPH("Allegrograph"),
	LUCENEDISK("LuceneDisk"),
	LUCENEMEMORY("LuceneMemory"),
	DYNAMODB("DynamoDB"),
	INDEXDB("IndexDB"),
	VITALPRIME("VitalPrime"),
	VITALSPARK("VitalSpark"),
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