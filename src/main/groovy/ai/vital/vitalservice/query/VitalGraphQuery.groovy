package ai.vital.vitalservice.query

class VitalGraphQuery extends VitalQuery {
	
	private static final long serialVersionUID = 1985L;
	
	VitalGraphArcContainer topContainer
	
	int offset = 0;
	
	int limit = 0;

	//named properties used in sorting
	List<VitalSortProperty> sortProperties = []
	
	boolean payloads = false
    
    /**
     * Returns total number of results, false by default
     */
    boolean projectionOnly = false

    /**
     * Includes total number of results, false by default
     */
    boolean includeTotalCount = false
    
    /**
     * sort style. Default inOrder
     */
    SortStyle sortStyle = SortStyle.inOrder
    
}
