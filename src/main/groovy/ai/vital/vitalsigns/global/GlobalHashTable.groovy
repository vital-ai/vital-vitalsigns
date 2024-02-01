package ai.vital.vitalsigns.global


import java.util.Map.Entry

import ai.vital.lucene.model.LuceneSegment
import ai.vital.lucene.model.LuceneSegmentType
import ai.vital.service.lucene.model.LuceneSegmentConfig
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.classes.ClassesRegistry
import ai.vital.vitalsigns.global.impl.LRUMapImpl;
import ai.vital.vitalsigns.global.impl.OldElementRemovedListener
import ai.vital.vitalsigns.model.GraphObject;

import ai.vital.vitalsigns.model.VITAL_GraphContainerObject;

import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization;
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.container.GraphObjectsIterable

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;


class GlobalHashTable implements GraphObjectsIterable, OldElementRemovedListener {

	private static GlobalHashTable singleton;
	
//	private HashInterface impl = new HashMapImpl();
//	private HashInterface impl = new WeakHashMapImpl();
//	private HashInterface impl = new GuavaCacheImpl();
	private HashInterface impl = null//new LRUMapImpl(10000);
	
	
	//edges index
	private Map<String, Set<String>> srcURI2Edge = new HashMap<String, Set<String>>();
	
	private Map<String, Set<String>> destURI2Edge = new HashMap<String, Set<String>>();
	
	//hyper edges index
	private Map<String, Set<String>> srcURI2HyperEdge = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> destURI2HyperEdge = new HashMap<String, Set<String>>();
	
	//node type index - all nodes are grouped by their class
	Map<Class<? extends GraphObject>, Set<String>> typesCache = new HashMap<Class<? extends GraphObject>, Set<String>>(); 

	private LuceneSegment luceneSegment = null;
	
	//for tests only
//	@PackageScope
	public static int LRU_MAP_SIZE = 10000;	
	
	private GlobalHashTable() {
		
		impl = new LRUMapImpl(LRU_MAP_SIZE, this);
		
        VitalOrganization org = new VitalOrganization()
        org.organizationID = VitalSigns.CACHE_DOMAIN
        
        VitalApp app = new VitalApp()
        app.appID = VitalSigns.CACHE_DOMAIN;
        
        VitalSegment seg = VitalSegment.withId(VitalSigns.CACHE_DOMAIN)
        
		LuceneSegmentConfig config = new LuceneSegmentConfig(LuceneSegmentType.memory, false, false, null)
		luceneSegment = new LuceneSegment(org, app, seg, config);
		luceneSegment.open()
		
	}
	
	private boolean edgeIndexEnabled = false;
	
	public static void changeImplementation(HashInterface newImpl) {
		get().impl = newImpl;
	}
	
	public static GlobalHashTable get() {
		
		if(singleton == null) {
			
			synchronized(GlobalHashTable.class) {
				
				if(singleton == null) {
					
					singleton = new GlobalHashTable();
					
				}
				
			}
			
		}
		
		return singleton;
		
	}

	@Override
	public GraphObject get(String uri) {
		return impl.get(uri);
	}

	public void putAll(Collection<GraphObject> objects) {

		for(GraphObject go : objects) {
			if(go.URI == null || go.URI.isEmpty()) {
				throw new RuntimeException("Cannot add a graph object to cache with null or empty URI")
			}
		}
				
		if(luceneSegment != null) {
			//filter the objects
			List<GraphObject> filtered = new ArrayList<GraphObject>()
			for(GraphObject go : objects) {
				if(go instanceof VITAL_GraphContainerObject) continue;
				filtered.add(go)
			}
			luceneSegment.insertOrUpdateBatch(filtered)
		}
		
		impl.putAll(objects);
		
		
		if(edgeIndexEnabled) {
			synchronized (this) {
				for(GraphObject o : objects) {
					
					//skip
					if(o instanceof VITAL_GraphContainerObject) {
						continue;
					}
					
					if(o instanceof VITAL_Edge) {
						VITAL_Edge e = o;
						Set<String> srcEdges = srcURI2Edge.get(e.sourceURI);
						if(srcEdges == null) {
							srcEdges = new HashSet<String>();
							srcURI2Edge.put(e.sourceURI, srcEdges);
						}
						srcEdges.add(e.URI);
						
						Set<String> destEdges = destURI2Edge.get(e.destinationURI);
						if(destEdges == null) {
							destEdges = new HashSet<String>();
							destURI2Edge.put(e.destinationURI, destEdges);
						}
						destEdges.add(e.URI);
					} else if(o instanceof VITAL_HyperEdge) {
						VITAL_HyperEdge he = o;
						Set<String> srcHyperEdges = srcURI2HyperEdge.get(he.sourceURI);
						if(srcHyperEdges == null) {
							srcHyperEdges = new HashSet<String>();
							srcURI2HyperEdge.put(he.sourceURI, srcHyperEdges);
						}
						srcHyperEdges.add(he.URI);
						
						Set<String> destHyperEdges = destURI2HyperEdge.get(he.destinationURI);
						if(destHyperEdges == null) {
							destHyperEdges = new HashSet<String>();
							destURI2HyperEdge.put(he.destinationURI, destHyperEdges);
						}
						destHyperEdges.add(he.URI);
					}
					
					
					Class<? extends GraphObject> cls = o.getClass();
					Set<String> objs = typesCache.get(cls);
					if(objs == null) {
						objs = new HashSet<String>();
						typesCache.put(cls, objs);
					}
					objs.add(o.URI);
				}
			}
		}
	}
		
	public GraphObject remove(String uri) {
		
		GraphObject g = impl.remove(uri);
		
		_removeFromIndex(g)
		
		return g;
		
	}
	
