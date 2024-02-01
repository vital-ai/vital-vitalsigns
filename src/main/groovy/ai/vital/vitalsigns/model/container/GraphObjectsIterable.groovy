package ai.vital.vitalsigns.model.container;

import java.util.Map;
import java.util.Set;

import ai.vital.lucene.model.LuceneSegment;
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalservice.query.VitalQuery;
import ai.vital.vitalsigns.model.GraphObject;

interface GraphObjectsIterable extends Iterable<GraphObject> {

	/**
	 * Returns objects of particular type
	 * @param iterator
	 * @param strict true if no subclasses, all compatible types otherwise
	 * @return
	 */
	public <T extends GraphObject> Iterator<T> iterator(Class<T> cls, boolean strict);

	/**
	 * Same as iterator(cls, false);	
	 */
	public <T extends GraphObject> Iterator<T> iterator(Class<T> cls);
	
	/**
	 * Returns the object with given URI (<code>null</code> if does not exist)
	 * @param uri
	 * @return graphObject if exists, <code>null</code> otherwise
	 */
	public GraphObject get(String uri);
	
	
	/**
	 * If <code>true</code> this iterable grants access to local edges index (hashmap)
	 * @return
	 */
	public boolean isEdgeIndexEnabled();
	
	
	public Map<String, Set<String>> getSrcURI2Edge();
	
	public Map<String, Set<String>> getDestURI2Edge();
	
	public Map<String, Set<String>> getSrcURI2HyperEdge();
	
	public Map<String, Set<String>> getDestURI2HyperEdge();
	
	/**
	 * only graph object iterables that have internal lucene queries support 
	 * @return
	 */
	public LuceneSegment getLuceneSegment();
		
	
}
