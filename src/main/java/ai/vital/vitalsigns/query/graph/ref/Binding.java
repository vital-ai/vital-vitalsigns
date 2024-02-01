package ai.vital.vitalsigns.query.graph.ref;

import java.util.ArrayList;

class Binding extends ArrayList<BindingEl> {

	private static final long serialVersionUID = 1L;

	public BindingEl getBindingElForArc(Arc arc) {

		for(BindingEl el : this) {
			
			if(el.getArc() == arc) return el;
			
		}
		
		return null;
	}


}
