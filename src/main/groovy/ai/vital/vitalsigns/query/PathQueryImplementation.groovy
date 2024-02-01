package ai.vital.vitalsigns.query


import java.util.Map.Entry

import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.query.Connector;
import ai.vital.vitalservice.query.Destination;
import ai.vital.vitalservice.query.GraphElement;
import ai.vital.vitalservice.query.QueryContainerType;
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.Source;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalservice.query.VitalGraphArcElement;
import ai.vital.vitalservice.query.VitalGraphBooleanContainer;
import ai.vital.vitalservice.query.VitalGraphCriteriaContainer;
import ai.vital.vitalservice.query.VitalGraphQueryContainer;
import ai.vital.vitalservice.query.VitalGraphQueryElement;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalservice.query.VitalGraphQueryTypeCriterion;
import ai.vital.vitalservice.query.VitalPathQuery;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.model.GraphObject;

import ai.vital.vitalsigns.model.VITAL_Edge_PropertiesHelper;

import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalOrganization;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.properties.PropertyMetadata;

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;


public class PathQueryImplementation {

	private VitalPathQuery pathQuery;

	private List<String> rootURIs = new ArrayList<String>();
	
	private LinkedHashMap<String, GraphObject> countedResults = new LinkedHashMap<String, GraphObject>();
	private LinkedHashMap<String, GraphObject> nonCounterResults = new LinkedHashMap<String, GraphObject>();

	private Map<String, List<VITAL_Edge>> result2Edges = new HashMap<String, List<VITAL_Edge>>();
	
	private int nodesCounter = 0;
	
	private boolean acceptCountableNodes = true;
	
	List<List<VitalSelectQuery>> containers = new ArrayList<List<VitalSelectQuery>>();
	
	private Integer maxNodesToGet = null;
	
	private PathQueryExecutor executor;
	
	//also remember the position of this counted node
    private Map<String, Integer> countTheseNodes = new HashMap<String, Integer>();
    
	public static abstract class PathQueryExecutor {
		
		protected VitalOrganization organization;
		protected VitalApp app;
		public PathQueryExecutor(VitalOrganization organization, VitalApp app) {
			super();
			this.organization = organization;
			this.app = app;
		}
		
		public abstract ResultList get(List<URIProperty> rootURIs) throws VitalServiceUnimplementedException, VitalServiceException;
		
		public abstract ResultList query(VitalSelectQuery rootSelect) throws VitalServiceUnimplementedException, VitalServiceException ;
		
	}
	
	
	public PathQueryImplementation(VitalPathQuery pathQuery, PathQueryExecutor executor) {
		this.pathQuery = pathQuery;
		this.executor = executor;
	}
	
	private void ex(String m) { throw new RuntimeException(m); }

	
	private boolean countNode() {
	    
	    nodesCounter++;
	    
	    if(maxNodesToGet != null && nodesCounter >= maxNodesToGet.intValue()) {
	        acceptCountableNodes = false;
	        return true;
	    }
	    
	    return false;
	    
	}
	
	static Comparator<GraphObject> uriComparator = new Comparator<GraphObject>(){

        @Override
        public int compare(GraphObject arg0, GraphObject arg1) {
            return arg0.getURI().compareTo(arg1.getURI());
        }
	    
	};
	
	
	/*
	public static class PathInfo {
		VitalSelectQuery nodesQuery;
		VitalSelectQuery edgesQuery;
		boolean forwardNotReverse;
	}
	*/
	
