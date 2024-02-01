package ai.vital.vitalsigns.model

class VitalSegment extends VITAL_Node {

	private static final long serialVersionUID = 1L;


	public VitalSegment() {
		super()
	}

	public VitalSegment(Map<String, Object> props) {
		super(props)
	}
    
    
    /**
     * Special constant value for all service segments case
     * @return
     */
    public static List<VitalSegment> getAllSegments() {
        return [ new VitalSegment([segmentID: '*'] )]
    }

    
    /**
     * validates and returns true if the value is allSegments one
     * @return
     */
    public static boolean isAllSegments(List<VitalSegment> segments) {
        
        if(segments == null || segments.size() < 1) throw new RuntimeException("Null or empty segments list")
        
        for(VitalSegment s : segments) {
            
            if(s.segmentID.toString().equals('*')) {
                if(segments.size() > 1) throw new RuntimeException("match-all segments list must contain exactly one element with ID='*'")
                return true;
            }
            
        }

        return false;
    }
    
    public static VitalSegment withId(String id) {
        VitalSegment segment = new VitalSegment(segmentID: id)
        segment.generateURI((VitalApp) null)
        return segment
    }
}
