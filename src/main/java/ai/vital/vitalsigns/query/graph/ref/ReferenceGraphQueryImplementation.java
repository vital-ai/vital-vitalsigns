package ai.vital.vitalsigns.query.graph.ref;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.vital.vitalservice.query.GraphElement;
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalservice.query.VitalGraphArcContainer.Capture;
import ai.vital.vitalservice.query.VitalGraphCriteriaContainer;
import ai.vital.vitalservice.query.VitalGraphQuery;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalservice.query.VitalGraphValueCriterion;
import ai.vital.vitalservice.query.VitalGraphValueCriterion.Comparator;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.query.graph.GraphObjectResolver;
import ai.vital.vitalsigns.query.graph.ref.QueryAnalysis.ProvidesValueParent;
import ai.vital.vitalsigns.rdf.RDFUtils;
import ai.vital.vitalsigns.uri.URIGenerator;

import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;
import ai.vital.vitalsigns.model.GraphMatch;




/**
 * Generic graph query implementation based on pure depth-first algorithm
 * Input is a vital graph query object.
 *
 */

public class ReferenceGraphQueryImplementation {

	public static interface Executor {
		
		public ResultList selectQuery(VitalSelectQuery sq);
		
	}
	
	private VitalGraphQuery graphQuery;

	private Executor executor;
	
	private GraphObjectResolver resolver;
	
	private List<GraphPattern> patternsQueue = new ArrayList<GraphPattern>();
	
	private Map<String, ProvidesValueParent> providesName2ValueParent = new HashMap<String, ProvidesValueParent>();
	
	List<Binding> allBindings = null;
	
	
	public ReferenceGraphQueryImplementation(Executor executor, VitalGraphQuery vitalGraphQuery) {
	    this(executor, vitalGraphQuery, null);
	}
	
	public ReferenceGraphQueryImplementation(Executor executor, VitalGraphQuery vitalGraphQuery, GraphObjectResolver resolver) {
		this.executor = executor;
		this.graphQuery = vitalGraphQuery;
		this.resolver = resolver;
	}

	
	public ResultList execute() {
		
		//sets the arcs tree
		analyzeQuery();
		doQuery();
		
		return assembleResults();
		
	}


	private void doQuery() {

		
		if(patternsQueue.size() == 0) throw new RuntimeException("No graph patterns to check");
		
		
		
		for(GraphPattern pattern : patternsQueue) {
			
			List<Binding> processGraphPattern = processGraphPattern(pattern);
			
			if(allBindings == null) {
				allBindings = processGraphPattern;
			} else {
				allBindings.addAll(processGraphPattern);
			}
			
		}
		
//		List<BindingSet> topBindings = processArc(topArc);
		
	}