	public ResultList execute() throws VitalServiceException, VitalServiceUnimplementedException {
		
	    if(pathQuery.getArcs().size() < 1) ex("No arcs set in a path query");
		
		if(pathQuery.getSegments().size() < 1) ex("No segments in a path query");
		
		
		if(!pathQuery.isProjectionOnly()) {
	            
            int offset = pathQuery.getOffset();
            if(offset < 0) offset = 0;
            int limit = pathQuery.getLimit();

            if(limit > 0) {
                maxNodesToGet = offset + limit;
            }
            
        }
		
		//each path is expanded independently
		for(VitalGraphArcContainer arc : pathQuery.getArcs()) {
			
			List<WrappedContainer> splitArc = splitArc(arc);
			WrappedContainer nodesContainer = splitArc.get(0);
			WrappedContainer edgesContainer = splitArc.get(1);
			
//			if(nodesContainer.nodesCriteria < 1) ex("No node constraints found in an ARC");
			if(edgesContainer.edgesCriteria < 1) ex("No edge constraints found in an ARC");
			
			//top arcs?
			VitalSelectQuery sq1 = null;
			
			//copy countArc to all select query container
			
			if(nodesContainer.nodesCriteria > 1) {
				sq1 = new VitalSelectQuery();
				sq1.setLimit(10000);
				sq1.setOffset(0);
				sq1.setSegments(pathQuery.getSegments());
				
				VitalGraphArcContainer c = new VitalGraphArcContainer(QueryContainerType.and, arc.getArc());
				c.add(nodesContainer.container);
				sq1.setTopContainer(c);
				sq1.getTopContainer().setCountArc(arc.getCountArc());
			}
			
			VitalSelectQuery sq2 = new VitalSelectQuery();
			sq2.setLimit(10000);
			sq2.setOffset(0);
			sq2.setSegments(pathQuery.getSegments());
			VitalGraphArcContainer c2 = new VitalGraphArcContainer(QueryContainerType.and, arc.getArc());
			c2.add(edgesContainer.container);
			sq2.setTopContainer(c2);
			sq2.getTopContainer().setCountArc(arc.getCountArc());
			
			containers.add(Arrays.asList(sq1, sq2));
			
		}
		
		if(pathQuery.getRootArc() != null && pathQuery.getRootURIs() != null) ex("cannot use both root arc and rooturis at the same time");
		
		if(pathQuery.getRootArc() == null && ( pathQuery.getRootURIs() == null || pathQuery.getRootURIs().size() < 1)) ex("Expected either root arc or uris list");
		
		Map<String, VITAL_Node> rootURI2Object = new HashMap<String, VITAL_Node>();
		
		boolean countRootNodes = true;
		
		if(pathQuery.getRootURIs() != null) {
			
		    countRootNodes = pathQuery.getCountRoot();
		    
		    List<GraphObject> list = new ArrayList<GraphObject>();
		    for(GraphObject g : executor.get(pathQuery.getRootURIs())) {
		        list.add(g);
		    }
		    Collections.sort(list, uriComparator);
			
			for(GraphObject g : list) {
				
			    if(!(g instanceof VITAL_Node)) ex("Not a vital node in root objects " + g.getClass().getCanonicalName()); 
			    
				rootURIs.add(g.getURI());
				rootURI2Object.put(g.getURI(), (VITAL_Node) g);
				
			}
			
			
		} else {
		    
		    countRootNodes = pathQuery.getRootArc().getCountArc();
		    
			List<WrappedContainer> splitArc = splitArc(pathQuery.getRootArc());
			
			WrappedContainer nodesContainer = splitArc.get(0);
			WrappedContainer edgesContainer = splitArc.get(1);
			
			if(nodesContainer.nodesCriteria < 1) ex("No node constraints found in ROOT");
			if(edgesContainer.edgesCriteria > 0) ex("ROOT arc must not have edge constraints");
			
			
			//execute root select query
			VitalSelectQuery rootSelect = new VitalSelectQuery();
			rootSelect.setSegments(pathQuery.getSegments());
			rootSelect.setOffset(0);
			rootSelect.setLimit(10000);
//			rootSelect.setPayloads(false);
//			rootSelect.s
			rootSelect.setTopContainer(pathQuery.getRootArc());
			
			List<GraphObject> list = new ArrayList<GraphObject>();
			for(GraphObject g : executor.query(rootSelect)) {
			    list.add(g);
			}
			Collections.sort(list, uriComparator);
			
			for(GraphObject go : list) {
				
			    if(!(go instanceof VITAL_Node)) {
			        ex("Root elements may only be nodes");
			    }
			    
			    
				rootURIs.add(go.getURI());
				rootURI2Object.put(go.getURI(), (VITAL_Node) go);
				
			}
			
		}
		
		if(rootURIs.size() == 0) {
		    doReturn();
		}
		
		//sort root uris by natural order
		Collections.sort(rootURIs);
		
		for(String rootURI : rootURIs) {
		    
		    VITAL_Node g = rootURI2Object.get(rootURI);
		    
		    
		    if(countRootNodes) {
		        
		        countedResults.put(g.getURI(), g);
		        
		        if(!countTheseNodes.containsKey(g.getURI())) {
		            countTheseNodes.put(g.getURI(), countTheseNodes.size());
		        }
		        
		        if( countNode() ) {
		            return doReturn();
		        }
		        
		    } else {
		        nonCounterResults.put(g.getURI(),  g);
		    }
		    
		}
		
		if(countedResults.size() < 1 && nonCounterResults.size() < 1) {
			return doReturn();
		}
		
		
		processPaths();
			
		return doReturn();
		
	}


