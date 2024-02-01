package ai.vital.vitalservice.query

import ai.vital.vitalsigns.model.property.IProperty

/**
 * A select query that returns AggregationResult only
 *
 */
class VitalSelectAggregationQuery extends VitalSelectQuery {

	private static final long serialVersionUID = 321321321321L;
	
	AggregationType aggregationType
	
	public VitalSelectAggregationQuery() {}
    
	public VitalSelectAggregationQuery(AggregationType aggregationType,
			IProperty property) {
		super();
		this.aggregationType = aggregationType;
		this.propertyURI = property.getURI()
	}
			
	public VitalSelectAggregationQuery(AggregationType aggregationType,
			String propertyURI) {
		super();
		this.aggregationType = aggregationType;
		this.propertyURI = propertyURI
	}
            
            
			
			
			
			
	/**
	 * Helper method to create a new select query instance with top arc container CURRENT,EMPTY,EMPTY
	 * @return
	 */
	public static VitalSelectAggregationQuery createAggInstance(AggregationType aggregationType, IProperty property) {
		
		VitalSelectAggregationQuery sq = new VitalSelectAggregationQuery(aggregationType, property);
		VitalGraphArcContainer tc = new VitalGraphArcContainer(new VitalGraphArcElement(Source.CURRENT, Connector.EMPTY, Destination.EMPTY));
		sq.setTopContainer(tc);
		tc.add(new VitalGraphCriteriaContainer(QueryContainerType.and));
				
		return sq
				
	}
	
	
	
	
	
}
