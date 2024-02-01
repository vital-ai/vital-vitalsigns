package ai.vital.vitalsigns.query.graph;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import ai.vital.vitalsigns.model.GraphMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalservice.query.GraphElement;
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalservice.query.VitalGraphArcContainer.Capture;
import ai.vital.vitalservice.query.VitalGraphQuery;
import ai.vital.vitalservice.query.VitalGraphValue;
import ai.vital.vitalservice.query.VitalGraphValueCriterion;
import ai.vital.vitalservice.query.VitalGraphValueCriterion.Comparator;
import ai.vital.vitalservice.query.VitalSortProperty;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.query.graph.Binding.BindingStatus;
import ai.vital.vitalsigns.query.graph.QueryAnalysis.ProvidesValueParent;
import ai.vital.vitalsigns.uri.URIGenerator;

/**
 * Generic graph query implementation based on pure depth-first algorithm.
 * This version iterates over all 
 * Input is a vital graph query object.
 *
 */
public class GraphQueryImplementation {

	public static interface ResultsProvider {
		
		//provives an iterator of GraphObject for given select query
		
		public Iterator<BindingEl> getIterator(Arc arc, GraphObject parent);
		
	}
	
	public final static int BINDINGS_HARD_LIMIT = 1000000;
	
	private final static Logger log = LoggerFactory.getLogger(GraphQueryImplementation.class);
	
	private VitalGraphQuery graphQuery;

	private ResultsProvider resultsProvider;
	
	private GraphObjectResolver resolver;

	private List<GraphPattern> patternsQueue = new ArrayList<GraphPattern>();
	
	private GraphPattern currentPattern = null;
	
	private PathElement currentRootPathElement = null;
	
	private Map<String, ProvidesValueParent> providesName2ValueParent = new HashMap<String, ProvidesValueParent>();
	
	List<Binding> allBindings = new ArrayList<Binding>();
	
	List<VitalSortProperty> sortProperties = null;
	List<ProvidesValueParent> pvps = null;
	
	List<String> boundVarsList = new ArrayList<String>();
	
	int offset = -1;
	int limit = 10000;
	
	
	
	
	
	public GraphQueryImplementation(ResultsProvider executor, VitalGraphQuery vitalGraphQuery) {
		this(executor, vitalGraphQuery, null);
	}
	
	public GraphQueryImplementation(ResultsProvider executor, VitalGraphQuery vitalGraphQuery, GraphObjectResolver resolver) {
		this.resultsProvider = executor;
		this.graphQuery = vitalGraphQuery;
		this.resolver = resolver;
		
		if(graphQuery.getOffset() < 0) {
			offset = 0;
		} else {
			offset = graphQuery.getOffset();
		}
		
		if(offset >= BINDINGS_HARD_LIMIT) {
		    throw new RuntimeException("offset must not exceed hard limit: " + BINDINGS_HARD_LIMIT + " offset: " + offset);
		}
		
		if(graphQuery.getLimit() > 0) {
			if(graphQuery.getLimit() > limit) {
				throw new RuntimeException("Graphquery hard limit is 10000");
			}
			limit = graphQuery.getLimit();
		}
		
	}

	
	public ResultList execute() {
		
		//sets the arcs tree
		analyzeQuery();
		doQuery();
		
		return assembleResults();
		
	}

    int resultsIterated = 0;

