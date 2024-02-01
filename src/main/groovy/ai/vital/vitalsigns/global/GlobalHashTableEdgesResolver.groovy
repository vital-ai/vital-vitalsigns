package ai.vital.vitalsigns.global


import ai.vital.lucene.model.LuceneSegment
import ai.vital.vitalsigns.meta.EdgesResolver;
import ai.vital.vitalsigns.meta.NodesListComparator;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_Container;

import ai.vital.vitalsigns.model.container.GraphObjectsIterable
import ai.vital.vitalsigns.block.BlockCacheRegistry;

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;


/**
 * Default edges access implementation, looks up objects in global hashtable, it ignores the containers
 *
 */
class GlobalHashTableEdgesResolver implements EdgesResolver {

	private GlobalHashTable ght;
	
	private boolean includeDomains = false
	
    private Map<String, LuceneSegment> ontologyURI2Segment = new HashMap<String, LuceneSegment>();
    
	public GlobalHashTableEdgesResolver(boolean includeDomains, Map<String, LuceneSegment> ontologyURI2Segment) {
		ght = GlobalHashTable.get();
        this.ontologyURI2Segment = ontologyURI2Segment
		this.includeDomains = includeDomains;
	}
	
	public GlobalHashTableEdgesResolver() {
		ght = GlobalHashTable.get();
	}

	private void setDefaultList(List<GraphObjectsIterable> srcList) {
		srcList.add(ght);
		if(includeDomains) {
			srcList.addAll( ontologyURI2Segment.values() )
		}
	}
	
	@Override
	public List<VITAL_Edge> getEdgesForSrcURI(String srcUri,
		VITAL_Container... containers) {
		return getEdgesForSrcURI(srcUri, null, false, containers)
	}


	@Override
	public List<VITAL_Edge> getEdgesForSrcURI(String srcUri, Class<? extends VITAL_Edge>[] edgesFilter, boolean directClass,
		VITAL_Container... containers) {
		return getEdgesForSrcURIAndDestURI(srcUri, null, edgesFilter, directClass, containers) 
	}	
				
	@Override
	public List<VITAL_Edge> getEdgesForSrcURIAndDestURI(String srcUri,
			String destUri, Class[] edgesFilter, boolean directClass,
			VITAL_Container... containers) {
		return getEdgesForSrcURIAndDestURIImpl( srcUri, destUri, true, edgesFilter, directClass, containers)
	}
			
	private List<VITAL_Edge> getEdgesForSrcURIAndDestURIImpl(String srcUri,
			String destUri, boolean forward, Class[] edgesFilter, boolean directClass,
			VITAL_Container... containers) {
			
		List<GraphObjectsIterable> srcList = new ArrayList<GraphObjectsIterable>();	

		if(containers != null && containers.length > 0) {
			
			for(VITAL_Container c : containers) {
				srcList.add(c);
			}
			
		} else {
		
			setDefaultList(srcList)
		
		}
		
		List<LuceneSegment> segments = new ArrayList<LuceneSegment>(ontologyURI2Segment.values())
		
		for(LuceneSegment s : segments) {
			srcList.add(s)
		}
		
		srcList.addAll(BlockCacheRegistry.get().getContainers())

		Set<VITAL_Edge> edges = new HashSet<VITAL_Edge>();
		
		for( GraphObjectsIterable c : srcList ) {
			
			if(c.isEdgeIndexEnabled() ) {

				Set<String> edgesURIs = forward ? c.srcURI2Edge.get(srcUri) : c.destURI2Edge.get(srcUri);
				
				if(edgesURIs != null){
					
					for(String edgeURI : edgesURIs) {
						
						VITAL_Edge e = c.get(edgeURI);
						
						if(e != null && acceptEdge(e, forward, edgesFilter, directClass, destUri)) {
							edges.add(e);
						}
						
					}
					
				}				
				
			} else {
			
				for( Iterator<VITAL_Edge> iter = c.iterator(VITAL_Edge.class, false); iter.hasNext(); ) {
					
					VITAL_Edge edge = iter.next();
					
					if( ( ( forward && srcUri.equals( edge.sourceURI ) ) || (!forward && srcUri.equals(edge.destinationURI ) ) ) && acceptEdge(edge, forward, edgesFilter, directClass, destUri) ) {
						
						edges.add(edge);
						
					}
					
				}
				
			}
			
			
		}
		
		return new ArrayList<VITAL_Edge>(edges);
		
	}