	private void filterGraphElementSymbol(VitalSelectQuery sq1) {
		
		VitalGraphArcContainer topContainer = sq1.getTopContainer();
		
		topContainer.setArc(new VitalGraphArcElement(Source.CURRENT, Connector.EMPTY, Destination.EMPTY));
		
		for(VitalGraphQueryContainer<?> c : topContainer) {
			
			if(c instanceof VitalGraphCriteriaContainer) {
				
				filterCC((VitalGraphCriteriaContainer) c);
				
			} else if(c instanceof VitalGraphQueryPropertyCriterion){
				
				VitalGraphQueryPropertyCriterion pc = (VitalGraphQueryPropertyCriterion) c;
				pc.setSymbol(GraphElement.Source);
				
			} else {
				throw new RuntimeException("Unexpected child of a top arc container of a select query: " + c);
			}
			
		}
		
	}

	private void filterCC(VitalGraphCriteriaContainer c) {
		
		for(VitalGraphQueryElement el : c) {
			if(el instanceof VitalGraphCriteriaContainer) {
				filterCC((VitalGraphCriteriaContainer) el);
			} else if( el instanceof VitalGraphQueryPropertyCriterion) {
				VitalGraphQueryPropertyCriterion pc = (VitalGraphQueryPropertyCriterion) el;
				pc.setSymbol(GraphElement.Source);
				
			} else {
				throw new RuntimeException("Unexpected child of a criteria container in a select query: " + el);
			}
		}
		
	}

