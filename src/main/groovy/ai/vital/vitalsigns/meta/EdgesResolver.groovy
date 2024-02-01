package ai.vital.vitalsigns.meta


import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Container;
import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;
import ai.vital.vitalsigns.model.VITAL_Node;


/**
 * Interface that wraps edge destination endpoints resolution 
 */

interface EdgesResolver {

	/**
	 * @param srcUri
	 * @param containers - optional context, may be ignored by some implementations
	 * @return
	 */
	public List<VITAL_Edge> getEdgesForSrcURI(String srcUri, VITAL_Container... containers);
	
	/**
	 * 
	 * @param srcUri
	 * @param edgesFilter
	 * @param directClass
	 * @param containers
	 * @return
	 */
	public List<VITAL_Edge> getEdgesForSrcURI(String srcUri, Class<? extends VITAL_Edge>[] edgesFilter, boolean directClass, VITAL_Container... containers);
	
	/**
	 * 
	 * @param srcUri
	 * @param destUri
	 * @param edgesFilter
	 * @param directClass
	 * @param containers
	 * @return
	 */
	public List<VITAL_Edge> getEdgesForSrcURIAndDestURI(String srcUri, String destUri, Class<? extends VITAL_Edge>[] edgesFilter, boolean directClass, VITAL_Container... containers);
	
	/**
	 * 
	 * @param srcUri
	 * @param destUri
	 * @param edgesFilter
	 * @param directClass
	 * @param containers
	 * @return
	 */
	public List<VITAL_HyperEdge> getHyperEdgesForSrcURIAndDestURI(String srcUri, String destUri, Class<? extends VITAL_HyperEdge>[] edgesFilter, boolean directClass, VITAL_Container... containers);
	
	/**
	 * @param destUri
	 * @param containers - optional context, may be ignored by some implementations
	 * @return
	 */
	public List<VITAL_Edge> getEdgesForDestURI(String destUri, VITAL_Container... containers);
	
	/**
	 * @param destUri
	 * @param edgesFilter
	 * @param directClass
	 * @param containers
	 * @return
	 */
	public List<VITAL_Edge> getEdgesForDestURI(String destUri, Class<? extends VITAL_Edge>[] edgesFilter, boolean directClass, VITAL_Container... containers);
	
	
	/**
	 * @param srcUri
	 * @param containers - optional context, may be ignored by some implementations
	 * @return
	 */
	public List<VITAL_HyperEdge> getHyperEdgesForSrcURI(String srcUri, VITAL_Container... containers);
	
	/**
	 * @param destUri
	 * @param containers - optional context, may be ignored by some implementations
	 * @return
	 */
	public List<VITAL_HyperEdge> getHyperEdgesForDestURI(String destUri, VITAL_Container... containers);
	
	/**
	 * @param srcUri
	 * @param containers - optional context, may be ignored by some implementations
	 * @return mix of nodes and edges
	 */
	public List<GraphObject> getEdgesWithNodesForSrcURI(String srcUri, VITAL_Container... containers);

	/**
	 * 
	 * @param srcUri
	 * @param edgeType
	 * @param containers optionall context, may be ignored by some implementations
	 * @return
	 */
	public List<VITAL_Node> getDestNodesForSrcURI(String srcUri, Class<? extends VITAL_Edge> edgeType, VITAL_Container... containers);

	
	/**
	 * @param destUri
	 * @param edgeType
	 * @param containers optionall context, may be ignored by some implementations
	 * @return 
	 */
	public List<VITAL_Node> getSourceNodesForDestURI(String destUri, Class<? extends VITAL_Edge> edgeType, VITAL_Container... containers);
	
	/**
	 * Notifies the edges resolver of new set of graph objects (for in-memory edges endpoints lookup)	
	 * @param res  results list
	 */
	public void registerObjects(List<GraphObject> res);	

	
	/**
	 * Returns the list of all objects connected to a node
	 * @param srcUri
	 * @param hyperEdgeType
	 * @param containers
	 * @return
	 */
	public List<GraphObject> getDestGraphObjectsForSrcURI(String srcUri, Class<? extends VITAL_HyperEdge> hyperEdgeType, VITAL_Container... containers);
		
	
	/**
	 * 
	 * @param srcUri
	 * @param hyperEdgesFilter
	 * @param directClass
	 * @param containers
	 * @return
	 */
	public List<VITAL_HyperEdge> getHyperEdgesForSrcURI(String srcUri, Class<? extends VITAL_HyperEdge>[] hyperEdgesFilter, boolean directClass,
		VITAL_Container... containers)

	/**
	 * 
	 * @param destUri
	 * @param edgeType
	 * @param containers
	 * @return
	 */
	public List<GraphObject> getSourceGraphObjectsForDestURI(String destUri,
			Class<? extends VITAL_HyperEdge> edgeType, VITAL_Container... containers)
	

	/**
	 * 
	 * @param srcUri
	 * @param containers
	 * @return
	 */
	public List<GraphObject> getHyperEdgesWithGraphObjectsForSrcURI(String srcUri,
			VITAL_Container... containers)
	
	/**
	 * 
	 * @param destUri
	 * @param hyperEdgesFilter
	 * @param directClass
	 * @param containers
	 * @return
	 */
	public List<VITAL_HyperEdge> getHyperEdgesForDestURI(String destUri,
		Class[] hyperEdgesFilter, boolean directClass,
		VITAL_Container... containers)
}