	@Override
	public List<VITAL_Node> getDestNodesForSrcURI(String srcUri,
			Class<? extends VITAL_Edge> edgeType, VITAL_Container... containers) {
		return getEndpointNodesForGivenURI(srcUri, true, edgeType, containers)
	}
	
	private List<VITAL_Node> getEndpointNodesForGivenURI(String srcUri, boolean forward,
				Class<? extends VITAL_Edge> edgeType, VITAL_Container... containers) {
	
		long start = System.currentTimeMillis();
			
		List<GraphObjectsIterable> srcList = new ArrayList<GraphObjectsIterable>();
			
		if(containers != null && containers.length > 0) {
			
			for(VITAL_Container c : containers) {
				srcList.add(c);
			}
				
		} else {
			
			setDefaultList(srcList)
			
		}
		
		List<LuceneSegment> segments = new ArrayList<LuceneSegment>(ontologyURI2Segment.values());
		
		for(LuceneSegment s : segments) {
			srcList.add(s)
		}
		
		srcList.addAll(BlockCacheRegistry.get().getContainers())
		
		Map<String, VITAL_Node> nodes = new HashMap<String, VITAL_Node>();
		
		Map<String, Integer> node2Index = new HashMap<String, Integer>();

		for(GraphObjectsIterable c : srcList) {

			if(c.isEdgeIndexEnabled()) {

				Set<String> edgesURIs = forward ? c.srcURI2Edge.get(srcUri) : c.destURI2Edge.get(srcUri);
				
				if(edgesURIs != null){
					
					for(String edgeURI : edgesURIs) {
						
						VITAL_Edge edge = c.get(edgeURI);
						
						if( edge == null || !edgeType.isInstance(edge)) continue;

						GraphObject node = c.get(forward ? edge.destinationURI : edge.sourceURI);
						
						if(node != null && node instanceof VITAL_Node) {
						
							nodes.put(node.URI, node);
							Integer index = edge.index;
							//if(index == null) { index = Integer.MAX_VALUE; }
							if(index != null) {
								node2Index.put(node.URI, index);
							}
								
						}		
						
					}
					
				}
			} else {
			
				for( Iterator edgesIterator = c.iterator(edgeType, false); edgesIterator.hasNext(); ) {
					
					VITAL_Edge edge = edgesIterator.next();
					
					if( ( forward && edge.sourceURI == srcUri ) || (!forward && edge.destinationURI == srcUri ) ) {
						
						GraphObject node = c.get(forward ? edge.destinationURI : edge.sourceURI);
						
						if(node != null && node instanceof VITAL_Node) {
							nodes.put(node.URI, node);
							Integer index = edge.index;
							//if(index == null) { index = Integer.MAX_VALUE; }
							if(index != null) {
								node2Index.put(node.URI, index);
							}
						}
						
					}
					
					
				}
				
			}	
							
				
		}
		
		
		
		List<VITAL_Node> nodesList = new ArrayList<VITAL_Node>(nodes.values());
		
		Collections.sort(nodesList, new NodesListComparator(node2Index));
		
		long stop = System.currentTimeMillis();
		
		//println("GOT COLLECTION: " + (stop-start) + "ms " + nodesList.size());
		
		return nodesList;
		
	}
			
			
	@Override
	public List<VITAL_Node> getSourceNodesForDestURI(String destUri,
			Class<? extends VITAL_Edge> edgeType, VITAL_Container... containers) {
			
		return getEndpointNodesForGivenURI(destUri, false, edgeType, containers)
		
	}


	@Override
	public void registerObjects(List<GraphObject> res) {
	}

