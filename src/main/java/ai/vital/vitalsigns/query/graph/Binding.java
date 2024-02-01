package ai.vital.vitalsigns.query.graph;

import java.util.ArrayList;

class Binding extends ArrayList<BindingEl> {

	public static enum BindingStatus {
		
		NO_BINDINGS_FOUND, //exception!
		
		NO_MORE_BINDINGS, //pool exhausted or optional binding
		
		EMPTY_OPTIONAL,
		
		OK
		
	}
	
	private static final long serialVersionUID = 1L;

	private BindingStatus status;
	
	public Binding(BindingStatus status) {
		super();
		if(status == null) throw new NullPointerException("Null status");
		this.status = status;
	}


	public BindingEl getBindingElForArc(Arc arc) {

		for(BindingEl el : this) {
			
			if(el.getArc() == arc) return el;
			
		}
		
		return null;
	}


	public BindingStatus getStatus() {
		return status;
	}


	public void setStatus(BindingStatus status) {
		this.status = status;
	}

}