	private List<Binding> processGraphPattern(GraphPattern pattern) {

		//keep processing
	
		try {
			
			for(Path path : pattern) {
				processPath(path);
			}
		} catch(NoBindingsException e) {
			
			//no results, nothing to verify
			
		}

		
		//check if root is common
		
		
		//merge path results
		Map<String, GraphObject> rootsMap = null;
		
		for(int i = 0 ; i < pattern.size(); i++) {
			
			Path path = pattern.get(i);
			
			PathElement root = path.get(0);
			
			if(root.endpointResults == null) continue;
			
			if(rootsMap == null) {
				rootsMap = new HashMap<String, GraphObject>();
				for(GraphObject g : root.endpointResults) {
					rootsMap.put(g.getURI(), g);
				}
			} else {
				
				Set<String> newRoots = new HashSet<String>();
				for(GraphObject g : root.endpointResults) {
					newRoots.add(g.getURI());
				}
				
				rootsMap.keySet().retainAll(newRoots);
				
				if(rootsMap.size() == 0) break;
				
			}
			
			
		}
		
		List<Binding> results = new ArrayList<Binding>();
		
		//no bindings
		if(rootsMap == null || rootsMap.size() == 0) return results;
		
		
		
		for(GraphObject endpoint : rootsMap.values() ) {
			
//			//collect matches
//			
//			Binding b = new Binding(pattern.get(0).get(0).arc, endpoint, null);
//			
//			Bindings bindings = new Bindings();
//			bindings.add(b);
//			
//			BindingSet bs = new BindingSet();
//			bs.add(bindings);
//			collectMatches(1, bs, endpoint, pattern);
			

			//simply walk all paths starting at root node
			
			//cartiesian product to be made
			List<List<Binding>> allBindings = new ArrayList<List<Binding>>();
			
			for(Path p : pattern) {
				
				List<Binding> pathBindings = null;
				
				if(p.size() == 1) {
					
					continue;
					
//					pathBindings = new ArrayList<Binding>();
//					
//					Binding bs = new Binding();
//					
//					bs.add(new BindingEl(p.get(0).arc, endpoint, null));
//					
//					pathBindings.add(bs);
					
				} else {
					
					try {
						pathBindings = walkPath(0, endpoint, p);
					} catch(NoBindingsException e) {}
					
				}
				
				if(pathBindings != null && pathBindings.size() > 0) { allBindings.add(pathBindings); };
				
				
			}
			
			
			if(allBindings.size() == 0) {

				Binding bs = new Binding();
//				
				bs.add(new BindingEl(pattern.get(0).get(0).arc, endpoint, null));
				
				results.add(bs);
				
				
			} else if(allBindings.size() == 1) {
				
				List<Binding> l = allBindings.get(0);
				
				
				for(Binding b : l) {
					
					b.add(new BindingEl(pattern.get(0).get(0).arc, endpoint, null));
					
				}
				
				//just append the binding to each
				results.addAll(l);
				
			} else {
				

				List<List<Binding>> cartesianProduct = cartesianProduct(allBindings);
				
				for(List<Binding> c : cartesianProduct) {
					
					Binding merged = new Binding();
					merged.add(new BindingEl(pattern.get(0).get(0).arc, endpoint, null));
					
					for(Binding b : c) {
						merged.addAll(b);
					}
					
					//smoosh binding and add the initial b
					results.add(merged);
					
				}
				
			}
			
		}
		
		return results;
		
	
		
	}
	
	private List<Binding> walkPath(int index, GraphObject endpoint, Path path) {

//		if(p.size() - 1 == index) {
//			
//			//no more paths
//			
//			//just check if there's 
//			
//			
//		}
		
		
		PathElement pathEl = path.get(index);
		
		boolean inOptionalClause = false;
		int j = index;
		while(j >= 0) {
			PathElement p = path.get(j);
			if(p.isOptional()) {
				inOptionalClause = true;
				break;
			}
			j--;
		}
		
		boolean nextIsOptional = index < path.size() -1 && path.get(index + 1).isOptional();
		
		List<BindingEl> newEndpoints = new ArrayList<BindingEl>();
		
		if(index > 0) {
		    
    		for(GraphObject o : pathEl.connectorResults ) {
    
    			String otherEndpoint = null;
    			
    			if(pathEl.isHyperArc()) {
    				
    				VITAL_HyperEdge he = (VITAL_HyperEdge) o; 
    				
    				
    				if ( pathEl.isForwardNotReverse() ) {
    					
    					if(he.getSourceURI().equals(endpoint.getURI())) {
    						otherEndpoint = he.getDestinationURI();
    					}
    					
    				} else {
    					
    					if(he.getDestinationURI().equals(endpoint.getURI())) {
    						otherEndpoint = he.getSourceURI();
    					}
    					
    				}
    			} else {
    				
    				VITAL_Edge e = (VITAL_Edge) o;
    				
    				if(pathEl.isForwardNotReverse()) {
    					
    					if(e.getSourceURI().equals(endpoint.getURI())) {
    						otherEndpoint = e.getDestinationURI();
    					}
    					
    				} else {
    					
    					if(e.getDestinationURI().equals(endpoint.getURI())) {
    						otherEndpoint = e.getSourceURI();
    					}
    					
    				}
    				
    			}
    			
    			if(otherEndpoint != null) {
    				GraphObject otherG = pathEl.getEndpoint(otherEndpoint);
    				if(otherG == null) throw new RuntimeException("(hyper)edge endpoint not found: " + otherEndpoint + " " + o);
    				newEndpoints.add( new BindingEl(pathEl.arc, otherG, o));
    			}
    			
    			
    		}
		
		} else {
	            
		    for(GraphObject o : pathEl.endpointResults ) {
		        newEndpoints.add(new BindingEl(pathEl.arc, o, null));
		    }
		}
	        
		
		try {
			
		if( newEndpoints.size() < 1 ) throw new NoBindingsException();
		
		if(path.size() - 1 == index) {
			
			//no more paths return binding
			
			List<Binding> l = new ArrayList<Binding>();
			
			for(BindingEl bel : newEndpoints) {
			
				
				//apply binding filter
				
				Binding b = new Binding();
				b.add(bel);
				if( providesConstraintTest(path, pathEl, b) ) {
					l.add(b);
				}
				
			}
			
			if(l.size() == 0) throw new NoBindingsException();
			
			return l;
			
		}

		List<Binding> output = new ArrayList<Binding>();
		
		for(BindingEl el : newEndpoints) {
			
			
			List<Binding> bindings = walkPath(index + 1, el.getEndpoint(), path);
			
			if(bindings.size() == 0) {
				if( ! nextIsOptional ) {
					throw new NoBindingsException();
				}
				
				//start new paths
				Binding newBinding = new Binding();
				newBinding.add(el);
				
				output.add(newBinding);
				
			} else {
				
				for(Binding b : bindings) {
					
					b.add(el);
					
					if( providesConstraintTest(path, pathEl, b) ) {
						output.add(b);
						
					}
					
				}
				
			}
			
			
			
			
		}
		
		if(output.size() == 0) throw new NoBindingsException();
		
		return output;
		
		} catch(NoBindingsException e) {
			
			if(inOptionalClause) {
				//just end all open bindings
				return new ArrayList<Binding>();
			}
			
			throw e;
			
		}
		
	}




