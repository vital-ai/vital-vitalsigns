package ai.vital.service.lucene.impl

import ai.vital.vitalsigns.model.GraphMatch
import ai.vital.vitalsigns.model.VITAL_Node
import java.util.Map.Entry
import org.apache.commons.collections.buffer.PriorityBuffer;
import ai.vital.lucene.model.LuceneSegment;
import ai.vital.lucene.model.impl.LuceneResultsProvider;
import ai.vital.lucene.query.URIResultElement;
import ai.vital.lucene.query.URIResultList;
import ai.vital.lucene.query.LuceneSelectQueryHandler;
import ai.vital.lucene.query.LuceneSelectQueryHandler.ExternalSortComparator;
import ai.vital.lucene.query.LuceneSelectQueryHandler.ResolvedFieldsResult;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.query.AggregationType;
import ai.vital.vitalservice.query.CollectStats;
import ai.vital.vitalservice.query.QueryStats;
import ai.vital.vitalservice.query.QueryTime;
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalExportQuery;
import ai.vital.vitalservice.query.VitalGraphQuery;
import ai.vital.vitalservice.query.VitalPathQuery;
import ai.vital.vitalservice.query.VitalQuery;
import ai.vital.vitalservice.query.VitalSelectAggregationQuery;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalservice.query.VitalSortProperty;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.global.GlobalHashTable;
import ai.vital.vitalsigns.model.AggregationResult;

import ai.vital.vitalsigns.model.GraphObject;

import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalOrganization;
import ai.vital.vitalsigns.query.PathQueryImplementation;
import ai.vital.vitalsigns.query.graph.GraphQueryImplementation;
import ai.vital.vitalsigns.query.graph.GraphQueryImplementation.ResultsProvider;
import ai.vital.vitalsigns.uri.URIGenerator;

public class LuceneServiceQueriesImpl {

