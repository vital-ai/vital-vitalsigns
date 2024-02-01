package ai.vital.query

import java.util.List;

abstract class BaseQuery extends Container {

	int limit = -1
	
	int offset = 0

	List segments = []
	
	boolean returnSparqlString = false

    String blockOnTransactionID = null
    
	List sortProperties = []
	
    String collectStats = null
    
    Integer timeout = null
    
}
