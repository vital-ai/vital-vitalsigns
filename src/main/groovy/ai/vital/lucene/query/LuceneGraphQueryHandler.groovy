package ai.vital.lucene.query;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.search.IndexSearcher;

import ai.vital.lucene.model.LuceneSegment;
import ai.vital.vitalservice.query.VitalGraphQuery;

//index always returns all objects, edges also contain endpoints
public class LuceneGraphQueryHandler {

//	private VitalGraphQuery query
	
	private VitalGraphQuery query;
	
	private IndexSearcher searcher;
	
	private Map<Integer, URIResultElement> cachedResults = new HashMap<Integer, URIResultElement>();
	
	private static URIResultElement NULL = new URIResultElement();
	
//	private Map<List<QueryPathElement>, Map<String, URIResultElement>> path2Results = new HashMap<List<QueryPathElement>, Map<String, URIResultElement>>();

	private boolean singleSegmentMode;
	
	private LuceneSegment luceneSegment;
	
	public LuceneGraphQueryHandler(VitalGraphQuery query, LuceneSegment luceneSegment, IndexSearcher searcher, boolean singleSegmentMode) {
		super();
		this.query = query;
		this.luceneSegment = luceneSegment;
		this.searcher = searcher;
		this.singleSegmentMode = singleSegmentMode;
	}