	private void doQuery() {

		currentPattern = patternsQueue.get(0);
		currentRootPathElement = currentPattern.get(0).get(0);
		currentRootPathElement.setStartEndoint(null);
		completeBindings = null;
		
		//disconnector the root path element
		for(Path p : currentPattern) {
			p.get(0).setChildren(new ArrayList<PathElement>(0));
		}
		
		if(sortProperties.size() > 0 || graphQuery.getProjectionOnly()) {
		    
		    //collect everything
		    Binding b = null;
		    
		    int queueLength = offset + limit;
		    
            PriorityQueue<Binding> priorityQueue = null;
            
            if(sortProperties.size() > 0) {
                priorityQueue = new PriorityQueue<Binding>(queueLength + 1, new BindingComparator(pvps, sortProperties, graphQuery.getSortStyle()){});
            }
            
		    
		    while( ( b = nextBinding() ) != null ) {
		        
		        resultsIterated++;
		        
		        if(resultsIterated >= BINDINGS_HARD_LIMIT) {
		            throw new RuntimeException("Bindings hard limit hit: " + BINDINGS_HARD_LIMIT);
		        }
		        
//		        if(resultsIterated)
		        
		        if(priorityQueue != null) {

		              priorityQueue.add(b);
		                
		                while(priorityQueue.size() > queueLength) {
		                    //head 
		                    priorityQueue.remove();
		                }

		        }
		        
		        
		    }
		    
		    if(priorityQueue != null) {

		        for(int i = 0 ; i < limit && priorityQueue.size() > offset; i++) {
	                
	                Binding binding = priorityQueue.remove();
	                
	                allBindings.add(0, binding);
	                
	            }
		        
		    }
		    
		    
		} else {
		    
		    int offsetPlusLimit = offset + limit;
		    
		    int localLimit = graphQuery.getIncludeTotalCount() ? BINDINGS_HARD_LIMIT + 1 : offsetPlusLimit; 
		    
		    
		    //just keep getting next results
		    for(int i = 0; i < localLimit; i++) {
		        
		        
		        if(resultsIterated >= BINDINGS_HARD_LIMIT) {
                    throw new RuntimeException("Bindings hard limit hit: " + BINDINGS_HARD_LIMIT);
                }
		        
		        Binding b = nextBinding();
		        
		        //break
		        if(b == null) {
		            break;
		        }
		        
		        resultsIterated++;
		        
		        if(i >= offset && i < offsetPlusLimit) {
		            allBindings.add(b);
		        }
		        
		    }
		    
		}
		
	}

	private Binding nextBinding() {

		Binding b = nextPatternBinding();
			
		if(b != null) {
			return b;
		}
		
		int indexOf = patternsQueue.indexOf(currentPattern);
		
		if(indexOf < patternsQueue.size() -1 ) {
			
			currentPattern = patternsQueue.get(indexOf + 1);
			currentRootPathElement = currentPattern.get(0).get(0); 
			currentRootPathElement.setStartEndoint(null);
			completeBindings = null;
			
			//disconnector the root path element
			for(Path p : currentPattern) {
				p.get(0).setChildren(new ArrayList<PathElement>(0));
			}
			
			return nextBinding();
			
		}
		
		return null;
	}

	List<Binding> subbindings = new ArrayList<Binding>();

	Binding sourceBinding = null;
	
	//this list gets filled why the query progresses
	List<List<Binding>> completeBindings = null;
	
	List<Iterator<Binding>> completeBindingsIterators = null;
	
