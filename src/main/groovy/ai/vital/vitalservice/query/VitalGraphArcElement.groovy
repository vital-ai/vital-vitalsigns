package ai.vital.vitalservice.query

class VitalGraphArcElement implements VitalGraphQueryElement {

	private static final long serialVersionUID = 26L;
	
	
	//edge case
	public Source source;
	
	public Connector connector;
	
	public Destination destination;

	public VitalGraphArcElement() {
        
    }
	public VitalGraphArcElement(Source src, Connector edge, Destination dest) {
		super();
		if(src == null) throw new NullPointerException("Null source");
		if(edge == null) throw new NullPointerException("Null connector");
		if(dest == null) throw new NullPointerException("Null destination");
		this.source = src;
		this.connector = edge;
		this.destination = dest;
		
		if(connector == Connector.EDGE) {
			if(source == Source.PARENT_CONNECTOR) throw new RuntimeException("Cannot use parent connector as arc source");
			if(destination == Destination.PARENT_CONNECTOR) throw new RuntimeException("Cannot use parent connector as arc destination");
		}
		
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if( obj == null ) return false;
		if( ! VitalGraphArcElement.class.isInstance(obj) ) return false;
		VitalGraphArcElement that = (VitalGraphArcElement) obj;
		return
			this.source == that.source &&
			this.connector == that.connector &&
			this.destination == that.destination;
	}
	
	@Override
	public String toString() {
		return "Source." + source.name() + "_Connector." + connector.name() + "_Destination." + destination.name();
	}
	
}