	private void processPaths() throws VitalServiceException, VitalServiceUnimplementedException {

		Set<String> currentRootURIs = new HashSet<String>(rootURIs);
		
		int currentDepth = 1;
		
		for( ; (pathQuery.getMaxdepth() < 1 || currentDepth <= pathQuery.getMaxdepth()) && currentRootURIs.size() > 0 ; currentDepth++ ) {

			Map<String, VITAL_Node> currentDepthNodes = new HashMap<String, VITAL_Node>();
			
			Map<String, List<VITAL_Edge>> node2Edges = new HashMap<String, List<VITAL_Edge>>(); 
			
			for(List<VitalSelectQuery> pair : containers) {
			
				//first select edges
				VitalSelectQuery edgesQ = null;
				try {
					edgesQ = (VitalSelectQuery) pair.get(1).clone();
				} catch (CloneNotSupportedException e) {
					ex(e.getLocalizedMessage());
				}
				
				boolean countArc = edgesQ.getTopContainer().getCountArc();
	
				VitalGraphCriteriaContainer cc = edgesQ.getCriteriaContainer();
				
				VITAL_Edge_PropertiesHelper h = new VITAL_Edge_PropertiesHelper();
				
				boolean forward = edgesQ.getTopContainer().getArc().source == Source.PARENT_SOURCE;
				
				VitalGraphQueryPropertyCriterion pc = forward ? h.getEdgeSource() : h.getEdgeDestination();
				
				List<URIProperty> uris = new ArrayList<URIProperty>();
				for(String root : currentRootURIs) {
					uris.add(new URIProperty(root));
				}
				pc.oneOf(uris);
				
				cc.add(pc);
				
				Set<String> newRootsCandidates = new HashSet<String>();

				filterGraphElementSymbol(edgesQ);
				
				Map<String, VITAL_Edge> thisContainerEdges = new HashMap<String, VITAL_Edge>();
				
				for(GraphObject g : executor.query(edgesQ)) {
					
					if(g instanceof VITAL_Edge) {
						VITAL_Edge e = (VITAL_Edge) g;
						
						String targetURI = e.getDestinationURI();
						if(forward) {
						    targetURI = e.getDestinationURI();
						} else {
						    targetURI = e.getSourceURI();
						}
						
						thisContainerEdges.put(targetURI, e);
						
						newRootsCandidates.add(targetURI);
						List<VITAL_Edge> edges = node2Edges.get(targetURI);
						if(edges == null) {
						    edges = new LinkedList<VITAL_Edge>();
						    node2Edges.put(targetURI, edges);
						}
						edges.add(e);
						
//						if(!results.containsKey(e.getURI())) {
//							results.put(e.getURI(), e);
//						}
						
					} else {
						ex("Expected only edges");
					}
					
				}
				
				if(newRootsCandidates.size() < 1) continue;
				
				if(pair.get(0) != null) {
					
					//do select nodes to verify the input candidates
					VitalSelectQuery nodesQ = null;
					try {
						nodesQ = (VitalSelectQuery) pair.get(0).clone();
					} catch(Exception e) {
						ex(e.getLocalizedMessage());
					}
					
					VitalGraphCriteriaContainer nc = nodesQ.getCriteriaContainer();
					
					List<URIProperty> nodeUris = new ArrayList<URIProperty>();
					for(String u : newRootsCandidates) {
						nodeUris.add(new URIProperty(u));
					}
					
					VitalGraphQueryPropertyCriterion uc = new VitalGraphQueryPropertyCriterion(VitalGraphQueryPropertyCriterion.URI);
					uc.oneOf(nodeUris);
					nc.add(uc);
					
					filterGraphElementSymbol(nodesQ);
					
					//always sort results
					List<GraphObject> nodes = new ArrayList<GraphObject>();
					for(GraphObject g : executor.query(nodesQ)) {
					    nodes.add(g);
					}
					Collections.sort(nodes, uriComparator);
					
					for(GraphObject g : nodes) {
						
						if(g instanceof VITAL_Node) {
							
							if(!countedResults.containsKey(g.getURI()) && !nonCounterResults.containsKey(g.getURI())) {
                                if(countArc) {
                                    if(!countTheseNodes.containsKey(g.getURI())) {
                                        countTheseNodes.put(g.getURI(), countTheseNodes.size());
                                    }
                                } else {
                                    //make sure the parent for this node is either a non-counting node or a counting node that's also in results page
                                    VITAL_Edge edge = thisContainerEdges.get(g.getURI());
                                    String parentURI = forward ? edge.getSourceURI() : edge.getDestinationURI();
                                    if(countTheseNodes.containsKey(parentURI)) {
                                        Integer thisOffset = countTheseNodes.get(parentURI);
                                        if(thisOffset < pathQuery.getOffset()) {
//                                            System.out.println("skipped 1: " + g.getURI());
                                            continue;
                                        }
                                    } 
                                }
                                currentDepthNodes.put(g.getURI(), (VITAL_Node) g);
//								results.put(g.getURI(), g);
								//detect loops, only new objects added as new roots
							}
							
						} else {
							
							ex("expected vital nodes only in target select query results");
							
						}
						
					}
					
				} else {
					
					List<URIProperty> targetUris = new ArrayList<URIProperty>(newRootsCandidates.size());
					for(String u : newRootsCandidates) {
						targetUris.add(URIProperty.withString(u));
					}
					
					//always sort nodes !
					List<GraphObject> nodes = new ArrayList<GraphObject>();
                    for(GraphObject g : executor.get(targetUris)) {
                        nodes.add(g);
                    }
	                Collections.sort(nodes, uriComparator);
					
					for(GraphObject g : nodes) {
						
						if(g instanceof VITAL_Node) {
							
							if(!countedResults.containsKey(g.getURI()) && !nonCounterResults.containsKey(g.getURI())) {
							    if(countArc) {
							        if(!countTheseNodes.containsKey(g.getURI())) {
							            countTheseNodes.put(g.getURI(), countTheseNodes.size());
							        }
							    } else {
							        //make sure the parent for this node is either a non-counting node or a counting node that's also in results page
							        VITAL_Edge edge = thisContainerEdges.get(g.getURI());
							        String parentURI = forward ? edge.getSourceURI() : edge.getDestinationURI();
							        if(countTheseNodes.containsKey(parentURI)) {
							            Integer thisOffset = countTheseNodes.get(parentURI);
							            if(thisOffset < pathQuery.getOffset()) {
//							                System.out.println("skipped 2: " + g.getURI());
							                continue;
							            }
							        } 
							    }
							    currentDepthNodes.put(g.getURI(), (VITAL_Node) g);
//								results.put(g.getURI(), g);
								//detect loops, only new objects added as new roots
//								newRoots.add(g.getURI());
							}
							
						} else {
							
							ex("expected vital nodes only in edge targets");
							
						}
						
					}
					
					
				}
				
			}
			
			List<String> newRoots = new ArrayList<String>(currentDepthNodes.keySet());
			Collections.sort(newRoots);
			
			currentRootURIs = new HashSet<String>();
			for(String newRootURI : newRoots) {

			    
			    boolean thisNodeCounts = countTheseNodes.containsKey(newRootURI);

			    boolean acceptThisNode = true;
			    
			    if( thisNodeCounts ) {
			        
			        if(acceptCountableNodes) {
			            
			            countNode();
			            
			        } else {
			            acceptThisNode = false;
			        }
			        
			    }

			    
			    if(acceptThisNode) {
			        
		            //edges first, easier to provide resutls page
	                VITAL_Node node = currentDepthNodes.get(newRootURI);
	                if(thisNodeCounts) {
	                    countedResults.put(newRootURI, node);
	                } else {
	                    nonCounterResults.put(newRootURI, node);
	                }
	                List<VITAL_Edge> edges = node2Edges.get(newRootURI);
	                Collections.sort(edges, new Comparator<VITAL_Edge>() {
	                    @Override
	                    public int compare(VITAL_Edge o1, VITAL_Edge o2) {
	                        return o1.getURI().compareTo(o2.getURI());
	                    }
	                    
	                });
	                result2Edges.put(newRootURI, edges);

	                currentRootURIs.add(newRootURI);
	                
			    }


			    
			}
			
			/*
			List<String> sorted = new ArrayList<String>(currentRootURIs);
			Collections.sort(sorted);
			System.out.println("new root uris: " + currentRootURIs.size());
			for(String u : sorted) {
			    System.out.println("    " + u);
			}
			*/
			
		}
		
	}

