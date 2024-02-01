package ai.vital.vitalsigns.model.property;

import ai.vital.vitalservice.VitalService;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Container;
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.utils.StringUtils;


public class URIProperty implements IProperty {

	// Move this to VitalURI
	public static final String MATCH_ALL_PREFIX = 'match-all'
	
	/**
	 * Constant used in queries and service operations
	 */
	public static final String MATCH_ANY_URI = 'match-any-uri'
	
	private static final long serialVersionUID = 1975297098145199044L;
	
	private String _uri;
	
	// helper methods
	public static URIProperty withString(String u) {
		if(u == null) { throw new RuntimeException("URIProperty cannot be set to a null value.") }
		return new URIProperty(u)
	}
	
	public static List<URIProperty> toList(Collection<String> strings) {
		List<URIProperty> uris = new ArrayList<URIProperty>(strings.size())
		for(String s : strings) {
			uris.add(new URIProperty(s))
		}
		return uris
	}
	
	public void setValue(String v) {
		
		if(v == null) { throw new RuntimeException("URIProperty cannot be set to a null value.") }
		else {
		
			this._uri = v
		}
	}
	
	public URIProperty() {
		super();
		this._uri = "";
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		//println "URIProperty clone: ${this}";
		return super.clone();
	}
	
	
	public URIProperty(String _uri) {
		super();
		
		if(_uri == null) {  throw new RuntimeException("URIProperty cannot be set to a null value." )
 }
		else {
			this._uri = _uri
		}
	}

	public String get() {
		return _uri;
	}
	
	
	@Override
	public String toString() {
		return _uri;
	}
	
	@Override
	public Object rawValue() {
		return _uri;
	}
	
	@Override
	public IProperty unwrapped() {
		return this;
	}
	
	/**
	 * Resolves given URI in vitalsigns context
	 * @param context
	 * @return
	 */
	public GraphObject resolve(GraphContext context, VITAL_Container... containers) {
		
		if(!_uri) throw new RuntimeException("No URI set - cannot resolve graph object!");
		
		if(context == GraphContext.ServiceWide) {
			
			if(containers != null && containers.length > 0) throw new RuntimeException("Cannot use containers for " + context);
			
			VitalService si = VitalSigns.get().getVitalService();
			
			if(si == null) throw new RuntimeException("Service Interface not set, cannot resolve graph object in ServiceWide context");
			
			return si.get(context, URIProperty.withString(_uri), false).first()
			
		} else if(context == GraphContext.Local) {
		
			if(containers != null && containers.length > 0) throw new RuntimeException("Cannot use containers for " + context);
		
			return VitalSigns.get().getFromCache(this._uri)
		
		} else {
		
			if(containers == null || containers.length == 0) throw new RuntimeException("No container set, context " + context + " requires at least container");

			for(VITAL_Container c : containers) {
				GraphObject g = c.get(_uri);
				if(g != null) return g;
			}
					
		}
		
		return null;
	}
	
	/**
	 * Resolves given URI in vitalsigns global cache.
	 * Alias for resolve(GraphContext.Local)
	 * @return
	 */
	public GraphObject resolve() {
		return this.resolve(GraphContext.Local);
	}

	
	/**
	 * creates external property criterion 
	 * @param propertyURI
	 * @return
	 */
	public static VitalGraphQueryPropertyCriterion create(String propertyURI) {
		VitalGraphQueryPropertyCriterion c = new VitalGraphQueryPropertyCriterion(propertyURI);
		c.externalProperty = true
		return c;
	}
	
	
	private String externalURI
	
	public String getURI() {
		if(externalURI != null) return externalURI
		throw new RuntimeException("getURI should be implemented by property trait");
	}
	
	
	public static URIProperty createInstance(String propertyURI, Object value) {
		String _uri = null
		if(value instanceof URIProperty) {
			_uri = ((URIProperty)value).get()
		} else if(value instanceof String || value instanceof GString){
			_uri = value.toString()
		} else {
			throw new RuntimeException("Invalid value type: ${value?.class.canonicalName}")
		}
		URIProperty bp = new URIProperty(_uri)
		bp.externalURI = propertyURI
		return bp
	}
	
	
	
	public static URIProperty getMatchAllURI(VitalSegment segment) {
		if(segment.segmentID == null) throw new NullPointerException("Segment ID mustn't be null")
		if(StringUtils.isEmpty(segment.segmentID)) throw new RuntimeException("Segment ID mustn't be empty")
		return URIProperty.withString(MATCH_ALL_PREFIX + segment.segmentID.toString())
	}

    @Override
    public void setExternalPropertyURI(String externalPropertyURI) {
        this.externalURI = externalPropertyURI;
    }
    
    
    /**
     * Creates an new a graph object provided this uri property points to a vitalsigns class
     * @throws exception when graph object class not found
     * @return
     */
    public GraphObject createGraphObject() throws Exception {
        if(!_uri) throw new Exception("No uri value set")
        
        Class<? extends GraphObject> clazz = VitalSigns.get().getClass(this.unwrapped())
        if(clazz == null) throw new Exception("Class not found: ${_uri}")
        
        return (GraphObject) clazz.newInstance()
        
    }
    
}