	public static ResultList handleQuery(VitalOrganization organization, VitalApp app, VitalQuery query, List<LuceneSegment> segments) {
		
	    CollectStats collectStats = query.getCollectStats();
	    QueryStats queryStats = null;
	    if(collectStats == CollectStats.detailed || collectStats == CollectStats.normal) {
	        queryStats = new QueryStats();
	        if(collectStats == CollectStats.detailed) {
	            queryStats.setQueriesTimes(new ArrayList<QueryTime>());
	        } else {
	            queryStats.setQueriesTimes(null);
	        }
	    }
	    
		if(query instanceof VitalSelectQuery) {
			return LuceneServiceQueriesImpl.selectQuery(segments, (VitalSelectQuery) query, queryStats);
		} else if(query instanceof VitalGraphQuery) {
		    long start = System.currentTimeMillis();
			// Executor executor = new LuceneGraphQueryExecutor(segments);
			// GraphQueryImplementation impl = new GraphQueryImplementation(executor, (VitalGraphQuery) query);
			ResultsProvider provider = new LuceneResultsProvider(segments, queryStats);
			GraphQueryImplementation impl = new GraphQueryImplementation(provider, (VitalGraphQuery) query);
			ResultList rl = impl.execute();
			if(queryStats != null) {
			    queryStats.setQueryTimeMS(System.currentTimeMillis() - start);
			}
			rl.setQueryStats(queryStats);
			return rl;
		} else if(query instanceof VitalPathQuery) {
			try {
				// App app = new App();
				//	app.setID("vitalsigns-internal");
				// app.setOrganizationID(organization.getID());
				return new PathQueryImplementation((VitalPathQuery)query, new LucenePathQueryExecutor(organization, app, segments, queryStats)).execute();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new RuntimeException("Local query of type " + query.getClass().getCanonicalName() + " not supported");
		}
		
	}
	
	public static URIResultList selectQueryInner(List<LuceneSegment> luceneSegments, VitalSelectQuery query, QueryStats queryStats) throws IOException {

		URIResultList outputList = null;
		
		AggregationType aggType = null;
		Integer agg_count = null;
		Double agg_sum = null;
		
		Double agg_min = null;
		Double agg_max = null;
		
		List distinctValues = null;
		
		if(query instanceof VitalSelectAggregationQuery) {
			
			if(query.isProjectionOnly()) {
				throw new RuntimeException("Cannot use projection only in VitalSelectAggregationQuery");
			}
			
			for(LuceneSegment s : luceneSegments) {
				
				if(!( s.getConfig().isStoreObjects() || s.getConfig().isStoreNumericFields())) {
					throw new RuntimeException("Segment ${s.getID()} does not support aggretaion select queries - not a store or storeNumericFields=false");
				}
				
			}
			
			// if it's aggregation the ignore limit and skip
			VitalSelectAggregationQuery vsaq = (VitalSelectAggregationQuery)query;
			aggType = vsaq.getAggregationType();
			agg_count = 0;
			agg_sum = 0d;
			
		}
		
		
		//if only one lucene segment specified just assemble a single page 
		if(luceneSegments.size() == 1) {
			
			outputList = luceneSegments.get(0).selectQuery(query, queryStats);
			
			distinctValues = outputList.getDistinctValues();
			
			agg_count = outputList.agg_count;
			agg_sum = outputList.agg_sum;
			
			agg_min = outputList.agg_min;
			agg_max = outputList.agg_max;
			
//			if(query.isProjectionOnly()) {
//				ResultList rs = new ResultList()
//				rs.totalResults = outputList.totalResults
//				return ou
//			} 
			
		} else {

			//for projections just sum up the elements
			if(query.isProjectionOnly()) {
				
				outputList = new URIResultList();
				outputList.totalResults = 0	;
				outputList.results = new ArrayList<URIResultElement>(0);
				
				for( LuceneSegment segment : luceneSegments ) {
				
					URIResultList  sublist = segment.selectQuery(query, queryStats);
					outputList.totalResults	 += sublist.totalResults;
					outputList.offset = query.getOffset();
					outputList.limit = query.getLimit();
				}
				
			} else {
				
				int initialOffset = query.getOffset();
				int initialLimit = query.getLimit();
			
				List<VitalSortProperty> sortProperties = query.getSortProperties();
				Map<String, ResolvedFieldsResult> resolvedResultsMap = null;
				boolean collectValues = false;
				if(sortProperties != null && sortProperties.size() > 0) {
				    collectValues = true;
				    resolvedResultsMap = new HashMap<String, ResolvedFieldsResult>();
				}
				
				outputList = new URIResultList();
				outputList.limit = initialLimit;
				outputList.offset = initialOffset;
				int _size = initialLimit-initialOffset; 
				outputList.results = new ArrayList<URIResultElement>(_size < 10000 && _size > 0 ? _size : 10000);
			
				//query with offset = 0
				query.setOffset(0);
				
				int totalResults = 0;
				
				int maxLength = initialOffset + initialLimit;
				
				if(maxLength < 1) maxLength = 1; 
				
				PriorityBuffer priorityBuffer = new PriorityBuffer(maxLength, true, collectValues ? new UriResultElementWithValuesComparator(sortProperties, resolvedResultsMap) : uriResultElementComparator);
				
				for( LuceneSegment segment : luceneSegments ) {

					URIResultList sublist = segment.selectQueryWithCollectResolvedFields(query, queryStats, collectValues);					
					
					List subDistinctValues = sublist.getDistinctValues();
					
					if(subDistinctValues != null) {
						if(distinctValues != null) {
							for(Object dv : subDistinctValues) {
								if(!distinctValues.contains(dv)) {
									distinctValues.add(dv);
								}
							}
						} else {
							distinctValues = subDistinctValues;
						}
					}
					
					totalResults += sublist.totalResults;
					
					if(aggType != null) {
						
						agg_count += sublist.agg_count;
						agg_sum   += sublist.agg_sum;
						
						if(sublist.agg_min != null && (agg_min == null || agg_min.doubleValue() > sublist.agg_min.doubleValue())) {
							agg_min = sublist.agg_min;
						}
						
						if(sublist.agg_max != null && (agg_max == null || agg_max.doubleValue() < sublist.agg_max.doubleValue() )) {
							agg_max = sublist.agg_max;
						}
						
					} else {
					
					    if(collectValues) {
					        for(ResolvedFieldsResult r : sublist.resolvedFields) {
					            resolvedResultsMap.put(r.URI, r);
					        }
					    }
					    
						for(URIResultElement el : sublist.results) {

	                      priorityBuffer.add(el);
						    
                          if(priorityBuffer.size() > maxLength) {
                              URIResultElement removed = (URIResultElement) priorityBuffer.remove();
                              if(resolvedResultsMap != null) {
                                  resolvedResultsMap.remove(removed.URI);
                              }
                          }
						        
						}
						
					}
					
					
				}
				
				if(distinctValues != null && query.getDistinctSort() != null) {
					
					boolean ascNotDesc = VitalSelectQuery.asc.equals(query.getDistinctSort());
					
					//sort them
					Collections.sort(distinctValues, new LuceneSelectQueryHandler.DistinctValuesComparator(ascNotDesc));
					
				}
				
				if(distinctValues != null) {
					if(query.getDistinctFirst() && distinctValues.size() > 1) {
						distinctValues = new ArrayList<Object>(Arrays.asList(distinctValues.get(0)));
					} else if(query.getDistinctLast() && distinctValues.size() > 1){
						distinctValues = new ArrayList<Object>(Arrays.asList(distinctValues.get(distinctValues.size()-1)));
					}
				}
				
				outputList.distinctValues = distinctValues;
				
				outputList.totalResults = totalResults;
				
				outputList.agg_count = agg_count;
				outputList.agg_sum = agg_sum;
				outputList.agg_max = agg_max;
				outputList.agg_min = agg_min;
				
				
				if(collectValues) {
				    
				    List<URIResultElement> tempList = new ArrayList<URIResultElement>(maxLength);
				    while(priorityBuffer.size() > 0) {
				        URIResultElement el = (URIResultElement) priorityBuffer.remove();
				        tempList.add(0, el);
				    }
				    
				    for(int i = initialOffset ; i < Math.min(tempList.size(), initialOffset + initialLimit); i++) {
				        outputList.results.add(tempList.get(i));
				    }
				    
				} else {
				    
				    //copy the results into list reverse order
				    int index = 0;
				    
	                while(priorityBuffer.size() > 0) {
	                    
	                    URIResultElement el = (URIResultElement) priorityBuffer.remove();
	                    
	                   if(index++ >= initialOffset) {
	                            
	                        outputList.results.add(0, el);
	                            
	                    }
	                    
	                }
				    
				}
				


			}

		}
		
		return outputList;
		
	}
	
	public static ResultList selectQuery(List<LuceneSegment> luceneSegments, VitalSelectQuery query) {
	    return selectQuery(luceneSegments, query, null);
	}
	
	public static ResultList selectQuery(List<LuceneSegment> luceneSegments, VitalSelectQuery query, QueryStats queryStats) {
		
		if(query instanceof VitalExportQuery) {
			
			try {
				if(luceneSegments.size() != 1) throw new IOException("Exactly 1 segment expected for export query");
				return luceneSegments.get(0).exportQuery((VitalExportQuery) query, queryStats);
			} catch (IOException e) {
				ResultList rl = new ResultList();
				rl.setStatus(VitalStatus.withError(e.getLocalizedMessage()));
				return rl;
			}
			
		}
		
		URIResultList outputList = null;
		try {
			outputList = selectQueryInner(luceneSegments, query, queryStats);
		} catch (IOException e) {
			ResultList rl = new ResultList();
			rl.setStatus(VitalStatus.withError(e.getLocalizedMessage()));
			return rl;
		}
		
		//if it's aggregate function
		ResultList results = null;
        try {
            results = convertURIsListToResultList(outputList, queryStats);
        } catch (IOException e) {
            ResultList rl = new ResultList();
            rl.setStatus(VitalStatus.withError(e.getLocalizedMessage()));
            return rl;
        }
        
		AggregationType aggType = null;
		Integer agg_count = outputList.agg_count;
		Double agg_sum = outputList.agg_sum;
		
		Double agg_min = outputList.agg_min;
		Double agg_max = outputList.agg_max;
		
		if(query instanceof VitalSelectAggregationQuery) {
			
			VitalSelectAggregationQuery vsaq = (VitalSelectAggregationQuery)query;
			aggType = vsaq.getAggregationType();
			
		}
		
		if(aggType != null) {
			
			Double value = null;
			
			if(AggregationType.average == aggType) {
				
				if(agg_count == 0) {
					value = Double.NaN;
				} else {
					value = agg_sum / (double)agg_count;
				}
				
			} else if(AggregationType.count == aggType) {
				value = (double) agg_count;
			} else if(AggregationType.max == aggType) {
				if(agg_max != null) {
					value = agg_max;
				} else {
					value = Double.NaN;
				}
			} else if(AggregationType.min == aggType) {
				if(agg_min != null) {
					value = agg_min;
				} else {
					value = Double.NaN;
				}
			} else if(AggregationType.sum == aggType) {
				value = agg_sum;
			} else {
				throw new RuntimeException("Unhandled aggregation type: " + aggType);
			}
			
			//nasty 
			AggregationResult res = new AggregationResult();
			res.setURI(URIGenerator.generateURI(null, res.getClass(), true));
			res.setProperty("aggregationType", aggType.name());
			res.setProperty("value", value);
			results.getResults().add(new ResultElement(res, 1D));
			
		}
				
		return results;
		
	}
	
	static ResultList convertURIsListToResultList(URIResultList outputList, QueryStats queryStats) throws IOException {
		
		ResultList results = new ResultList();
		results.setLimit(outputList.limit);
		results.setOffset(outputList.offset);
		results.setTotalResults(outputList.totalResults);
		results.setQueryStats(queryStats);
		
		if(outputList.distinctValues != null) {
			
			double score = 0d;
			for(Object dv : outputList.distinctValues) {
				
				GraphMatch gm = (GraphMatch) new GraphMatch().generateURI((VitalApp)null);
				gm.setProperty("value", dv);
				
				results.getResults().add(new ResultElement(gm, score));
				score += 1d;
				
			}
			
			
			return results;
			
		}
		
		Map<LuceneSegment, Set<String>> segmentToURIs = new HashMap<LuceneSegment, Set<String>>();
		Map<String, GraphObject> resolvedGraphObjects = new HashMap<String, GraphObject>();
		
		for(URIResultElement e : outputList.results) {
			
			Set<String> uris = segmentToURIs.get(e.segment);
			if(uris == null) {
				uris = new HashSet<String>();
				segmentToURIs.put(e.segment, uris);
			}

			uris.add(e.URI);
						
		}
		
		
		for(Entry<LuceneSegment, Set<String>> e : segmentToURIs.entrySet()) {
			
			
			//special case for cache segment 
			if(e.getKey().getID().equals(VitalSigns.CACHE_DOMAIN)) {
				
				for(String uri : e.getValue()) {
					
					GraphObject g = GlobalHashTable.get().get(uri);
					
					if(g != null) {
						resolvedGraphObjects.put(g.getURI(), g);
					}
					
				}
				
			} else {
			
				if( e.getKey().getConfig().isStoreObjects() ) {
					
					List<GraphObject> gos = e.getKey().getGraphObjectsBatch(e.getValue(), queryStats);
							
					for(GraphObject g : gos) {
						resolvedGraphObjects.put(g.getURI(), g);
					}
					
				} else {
					
					//fake resolving, just listing URIs
					
					for(String uri : e.getValue()) {
						GraphObject g = new VITAL_Node();
						g.setURI(uri);
						resolvedGraphObjects.put(uri, g);
					}
					
				}
				
			}
			
		}
		
		results.setResults(new ArrayList<ResultElement>(outputList.results.size()));
		
		for(URIResultElement el : outputList.results) {
			
			GraphObject g = resolvedGraphObjects.get(el.URI);
			if(g == null) continue;
			
			results.getResults().add(new ResultElement(g, el.score));
			
		}
		
		return results;
		
	}
	
	
	static Comparator<URIResultElement> uriResultElementComparator = new Comparator<URIResultElement>(){
		
		public int compare(URIResultElement e1, URIResultElement e2) {
			
			int c = new Double(e1.score).compareTo(e2.score);
			
			if(c != 0) return c;
			
			return e1.URI.compareTo(e2.URI);
			
		}
		
	};
	
	static class UriResultElementWithValuesComparator implements Comparator<URIResultElement> {

	    Map<String, ResolvedFieldsResult> map = null;
	    
	    ExternalSortComparator comparator = null;
	    
	    public UriResultElementWithValuesComparator(List<VitalSortProperty> sortProperties, Map<String, ResolvedFieldsResult> resultsMap) {
	        comparator = new ExternalSortComparator(LuceneSelectQueryHandler.toSortPropertyColumns(sortProperties), true);
	        this.map = resultsMap;
        }
	    
        @Override
        public int compare(URIResultElement e1, URIResultElement e2) {
            ResolvedFieldsResult r1 = map.get(e1.URI);
            ResolvedFieldsResult r2 = map.get(e2.URI);
            if(r1 == null) throw new RuntimeException("No resolved field value for result: " + e1.URI);
            if(r2 == null) throw new RuntimeException("No resolved field value for result: " + e2.URI);
            return comparator.compare(r1, r2);
        }
        
	}


	private Map<String, URIResultElement> cached = new HashMap<String, URIResultElement>();
	
	private URIResultElement NULL = new URIResultElement();

//	private Map<List<QueryPathElement>, Map<String, URIResultElement>> path2Results = new HashMap<List<QueryPathElement>, Map<String, URIResultElement>>();

	
	public ResultList graphQuery(List<LuceneSegment> luceneSegments, VitalGraphQuery query) {
		throw new RuntimeException("TODO, not implemented");
	}
	
	/*
	public ResultList graphQuery(List<LuceneSegment> luceneSegments, AbstractVitalGraphQuery query) {
		URIResultList urisRS = null
		
		if(luceneSegments.size() == 1) {
			
			urisRS = luceneSegments.get(0).graphQuery(query, true)
			
		} else {
		
			//process paths one level at a time
//			Map<String, URIResultElement> resultsElements = new LinkedHashMap<String, URIResultElement>();
		
			List<List<QueryPathElement>> inputPaths = new ArrayList<List<QueryPathElement>>(query.pathsElements)
			
			
			List<String> rootUris = null;
			
			if(query instanceof VitalGraphQuery) {
				
				//execute select query over all segments

				VitalGraphQuery vgq = (VitalGraphQuery) query;
				
				VitalQueryContainer rootQuery = vgq.getRootQuery();
				
				//execute select query to obtain roots
				
				//find root uris by executing select query
				VitalSelectQuery vsq = new VitalSelectQuery();
				
				vsq.setComponents(rootQuery.getComponents());
				vsq.setOffset(0);
				vsq.setLimit(10000);
				vsq.setProjectionOnly(false);
				vsq.setSegments(vgq.getSegments());
				vsq.setType(rootQuery.getType());
				
				URIResultList sqr = selectQueryInner(luceneSegments, vsq)
				
				rootUris = new ArrayList<String>(sqr.getResults().size());
				
				for(URIResultElement ure : sqr.getResults()) {
					
					rootUris.add(ure.URI);
					
				}
								
			} else if(query instanceof VitalPathQuery){
			
				rootUris = ((VitalPathQuery)query).rootUris;
			
			} else {
				throw new RuntimeException("Unhandled query type: " + query.getClass().getCanonicalName())
			}
			
			
			processDepth(inputPaths, luceneSegments, 0, new HashSet<String>(rootUris));
			
			urisRS = new URIResultList()
			
			
			Map<String, URIResultElement> t = new HashMap<String, URIResultElement>();
			
			for(Map<String, URIResultElement> m : path2Results.values()) {
				t.putAll(m)
			}
			
			urisRS.results = new ArrayList<URIResultElement>(t.values());
			urisRS.totalResults = urisRS.results.size()
			
		}
		
		ResultList results = convertURIsListToResultList(urisRS);
		
		return results;
		
	}
	
	void processDepth(List<List<QueryPathElement>> inputPaths, List<LuceneSegment> luceneSegments, int depth, Set<String> rootURIs) {
		
		if(rootURIs.size() < 1) return
		
		Set<String> newRootURIs = new HashSet<String>()
		
		List<List<QueryPathElement>> shortPaths = [] 
		
		Map<List<QueryPathElement>, List<QueryPathElement>> shortPathToFull = new HashMap<List<QueryPathElement>, List<QueryPathElement>>();
		
		for(List<QueryPathElement> path : inputPaths) {
			if(depth < path.size()) {
				List<QueryPathElement> shortPath = [path.get(depth)]
				shortPaths.add(shortPath)
				shortPathToFull.put(shortPath, path);
			}
		}
		
		if(shortPaths.size() < 1) return
		
		VitalPathQuery subQuery = new VitalPathQuery()
		subQuery.rootUris = new ArrayList<String>(rootURIs);
		subQuery.pathsElements = shortPaths;
		
		//sub graph query
		Map<String, URIResultElement> endpoints = new HashMap<String, URIResultElement>();
		
		//check each element if endpoints/edges are collected and new uris
		List<URIResultElement> edges = new ArrayList<URIResultElement>();
		
		Set<String> missingNodes = new HashSet<String>();
		
		for(LuceneSegment segment : luceneSegments) {
			
			URIResultList subRS = segment.graphQuery(subQuery, false);
			
			for(URIResultElement el : subRS.results) {
				
				//filter the results
				if(el.sourceURI != null) {
					//edge/hyperedge
					//check if it's edge
					edges.add(el);
					
				} else {
					//nodes / hypernodes
					endpoints.put(el.URI, el)
				}
				
				cached.put(el.URI, el);
				
				
			}
			
			
		}
		
		if(depth == 0) {
			
			for(List<URIResultElement> path: inputPaths) {
				Map<String, URIResultElement> m = new HashMap<String, URIResultElement>()
				path2Results.put(path, m)
			}
			
			for(String u : rootURIs) {
				URIResultElement e = endpoints.get(u);
				if(e == null) {
					e = fetchURIElement(u, luceneSegments)
				}
				if(e == null) continue;

				for(List<URIResultElement> path: inputPaths) {
					path2Results.get(path).put(e.URI, e);
				}
				
			}
		}
		
		List<List<QueryPathElement>> pathsToValidate = new ArrayList<List<QueryPathElement>>(inputPaths)
		
		for(URIResultElement edge : edges) {

			for(List<QueryPathElement> shortPath : shortPaths) {
				
				QueryPathElement qpe = shortPath[0];

				if(qpe.edgeTypeURI != edge.typeURI) continue;

				boolean passed = false
				
				String baseEndpointURI = null
				String otherEndpointURI = null
				
				if(qpe.direction == Direction.forward && rootURIs.contains(edge.sourceURI)) {
				
//					if( qpe.destObjectTypeURI == null || qpe.destObjectTypeURI == edge.typeURI ) {
						otherEndpointURI = edge.destinationURI
						baseEndpointURI = edge.sourceURI
						passed = true
//					}
					
					
						
				} else if(qpe.direction == Direction.reverse && rootURIs.contains(edge.destinationURI)) {
				
//					if( qpe.destObjectTypeURI == null || qpe.destObjectTypeURI == edge.typeURI ) {
						otherEndpointURI = edge.sourceURI;
						baseEndpointURI = edge.destinationURI
						passed = true
//					}
				}			
				
				if(!passed) continue
				
				List<QueryPathElement> longPath = shortPathToFull.get(shortPath)
				
				pathsToValidate.remove(longPath);
				
				URIResultElement baseEndpoint = endpoints.get(baseEndpointURI);
				URIResultElement otherEndpoint = endpoints.get(otherEndpointURI);
				
				if(baseEndpoint == null) {
					baseEndpoint = fetchURIElement(baseEndpointURI, luceneSegments)
					if(baseEndpoint != null) {
						endpoints.put(baseEndpointURI, baseEndpoint)
					}
				}
				
				if(otherEndpoint == null) {
					otherEndpoint = fetchURIElement(otherEndpointURI, luceneSegments)
					if(otherEndpoint != null) {
						endpoints.put(otherEndpointURI, otherEndpoint)
					}
				}
				
				//check other endpoint now
				if( qpe.destObjectTypeURI == null || qpe.destObjectTypeURI == otherEndpoint?.typeURI ) {
					passed = true
				} else {
					continue;
				}
				
				if(otherEndpoint != null) {
					
					newRootURIs.add(otherEndpoint.URI)
					if( qpe.collectDestObjects == CollectDestObjects.yes ) {
						//
						path2Results.get(longPath).put(otherEndpoint.URI, otherEndpoint)
					}
					
				}
				
				if(qpe.collectEdges == CollectEdges.yes) {
					path2Results.get(longPath).put(edge.URI, edge)
				}
			}
			
		}
		
		List<List<QueryPathElement>> newPaths = new ArrayList<List<QueryPathElement>>()
		
		for(List<QueryPathElement> path : inputPaths) {
			
			if(pathsToValidate.contains(path)) {
				path2Results.remove(path)
				continue
			}
			
			if( depth + 1 < path.size() ) {
				newPaths.add(path);
			}
			
		}
		if(newPaths.size() < 1) return;
		
		processDepth(newPaths, luceneSegments, depth + 1, newRootURIs);
		
	}
	
	public URIResultElement fetchURIElement(String URI, List<LuceneSegment> luceneSegments) {
		
		URIResultElement el = cached.get(URI);
		if(el == NULL) return null;
		
		if(el != null) return el;
		
		for(LuceneSegment s : luceneSegments) {
			
			el = s.getURIResultElement(URI);
			if(el != null) {
				cached.put(URI, el);
				return el
			}
			
		}
		
		cached.put(URI, NULL);
		return null
		
	}
	
	*/
}
