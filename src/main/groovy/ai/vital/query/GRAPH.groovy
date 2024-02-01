package ai.vital.query

class GRAPH extends BaseQuery {
	
	boolean inlineObjects = false
	
	ARC_BASE topArc
    
	boolean projection = false
    
    boolean includeTotalCount = false
    
    //either String or SortStyle enum
    Object sortStyle = null

}