	@Override
	public List<GraphObject> getEdgesWithNodesForSrcURI(String srcUri,
			VITAL_Container... containers) {
			
		List<GraphObjectsIterable> srcList = new ArrayList<GraphObjectsIterable>();
			
		if(containers != null && containers.length > 0) {
			
			for(VITAL_Container c : containers) {
				srcList.add(c);
			}
			
		} else {
		
			setDefaultList(srcList)
		
		}
		
		List<LuceneSegment> segments = new ArrayList<LuceneSegment>( ontologyURI2Segment.values() )
		
		for(LuceneSegment s : segments) {
			srcList.add(s)
		}
		
		srcList.addAll(BlockCacheRegistry.get().getContainers())
		
		Map<String, GraphObject> destMap = new HashMap<String, GraphObject>();
		
		for( GraphObjectsIterable c : srcList ) {

			if(c.isEdgeIndexEnabled()) {

				Set<String> edges = c.srcURI2Edge.get(srcUri);
				
				if(edges != null) {
					
					for(String edgeURI : edges) {
					
						VITAL_Edge edge = c.get(edgeURI);
						
						if(edge != null) {
							
							destMap.put(edge.URI, edge);
							
							GraphObject dest = c.get(edge.destinationURI);
							
							if(dest != null) {
								destMap.put(dest.URI, dest);
							}
							
						}	
						
					}
					
				}
								
			} else {
			
				for( Iterator<VITAL_Edge> iter = c.iterator(VITAL_Edge.class, false); iter.hasNext(); ) {
					
					VITAL_Edge edge = iter.next();
					
					if( srcUri.equals( edge.sourceURI ) ) {
						
						destMap.put(edge.URI, edge);				
						
						GraphObject dest = c.get(edge.destinationURI);
						if(dest != null) {
							destMap.put(dest.URI, dest);
						}
						
					}
					
				}
				
			}
						
			
		}
		
		return new ArrayList<GraphObject>(destMap.values());
		
	}


	@Override
	public List<VITAL_Edge> getEdgesForDestURI(String destUri,
			VITAL_Container... containers) {
		return getEdgesForDestURI(destUri, null, false, containers)
	}
				
	@Override
	public List<VITAL_Edge> getEdgesForDestURI(String destUri,
			Class[] edgesFilter, boolean directClass,
			VITAL_Container... containers) {
		return getEdgesForSrcURIAndDestURIImpl( destUri, null, false, edgesFilter, directClass, containers)
	}
			
