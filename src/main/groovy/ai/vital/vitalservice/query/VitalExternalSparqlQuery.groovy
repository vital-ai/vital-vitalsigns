package ai.vital.vitalservice.query

import java.util.List;

/**
 * Unlike {@link ai.vital.vitalservice.query.VitalSelectQuery VitalSelectQuery} this query is targeted at
 * external named databases  
 *
 */
class VitalExternalSparqlQuery extends VitalQuery {

    private static final long serialVersionUID = 1L;
    
    /**
     * database name
     */
    String database
    
    /**
     * sparql query string 
     */
    String sparql
    
    
    @Override
    public List<String> toSparql(Object... args) {
        //short-circuit method - not triplestore roundtrips etc.
        return [sparql]
    }
    
}