	private List<WrappedContainer> splitArc(VitalGraphArcContainer arc) {

		List<VitalGraphCriteriaContainer> rootContainers = new ArrayList<VitalGraphCriteriaContainer>();
		
		for( VitalGraphQueryContainer<?> c : arc ) {
			
			if(c instanceof VitalGraphArcContainer) ex("Nested ARCs forbidden");
			
			if(c instanceof VitalGraphBooleanContainer) ex("ARC boolean containers forbidden");
			
			if(c instanceof VitalGraphCriteriaContainer) {
				
				rootContainers.add((VitalGraphCriteriaContainer) c);
				
			}
			
		}
		
		if(rootContainers.size() < 1) ex("No criteria containers found in an ARC");
		
		VitalGraphCriteriaContainer topContainer = null;
		
		if(rootContainers.size() == 1) {
			
			topContainer = rootContainers.get(0);
			
			if(topContainer.getType() != QueryContainerType.and) ex("Top criteria container must be of type AND");
			
		} else {
			
			topContainer = new VitalGraphCriteriaContainer(QueryContainerType.and);
			
			topContainer.addAll(rootContainers);
			
		}
		
		
		return splitTopContainer(topContainer);
		
		
		
	}

	public static class WrappedContainer {
		
		public VitalGraphCriteriaContainer container = new VitalGraphCriteriaContainer(QueryContainerType.and);
		public int edgesCriteria = 0;
		public int nodesCriteria = 0;
		
	}
	