	/**
	 * Single segment query always returns all matching objects
	 * @param segment
	 * @param expansionCriteria
	 * @return
	 * @throws IOException 
	 */
	/*
	public URIResultList processGraphQuery() throws IOException {
		
		List<String> rootUris = null;
		
		if(query instanceof VitalPathQuery) {
			rootUris = ((VitalPathQuery)query).getRootUris();
			if(rootUris == null || rootUris.size() < 1) {
				throw new IOException("VitalPathQuery rootUris list mustn't be null nor empty");
			}
		} else if(query instanceof VitalGraphQuery) {
		
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
			
			LuceneSelectQueryHandler sqh = new LuceneSelectQueryHandler(vsq, luceneSegment, searcher);
			
			URIResultList sqr = sqh.handleSelectQuery();

			rootUris = new ArrayList<String>(sqr.getResults().size());
			
			for(URIResultElement ure : sqr.getResults()) {
				
				rootUris.add(ure.URI);
				
			}
			
		}
		 
		
		List<List<QueryPathElement>> paths = new ArrayList<List<QueryPathElement>>(query.getPathsElements());
		
		int maxDepth = 0;
		
		//inspect path to see the direction of node expansion
		for(List<QueryPathElement> path : paths) {
			maxDepth = Math.max(maxDepth, path.size() - 1);
			path2Results.put(path, new HashMap<String, URIResultElement>());
		}
	
		
		Map<String, URIResultElement> alreadyResolved = new HashMap<String, URIResultElement>();
		
		Map<String, URIResultElement> results = new HashMap<String, URIResultElement>();
		
		
		
		//resolve root uris
		List<URIResultElement> resolved = resolve(rootUris);
		
		//if no node the exit
		if(resolved == null) resolved = new ArrayList<URIResultElement>();
		
		for(URIResultElement e : resolved) {
			alreadyResolved.put(e.URI, e);
			results.put(e.URI, e);
		}
		
		if(rootUris.size() > 0) {
			
			expand(rootUris, paths, alreadyResolved, 0, maxDepth);
			
			for(Map<String, URIResultElement> m : path2Results.values() ) {
				results.putAll(m);
			}
			
		}
		
		//now process results
		
		URIResultList l = new URIResultList();
		l.setResults(new ArrayList<URIResultElement>(results.values()));
		return l;
	}
	
	
	private List<URIResultElement> resolve(Collection<String> uris) throws IOException {

		List<URIResultElement> results = null;
		if(uris.size() < 1) return results;
		
		BooleanQuery q = new BooleanQuery();
		for(String u : uris) {
			q.add(new TermQuery(new Term(VitalSignsLuceneBridge.URI_FIELD, u)), Occur.SHOULD);
		}
		
		for(ScoreDoc sd : searcher.search(q, uris.size()).scoreDocs) {
			URIResultElement e = resolveObject(sd.doc, VitalSignsLuceneBridge.allStaticFieldsToLoad);
			if(e != null) {
				if(results == null) results = new ArrayList<URIResultElement>(uris.size());
				results.add(e);
			}
		}
		
		return results;
	}



	
	public void expand(Collection<String> rootURIs, List<List<QueryPathElement>> inputPaths, Map<String, URIResultElement> alreadyResolved, int depth, int maxDepth) throws IOException {
		
		if(rootURIs.size() < 1) return;
		
		//inspect path to see the direction of node expansion
		
		BooleanQuery edgesQuery = new BooleanQuery(true);
		
		//construct huge boolean

		for(String rootURI : rootURIs) {
			
			for(List<QueryPathElement> path : inputPaths) {
				
				if( depth < path.size() ) {
					
					QueryPathElement pe = path.get(depth);
					
					String field = null;
					
					if(VITAL_HyperEdge.class.isAssignableFrom( pe.getEdgeType() ) ) {
						
						if(pe.direction == Direction.reverse) {
							
							field = VitalSignsLuceneBridge.HYPER_EDGE_DEST_URI_FIELD;
							
						} else {
							
							field = VitalSignsLuceneBridge.HYPER_EDGE_SRC_URI_FIELD;
						}
						
					} else {
						
						if(pe.direction == Direction.reverse) {

							field = VitalSignsLuceneBridge.EDGE_DEST_URI_FIELD;
							
						} else {
							
							field = VitalSignsLuceneBridge.EDGE_SRC_URI_FIELD;
							
						}
					}
					
					BooleanQuery hEdgeQuery = new BooleanQuery();
					hEdgeQuery.add(new TermQuery(new Term(VitalSignsLuceneBridge.TYPE_FIELD, pe.getEdgeTypeURI())), Occur.MUST);
					hEdgeQuery.add(new TermQuery(new Term(field, rootURI)), Occur.MUST);
					edgesQuery.add(hEdgeQuery, Occur.SHOULD);
					
				}
			}
			
		}
		
		TopDocs docs = searcher.search(edgesQuery, 1000000);
		
		//when no edges found discard all the source nodes
		if(docs.scoreDocs.length < 1) {
			//invalidate all input paths
			for(List<QueryPathElement> path : inputPaths) {
				path2Results.remove(path);
			}
			return;
		}
		
		Set<String> toCollect = null;//new HashSet<String>(); 
		
		for(ScoreDoc sd : docs.scoreDocs) {
			
			//analyze the edges
			URIResultElement e = resolveObject(sd.doc, VitalSignsLuceneBridge.allStaticFieldsToLoad);
			if(e == null) continue;

			alreadyResolved.put(e.URI, e);
			
			//resolve other endpoints
			
			if(e.sourceURI != null && !alreadyResolved.containsKey(e.sourceURI)) {
				if(toCollect == null) toCollect = new HashSet<String>();
				toCollect.add(e.sourceURI);
			}
			
			if(e.destinationURI != null && !alreadyResolved.containsKey(e.destinationURI)) {
				if(toCollect == null) toCollect = new HashSet<String>();
				toCollect.add(e.destinationURI);
			}
			
		}
		
		//at this point all paths are assumed to be continuous
		
		//remove unverified paths
		
		Set<String> newURIs = null;
		
		if(toCollect != null) {
			List<URIResultElement> resolvedEndoints = resolve(toCollect);
			if(resolvedEndoints != null) {
				for(URIResultElement el : resolvedEndoints) {
					alreadyResolved.put(el.URI, el);
				}
			}
		}
		
		List<List<QueryPathElement>> pathsToVerify = new ArrayList<List<QueryPathElement>>(inputPaths);
		
		for(ScoreDoc sd : docs.scoreDocs) {
			
			//should already be resolved
			URIResultElement e = cachedResults.get(sd.doc);
			if(e == null) continue;
			
			//edge direction
			boolean forwardEdgeMatch = rootURIs.contains(e.sourceURI);
			boolean reverseEdgeMatch = rootURIs.contains(e.destinationURI);
			
			URIResultElement src = alreadyResolved.get(e.sourceURI);
			URIResultElement dest = alreadyResolved.get(e.destinationURI);
			
			//if(singleSegmentMode && ( src == null || dest == null ) ) continue;
			
			for(List<QueryPathElement> path : inputPaths) {
				
				QueryPathElement qpe = path.get(depth);
				
				if( ! qpe.getEdgeTypeURI().equals(e.typeURI)) continue;
				
				boolean match = false;
				
				if(forwardEdgeMatch && qpe.direction == Direction.forward) {

					if(!singleSegmentMode ||qpe.getDestObjectTypeURI() == null || dest==null || qpe.getDestObjectTypeURI().equals(dest.typeURI)) {
						
						match = true;
						
						if(depth < path.size() - 1) {
							if(newURIs == null) newURIs = new HashSet<String>();
							newURIs.add(e.destinationURI);
						}
						
					}
					
				} 
				
				if(reverseEdgeMatch && qpe.direction == Direction.reverse) {
					
					if(!singleSegmentMode || qpe.getDestObjectType() == null || src == null || qpe.getDestObjectTypeURI().equals(src.typeURI)) {
						
						match = true;
						
						if(depth < path.size() - 1) {
							if(newURIs == null) newURIs = new HashSet<String>();
							newURIs.add(e.sourceURI);
						}
						
					}
					
				}
				
				if(!match) continue;
				
				Map<String, URIResultElement> map = path2Results.get(path);
				if(src != null) {
					map.put(src.URI, src);
				}
				map.put(e.URI, e);
				if(dest != null) {
					map.put(dest.URI, dest);
				}
				
				//continuous path
				pathsToVerify.remove(path);
				
				
			}
			
		}
		
		//check which paths didn't make it
		for(List<QueryPathElement> path : pathsToVerify) {
			//broken path
			path2Results.remove(path);
			inputPaths.remove(path);
		}
		
		for(Iterator<List<QueryPathElement>> iterator = inputPaths.iterator(); iterator.hasNext(); ) {
			List<QueryPathElement> el = iterator.next();
			if(el.size() - 1 == depth) iterator.remove();
		}
		
		//no longer
		if(newURIs == null || inputPaths.size() < 1) return;
		
		
		expand(newURIs, inputPaths, alreadyResolved, depth + 1, maxDepth);
			
	}
	
	private URIResultElement resolveObject(int docID, Set<String> fieldsToLoad) throws IOException {

		URIResultElement r = cachedResults.get(docID);
		
		if(r == NULL) {
			return null;
		} else if(r != null) {
			return r;
		}

		Document doc = null;
		if(fieldsToLoad != null) {
			doc = searcher.getIndexReader().document(docID, fieldsToLoad);
		} else {
			doc = searcher.getIndexReader().document(docID);
		}
		if(doc == null) {
			cachedResults.put(docID, NULL);
			return null;
		}
		
		URIResultElement el = new URIResultElement();
		el.segment = luceneSegment; 
		el.URI = doc.get(VitalSignsLuceneBridge.URI_FIELD);
		el.typeURI = doc.get(VitalSignsLuceneBridge.VITAL_TYPE_FIELD);
		
		ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(el.typeURI);
		
		if(cm == null) throw new RuntimeException("class not found in VitalSigns: " + el.typeURI);
		
		Class<? extends GraphObject> cls = cm.getClazz();
		
		if(VITAL_Edge.class.isAssignableFrom(cls)) {
			el.destinationURI = doc.get(VitalSignsLuceneBridge.EDGE_DEST_URI_FIELD);
			el.sourceURI = doc.get(VitalSignsLuceneBridge.EDGE_SRC_URI_FIELD);
		} else if(VITAL_HyperEdge.class.isAssignableFrom(cls)) {
			el.destinationURI = doc.get(VitalSignsLuceneBridge.EDGE_DEST_URI_FIELD);
			el.sourceURI = doc.get(VitalSignsLuceneBridge.EDGE_SRC_URI_FIELD); 
		}
		
		cachedResults.put(docID, el);
		
		return el;
	}
	*/

}
