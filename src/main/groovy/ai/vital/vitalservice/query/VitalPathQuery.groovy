package ai.vital.vitalservice.query

import ai.vital.vitalsigns.model.property.URIProperty;

class VitalPathQuery extends VitalQuery {

	private static final long serialVersionUID = 1L;

	//mutually exclusive with rootURIs
	VitalGraphArcContainer rootArc
	
	//mutually exclusive with rootArc
	List<URIProperty> rootURIs = null
	
    boolean countRoot = true
    
	List<VitalGraphArcContainer> arcs = []
	
	
	//unlimited by default
	int maxdepth = 0
	
    int offset = 0;
    
    int limit = 0;

    boolean projectionOnly = false
}
