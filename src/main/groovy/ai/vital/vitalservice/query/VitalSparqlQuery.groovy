package ai.vital.vitalservice.query

import java.util.List;

/**
 * Arbitrary sparql query. Only some endpoints support sparql.
 * 
 *
 */
class VitalSparqlQuery extends VitalQuery {

	private static final long serialVersionUID = 909090909090909090L;
	
	String sparql
	
	@Override
	public List<String> toSparql(Object... args) {
		//short-circuit method - not triplestore roundtrips etc.
		return [sparql]
	}
	
}