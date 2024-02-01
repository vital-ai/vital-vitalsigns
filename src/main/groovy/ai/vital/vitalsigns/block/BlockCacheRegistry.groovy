package ai.vital.vitalsigns.block;

import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Container;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.BlockIterator;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.VitalBlock
import org.apache.commons.collections.map.LRUMap

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

class BlockCacheRegistry {

	private static BlockCacheRegistry singleton
	
	public static BlockCacheRegistry get() {
		if(singleton == null) {
			synchronized(BlockCacheRegistry.class) {
				if(singleton == null) {
					singleton = new BlockCacheRegistry()
				}
			}
		}
		
		return singleton
	} 
	
	private BlockCacheRegistry() {}
	
	// BlockIterator, VITAL_Container
	private Map<BlockIterator, VITAL_Container> iterator2Container = new HashMap<BlockIterator, VITAL_Container>()
	
	public synchronized addNewBlock(BlockIterator iterator, VitalBlock block) {
		
		// iterator2Container.remove(iterator)
		
		VITAL_Container container = new CacheContainer()
		container.graphObjects.put(block.mainObject.URI, block.mainObject)
		container.putGraphObjects(block.dependentObjects)
		
		iterator2Container.put(iterator, container)
		
	}
	
	public synchronized removeIterator(BlockIterator iterator) {
		iterator2Container.remove(iterator);
	}
	
	public synchronized Collection<VITAL_Container> getContainers() {
		return new ArrayList<VITAL_Container>(iterator2Container.values())
	}
	
	private static class CacheContainer extends VITAL_Container {

		public CacheContainer() {
			super();
		}

		public CacheContainer(boolean queryable) {
			super(queryable);
		}

	}
	
	public synchronized GraphObject getURI(String URI) {
		
		GraphObject g = null
		for(VITAL_Container container : iterator2Container.values()) {
			g = container.get(URI)
			if(g != null) return g;
		}
		
	} 

}