	private List<WrappedContainer> splitTopContainer(VitalGraphCriteriaContainer topContainer) {

//		int topEdges = 0;
//		int topNodes = 0;

		WrappedContainer nodesContainer = new WrappedContainer();
		WrappedContainer edgesContainer = new WrappedContainer();
		
		
		for( VitalGraphQueryElement el : topContainer ) {
			
			if(el instanceof VitalGraphQueryTypeCriterion) {
				
				Class<? extends GraphObject> c = ((VitalGraphQueryTypeCriterion)el).getType();
				
				if(VITAL_Node.class.isAssignableFrom(c)) {
					nodesContainer.container.add(el);
					nodesContainer.nodesCriteria++;
				} else if(VITAL_Edge.class.isAssignableFrom(c)) {
					edgesContainer.container.add(el);
					edgesContainer.edgesCriteria++;
				} else {
					ex("only node/edge constraints allowed, invalid type: " + c);
				}
				
			} else if(el instanceof VitalGraphQueryPropertyCriterion) {
				
				String pURI = ((VitalGraphQueryPropertyCriterion) el).getPropertyURI();
				
				PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(pURI);
				if(pm == null) ex("Property with URI not found: " + pURI);
				
				boolean isNodeClass = false;
				boolean isEdgeClass = false;
				String s = "";
				for( ClassMetadata domain : pm.getDomains() ) {
					if(VITAL_Node.class.isAssignableFrom(domain.getClazz())) {
						isNodeClass = true;
					} else if(VITAL_Edge.class.isAssignableFrom(domain.getClazz())) {
						isEdgeClass = true;
					}
					if(s.length() > 0) {
						s += ", ";
					}
					s += domain.getClazz().getCanonicalName();
				}
				
				if(isNodeClass && isEdgeClass) ex("Ambiguous property - both edge and node domain: " + pURI + " " + s);
				if(!isNodeClass && !isEdgeClass) ex("Property not a node nor edge property: " + pURI);
				
				if(isNodeClass) {
					nodesContainer.container.add(el);
					nodesContainer.nodesCriteria++;
				} else if(isEdgeClass) {
					edgesContainer.container.add(el);
					edgesContainer.edgesCriteria++;
				}
				
			} else if(el instanceof VitalGraphCriteriaContainer) {
				
				VitalGraphCriteriaContainer cc = (VitalGraphCriteriaContainer) el;
				
				WrappedContainer wrapped = new WrappedContainer();
				wrapped.container = cc;
				
				analyzeContainer(wrapped, cc);
				
				
				if(wrapped.nodesCriteria > 0 && wrapped.edgesCriteria > 0) ex("Edges and nodes criteria cannot be mixed in same container (except root)");
				if(wrapped.nodesCriteria == 0 && wrapped.edgesCriteria == 0) ex("No nodes or edges criteria found in a sub criteria container");
				if(wrapped.nodesCriteria > 0) {
					nodesContainer.container.add(cc);
					nodesContainer.nodesCriteria++;
				} else {
					edgesContainer.container.add(cc);
					edgesContainer.edgesCriteria++;
				}
				
				
			} else {
				ex("unexpected child of a criteria container");
			}
			
		}
		
		//don't throw it here yet
//		if(nodesContainer.nodesCriteria < 1) ex("No node constraints found in an ARC");
//		if(edgesContainer.edgesCriteria < 1) ex("No edge constraints found in an ARC");
		
		return Arrays.asList(nodesContainer, edgesContainer);
		
		
	}

