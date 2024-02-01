package ai.vital.vitalsigns.global.impl

import java.util.Collection
import java.util.Iterator

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.Cache;

import ai.vital.vitalsigns.global.HashInterface;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_GraphContainerObject

class GuavaCacheImpl implements HashInterface {

	private Cache<String, GraphObject> cache;
	
	public GuavaCacheImpl() {
		cache = CacheBuilder.newBuilder().weakValues().build();
	}
	
	
	@Override
	public GraphObject get(String uri) {
		return cache.getIfPresent(uri);
	}

	@Override
	public void putAll(Collection<GraphObject> objects) {

		for(GraphObject go : objects) {
			if(go instanceof VITAL_GraphContainerObject) continue;
			cache.put(go.URI, go);
		}
		
	}

	@Override
	public GraphObject remove(String uri) {
		GraphObject o = cache.getIfPresent(uri);
		cache.invalidate(uri);
		return o;
	}

	@Override
	public void purge() {
		cache.invalidateAll();
	}

	@Override
	public int size() {
		return (int)cache.size();
	}

	@Override
	public Iterator<GraphObject> iterator() {
		return cache.asMap().values().iterator();
	}

}
