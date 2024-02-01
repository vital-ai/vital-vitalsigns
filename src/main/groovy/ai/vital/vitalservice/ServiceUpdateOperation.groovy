package ai.vital.vitalservice

import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.property.URIProperty;

class ServiceUpdateOperation extends ServiceOperation {

	private static final long serialVersionUID = 1L;
	
	//segment is organization/app/segment specific
//	VitalSegment segment
	
	//the new version of the graph object
	//mutually exclusive with closure
	GraphObject graphObject;

	//the closure that will executed on the fetched object
	//mutually exclusive with graphObject
	private Closure closure
	
	public Closure getClosure() {
		return closure
	}
	
	/**
	 * Sets the closure to be executed over graph object.
	 * A dehydrated closure copy will be set to avoid serialization issues.
	 * Closure single GraphObject param assumed. 
	 * @param closure
	 */
	public void setClosure(Closure closure) {
		this.closure = closure.dehydrate()
	}
	URIProperty URI
		
}
