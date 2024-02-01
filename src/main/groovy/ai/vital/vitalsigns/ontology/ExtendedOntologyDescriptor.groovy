package ai.vital.vitalsigns.ontology;

/**
 * An interface that provides additional information about ontology (domain)
 *
 */
public interface ExtendedOntologyDescriptor extends OntologyDescriptor {

    /**
     * 
     * @return
     */
    public String getVitalSignsVersion();
}
