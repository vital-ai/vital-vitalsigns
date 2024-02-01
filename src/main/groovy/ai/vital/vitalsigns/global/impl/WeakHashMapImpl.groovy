package ai.vital.vitalsigns.global.impl;

import java.lang.ref.WeakReference
import java.util.Collection
import java.util.Iterator
import java.util.NoSuchElementException
import ai.vital.vitalsigns.global.HashInterface
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_GraphContainerObject

class WeakHashMapImpl implements HashInterface {

	private WeakHashMap<String, WeakReference<GraphObject>> map = Collections.synchronizedMap(new WeakHashMap<String, WeakReference<GraphObject>>());
	
	@Override
	public GraphObject get(String uri) {
		WeakReference<GraphObject> ref = map.get(uri);
		if(ref) return ref.get();
		return null;
	}

	@Override
	public void putAll(Collection<GraphObject> objects) {
		for(GraphObject go : objects) {
			if(go instanceof VITAL_GraphContainerObject) continue;
			map.put(go.URI, new WeakReference<GraphObject>(go));
		}
	}

	@Override
	public GraphObject remove(String uri) {
		WeakReference<GraphObject> ref = map.remove(uri);
		if(ref) return ref.get();
		return null;
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
		return new IteratorWrapper(map.values().iterator());
	}

	static class IteratorWrapper implements Iterator<GraphObject> {
		
		private Iterator<WeakReference<GraphObject>> parentIterator;	
		
		public IteratorWrapper(Iterator<WeakReference<GraphObject>> parentIterator) {
			this.parentIterator = parentIterator;
		}
		
		@Override
		public boolean hasNext() {
			return parentIterator.hasNext();
		}
		
		@Override
		public GraphObject next() {
			WeakReference ref = parentIterator.next();
			if(ref) return ref.get();
			return null;
		}
			
		@Override
		public void remove() {
			parentIterator.remove();
		}
	}
	
}
