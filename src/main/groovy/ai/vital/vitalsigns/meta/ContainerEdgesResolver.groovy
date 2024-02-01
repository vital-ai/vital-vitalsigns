package ai.vital.vitalsigns.meta


import ai.vital.vitalsigns.global.GlobalHashTableEdgesResolver;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_Container;
import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;


import ai.vital.vitalsigns.model.container.GraphObjectsIterable;

public class ContainerEdgesResolver implements EdgesResolver {

	@Override
	public List<VITAL_Edge> getEdgesForSrcURI(String srcUri,
			VITAL_Container... containers) {
		return getEdgesForSrcURI(srcUri, null, false, containers);
	}
			
	@Override
	public List<VITAL_Edge> getEdgesForSrcURI(String srcUri,
			Class[] edgesFilter, boolean directClass,
			VITAL_Container... containers) {
		return getEdgesForSrcURIAndDestURI(srcUri, null, edgesFilter, directClass,containers)
	}
	
	@Override
	public List<VITAL_Edge> getEdgesForSrcURIAndDestURI(String srcUri,
			String destUri, Class[] edgesFilter, boolean directClass,
			VITAL_Container... containers) {
			
		if(containers == null || containers.length < 1) {
			throw new RuntimeException("Cannot use container resolver without containers.");
		}
		
		List<VITAL_Edge> edges = new ArrayList<VITAL_Edge>();
		
		for( GraphObjectsIterable c : containers ) {
			
			for( Iterator<VITAL_Edge> iter = c.iterator(VITAL_Edge.class, false); iter.hasNext(); ) {
				
				VITAL_Edge edge = iter.next();
						
				if( srcUri.equals( edge.sourceURI ) && GlobalHashTableEdgesResolver.acceptEdge(edge, true, edgesFilter, directClass, destUri)) {
									
					edges.add(edge);
									
				}
								
			}
			
		}
				
			
		return edges;
	}
			
	@Override
	public List<VITAL_Edge> getEdgesForDestURI(String destUri,
			VITAL_Container... containers) {
		return getEdgesForDestURI(destUri, null, false, containers);
	}
			
	@Override
	public List<VITAL_Edge> getEdgesForDestURI(String destUri,
			Class[] edgesFilter, boolean directClass,
			VITAL_Container... containers) {
		
		if(containers == null || containers.length < 1) {
			throw new RuntimeException("Cannot use container resolver without containers.");
		}
		
		List<VITAL_Edge> edges = new ArrayList<VITAL_Edge>();
		
		for( GraphObjectsIterable c : containers ) {
			
			for( Iterator<VITAL_Edge> iter = c.iterator(VITAL_Edge.class, false); iter.hasNext(); ) {
				
				VITAL_Edge edge = iter.next();
				
				if( destUri.equals( edge.destinationURI ) && GlobalHashTableEdgesResolver.acceptEdge(edge, false, edgesFilter, directClass, null)) {
					
					edges.add(edge);
					
				}
				
			}
			
		}
		
		
		return edges;
	}