	public static boolean acceptEdge(VITAL_Edge edge, boolean forward, Class[] edgesFilter, boolean directClass, String destUri) {
		if(edgesFilter == null) {
			
			if(destUri == null || (forward && destUri == edge.getDestinationURI()) || (!forward && destUri == edge.getSourceURI()) ) {
				return true;
			}
		}
		
		for(Class c : edgesFilter) {
			
			if(directClass && edge.getClass().equals(c)) {
				
				if(destUri == null || (forward && destUri == edge.getDestinationURI()) || (!forward && destUri == edge.getSourceURI()) ) {
					return true;
				}
				
			} else if(c.isAssignableFrom(edge.getClass())) {
				if(destUri == null || (forward && destUri == edge.getDestinationURI()) || (!forward && destUri == edge.getSourceURI()) ) {
					return true;
				}
			} 
			
		}
		
		return false
	}

	
	
	
	
	
	/*
	@Override
	public List<GraphObject> getDestGraphObjectsForSrcURI(String srcUri,
			Class<? extends VITAL_HyperEdge> hyperEdgeType,
			VITAL_Container... containers) {

		long start = System.currentTimeMillis();

		List<GraphObjectsIterable> srcList = new ArrayList<GraphObjectsIterable>();
		
		if(containers != null && containers.length > 0) {
					
			for(VITAL_Container c : containers) {
				srcList.add(c);
			}
				
		} else {
				
			srcList.add(ght);
				
		}
		
		List<LuceneSegment> segments = VitalSigns.get().s();
		
		for(LuceneSegment s : segments) {
			srcList.add(s)
		}
		
		srcList.addAll(BlockCacheRegistry.get().getContainers())
					
		Map<String, GraphObject> nodes = new HashMap<String, GraphObject>();
		
		Map<String, Integer> node2Index = new HashMap<String, Integer>();

		for(GraphObjectsIterable goi : srcList) {

			if(goi.isEdgeIndexEnabled()) {
				
				Set<String> hyperEdgesURIs = goi.srcURI2HyperEdge.get(srcUri);
				
				if(hyperEdgesURIs != null){
					
					for(String edgeURI : hyperEdgesURIs) {
						
						VITAL_HyperEdge hyperEdge = goi.get(edgeURI);
						
						if( hyperEdge == null || !hyperEdgeType.isInstance(hyperEdge)) continue;

						GraphObject node = goi.get(hyperEdge.destinationURI);
						
						if(node != null && node instanceof VITAL_Node) {
						
							nodes.put(node.URI, node);
							Integer index = hyperEdge.index;
							//if(index == null) { index = Integer.MAX_VALUE; }
							if(index != null) {
								node2Index.put(node.URI, index);
							}
								
						}
						
					}
					
				}
				
			} else {
			
				for(GraphObject go : goi.iterator()) {
					
					if(hyperEdgeType.isInstance(go)) {
						
						VITAL_HyperEdge edge = go;
						
						if(edge.sourceURI == srcUri) {
							
							GraphObject node = goi.get(edge.destinationURI);
							
							if(node != null) {
								nodes.put(node.URI, node);
								Integer index = edge.index;
								//if(index == null) { index = Integer.MAX_VALUE; }
								if(index != null) {
									node2Index.put(node.URI, index);
								}
							}
							
						}
						
					}
					
				}
				
			}
						
		}
		
		
		List<GraphObject> objectsList = new ArrayList<GraphObject>(nodes.values());
		
		//Collections.sort(objectsList, new NodesListComparator(node2Index));
		
		long stop = System.currentTimeMillis();
		
		//println("GOT COLLECTION: " + (stop-start) + "ms " + nodesList.size());
		
		return objectsList;
	}

	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForSrcURI(String srcUri,
			VITAL_Container... containers) {
		return getHyperEdgesForEndpointURI(srcUri, true, containers)
	}
			
	private List<VITAL_HyperEdge> getHyperEdgesForEndpointURI(String srcUri, boolean forward,
		VITAL_Container... containers) {
			
		List<GraphObjectsIterable> srcList = new ArrayList<GraphObjectsIterable>();	

		if(containers != null && containers.length > 0) {
			
			for(VITAL_Container c : containers) {
				srcList.add(c);
			}
			
		} else {
		
			srcList.add(ght);
		
		}
		
		List<LuceneSegment> segments = VitalSigns.get().s();
		
		for(LuceneSegment s : segments) {
			srcList.add(s)
		}
		
		srcList.addAll(BlockCacheRegistry.get().getContainers())

		Set<VITAL_HyperEdge> hyperEdges = new HashSet<VITAL_HyperEdge>();
		
		for( GraphObjectsIterable c : srcList ) {
			
			if( c.isEdgeIndexEnabled()) {
				
				Set<String> hyperEdgesURIs = forward ? c.srcURI2HyperEdge.get(srcUri) : c.destURI2HyperEdge.get(srcUri);
				
				if(hyperEdgesURIs != null){
					
					for(String hyperEdgeURI : hyperEdgesURIs) {
						
						VITAL_HyperEdge he = c.get(hyperEdgeURI);
						
						if(he != null) {
							hyperEdges.add(he);
						}
						
					}
					
				}
				
			} else {
			
				for( Iterator<VITAL_HyperEdge> iter = c.iterator(VITAL_HyperEdge.class, false); iter.hasNext(); ) {
					
					VITAL_HyperEdge hEdge = iter.next();
					
					if( ( forward && srcUri.equals( hEdge.sourceURI ) ) || ( !forward && srcUri.equals( hEdge.destinationURI ) ) ) {
						
						hyperEdges.add(hEdge);
						
					}
					
				}
				
			}
			
			
		}
		
		return new ArrayList<VITAL_HyperEdge>(hyperEdges);
		
	}

	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForDestURI(String destUri,
			VITAL_Container... containers) {

		return 	getHyperEdgesForEndpointURI(destUri, false, containers)

	}

	*/
	
	
	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForSrcURI(String srcUri,
		VITAL_Container... containers) {
		return getHyperEdgesForSrcURI(srcUri, null, false, containers)
	}


	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForSrcURI(String srcUri, Class<? extends VITAL_HyperEdge>[] hyperEdgesFilter, boolean directClass,
		VITAL_Container... containers) {
		return getHyperEdgesForSrcURIAndDestURI(srcUri, null, hyperEdgesFilter, directClass, containers)
	}
				
	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForSrcURIAndDestURI(String srcUri,
			String destUri, Class[] hyperEdgesFilter, boolean directClass,
			VITAL_Container... containers) {
		return getHyperEdgesForSrcURIAndDestURIImpl( srcUri, destUri, true, hyperEdgesFilter, directClass, containers)
	}
			
