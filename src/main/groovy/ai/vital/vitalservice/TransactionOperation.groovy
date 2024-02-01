package ai.vital.vitalservice

import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization

abstract class TransactionOperation implements Serializable {

	private static final long serialVersionUID = 1L;
	
	VitalOrganization organization
	
	VitalApp app
	
}