	private Binding nextPatternBinding() {

		if(currentPattern == null) throw new RuntimeException("No current binding set!");

		if(completeBindings == null) {
			completeBindings = new ArrayList<List<Binding>>();
			completeBindingsIterators = new ArrayList<Iterator<Binding>>();
			for(int i = 0; i < currentPattern.size(); i ++ ) {
				completeBindings.add(new ArrayList<Binding>());
				completeBindingsIterators.add(null);
			}
		}
		
		//we need to iterate over all paths, starting from 1 connector and fixed endpoint
		
		while(true) {
			
			if(sourceBinding == null) {
				sourceBinding = currentRootPathElement.getNextBinding();
				if(sourceBinding.getStatus() != BindingStatus.OK) {
					sourceBinding = null;
					return null;
				} else {
					
					completeBindings = new ArrayList<List<Binding>>();
					completeBindingsIterators = new ArrayList<Iterator<Binding>>();
					for(int i = 0; i < currentPattern.size(); i ++ ) {
						completeBindings.add(new ArrayList<Binding>());
						completeBindingsIterators.add(null);
					}
					
					for(Path p : currentPattern) {
						
						if(p.size() > 1) {
							p.get(1).setStartEndoint(sourceBinding.get(0).getEndpoint());
						}
						
					}
					
				}
			}
			
			while(subbindings.size() < currentPattern.size()) {
				
				//keep filling the record
				for(int i = subbindings.size(); i < currentPattern.size(); i++) {
					
					Binding nextBinding = null;
					
					Iterator<Binding> iterator = completeBindingsIterators.get(i);
					if(iterator != null) {
						if(iterator.hasNext()) {
							nextBinding = iterator.next();
						} else {
							//reset
							completeBindingsIterators.set(i, completeBindings.get(i).iterator());
						}
					} else {
					
						nextBinding = currentPattern.get(i).get(1).getNextBinding();
						
						List<Binding> list = completeBindings.get(i);
						if(nextBinding.getStatus() == BindingStatus.OK) {
							list.add(nextBinding);
						} else {
							//just put the list on the stack
							completeBindingsIterators.set(i, list.iterator());
						}
						
					}
					
//					Binding nextBinding = currentPattern.get(i).get(1).getNextBinding();
					if(nextBinding != null && nextBinding.getStatus() == BindingStatus.OK) {
						subbindings.add(nextBinding);
					} else {
						
						//no more bindings, remove parent from the stack
						if(subbindings.size() > 0) {
							subbindings.remove(subbindings.size()-1);
							break;
							
						}
						
						if(subbindings.size() == 0) {
							sourceBinding = null;
							break;
						}
						
					}
					
				}
				
				if(sourceBinding == null) break;
				
			}
			
			//we have another solution here, just return it
			if(subbindings.size() == currentPattern.size()) {
				
				
				//don't include root nodes if they already appeared in an optional clause
				Binding b = new Binding(BindingStatus.OK);
				b.addAll(sourceBinding);
				for(Binding l : subbindings) {
					b.addAll(l);
				}
				
				//remove last element
				subbindings.remove(subbindings.size()-1);
				
				
				//this is going to be skipped
				if(b.size() == 1) {
					BindingEl bindingEl = b.get(0);
					GraphObject endpoint = bindingEl.getEndpoint();
					for(Binding x : allBindings) {
						if(x.get(0).getEndpoint().getURI().equals(endpoint.getURI())) {
							b = null;
							break;
						}
						
					}
					
				}
				
				//check all cross branches constraints in this run
				if(b != null) {
	                for(int i = 0 ; i < b.size(); i++) {
	                    BindingEl bindingEl = b.get(i);
	                    if( ! providesConstraintTest(bindingEl.getArc(), b, false) ) {
	                        b = null;
	                        break;
	                    }
	                }
				}
				
//				if( !providesConstraintTest(currentRootPathElement.arc, b) ) {
//				    b = null;
//				}
				
				if( b != null) return b;
			}
			
			//time to change the source
			
		}
		
		/*
		Binding nextBinding = currentPattern.get(0).get(0).getNextBinding();
		
		if(nextBinding.getStatus() == BindingStatus.OK) {
			return nextBinding;
		}
		
		return null;
		*/
	}


	
	public boolean providesConstraintTest(Arc arc, Binding b, boolean ignoreNotFoundBoundValues) {

		List<VitalGraphValueCriterion> valueCriteria = arc.arcContainer.getValueCriteria();
		if(valueCriteria == null || valueCriteria.size() < 1) return true;
		
		
		for(VitalGraphValueCriterion c : valueCriteria) {
			
			String n1 = c.getName1();
			
			String n2 = c.getName2();
		
			ProvidesValueParent pvp1 = providesName2ValueParent.get(n1);
			if(pvp1 == null) throw new RuntimeException("Provides value not found: " + n1);
			 
			ProvidesValueParent pvp2 = providesName2ValueParent.get(n2);
			if(pvp2 == null) throw new RuntimeException("Provides value not found: " + n2);
		
			
			BindingEl b1 = b.getBindingElForArc(pvp1.arc);
			if(b1 == null) {
			    if(ignoreNotFoundBoundValues) {
			        continue;
			    }
			    throw new RuntimeException("No bound value for name: " + n1);
			}
			BindingEl b2 = b.getBindingElForArc(pvp2.arc);
			if(b2 == null) {
			    if(ignoreNotFoundBoundValues) {
			        continue;
			    }
			    throw new RuntimeException("No bound value for name: " + n2);
			}
			
			GraphElement symbol1 = pvp1.value.getSymbol();
			GraphObject g1 = null;
			if(symbol1 == GraphElement.Connector) {
				if(pvp1.arc.isTopArc()) throw new RuntimeException("Cannot use provided connector value in top arc");
				g1 = b1.getConnector();
			} else {
				g1 = b1.getEndpoint();
			}
			
			
			GraphElement symbol2 = pvp2.value.getSymbol();
			GraphObject g2 = null;
			if(symbol2 == GraphElement.Connector) {
				if(pvp2.arc.isTopArc()) throw new RuntimeException("Cannot use provided connector value in top arc");
				g2 = b2.getConnector();
			} else {
				g2 = b2.getEndpoint();
			}
			
			
			//what about external properties
			
			Object p1 = null;
			Object p2 = null;
			
			if(pvp1.value.getPropertyURI().equals(VitalGraphValue.URI)) {
			    p1 = g1.getURI();
			} else {
			    p1 = g1.getPropertiesMap().get(pvp1.value.getPropertyURI());
			}
			
			if(pvp2.value.getPropertyURI().equals(VitalGraphValue.URI)) {
			    p2 = g2.getURI();
			} else {
			    p2 = g2.getPropertiesMap().get(pvp2.value.getPropertyURI());
			}
			
//			g1.getPropertiesMap().get(pvp)
			
//			Object p1 = g1.getProperty(RDFUtils.getPropertyShortName(pvp1.value.getPropertyURI()));
//			Object p2 = g2.getProperty(RDFUtils.getPropertyShortName(pvp2.value.getPropertyURI()));

			//both values must exist!
			if(p1 == null || p2 == null) return false;
			
			if(p1 instanceof IProperty) p1 = ((IProperty)p1).rawValue();
			if(p2 instanceof IProperty) p2 = ((IProperty)p2).rawValue();
			
			
			Comparator comparator = c.getComparator();
			
// 			if(comparator == Comparator.EQ) {
				
			boolean passed = false;
 				
 			if(p1 instanceof Collection && p2 instanceof Collection) {
 					
 				for(Object v1 : (Collection<?>)p1) {
 						
 					for(Object v2 : (Collection<?>)p2) {

 						passed = doCompare(comparator, v1, v2);
 							
 						if(passed) break;
 						
 					}
 						
 					if(passed) break;
 					
 				}
 					
 					
 			} else if(p1 instanceof Collection) {
 				
 				for(Object v1 : (Collection<?>) p1) {
 					
 					passed = doCompare(comparator, v1, p2);
 				
 					if(passed) break;
 					
 				}
 				
 			} else if(p2 instanceof Collection) {

 				for(Object v2 : (Collection<?>) p2) {
 					
 					passed = doCompare(comparator, p1, v2);
 				
 					if(passed) break;
 					
 				}
 				
 			} else {
 				
 				passed = doCompare(comparator, p1, p2);
 				
 			}
 			
 			if(!passed) return false;
 			
 			/*
 			if(!passed) return false;
 				
				if(p1 == null && p2 == null) {
					
				} else if( ( p1 != null && p2 == null ) || ( p1 ==null && p2 != null) ) {
					return false;
				} else {
					if ( ! p1.equals(p2) ) return false;
				}
				
			} else if(comparator == Comparator.NE) {
				
				if(p1 == null && p2 == null) {
					return false;
				} else if( ( p1 != null && p2 == null ) || ( p1 ==null && p2 != null) ) {

				} else {
					if ( p1.equals(p2) ) return false;
				}
				
			} else {
				
				if(p1 == null &&)
				
			}
			*/
			
			
		}
		

		return true;
	}