	private List<VITAL_HyperEdge> getHyperEdgesForSrcURIAndDestURIImpl(String srcUri,
			String destUri, boolean forward, Class[] hyperEdgesFilter, boolean directClass,
			VITAL_Container... containers) {
			
		List<GraphObjectsIterable> srcList = new ArrayList<GraphObjectsIterable>();

		if(containers != null && containers.length > 0) {
			
			for(VITAL_Container c : containers) {
				srcList.add(c);
			}
			
		} else {
		
			setDefaultList(srcList)
		
		}
		
		List<LuceneSegment> segments = new ArrayList<LuceneSegment>(ontologyURI2Segment.values())
		
		for(LuceneSegment s : segments) {
			srcList.add(s)
		}
		
		srcList.addAll(BlockCacheRegistry.get().getContainers())

		Set<VITAL_HyperEdge> hyperEdges = new HashSet<VITAL_HyperEdge>();
		
		for( GraphObjectsIterable c : srcList ) {
			
			if(c.isEdgeIndexEnabled() ) {

				Set<String> edgesURIs = forward ? c.srcURI2HyperEdge.get(srcUri) : c.destURI2HyperEdge.get(srcUri);
				
				if(edgesURIs != null){
					
					for(String edgeURI : edgesURIs) {
						
						VITAL_HyperEdge e = c.get(edgeURI);
						
						if(e != null && acceptHyperEdge(e, forward, hyperEdgesFilter, directClass, destUri)) {
							hyperEdges.add(e);
						}
						
					}
					
				}
				
			} else {
			
				for( Iterator<VITAL_HyperEdge> iter = c.iterator(VITAL_HyperEdge.class, false); iter.hasNext(); ) {
					
					VITAL_HyperEdge edge = iter.next();
					
					if( ( ( forward && srcUri.equals( edge.sourceURI ) ) || (!forward && srcUri.equals(edge.destinationURI ) ) ) && acceptHyperEdge(edge, forward, hyperEdgesFilter, directClass, destUri) ) {
						
						hyperEdges.add(edge);
						
					}
					
				}
				
			}
			
			
		}
		
		return new ArrayList<VITAL_HyperEdge>(hyperEdges);
		
	}

	@Override
	public List<GraphObject> getDestGraphObjectsForSrcURI(String srcUri,
			Class<? extends VITAL_HyperEdge> hyperEdgeType, VITAL_Container... containers) {
		return getEndpointGraphObjectsForGivenURI(srcUri, true, hyperEdgeType, containers)
	}
	
	private List<GraphObject> getEndpointGraphObjectsForGivenURI(String srcUri, boolean forward,
				Class<? extends VITAL_HyperEdge> hyperEdgeType, VITAL_Container... containers) {
	
		long start = System.currentTimeMillis();
			
		List<GraphObjectsIterable> srcList = new ArrayList<GraphObjectsIterable>();
			
		if(containers != null && containers.length > 0) {
			
			for(VITAL_Container c : containers) {
				srcList.add(c);
			}
				
		} else {
			
			setDefaultList(srcList)
			
		}
		
		List<LuceneSegment> segments = new ArrayList<LuceneSegment>(ontologyURI2Segment.values())
		
		for(LuceneSegment s : segments) {
			srcList.add(s)
		}
		
		srcList.addAll(BlockCacheRegistry.get().getContainers())
		
		Map<String, GraphObject> targets = new HashMap<String, GraphObject>();
		
		Map<String, Integer> target2Index = new HashMap<String, Integer>();

		for(GraphObjectsIterable c : srcList) {

			if(c.isEdgeIndexEnabled()) {

				Set<String> hyperEdgesURIs = forward ? c.srcURI2HyperEdge.get(srcUri) : c.destURI2HyperEdge.get(srcUri);
				
				if(hyperEdgesURIs != null){
					
					for(String hyperEdgeURI : hyperEdgesURIs) {
						
						VITAL_HyperEdge hyperEdge = c.get(hyperEdgeURI);
						
						if( hyperEdge == null || !hyperEdgeType.isInstance(hyperEdge)) continue;

						GraphObject target = c.get(forward ? hyperEdge.destinationURI : hyperEdge.sourceURI);
						
						if(target != null) {
						
							targets.put(target.URI, target);
							Integer index = hyperEdge.index;
							//if(index == null) { index = Integer.MAX_VALUE; }
							if(index != null) {
								target2Index.put(target.URI, index);
							}
								
						}
						
					}
					
				}
			} else {
			
				for( Iterator hyperEdgesIterator = c.iterator(hyperEdgeType, false); hyperEdgesIterator.hasNext(); ) {
					
					VITAL_HyperEdge hyperEdge = hyperEdgesIterator.next();
					
					if( ( forward && hyperEdge.sourceURI == srcUri ) || (!forward && hyperEdge.destinationURI == srcUri ) ) {
						
						GraphObject target = c.get(forward ? hyperEdge.destinationURI : hyperEdge.sourceURI);
						
						if(target != null) {
							targets.put(target.URI, target);
							Integer index = hyperEdge.index;
							//if(index == null) { index = Integer.MAX_VALUE; }
							if(index != null) {
								target2Index.put(target.URI, index);
							}
						}
						
					}
					
					
				}
				
			}
							
				
		}
		
		
		
		List<GraphObject> targetsList = new ArrayList<GraphObject>(targets.values());
		
		Collections.sort(targetsList, new NodesListComparator(target2Index));
		
		long stop = System.currentTimeMillis();
		
		//println("GOT COLLECTION: " + (stop-start) + "ms " + nodesList.size());
		
		return targetsList;
		
	}
			
			
	@Override
	public List<GraphObject> getSourceGraphObjectsForDestURI(String destUri,
			Class<? extends VITAL_HyperEdge> edgeType, VITAL_Container... containers) {
			
		return getEndpointGraphObjectsForGivenURI(destUri, false, edgeType, containers)
		
	}


