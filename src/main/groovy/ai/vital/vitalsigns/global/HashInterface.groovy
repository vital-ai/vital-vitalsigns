package ai.vital.vitalsigns.global

import java.util.Collection
import java.util.Iterator
import ai.vital.vitalsigns.model.GraphObject

public interface HashInterface {

	public GraphObject get(String uri)

	public void putAll(Collection<GraphObject> objects)
		
	public GraphObject remove(String uri)
	
	public void purge()
	
	public int size()
	
	public Iterator<GraphObject> iterator()
	
}
