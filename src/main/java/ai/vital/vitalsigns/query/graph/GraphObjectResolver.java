package ai.vital.vitalsigns.query.graph;

import java.util.Map;

import ai.vital.vitalsigns.model.GraphObject;

/**
 * A filter that resolves full graph objects
 * An optional interface for graph query implementation
 *
 */
public interface GraphObjectResolver {

	/**
	 * resolves the full graph object, it should return the input instance
	 * This implementation is only used if {@link #supportsBatchResolve()} returns <code>false</code>
	 * @param graphObject
	 * @return
	 */
	public GraphObject resolveGraphObject(GraphObject graphObject);
	
	/**
	 * If true the implementation will use another method
	 * @return
	 */
	public boolean supportsBatchResolve();
	
	/**
	 * This implementation is only used if {@link #supportsBatchResolve()} returns <code>true</code> 
	 * @param graphObject
	 * @return
	 */
	public Map<String, GraphObject> resolveGraphObjects(Map<String, GraphObject> graphObject);
}
