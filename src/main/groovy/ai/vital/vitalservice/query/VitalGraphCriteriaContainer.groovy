package ai.vital.vitalservice.query

/**
 * Container for query criteria
 *
 */
class VitalGraphCriteriaContainer extends ArrayList<VitalGraphQueryElement> implements VitalGraphQueryContainer<VitalGraphQueryElement> {

	private static final long serialVersionUID = 2000L;
	
	private QueryContainerType type = QueryContainerType.and
	
	public VitalGraphCriteriaContainer() {
		super()			
	}
	
	public VitalGraphCriteriaContainer(QueryContainerType type) {
		super();
		this.type = type;
	}

	public QueryContainerType getType() {
		return this.type
	}
	
	public void setType(QueryContainerType type) {
		this.type = type
	}

    @Override
    public String printContainer(String indent) {
        String s = "${indent}${this.getClass().getSimpleName()} ${this.type} (${this.size()}) {\n";
        for(VitalGraphQueryElement el : this ) {
            if(el instanceof VitalGraphQueryContainer) {
                s += ( el.printContainer(indent + '  ') + "\n" ) 
            } else {
                s += "${indent}  ${el.toString()}\n"
            }
        }
        s += "${indent}}"
        return s
    }
	
}
