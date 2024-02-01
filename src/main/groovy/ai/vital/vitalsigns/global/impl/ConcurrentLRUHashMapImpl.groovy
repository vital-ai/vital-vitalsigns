package ai.vital.vitalsigns.global.impl

import java.util.Collection
import java.util.Iterator
import java.util.concurrent.ConcurrentMap;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import ai.vital.vitalsigns.global.HashInterface
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_GraphContainerObject

class ConcurrentLRUHashMapImpl implements HashInterface {

	ConcurrentMap<String, GraphObject> map;
	
	public ConcurrentLRUHashMapImpl() {
		
		map = new ConcurrentLinkedHashMap.Builder<String, GraphObject>().maximumWeightedCapacity(10000).build();
		
	}
	
	public ConcurrentLRUHashMapImpl(int maxCapacity) {
		
		map = new ConcurrentLinkedHashMap.Builder<String, GraphObject>().maximumWeightedCapacity(maxCapacity).build();
		
	}
	
	@Override
	public GraphObject get(String uri) {
		return map.get(uri);
	}

	@Override
	public void putAll(Collection<GraphObject> objects) {
		for(GraphObject go : objects) {
			if(go instanceof VITAL_GraphContainerObject) continue;
			map.put(go.getURI(), go);
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