	public void _removeFromIndex(GraphObject g) {
		
		if(g != null && edgeIndexEnabled) {
			
			if(g instanceof VITAL_Edge) {
				VITAL_Edge e = g;
				
				synchronized(this) {
					
					clearIndex(srcURI2Edge, e.URI);
					
					clearIndex(destURI2Edge, e.URI);
					
				}
			} else if(g instanceof VITAL_HyperEdge) {
		
				VITAL_HyperEdge he = g;
			
				synchronized(this) {
					
					clearIndex(srcURI2HyperEdge, he.URI);
					
					clearIndex(destURI2HyperEdge, he.URI);
					
				}
				
			} 
	
			Set<String> uris = typesCache.get(g.getClass());
			
			if(uris != null) {
				uris.remove(g.URI);
				if(uris.size() == 0) {
					typesCache.remove(g.getClass());
				}
			}
			
			
			/*		
				for(Iterator<Entry<Class<? extends VITAL_Node>, Set<String>>> iterator = typesCache.entrySet().iterator(); iterator.hasNext(); ) {
					
					Entry<Class<? extends VITAL_Node>, Set<String>> entry = iterator.next();
					
					Set<String> nodes = entry.getValue();
					
					if(nodes != null) {
						nodes.remove(uri);
						if(nodes.size() == 0) {
							iterator.remove();
						}
					}
					
				}
				
			
			}
			*/
			
		
		}
		
	}
	
	public void clearIndex(Map<String, Set<String>> index, String eURI) {
		
		for(Iterator<Entry<String,Set<String>>> iterator = index.entrySet().iterator(); iterator.hasNext(); ) {
			
			Entry<String,Set<String>> entry = iterator.next();
			
			Set<String> edges = entry.getValue();
			
			edges.remove(eURI);
			
			if(edges.size() == 0) {
				
				iterator.remove();
				
			}
			
		}
		
	}
	
	public void purge() {
		
		if(luceneSegment != null) {
			luceneSegment.close()
			luceneSegment.open()
		}
		
		impl.purge();
		srcURI2Edge.clear();
		destURI2Edge.clear();
		
		srcURI2HyperEdge.clear();
		destURI2HyperEdge.clear();
		
		typesCache.clear();
		
	}
	
	public int size() {
		return impl.size();
	}
	
	@Override
	public Iterator<GraphObject> iterator() {
		return impl.iterator();
	}
	
	@Override
	public <T extends GraphObject> Iterator<T> iterator(Class<T> cls, boolean strict) {
		
		List<T> typedList = new ArrayList<T>();
		
		
		if(edgeIndexEnabled) {
			
			if(strict) {
				
				Set<String> uris= typesCache.get(cls);
				
				if(uris != null) {
					
					for(String s : uris) {
						GraphObject go = get(s);
						if(go != null) {
							typedList.add(go);
						}
					}
					
				}
				
			} else {
			
				// we need to iterate over type list and collect
			
				for(Iterator<Entry<Class<? extends VITAL_Node>, Set<String>>> iterator = typesCache.entrySet().iterator(); iterator.hasNext(); ) {
					
					Entry<Class<? extends GraphObject>, Set<String>> entry = iterator.next();
					
					Class<? extends GraphObject> _class = entry.getKey();
					
					if(cls.isAssignableFrom(_class)) {
						Set<String> uris = entry.getValue();
						if(uris != null) {
							for(String s : uris) {
								GraphObject go = get(s);
								if(go != null) {
									typedList.add(go);
								}
							}
						}
					}
					
				}
			
			
			}
			
			
		} else {
		
			for(Iterator<GraphObject> iter = this.iterator(); iter.hasNext();) {
				
				GraphObject go = iter.next();
				
				if(strict) {
					
					if( go.getClass().equals(cls) ) {
						typedList.add(go);
					}
					
				} else if(cls.isInstance(go)) {
					
					typedList.add(go);
					
				}
				
			}
			
		}
		
		
		
		return typedList.iterator();
		
	}
	
	@Override
	public <T extends GraphObject> Iterator<T> iterator(Class<T> cls) {
		return this.iterator(cls, false);
	}

	public boolean isEdgeIndexEnabled() {
		return edgeIndexEnabled;
	}	
	
	public void setEdgeIndexEnabled(boolean _edgeIndexEnabled) {
		this.edgeIndexEnabled = _edgeIndexEnabled;
	}

	@Override
	public Map<String, Set<String>> getSrcURI2Edge() {
		return srcURI2Edge;
	}

	@Override
	public Map<String, Set<String>> getDestURI2Edge() {
		return destURI2Edge;
	}

	@Override
	public Map<String, Set<String>> getSrcURI2HyperEdge() {
		return srcURI2HyperEdge;
	}

	@Override
	public Map<String, Set<String>> getDestURI2HyperEdge() {
		return destURI2HyperEdge;
	}

	@Override
	public void onOldElementRemoved(Object k, Object v) {

		if(luceneSegment != null) {
			
			//delete it
			String uri = k
			
			luceneSegment.delete(uri)
			
		}
		
		GraphObject g = v

		_removeFromIndex(g)		
		
	}
	
	@Override
	public LuceneSegment getLuceneSegment() {
		return this.luceneSegment;
	}

    /**
     * Purges objects from given domain only
     */
    public void purgeDomain(String ontologyURI) {

        ClassesRegistry cr = VitalSigns.get().getClassesRegistry();
        
        for( Iterator<GraphObject> iter = impl.iterator(); iter.hasNext(); ) {
            
            GraphObject g = iter.next();
            
            String cURI = cr.getClassURI(g.getClass())
            
            if(cURI == null || cURI.startsWith(ontologyURI + '#')) {

                remove(g.URI)
                
            }
            
        }
        
        
    }

}
