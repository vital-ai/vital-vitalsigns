package ai.vital.vitalservice.query

import java.util.List;

/**
 * Unlike {@link ai.vital.vitalservice.query.VitalSelectQuery VitalSelectQuery} this query is targeted at
 * external named databases  
 *
 */
class VitalExternalSqlQuery extends VitalQuery {

    private static final long serialVersionUID = 1L;
    
    /**
     * database name
     */
    String database
    
    /**
     * sql query string 
     */
    String sql
    
    
    @Override
    public List<String> toSparql(Object... args) {
        throw new RuntimeException("SQL does not return sparql string")
    }
    
}
