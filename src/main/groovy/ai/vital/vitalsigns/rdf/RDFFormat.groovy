package ai.vital.vitalsigns.rdf;

/**
 * RDF format types enum
 *
 */
public enum RDFFormat {

	// "RDF/XML", "N-TRIPLE", "TURTLE" (or "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for "RDF/XML". 

	RDF_XML("RDF/XML"),
	N_TRIPLE("N-TRIPLE"),
	TURTLE("TURTLE"),
	N3("N3");
	
	private String jenaTypeString;
		
	private RDFFormat(String jenaTypeString) {
		this.jenaTypeString = jenaTypeString;
	}
	
	public String toJenaTypeString() {
		return jenaTypeString;
	}
	
	
}
