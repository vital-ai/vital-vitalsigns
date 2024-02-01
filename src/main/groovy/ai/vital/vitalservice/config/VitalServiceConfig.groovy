package ai.vital.vitalservice.config

import ai.vital.vitalservice.EndpointType;
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization;

abstract class VitalServiceConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    
	// abstract class for config of an Endpoint
	// an implementation would extend and implement this

	EndpointType endpointtype
	
	String defaultSegmentName
	
    //for local endpoints only
    VitalApp app
    
	//local or 
	URIGenerationStrategy uriGenerationStrategy = URIGenerationStrategy.local
	
    //for local endpoints only
    VitalOrganization organization
    
}