	static boolean doCompare(Comparator comparator, Object v1, Object v2) {

		
		
//		if(comparator == Comparator.EQ) {
//			if(v1.equals(v2)) {
//				return true;
//			}
//		} else if(comparator == Comparator.NE) {
//			if(!v1.equals(v2)) {
//				return true;
//			}
//		} else {
			
			if(v1 instanceof Comparable && v2 instanceof Comparable) {
		
				@SuppressWarnings({ "rawtypes", "unchecked" })
				int c = ((Comparable)v1).compareTo((Comparable)v2);

				if(comparator == Comparator.GE) {
					return c >= 0;
				} else if(comparator == Comparator.GT) {
					return c > 0;
				} else if(comparator == Comparator.LE) {
					return c <= 0;
				} else if(comparator == Comparator.LT) {
					return c < 0;
				} else if(comparator == Comparator.EQ) {
					return c == 0;
				} else if(comparator == Comparator.NE) {
					return c != 0;
				}	
			}
			
//		}
		
		return false;

	
	
	}


	private void analyzeQuery() {

		//decompose the arcs!
		VitalGraphArcContainer topContainer = graphQuery.getTopContainer();
	
		
		//collect provdes names and validate
//		QueryAnalysis.getProvidesMap();
//		Map<String, VitalGraphValue> providesMap = topContainer.getProvidesMap();
		QueryAnalysis.collectAndValidateProvides(providesName2ValueParent, topContainer);
		
		
		//sort analysis
		sortProperties = graphQuery.getSortProperties();
		if(sortProperties == null) sortProperties = Collections.emptyList();
		
		pvps = new ArrayList<ProvidesValueParent>();
		
		for(VitalSortProperty vsp : sortProperties) {
		    
		    String providedName = vsp.getProvidedName();
		    
		    if(providedName == null || providedName.isEmpty()) throw new RuntimeException("Sort properties in graph queries must have provided name");
		    
		    ProvidesValueParent pvp = providesName2ValueParent.get(providedName);
		    
		    if(pvp == null) throw new RuntimeException("Provided variable for sort not found: " + providedName);
		    
		    pvps.add(pvp);
		    
		}
		
		
		QueryDecomposer decomposer = new QueryDecomposer(topContainer);
		List<GraphPattern> tempPatternsQueue = decomposer.decomposeQuery();
		
		//link criteria to arcs and set next elements
		if(tempPatternsQueue.size() == 0) throw new RuntimeException("No graph patterns to check");
		
		for(GraphPattern gp : tempPatternsQueue) {
			
			for(Path p : gp) {
				
//				PathElement previousPath = null;
				
				for(PathElement pe : p) {
					
					pe.setProvider(resultsProvider);
					pe.setQueryImpl(this);
					
//					if(previousPath != null) {
//						previousPath.getChildren().add(pe);
//					}
//					
//					previousPath = pe;
					
					
					Arc a = pe.arc;
					
					for( ProvidesValueParent pvp : providesName2ValueParent.values() ) {
						
						if(pvp.container == a.arcContainer) {
							
							pvp.arc = a;
							
						}
						
					}
					
				}
				
			}
			
		}
		
		
		//prepare bound variables names list
        for(GraphPattern gp : tempPatternsQueue) {
        
            for(Path path : gp) {
                
                for(PathElement pe : path) {
                    
                    Arc arc = pe.arc;
                    
                    Capture capture = arc.getArcContainer().getCapture();
                    
                    if(arc.isTopArc()) {
                        
                        if( capture != Capture.NONE ) {
                            
                            String sourceBind = arc.getSourceBind();
                            if(!boundVarsList.contains(sourceBind)) boundVarsList.add(sourceBind);
                        }
                        
                    } else {
                        
                        //connector first
                        if(capture == Capture.BOTH || capture == Capture.CONNECTOR) {

                            String cBind = arc.getConnectorBind();
                            
                            if(!boundVarsList.contains(cBind)) boundVarsList.add(cBind);
                            
                        }
                        
                        if( capture == Capture.BOTH || capture == Capture.SOURCE || capture == Capture.TARGET) {
                            
                            if(arc.isForwardNotReverse()) {

                                String destBind = arc.getDestinationBind();
                                if(!boundVarsList.contains(destBind)) boundVarsList.add(destBind);
                                
                            } else {

                                String srcBind = arc.getSourceBind();
                                if(!boundVarsList.contains(srcBind)) boundVarsList.add(srcBind);
                                
                            }
                            
                        }
                        
                    }
                    
                }
                
            }
            
            
        }
		
		patternsQueue = tempPatternsQueue;
		
		
		/*
		List<GraphPattern> newPatterns = new ArrayList<GraphPattern>();
		
		for(GraphPattern p : tempPatternsQueue) {
			
			//in this phase merge the AND paths into common root path with children, ie. 
			// A -> B -> C  AND A -> D -> E   ==>    A -> [B-> C, D->E]
			
			//sort pattern, based on path length
			Collections.sort(p, new java.util.Comparator<Path>() {
				@Override
				public int compare(Path p1, Path p2) {
					return new Integer(p2.size()).compareTo(p1.size());
				}
			});
			
			Path mergedPath = null;
			
			for(int i = 0; i < p.size(); i++ ) {
				
				Path path = p.get(i); 
				if(i == 0) {
					mergedPath = path;
					
				} else {
					
					if(mergedPath.get(0).arc != path.get(0).arc) throw new RuntimeException("merged paths ARC must be the same: " + mergedPath.get(0).arc.getLabel() + " vs. " + path.get(0).arc.getLabel());
					
					for(int j = 1; j < path.size(); j++) {
						
						mergedPath.get(j-1).getChildren().add(path.get(j));
						
					}
					
				}
				

				
				for( int j = 0 ; j < path.size(); j++ ) {
					
					PathElement pe  = path.get(j);

					pe.setQueryImpl(this);
					pe.setProvider(resultsProvider);
					
//					if(j < path.size() - 1) {
//						pe.setNextElement(path.get(j+1));
//					}
					
					Arc a = pe.arc;
					
					for( ProvidesValueParent pvp : providesName2ValueParent.values() ) {
						
						if(pvp.container == a.arcContainer) {
							
							pvp.arc = a;
							
						}
						
					}
					
				}
				
			}
			
			GraphPattern newPattern = new GraphPattern();
			newPattern.add(mergedPath);
			
			newPatterns.add(newPattern);
			
		}
		*/
		
//		this.patternsQueue = newPatterns;
		
		//validate pvp map
		for(ProvidesValueParent pvp : providesName2ValueParent.values() ) {
			if(pvp.arc == null) throw new RuntimeException("Internal error: no arc associated with value criteria");
		}
		
	}