	private boolean providesConstraintTest(Path p, PathElement pathEl, Binding b) {

		List<VitalGraphValueCriterion> valueCriteria = pathEl.arc.arcContainer.getValueCriteria();
		if(valueCriteria == null || valueCriteria.size() < 1) return true;
		
		
		for(VitalGraphValueCriterion c : valueCriteria) {
			
			String n1 = c.getName1();
			
			String n2 = c.getName2();
		
			ProvidesValueParent pvp1 = providesName2ValueParent.get(n1);
			if(pvp1 == null) throw new RuntimeException("Provides value not found: " + n1);
			 
			ProvidesValueParent pvp2 = providesName2ValueParent.get(n2);
			if(pvp2 == null) throw new RuntimeException("Provides value not found: " + n2);
		
			
			BindingEl b1 = b.getBindingElForArc(pvp1.arc);
			if(b1 == null) throw new RuntimeException("No bound value for name: " + n1);
			BindingEl b2 = b.getBindingElForArc(pvp2.arc);
			if(b2 == null) throw new RuntimeException("No bound value for name: " + n2);
			
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
			
			
			Object p1 = g1.getProperty(RDFUtils.getPropertyShortName(pvp1.value.getPropertyURI()));
			Object p2 = g2.getProperty(RDFUtils.getPropertyShortName(pvp2.value.getPropertyURI()));

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

	private boolean doCompare(Comparator comparator, Object v1, Object v2) {

		
		
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


	private void processPath(Path path) {
	
		PathElement previousEl = null;
		
		//skip to last non-optional path
		
		int lastNonOptionalIndex = -1;
		
		for(int i = path.size() - 1; i >= 0; i--) {
			
			PathElement pathEl = path.get(i);
			
			if( ! pathEl.isOptional()) {
				lastNonOptionalIndex = i;
				break;
			}
			
		}
		
		if(lastNonOptionalIndex < 0 ) throw new RuntimeException("Full optional paths are fobidden: " + path.toString());
		
			
		
		
		for( int i = path.size() - 1; i >= 0; i-- ) {
			
			PathElement pathEl = path.get(i);

			
			boolean inOptionalClause = false;
			int j = i;
			int optionalParentStartIndex = -1;
			while(j >= 0) {
				PathElement p = path.get(j);
				if(p.isOptional()) {
					optionalParentStartIndex = j;
					inOptionalClause = true;
					break;
				}
				j--;
			}
			
			
			Set<String> endpoints = null;
			
			VitalGraphCriteriaContainer endpointsContainer = null;
			
			if(previousEl != null && ! previousEl.isOptional()) {
				
				endpoints = new HashSet<String>();
				
				for(GraphObject g : previousEl.connectorResults ) {

					String endpointURI = null;
					
					if(previousEl.isHyperArc()) {
						if(!( g instanceof VITAL_HyperEdge) ) throw new RuntimeException("hyperedge arc connectors are expected  to be hyperedges");
						VITAL_HyperEdge hedge = (VITAL_HyperEdge) g;
						if(previousEl.isForwardNotReverse()) {
							endpointURI = hedge.getSourceURI();
						} else {
							endpointURI = hedge.getDestinationURI();
						}
					} else {
						if(!( g instanceof VITAL_Edge) ) throw new RuntimeException("edge arc connectors are expected  to be edges");
						VITAL_Edge edge = (VITAL_Edge) g;
						if(previousEl.isForwardNotReverse()) {
							endpointURI = edge.getSourceURI();
						} else {
							endpointURI = edge.getDestinationURI();
						}
					}
					
					endpoints.add(endpointURI);
					
				}
				
				/*
				if(endpoints.size() == 0) {
					
					if(previousEl.isOptional()) {
						//allow for empty endpoints
					} else {
						throw new NoBindingsException();
					}
				}
				*/
				
				
				
			}
			
			try {
				
			VitalSelectQuery endpointsSelect = VitalSelectQuery.createInstance();
			endpointsSelect.setOffset(0);
			endpointsSelect.setLimit(10000);
			endpointsSelect.setSegments(graphQuery.getSegments());
			
			VitalGraphCriteriaContainer endpointCriteriaContainer = endpointsSelect.getCriteriaContainer();
			
			if( pathEl.arc.endpointContainer.endpointCriteria > 0 ) {
				endpointsContainer = (VitalGraphCriteriaContainer) pathEl.arc.endpointContainer.container.clone();
				endpointCriteriaContainer.add(endpointsContainer);
			}
			if( endpoints != null && endpoints.size() > 0) {
				endpointCriteriaContainer.add(new VitalGraphQueryPropertyCriterion(VitalGraphQueryPropertyCriterion.URI).oneOf(toURIPropertiesList(endpoints)));
			}
			
			//select all?
			if(endpointCriteriaContainer.size() == 0 ){
				endpointCriteriaContainer.add(new VitalGraphQueryPropertyCriterion(VitalGraphQueryPropertyCriterion.URI).exists());
			}
			
			
			ResultList endpointsRS = executor.selectQuery(endpointsSelect);
			
			pathEl.endpointResults = endpointsRS;
			
			if(endpointsRS.getResults().size() == 0) {
				throw new NoBindingsException();
			}
			
			
			if(pathEl.arc.isTopArc()) {
				return;
			}
			
			Set<String> newEndpoints = new HashSet<String>();
			
			for(GraphObject ne : endpointsRS) {
				newEndpoints.add(ne.getURI());
			}
			
			
			String connectorProperty = null;
			
			if(pathEl.isHyperArc()) {
				
				if(pathEl.isForwardNotReverse()) {
					connectorProperty = VitalCoreOntology.hasHyperEdgeDestination.getURI();
				} else {
					connectorProperty = VitalCoreOntology.hasHyperEdgeSource.getURI();
				}
				
			} else {
				
				if( pathEl.isForwardNotReverse() ) {
					connectorProperty = VitalCoreOntology.hasEdgeDestination.getURI();
				} else {
					connectorProperty = VitalCoreOntology.hasEdgeSource.getURI();
					
				}
				
			}
			
			
			VitalSelectQuery edgesSelect = VitalSelectQuery.createInstance();
			edgesSelect.setOffset(0);
			edgesSelect.setLimit(10000);
			edgesSelect.setSegments(graphQuery.getSegments());
			
			VitalGraphCriteriaContainer edgesCC = edgesSelect.getCriteriaContainer();
			
			if(pathEl.arc.connectorContainer.connectorCriteria > 0) {
				edgesCC.add( (VitalGraphCriteriaContainer) pathEl.arc.connectorContainer.container.clone());
			}
			edgesCC.add(new VitalGraphQueryPropertyCriterion(connectorProperty).oneOf(toURIPropertiesList(newEndpoints)));
			
			
			ResultList connectorsRS = executor.selectQuery(edgesSelect);
			
//			if(endpoints != null && endpoints.size() >)
			pathEl.connectorResults = connectorsRS;
			
			
			if(connectorsRS.getResults().size() == 0) {
				
				throw new NoBindingsException();
			}
				
			previousEl = pathEl;  
			
			} catch (NoBindingsException e ) {
			
				if(inOptionalClause) {
						
					int forward = i;
					while(forward < path.size()) {
						path.get(forward).endpointResults = new ResultList();
						path.get(forward).connectorResults = new ResultList();
						forward++;
					}
						
					//clear resultsand rewind to pre optional start pathElement 
					while(i > optionalParentStartIndex) {
						i--;
						path.get(i).connectorResults = new ResultList();
						path.get(i).endpointResults = new ResultList();
					}

					previousEl = path.get(i);
					
				} else {
					throw new NoBindingsException();
				}
				
			}
				
			
		}
		
		
	}


	static class NoBindingsException extends RuntimeException {

		private static final long serialVersionUID = 1L;
		
	}



	private List<URIProperty> toURIPropertiesList(Set<String> nodesURIs) {
		List<URIProperty> l = new ArrayList<URIProperty>();
		for(String n : nodesURIs) {
			l.add(URIProperty.withString(n));
		}
		return l;
	}


	
	private void analyzeQuery() {

		//decompose the arcs!
		VitalGraphArcContainer topContainer = graphQuery.getTopContainer();
	
		
		//collect provdes names and validate
//		QueryAnalysis.getProvidesMap();
//		Map<String, VitalGraphValue> providesMap = topContainer.getProvidesMap();
		QueryAnalysis.collectAndValidateProvides(providesName2ValueParent, topContainer);
		
		
		QueryDecomposer decomposer = new QueryDecomposer(topContainer);
		patternsQueue = decomposer.decomposeQuery();
		
		
		//link criteria to arcs
		for(GraphPattern p : patternsQueue) {
			
			for( Path path : p ) {
				
				for( PathElement pe : path ) {
					
					Arc a = pe.arc;
					
					for( ProvidesValueParent pvp : providesName2ValueParent.values() ) {
						
						if(pvp.container == a.arcContainer) {
							
							pvp.arc = a;
							
						}
						
					}
					
				}
				
			}
			
		}
		
		//validate pvp map
		for(ProvidesValueParent pvp : providesName2ValueParent.values() ) {
			if(pvp.arc == null) throw new RuntimeException("Internal error: no arc associated with value criteria");
		}
		
	}



	private ResultList assembleResults() {
		
		ResultList r = new ResultList();
		
		Set<String> alreadyExported = null;
		if(graphQuery.getPayloads()) {
			alreadyExported = new HashSet<String>();
		}
		
		for(Binding b : allBindings) {
			
			GraphMatch gm = new GraphMatch();
			
			gm.setURI(URIGenerator.generateURI((VitalApp)null, GraphMatch.class));
			
			
			for(BindingEl el : b) {
				
				Arc arc = el.getArc();
				
				Capture capture = arc.getArcContainer().getCapture();
				
				
                if(arc.isTopArc()) {
					
                    GraphObject endpoint = el.getEndpoint();
					
					if( capture != Capture.NONE ) {
						
						gm.setProperty(arc.getSourceBind(), URIProperty.withString( endpoint.getURI() ) );
						
						if(alreadyExported != null && alreadyExported.add(endpoint.getURI())) {

						    if(resolver != null) endpoint = resolver.resolveGraphObject(endpoint);
						    
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

						    if(resolver != null) endpoint = resolver.resolveGraphObject(endpoint);
						    
							gm.setProperty(endpoint.getURI(), endpoint.toCompactString());
							
						}

						
					}
					
					
					if(capture == Capture.BOTH || capture == Capture.CONNECTOR) {
						
						GraphObject connector = el.getConnector();
						
                        gm.setProperty(arc.getConnectorBind(), URIProperty.withString( connector.getURI() ) );
						
						if(alreadyExported != null && alreadyExported.add(connector.getURI())) {
						
						    if(resolver != null) connector = resolver.resolveGraphObject(connector);
						    
							gm.setProperty(connector.getURI(), connector.toCompactString());
							
						}
						
					}
					
				}
				
			}
			
			r.getResults().add(new ResultElement(gm, 1D));
			
			
		}

		
		return r;
		
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
