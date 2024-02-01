package ai.vital.vitalsigns.global.impl

import java.util.Collection
import java.util.Iterator
import ai.vital.vitalsigns.global.HashInterface
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_GraphContainerObject

class HashMapImpl implements HashInterface {

	private HashMap<String, GraphObject> map = Collections.synchronizedMap(new HashMap<String, GraphObject>());
	
	@Override
	public GraphObject get(String uri) {
		return map.get(uri);
	}

	@Override
	public void putAll(Collection<GraphObject> objects) {
		for(GraphObject go : objects) {
			if(go instanceof VITAL_GraphContainerObject) continue;
			map.put(go.URI, go);
		}
	}

	@Override
	public GraphObject remove(String uri) {
		return map.remove(uri);
	}

	@Override
	public void purge() {
		map.clear();
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public Iterator<GraphObject> iterator() {
		return map.values().iterator();
	}
	
}