	private ResultList assembleResults() {
		
		ResultList r = new ResultList();
		r.setBindingNames(boundVarsList);
		r.setLimit(limit);
		r.setOffset(offset);
		
		Set<String> alreadyExported = null;
		if(graphQuery.getPayloads()) {
			alreadyExported = new HashSet<String>();
		}
		
		boolean batchResolving = graphQuery.getPayloads() && resolver != null && resolver.supportsBatchResolve();
		
		if(graphQuery.getProjectionOnly() || sortProperties.size() > 0 || graphQuery.getIncludeTotalCount()) {
		    r.setTotalResults(resultsIterated);
		    batchResolving = false;
		}
		
		Map<String, GraphObject> alreadyResolvedMap = null;
		
		if(batchResolving) {
		    
		    Map<String, GraphObject> inputMap = new HashMap<String, GraphObject>();
		    
//		    log.debug("Batch resolving all objects");
		    for(Binding b : allBindings) {
		        
		        for(BindingEl el : b) {
		            
		            Arc arc = el.getArc();
	                
	                Capture capture = arc.getArcContainer().getCapture();
	                
	                if(arc.isTopArc()) {
	                    
	                    if( capture != Capture.NONE ) {
	                        
	                        GraphObject endpoint = el.getEndpoint();
	                
	                        inputMap.put(endpoint.getURI(), endpoint);
	                        
	                    }
	                    
	                } else {
	                    
	                    
	                    if(capture == Capture.BOTH || capture == Capture.SOURCE || capture == Capture.TARGET) {
	                        
	                        GraphObject endpoint = el.getEndpoint();
	                        
	                        inputMap.put(endpoint.getURI(), endpoint);
	                        
	                    }
	                    
	                    
	                    if(capture == Capture.BOTH || capture == Capture.CONNECTOR) {
	                        
	                        GraphObject connector = el.getConnector();

	                        inputMap.put(connector.getURI(), connector);
	                        
	                    }
	                    
	                }
		            
		        }
		        
		    }
		    
		    
		    alreadyResolvedMap = resolver.resolveGraphObjects(inputMap); 
		    
		    if(alreadyResolvedMap == null) throw new RuntimeException("GraphObject resolver implementation is invalid - returns null map: " + resolver.getClass().getCanonicalName());
		    
		    if(alreadyResolvedMap.size() != inputMap.size()) {
		        throw new RuntimeException("GraphObject resolver implementation is invalid - returns map of different size [" + alreadyResolvedMap.size() + "] than input map [" + inputMap.size());
		    }
		    
		}
		
		for(Binding b : allBindings) {
			
			GraphMatch gm = new GraphMatch();
			
			gm.setURI(URIGenerator.generateURI((VitalApp)null, GraphMatch.class));
			
			
			
			for(BindingEl el : b) {
				
				Arc arc = el.getArc();
				
				Capture capture = arc.getArcContainer().getCapture();
				
				if(arc.isTopArc()) {
					
					if( capture != Capture.NONE ) {
						
						GraphObject endpoint = el.getEndpoint();
						
						gm.setProperty(arc.getSourceBind(), URIProperty.withString( endpoint.getURI() ) );
						
						if(alreadyExported != null && alreadyExported.add(endpoint.getURI())) {

						    if(alreadyResolvedMap != null) {
						        
						        endpoint = getResolved(alreadyResolvedMap, endpoint);
						        
						    } else if(resolver != null) endpoint = resolver.resolveGraphObject(endpoint);
							
							gm.setProperty(endpoint.getURI(), endpoint.toCompactString());
							
						}
					}
					
					
				} else {
					
					
					if(capture == Capture.BOTH || capture == Capture.SOURCE || capture == Capture.TARGET) {
						
						GraphObject endpoint = el.getEndpoint();
						
						if(arc.isForwardNotReverse()) {
							
							gm.setProperty(arc.getDestinationBind(), URIProperty.withString( endpoint.getURI() ) );
							
						} else {
							
							gm.setProperty(arc.getSourceBind(), URIProperty.withString( endpoint.getURI() ) );
							
							
						}
						
						if(alreadyExported != null && alreadyExported.add(endpoint.getURI())) {

						    if(alreadyResolvedMap != null) {
						        
						        endpoint = getResolved(alreadyResolvedMap, endpoint);
						        
						    } else if(resolver != null) endpoint = resolver.resolveGraphObject(endpoint);
							
							gm.setProperty(endpoint.getURI(), endpoint.toCompactString());
							
						}

						
					}
					
					
					if(capture == Capture.BOTH || capture == Capture.CONNECTOR) {
						
						GraphObject connector = el.getConnector();
						
						gm.setProperty(arc.getConnectorBind(),  URIProperty.withString( connector.getURI() ) );
						
						if(alreadyExported != null && alreadyExported.add(connector.getURI())) {
						
						    if(alreadyResolvedMap != null) {
						        
						        connector = getResolved(alreadyResolvedMap, connector);
						        
						    } else if(resolver != null) connector = resolver.resolveGraphObject(connector);
							
							gm.setProperty(connector.getURI(), connector.toCompactString());
							
						}
						
					}
					
				}
				
			}
			
			r.getResults().add(new ResultElement(gm, 1D));
			
			
		}

		
		return r;
		
	}
	

	
	private GraphObject getResolved(Map<String, GraphObject> alreadyResolvedMap,
            GraphObject endpoint) {
        
        GraphObject updated = alreadyResolvedMap.get(endpoint.getURI());
        if(updated != null) {
            endpoint = updated;
        } else {
            log.warn("Resolved object not found: {}", endpoint.getURI());
            
        }
        
        return endpoint; 
        
    }

    //http://stackoverflow.com/questions/714108/cartesian-product-of-arbitrary-sets-in-java
	public static List<List<Binding>> cartesianProduct(List<List<Binding>> sets) {
	    if (sets.size() < 2)
	        throw new IllegalArgumentException(
	                "Can't have a product of fewer than two sets (got " +
	                sets.size() + ")");

	    return _cartesianProduct(0, sets);
	}

	private static List<List<Binding>> _cartesianProduct(int index, List<List<Binding>> sets) {
	    List<List<Binding>> ret = new ArrayList<List<Binding>>();
	    if (index == sets.size()) {
	        ret.add(new ArrayList<Binding>());
	    } else {
	        for (Binding obj : sets.get(index)) {
	            for (List<Binding> set : _cartesianProduct(index+1, sets)) {
	                set.add(obj);
	                ret.add(set);
	            }
	        }
	    }
	    return ret;
	}
	
}