	@Override
	public List<GraphObject> getHyperEdgesWithGraphObjectsForSrcURI(String srcUri,
			VITAL_Container... containers) {
			
		List<GraphObjectsIterable> srcList = new ArrayList<GraphObjectsIterable>();
			
		if(containers != null && containers.length > 0) {
			
			for(VITAL_Container c : containers) {
				srcList.add(c);
			}
			
		} else {
		
			setDefaultList(srcList)
		
		}
		
		List<LuceneSegment> segments = new ArrayList<LuceneSegment>(ontologyURI2Segment.values())
		
		for(LuceneSegment s : segments) {
			srcList.add(s)
		}
		
		srcList.addAll(BlockCacheRegistry.get().getContainers())
		
		Map<String, GraphObject> destMap = new HashMap<String, GraphObject>();
		
		for( GraphObjectsIterable c : srcList ) {

			if(c.isEdgeIndexEnabled()) {

				Set<String> hyperEdges = c.srcURI2HyperEdge.get(srcUri);
				
				if(hyperEdges != null) {
					
					for(String hyperEdgeURI : hyperEdges) {
					
						VITAL_HyperEdge hyperEdge = c.get(hyperEdgeURI);
						
						if(hyperEdge != null) {
							
							destMap.put(hyperEdge.URI, hyperEdge);
							
							GraphObject dest = c.get(hyperEdge.destinationURI);
							
							if(dest != null) {
								destMap.put(dest.URI, dest);
							}
							
						}
						
					}
					
				}
								
			} else {
			
				for( Iterator<VITAL_HyperEdge> iter = c.iterator(VITAL_HyperEdge.class, false); iter.hasNext(); ) {
					
					VITAL_HyperEdge hyperEdge = iter.next();
					
					if( srcUri.equals( hyperEdge.sourceURI ) ) {
						
						destMap.put(hyperEdge.URI, hyperEdge);
						
						GraphObject dest = c.get(hyperEdge.destinationURI);
						if(dest != null) {
							destMap.put(dest.URI, dest);
						}
						
					}
					
				}
				
			}
						
			
		}
		
		return new ArrayList<GraphObject>(destMap.values());
		
	}


	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForDestURI(String destUri,
			VITAL_Container... containers) {
		return getHyperEdgesForDestURI(destUri, null, false, containers)
	}
				
	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForDestURI(String destUri,
			Class[] hyperEdgesFilter, boolean directClass,
			VITAL_Container... containers) {
		return getHyperEdgesForSrcURIAndDestURIImpl( destUri, null, false, hyperEdgesFilter, directClass, containers)
	}
			
	public static boolean acceptHyperEdge(VITAL_HyperEdge hyperEdge, boolean forward, Class[] hyperEdgesFilter, boolean directClass, String destUri) {
		if(hyperEdgesFilter == null) {
			
			if(destUri == null || (forward && destUri == hyperEdge.getDestinationURI()) || (!forward && destUri == hyperEdge.getSourceURI()) ) {
				return true;
			}
		}
		
		for(Class c : hyperEdgesFilter) {
			
			if(directClass && hyperEdge.getClass().equals(c)) {
				
				if(destUri == null || (forward && destUri == hyperEdge.getDestinationURI()) || (!forward && destUri == hyperEdge.getSourceURI()) ) {
					return true;
				}
				
			} else if(c.isAssignableFrom(hyperEdge.getClass())) {
				if(destUri == null || (forward && destUri == hyperEdge.getDestinationURI()) || (!forward && destUri == hyperEdge.getSourceURI()) ) {
					return true;
				}
			}
			
		}
		
		return false
	}
}
