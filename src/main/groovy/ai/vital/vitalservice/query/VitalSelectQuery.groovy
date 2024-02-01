package ai.vital.vitalservice.query

import java.util.List;

/**
 * 
 * Select query is a narrow graph query - single arc with single element
 * This is just a marker. Implementation will validate the structure 
 *
 */
class VitalSelectQuery extends VitalQuery {
	
	VitalGraphArcContainer topContainer
	
	int offset = 0;
	
	int limit = 0;

	//named properties used in sorting
	List<VitalSortProperty> sortProperties = []
	
	
	public final static String asc = 'asc'
	
	public final static String desc = 'desc'
	
	private static final long serialVersionUID = 1L;

	boolean projectionOnly = false
	
	//property trait
	//select some property rather than the object, to be used with distinct in regular select
	//also used in all aggregation queries  
	String propertyURI
	
	boolean distinct = false
	
	String distinctSort
	
	boolean distinctExpandProperty = false
	
	boolean distinctFirst = false
	boolean distinctLast = false
	
	/**
	 * Helper method to create a new select query instance with top arc container CURRENT,EMPTY,EMPTY
	 * @return
	 */
	public static VitalSelectQuery createInstance() {
	
		VitalSelectQuery sq = new VitalSelectQuery();
		VitalGraphArcContainer tc = new VitalGraphArcContainer(new VitalGraphArcElement(Source.CURRENT, Connector.EMPTY, Destination.EMPTY));
		sq.setTopContainer(tc);
		tc.add(new VitalGraphCriteriaContainer(QueryContainerType.and));
			
		return sq
		
	}
	
	/**
	 * Helper method to return the only criteria container of the parent arc container
	 * @return
	 */
	public VitalGraphCriteriaContainer getCriteriaContainer() {
		if(topContainer == null) throw new RuntimeException("Top container not set!")
		if(topContainer.size() != 1 ) throw new RuntimeException("Top container expected to have exactly 1 child, got: ${topContainer.size()}")
		if( !( topContainer[0] instanceof VitalGraphCriteriaContainer ) ) throw new RuntimeException("The only top container's child must be an instanceof ${VitalGraphCriteriaContainer.class.canonicalName}")
		return (VitalGraphCriteriaContainer) topContainer[0]
		
	}
    
    @Override
    protected String innerDebugString() {
        
        String distinctString = '';
        
        if(distinct) {
            
            distinctString = 
                ( distinct ? "  distinct: true\n" : '' ) +
                 "  distinctSort: ${distinctSort}\n" +
                 "  distinctExpandProperty: ${distinctExpandProperty}\n" +
                 "  distinctFirst: ${distinctFirst}\n" +
                 "  distinctLast: ${distinctLast}\n" 
            ;
            
        }
        
        
        String topContainerString = topContainer ? getCriteriaContainer().printContainer("  ") : '  null top container' 
        
        String s = 
            "  offset: ${offset}\n" +
            "  limit: ${limit}\n" +
            "  projectionOnly: ${projectionOnly}\n" +
            "  sortProperties: ${sortProperties}\n" + 
            ( propertyURI ? "  propertyURI: ${propertyURI}\n" : '') +
            distinctString +
            topContainerString;
            
        return s;
    }
	
}
