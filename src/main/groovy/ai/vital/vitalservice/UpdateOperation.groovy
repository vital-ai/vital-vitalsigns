package ai.vital.vitalservice

import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VitalSegment

class UpdateOperation extends TransactionOperation {

	private static final long serialVersionUID = 1L;
	
	//segment is organization/app/segment specific
	VitalSegment segment
	
	GraphObject graphObject;
	
}
