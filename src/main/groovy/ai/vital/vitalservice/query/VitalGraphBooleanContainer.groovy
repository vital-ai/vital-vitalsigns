package ai.vital.vitalservice.query

/**
 * a container that groups arc- or other boolean- containers
 * 
 *
 */
class VitalGraphBooleanContainer extends ArrayList<VitalGraphQueryContainer<?>> implements VitalGraphQueryContainer<VitalGraphQueryContainer<?>> {

    private static final long serialVersionUID = 2000L;
    
	private QueryContainerType type = QueryContainerType.and

	public VitalGraphBooleanContainer(QueryContainerType type) {
		super();
		this.type = type;
	}

	@Override
	public QueryContainerType getType() {
		return type;
	}

	@Override
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
