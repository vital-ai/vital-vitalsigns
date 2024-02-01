package ai.vital.query

import java.util.List;

class PATH extends BaseQuery {

	//strings or uri properties allowed
	List rootURIs = null
	
    boolean countRoot = true
    
	ARC root
	
	//arcs only?
	List<ARC> arcs = []

	//either integer or '*'
	Object maxdepth
    
    //when projection == true it will traverse the entire tree counting all nodes
    //projection false with skip/limit enabled wouldn't return totalResults
    boolean projection = false
	
}
