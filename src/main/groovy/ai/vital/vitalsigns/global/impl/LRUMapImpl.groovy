package ai.vital.vitalsigns.global.impl

import java.util.Collection
import java.util.Iterator
import org.apache.commons.collections.map.LRUMap
import org.apache.commons.collections.map.AbstractLinkedMap.LinkEntry
import ai.vital.vitalsigns.global.HashInterface
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_GraphContainerObject

/**
 * The lru hashmap implementation for global hashtable
 * Because of the iterator interface, it needs to synchronize over the map rather than use Collections.synchronizedMap().
 *
 */

class LRUMapImpl implements HashInterface {

	private Map map;
	
	private OldElementRemovedListener listener
	
	public LRUMapImpl() {
		this(5000);
	}
	
	public LRUMapImpl(int cacheSize) {
		map = new LRUMap(cacheSize);
	}
	
	public LRUMapImpl(int cacheSize, final OldElementRemovedListener listener) {
		this.listener = listener;
		map = new LRUMap(cacheSize) {

			@Override
			protected boolean removeLRU(LinkEntry entry) {
				
				if(listener != null) {
					listener.onOldElementRemoved(entry.getKey(), entry.getValue())
				}
				
				return true;
			}
			
		};
	}
	
	
	@Override
	public GraphObject get(String uri) {
		synchronized(map) {
			return map.get(uri);
		}
	}

	@Override
	public void putAll(Collection<GraphObject> objects) {
		synchronized(map) {
			for(GraphObject go : objects) {
				if(go instanceof VITAL_GraphContainerObject) continue;
				map.put(go.getURI(), go);
			}
		}
	}

	@Override
	public GraphObject remove(String uri) {
		synchronized(map) {
			return map.remove(uri);
		}
	}

	@Override
	public void purge() {
		synchronized(map) {
			map.clear();
		}
	}

	@Override
	public int size() {
		synchronized(map) {
			return map.size();
		}
	}

	@Override
	public Iterator<GraphObject> iterator() {
		synchronized(map) {
			return new HashSet(map.values()).iterator();
		}
	}

}
