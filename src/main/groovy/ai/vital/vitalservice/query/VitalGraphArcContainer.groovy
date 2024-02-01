package ai.vital.vitalservice.query

/**
 * Container for arc- and criteria- containers
 * type determines the arc- containers relation (AND by default)
 * all criteria containers are AND-ed as well
 *
 */
class VitalGraphArcContainer extends ArrayList<VitalGraphQueryContainer<?>> implements VitalGraphQueryContainer<VitalGraphQueryContainer<?>>{

	
	public static enum Capture {
		
		NONE, SOURCE, BOTH, TARGET, CONNECTOR
		
	}
	
	private static final long serialVersionUID = 2000L;
	
	private QueryContainerType type = QueryContainerType.and
	
	VitalGraphArcElement arc
	
	List<VitalGraphValueCriterion> valueCriteria = []
	
	Map<String, VitalGraphValue> providesMap = [:]
	
	//determines if this arc is optional
	boolean optional = false
	
	//if capture=true then all variables are collected
	//it is important to note that optional=true capture=false arc container does not make sense
	Capture capture = Capture.BOTH
	
	String sourceBind = null
	
	String connectorBind = null
	
	String targetBind = null
	
	String label = null
    
    //path
    boolean countArc = true
	
    public VitalGraphArcContainer() {
        
    }
    
	public VitalGraphArcContainer(VitalGraphArcElement arc) {
		this.arc = arc
	}
	 
	public VitalGraphArcContainer(QueryContainerType type, VitalGraphArcElement arc) {
		this.type = type
		this.arc = arc
	} 
	
	public QueryContainerType getType() {
		return this.type
	}
	
	public void setType(QueryContainerType type) {
		this.type = type
	}

	
	public VitalGraphArcContainer addValueCriterion(VitalGraphValueCriterion valueCriterion) {
		if(valueCriterion == null) throw new NullPointerException("providesCriterion cannot be null")
		this.valueCriteria.add(valueCriterion)
		return this
	}
	
	public VitalGraphArcContainer addProvides(String name, VitalGraphValue value) {
		if(name == null) throw new NullPointerException("name cannot be null")
		if(value == null) throw new NullPointerException("value cannot be null")
		this.providesMap.put(name, value)
		return this
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return this.label;
	}

    @Override
    public String printContainer(String indent) {

        String s = "${indent}${this.class.simpleName} ${type} ${arc} optional? ${optional} capture: ${capture} srcBind:${sourceBind} connBind:${connectorBind} targetBind:${targetBind} "
        s += "${label ? 'label:' + label : ''} ${providesMap.size() > 0 ? 'providesMap:' + providesMap : ''} ${valueCriteria.size() > 0 ? 'valueCriteria: ' + valueCriteria : ''} (${this.size()}) {"
        
        for(VitalGraphQueryContainer<?> c : this) {
            
            s += ( c.printContainer(indent + '  ') + '\n' )
            
        }
        
        s += "\n${indent}}"
        return s;
    }

}