	@Override
	public List<GraphObject> getEdgesWithNodesForSrcURI(String srcUri,
			VITAL_Container... containers) {
		if(containers == null || containers.length < 1) {
			throw new RuntimeException("Cannot use container resolver without containers.");
		}
		
		Map<String, GraphObject> destMap = new HashMap<String, GraphObject>();
	
		for( GraphObjectsIterable c : containers ) {
		
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
			
		
		return new ArrayList<GraphObject>(destMap.values());
	}

	@Override
	public List<VITAL_Node> getDestNodesForSrcURI(String srcUri,
			Class<? extends VITAL_Edge> edgeType, VITAL_Container... containers) {
		if(containers == null || containers.length < 1) {
			throw new RuntimeException("Cannot use container resolver without containers.");
		}
		
		
		
		Map<String, VITAL_Node> nodes = new HashMap<String, VITAL_Node>();
	
		Map<String, Integer> node2Index = new HashMap<String, Integer>();

		for(GraphObjectsIterable c : containers) {
			
			for( Iterator edgesIterator = c.iterator(edgeType, false); edgesIterator.hasNext(); ) {
				
				VITAL_Edge edge = edgesIterator.next();
				
				if(edge.sourceURI == srcUri) {
					
					GraphObject node = c.get(edge.destinationURI);
					
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
	
		List<VITAL_Node> nodesList = new ArrayList<VITAL_Node>(nodes.values());
	
		Collections.sort(nodesList, new NodesListComparator(node2Index));
	
	
	
		return nodesList;
		
	}
			
	@Override
	public List<VITAL_Node> getSourceNodesForDestURI(String destUri,
			Class<? extends VITAL_Edge> edgeType, VITAL_Container... containers) {
		if(containers == null || containers.length < 1) {
			throw new RuntimeException("Cannot use container resolver without containers.");
		}
		
		
		
		Map<String, VITAL_Node> nodes = new HashMap<String, VITAL_Node>();
		
		Map<String, Integer> node2Index = new HashMap<String, Integer>();
		
		for(GraphObjectsIterable c : containers) {
			
			for( Iterator edgesIterator = c.iterator(edgeType, false); edgesIterator.hasNext(); ) {
				
				VITAL_Edge edge = edgesIterator.next();
				
				if(edge.destinationURI == destUri) {
					
					GraphObject node = c.get(edge.sourceURI);
					
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
		
		List<VITAL_Node> nodesList = new ArrayList<VITAL_Node>(nodes.values());
		
		Collections.sort(nodesList, new NodesListComparator(node2Index));
		
		return nodesList;
		
	}

	@Override
	public void registerObjects(List<GraphObject> res) {

	}


	
	//hyper nodes - copy of edge methods 

	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForSrcURI(String srcUri,
			VITAL_Container... containers) {
		return getHyperEdgesForSrcURI(srcUri, null, false, containers);
	}
			
	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForSrcURI(String srcUri,
			Class[] hyperEdgesFilter, boolean directClass,
			VITAL_Container... containers) {
		return getHyperEdgesForSrcURIAndDestURI(srcUri, null, hyperEdgesFilter, directClass, containers)
	}
	
	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForSrcURIAndDestURI(String srcUri,
			String destUri, Class[] hyperEdgesFilter, boolean directClass,
			VITAL_Container... containers) {
			
		if(containers == null || containers.length < 1) {
			throw new RuntimeException("Cannot use container resolver without containers.");
		}
		
		List<VITAL_Edge> hyperEdges = new ArrayList<VITAL_Edge>();
		
		for( GraphObjectsIterable c : containers ) {
			
			for( Iterator<VITAL_HyperEdge> iter = c.iterator(VITAL_HyperEdge.class, false); iter.hasNext(); ) {
				
				VITAL_HyperEdge hyperEdge = iter.next();
						
				if( srcUri.equals( hyperEdge.sourceURI ) && GlobalHashTableEdgesResolver.acceptHyperEdge(hyperEdge, true, hyperEdgesFilter, directClass, destUri)) {
									
					hyperEdges.add(hyperEdge);
									
				}
								
			}
			
		}
				
			
		return hyperEdges;
	}
			
	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForDestURI(String destUri,
			VITAL_Container... containers) {
		return getHyperEdgesForDestURI(destUri, null, false, containers);
	}
			
	@Override
	public List<VITAL_HyperEdge> getHyperEdgesForDestURI(String destUri,
			Class[] hyperEdgesFilter, boolean directClass,
			VITAL_Container... containers) {
		
		if(containers == null || containers.length < 1) {
			throw new RuntimeException("Cannot use container resolver without containers.");
		}
		
		List<VITAL_HyperEdge> hyperEdges = new ArrayList<VITAL_HyperEdge>();
		
		for( GraphObjectsIterable c : containers ) {
			
			for( Iterator<VITAL_HyperEdge> iter = c.iterator(VITAL_HyperEdge.class, false); iter.hasNext(); ) {
				
				VITAL_HyperEdge hyperEdge = iter.next();
				
				if( destUri.equals( hyperEdge.destinationURI ) && GlobalHashTableEdgesResolver.acceptHyperEdge(hyperEdge, false, hyperEdgesFilter, directClass, null)) {
					
					hyperEdges.add(hyperEdge);
					
				}
				
			}
			
		}
		
		
		return hyperEdges;
	}

	@Override
	public List<GraphObject> getHyperEdgesWithGraphObjectsForSrcURI(String srcUri,
			VITAL_Container... containers) {
		if(containers == null || containers.length < 1) {
			throw new RuntimeException("Cannot use container resolver without containers.");
		}
		
		Map<String, GraphObject> destMap = new HashMap<String, GraphObject>();
	
		for( GraphObjectsIterable c : containers ) {
		
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
			
		
		return new ArrayList<GraphObject>(destMap.values());
	}

	@Override
	public List<GraphObject> getDestGraphObjectsForSrcURI(String srcUri,
			Class<? extends VITAL_HyperEdge> hyperEdgeType, VITAL_Container... containers) {
		if(containers == null || containers.length < 1) {
			throw new RuntimeException("Cannot use container resolver without containers.");
		}
		
		
		
		Map<String, GraphObject> targets = new HashMap<String, GraphObject>();
	
		Map<String, Integer> target2Index = new HashMap<String, Integer>();

		for(GraphObjectsIterable c : containers) {
			
			for( Iterator hyperEdgesIterator = c.iterator(hyperEdgeType, false); hyperEdgesIterator.hasNext(); ) {
				
				VITAL_HyperEdge hyperEdge = hyperEdgesIterator.next();
				
				if(hyperEdge.sourceURI == srcUri) {
					
					GraphObject node = c.get(hyperEdge.destinationURI);
					
					if(node != null) {
						targets.put(node.URI, node);
						Integer index = hyperEdge.index;
						//if(index == null) { index = Integer.MAX_VALUE; }
						if(index != null) {
							target2Index.put(node.URI, index);
						}
					}
					
				}
				
				
			}
			
		}
	
		List<GraphObject> targetsList = new ArrayList<GraphObject>(targets.values());
	
		Collections.sort(targetsList, new NodesListComparator(target2Index));
	
	
	
		return targetsList;
		
	}
			
	@Override
	public List<GraphObject> getSourceGraphObjectsForDestURI(String destUri,
			Class<? extends VITAL_HyperEdge> hyperEdgeType, VITAL_Container... containers) {
		if(containers == null || containers.length < 1) {
			throw new RuntimeException("Cannot use container resolver without containers.");
		}
		
		
		
		Map<String, GraphObject> targets = new HashMap<String, GraphObject>();
		
		Map<String, Integer> target2Index = new HashMap<String, Integer>();
		
		for(GraphObjectsIterable c : containers) {
			
			for( Iterator edgesIterator = c.iterator(hyperEdgeType, false); edgesIterator.hasNext(); ) {
				
				VITAL_HyperEdge hyperEdge = edgesIterator.next();
				
				if(hyperEdge.destinationURI == destUri) {
					
					GraphObject node = c.get(hyperEdge.sourceURI);
					
					if(node != null) {
						targets.put(node.URI, node);
						Integer index = hyperEdge.index;
						//if(index == null) { index = Integer.MAX_VALUE; }
						if(index != null) {
							target2Index.put(node.URI, index);
						}
					}
					
				}
				
				
			}
			
		}
		
		List<GraphObject> targetsList = new ArrayList<GraphObject>(targets.values());
		
		Collections.sort(targetsList, new NodesListComparator(target2Index));
		
		return targetsList;
		
	}

}
