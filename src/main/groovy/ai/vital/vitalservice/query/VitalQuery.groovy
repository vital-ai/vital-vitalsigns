package ai.vital.vitalservice.query

import java.io.Serializable
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.java.VitalJavaSerializationUtils;
import ai.vital.vitalsigns.model.VitalSegment

abstract class VitalQuery implements Serializable, Cloneable {

	private static final long serialVersionUID = 1985L;
	
	List<VitalSegment> segments = new ArrayList<VitalSegment>()
	
	/**
	 * Flag for testing purposes. If set the ResultList will be empty and the status message field will
	 * contain the sparql strings, sepa
	 */
	boolean returnSparqlString = false
    
    
    /**
     * collect stats for query execution, none by default
     */
    CollectStats collectStats = CollectStats.none
    
    
    /**
     * Specifies that the query execution should block until given transaction is complete (committed or rolled back) 
     */
    String blockOnTransactionID
    
    
    /**
     * Query timeout [seconds]
     */
    Integer timeout = null
	
	public List<String> toSparql(Object... args) {
//		VitalQueryHelper helper = VitalSigns.get().getQueryHelper()
//		if(helper == null) throw new RuntimeException("No query helper set")
//		VitalQuery cloned = this.clone()
//		return helper.toSparql(cloned);
		throw new RuntimeException("toSparql() not implemented by upper layer");
	}
	@Override
	public Object clone() throws CloneNotSupportedException {

		//deep copy by serializing / deserializing:
		//http://stackoverflow.com/questions/665860/deep-clone-utility-recomendation
		return VitalJavaSerializationUtils.clone(this)
		
	}
	
    protected String innerDebugString() {
        return "  TODO!"
    }
    
    public String debugString() {
        
        String segmentsString = "null"
        if(segments != null) {
            segmentsString = "(${segments.size()}) ["
            boolean first = true
            for(VitalSegment s : segments) {
                if(!first) segmentsString += " , "
                segmentsString += ( '' + s.segmentID + ' [' + s.URI + ']' ) 
                first = false
            }
            segmentsString += "]"
        }
        
        return "" +
            this.getClass().getSimpleName() + " {\n" +
                "  blockOnTransactionID: ${blockOnTransactionID}\n" +
                "  collectStats: ${collectStats}\n"+
                "  returnSparqlString: ${returnSparqlString}\n" +
                "  segments: ${segmentsString}\n" +
                "  timeout: ${timeout} seconds\n" +
                innerDebugString() + "\n" +
            "}"           
        
    }
    
}
