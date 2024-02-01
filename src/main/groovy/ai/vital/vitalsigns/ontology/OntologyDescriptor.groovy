package ai.vital.vitalsigns.ontology;

import java.io.InputStream;

/**
 * The interface that needs to be implemented in order to automatically register the ontology in VitalSigns singleton.
 */

public interface OntologyDescriptor {

	/**
	 * @return owl file input stream
	 */
	public InputStream getOwlInputStream();
	
	/**
	 * @return domain ontology IRI
	 */
	public String getOntologyIRI();
	
	/**
	 * @return package name, without '.'
	 */
	public String getPackage();
	
	
}
