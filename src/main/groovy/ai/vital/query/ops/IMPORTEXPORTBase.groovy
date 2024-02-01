package ai.vital.query.ops

import ai.vital.vitalservice.FileType
import ai.vital.vitalsigns.model.VitalSegment

class IMPORTEXPORTBase {

	String path
	
	FileType fileType
	
	Boolean compressed
	
	VitalSegment segment
    
    //don't override by default
    String datasetURI = ''
	
}
