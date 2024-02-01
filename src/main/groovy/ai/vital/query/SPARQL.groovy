package ai.vital.query

import java.util.List;

/**
 * SPARQL is converted into either VitalSparqlQuery or VitalExternalSparqlQuery based on segments / database field
 *
 */
class SPARQL {

    List segments = null
    
    String database = null

    String sparql = null
    
    Integer timeout = null
    
}
