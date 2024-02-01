package ai.vital.vitalservice.query

/**
 * Base class for arc- and criteria- containers
 *
 */
abstract interface VitalGraphQueryContainer<T> extends VitalGraphQueryElement, List<T> {

	public QueryContainerType getType()
	
	public void setType(QueryContainerType type)
	
    public String printContainer(String indent)
    	
}