	private void analyzeContainer(WrappedContainer wrapped, VitalGraphCriteriaContainer cc) {

		for( VitalGraphQueryElement el : cc ) {
			
			if(el instanceof VitalGraphQueryTypeCriterion) {
				
				Class<? extends GraphObject> c = ((VitalGraphQueryTypeCriterion)el).getType();
				
				if(VITAL_Node.class.isAssignableFrom(c)) {
					wrapped.nodesCriteria++;
				} else if(VITAL_Edge.class.isAssignableFrom(c)) {
					wrapped.edgesCriteria++;
				} else {
					ex("only node/edge constraints allowed, invalid type: " + c);
				}
				
			} else if(el instanceof VitalGraphQueryPropertyCriterion) {
				
				String pURI = ((VitalGraphQueryPropertyCriterion) el).getPropertyURI();
				
				PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(pURI);
				if(pm == null) ex("Property with URI not found: " + pURI);
				
				boolean isNodeClass = false;
				boolean isEdgeClass = false;
				String s = "";
				for( ClassMetadata domain : pm.getDomains() ) {
					if(VITAL_Node.class.isAssignableFrom(domain.getClazz())) {
						isNodeClass = true;
					} else if(VITAL_Edge.class.isAssignableFrom(domain.getClazz())) {
						isEdgeClass = true;
					}
					if(s.length() > 0) {
						s += ", ";
					}
					s += domain.getClazz().getCanonicalName();
				}
				
				if(isNodeClass && isEdgeClass) ex("Ambiguous property - both edge and node domain: " + pURI + " " + s);
				if(!isNodeClass && !isEdgeClass) ex("Property not a node nor edge property: " + pURI);
				
				if(isNodeClass) {
					wrapped.nodesCriteria++;
				} else if(isEdgeClass) {
					wrapped.edgesCriteria++;
				}
				
			} else if(el instanceof VitalGraphCriteriaContainer) {
				
				analyzeContainer(wrapped, (VitalGraphCriteriaContainer) el);
				
			} else {
				ex("unexpected child of a criteria container");
			}
			
		}
		
	}


	private ResultList doReturn() {

		ResultList rl = new ResultList();
		
		rl.setLimit(pathQuery.getLimit());
		rl.setOffset(pathQuery.getOffset());
		rl.setStatus(VitalStatus.OK);
		
		if(pathQuery.isProjectionOnly()) {
		    
		    rl.setTotalResults(nodesCounter);
		    
		} else {
		    
		    int offset = pathQuery.getOffset();
		    if(offset < 0) offset = 0;
		    
		    Integer limit = pathQuery.getLimit();
		    if(limit <= 0 ) limit = null;
		    
		    rl.setTotalResults(-1);
		    
		    int i = 0;
		    int added = 0;
		    for(Entry<String, GraphObject> e : countedResults.entrySet()) {

//		        boolean thisElementCounts = countTheseNodes.containsKey(e.getKey());
		        
		        if(i >= offset) {
		            List<VITAL_Edge> edges = result2Edges.get(e.getKey());
		            if(edges != null) {
		                for(VITAL_Edge edge : edges ){
		                    rl.addResult(edge);
		                }
		            }
		            rl.getResults().add(new ResultElement(e.getValue(), 1D));
	                added++;
		        }

	            i++;
		        
		        if(limit != null && added >= limit) {
		            break;
		        }
		        
		    }
		    
		    for(Entry<String, GraphObject> e : nonCounterResults.entrySet()) {
		        
		        List<VITAL_Edge> edges = result2Edges.get(e.getKey());
		        if(edges != null) {
		            for(VITAL_Edge edge : edges ){
		                rl.addResult(edge);
		            }
		        }
		        
		        rl.getResults().add(new ResultElement(e.getValue(), 1D));
		    }
		    
		}
		
		
		return rl;
		
	}
	
}
