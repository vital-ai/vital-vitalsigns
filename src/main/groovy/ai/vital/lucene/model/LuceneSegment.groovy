package ai.vital.lucene.model


import java.nio.charset.StandardCharsets
import java.util.Map.Entry

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TrackingIndexWriter;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IOContext.Context;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.lucene.query.LuceneExportQueryHandler;
import ai.vital.lucene.query.LuceneSelectQueryHandler;
import ai.vital.lucene.query.URIResultElement;
import ai.vital.lucene.query.URIResultList;
import ai.vital.service.lucene.model.LuceneSegmentConfig;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.query.QueryStats;
import ai.vital.vitalservice.query.QueryTime;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalExportQuery;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.BlockIterator;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.VitalBlock;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.model.GraphObject;

import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalOrganization;
import ai.vital.vitalsigns.model.VitalSegment;
import ai.vital.vitalsigns.model.container.GraphObjectsIterable;
import ai.vital.vitalsigns.model.properties.Property_hasProvenance;
import ai.vital.vitalsigns.model.properties.Property_hasSegmentID;
import ai.vital.vitalsigns.model.properties.Property_isReadOnly;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;


public class LuceneSegment implements GraphObjectsIterable, Closeable {

    public static boolean TRACKING_INDEX_WRITER = false;
    
	private final static Logger log = LoggerFactory.getLogger(LuceneSegment.class);
	
	private Object readerSearcherWrapperLock = new Object();
	
	private Object commitsLock = new Object();
	
	int commitsCount = 0;
	
	private int bufferedOpsCount = 0;
	
	private Long lastCommitTimestamp = null; 
	
	private Long firstBufferedElementTimestamp = null;
	
	// this is basically "Index"
	
	// in memory or on disk
	
    VitalOrganization organization;
    
    VitalApp app;
    
    VitalSegment segment;
    
	LuceneSegmentConfig config;
	
	private String path;
	
	private boolean opened = false;
	
	private IndexWriter writer;
	
	//additional 
	private TrackingIndexWriter tiwriter;
	private ReferenceManager<IndexSearcher> searcherManager; 
	ControlledRealTimeReopenThread<IndexSearcher> nrtReopenThread;
	
	private Directory dir;
	
	private IndexReaderSearcherWrapper readerSearcherWrapper;
	
//	private SimpleNoLCAnalyzer analyzer = new SimpleNoLCAnalyzer()
		
	private VitalWhitespaceAnalyzer analyzer = new VitalWhitespaceAnalyzer(Version.LUCENE_47);
	//	private IndexReader reader;
		
	
	private Map<String, GraphObject> inmemoryStore = null;
	
	
	//node type index - all nodes are grouped by their class
	Map<Class<? extends GraphObject>, Set<String>> typesCache = null;
	
	//edges index
	private Map<String, Set<String>> srcURI2Edge = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> destURI2Edge = new HashMap<String, Set<String>>();
	
	//hyper edges index
	private Map<String, Set<String>> srcURI2HyperEdge = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> destURI2HyperEdge = new HashMap<String, Set<String>>();
	
	public String getID() {
		return (String) segment.getRaw(Property_hasSegmentID.class);
	}
	
	public boolean isReadOnly() {
	    Boolean v = (Boolean) segment.getRaw(Property_isReadOnly.class);
	    return v != null ? v.booleanValue() : false;
	}
	
	public LuceneSegment(VitalOrganization organization, VitalApp app, VitalSegment segment, LuceneSegmentConfig config) {
        this.organization = organization;
        this.app = app;
        this.segment = segment;
        this.config = config;
	}
	
	public synchronized void open() throws IOException {
		if(opened) throw new IOException("Segment already opened: " + getID());
		
		LuceneSegmentType type = config.getType();
		
		log.debug("Opening lucene index - type: {}, path: {}", type, path);
		
		long start = System.currentTimeMillis();

		String path = config.getPath();
		
		if(path != null) {
			File pathF = new File(path);
			if(!pathF.exists()) {
				if(!pathF.mkdirs()) throw new IOException("Couldn't create index directory: " + pathF.getAbsolutePath());
			}
			if(!pathF.isDirectory()) {
				throw new IOException("Index path is not a directory: " + pathF.getAbsolutePath());
			}
		}
				
		try {
			
			if(type == LuceneSegmentType.disk) {
		
				if(path == null) throw new IOException("Index path is required for disk index type, id: " + getID());
				
				dir = FSDirectory.open(new File(path));
				
				if(!TRACKING_INDEX_WRITER) {
				    
				    if(this.config.isBufferWrites()) {
				    
				        dir = new NRTCachingDirectory(dir, 10, 128);
				        
				    }
				    
				}
				
			} else if(type == LuceneSegmentType.memory) {
				
			
				if(path != null) {
					
					log.debug("Loading RAM directory from path: {}", path);
				
					dir = new RAMDirectory(FSDirectory.open(new File(path)), new IOContext(Context.READ));
					
				} else {
				
					log.debug("Creating empty RAM directory - no path specified");
				
					dir = new RAMDirectory();
				
				}
				
				if(config.isStoreObjects()) {
					this.inmemoryStore = Collections.synchronizedMap( new HashMap<String, GraphObject>(65536) );
					this.typesCache = Collections.synchronizedMap(new HashMap<Class<? extends GraphObject>, Set<String>>());
					this.srcURI2Edge = new HashMap<String, Set<String>>();
					this.destURI2Edge = new HashMap<String, Set<String>>();
					this.srcURI2HyperEdge = new HashMap<String, Set<String>>();
					this.destURI2HyperEdge = new HashMap<String, Set<String>>();
				}
			
				
			} else {
				
				throw new IOException("Unknown lucene index type: " + type);
				
			}

			IndexReader _reader = null;
			
			if(isReadOnly()) {
				
			    _reader = DirectoryReader.open(dir);
				
//				_reader = IndexReader.open(dir, true);
				
				
			} else {
			
				IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47, analyzer);

				//reenable
//				iwc.setRAMBufferSizeMB(128);
//				iwc.setMaxBufferedDocs(1000000);
				
//				StoredFieldsFormat sff = iwc.codec.storedFieldsFormat()
//				
//				if(config.isCompressStoredFields()) {
//				
//					iwc.setCodec(new CustomLucenceCodec())
//						
//				} else {
//				
//				}
//				
								
				writer = new VitalIndexWriter(dir, iwc);
				
				if(TRACKING_INDEX_WRITER) {
				    
				    tiwriter = new TrackingIndexWriter(writer);
				    searcherManager = new SearcherManager(writer, true, null);
				    
				    //=========================================================
				    // This thread handles the actual reader reopening.
				    //=========================================================
				    nrtReopenThread = new ControlledRealTimeReopenThread<IndexSearcher>(
				            tiwriter, searcherManager, 1.0, 0.1);
				    nrtReopenThread.setName("NRT Reopen Thread");
				    nrtReopenThread.setPriority(Math.min(Thread.currentThread().getPriority() + 2, Thread.MAX_PRIORITY));
				    nrtReopenThread.setDaemon(true);
				    nrtReopenThread.start();
				    
				} else {
				    
    				_reader = IndexReader.open(writer, true);
				    
				}
				
			}

			if(TRACKING_INDEX_WRITER) {
			   
			} else {
			    IndexSearcher _searcher = new IndexSearcher(_reader);
//			    _searcher.setDefaultFieldSortScoring(true, false);
			    readerSearcherWrapper = new IndexReaderSearcherWrapper(_reader, _searcher);
			    
			}
			
			
			
			
		} catch(IOException ex) {

			log.error("Error when opening lucene index: " + ex.getLocalizedMessage(), ex);
			
			ex.printStackTrace();
			
			throw ex;

		}
		
		long stop = System.currentTimeMillis();
		
		log.debug("Lucene index opened successfully, documents count: {}, operation time: {}ms", writer != null ? writer.numDocs() : "", (stop -start));
		
		if(config.isBufferWrites()) {
		    lastCommitTimestamp = System.currentTimeMillis();
		}
		
		opened = true;
		
	}
	
	int getFlushesCount() {
	    return writer instanceof VitalIndexWriter ? ((VitalIndexWriter)writer).getFlushesCount() : -1;
	}
	
	@Override
	public synchronized void close() throws IOException {
		
	    
		if(!opened) throw new IOException("Segment not opened " + getID());
		
		
		if(writer != null) {
			writer.close();
		}
		
		if(TRACKING_INDEX_WRITER) {
		    nrtReopenThread.close();
		    searcherManager.close();
		} else {
		    readerSearcherWrapper.close();
		}
		
		opened = false;
		
		if(this.inmemoryStore != null) {
			this.inmemoryStore.clear();
			this.typesCache.clear();
			this.srcURI2Edge.clear();
			this.destURI2Edge.clear();
			this.srcURI2HyperEdge.clear();
			this.destURI2HyperEdge.clear();
		}
		
	}
	
	public synchronized Collection<GraphObject> insertOrUpdateBatch(Collection<GraphObject> objects) throws IOException {
		
		int s = objects.size();
		
		if(s == 0 ) { 
			return objects;
		} else if( s == 1) {
			insertOrUpdate(objects.iterator().next());
			return objects;
		}
		
		//delete
		List<Term> terms = new ArrayList<Term>(s);
		
		for(GraphObject g : objects) {
			if(g.getURI() == null) throw new IOException("Graph object URI must be set!");
			terms.add(new Term(VitalSignsLuceneBridge.URI_FIELD, g.getURI()));
		}
		
		List<Document> docs = new ArrayList<Document>(s);
		
		for(GraphObject g : objects) {
			docs.add(VitalSignsLuceneBridge.get().graphObjectToDocument(g, config.isStoreObjects() && inmemoryStore == null, config.isStoreNumericFields()));
		} 
		
		
		if(TRACKING_INDEX_WRITER) {
		    
		    tiwriter.deleteDocuments(terms.toArray(new Term[terms.size()]));
		    tiwriter.addDocuments(docs);
		    
		    searcherManager.maybeRefresh();
		    
		} else {
		    
		    writer.deleteDocuments(terms.toArray(new Term[terms.size()]));
		    
		    writer.addDocuments(docs);
		    
		}
		 
		
		commitWriter();
			
		reopenReader();
		
		if(this.inmemoryStore != null) {
			
			for(GraphObject g : objects) {

				insertObjectIntoCache(g);
								
			}
		}
		
		return objects;
		
	} 
	
	private void commitWriter() throws IOException {
	    
	    if(config.isBufferWrites()) {

	        synchronized(commitsLock) {
	            
	            bufferedOpsCount++;
	            
	            if(bufferedOpsCount == 1) {
	                firstBufferedElementTimestamp = System.currentTimeMillis();
	            }
	            
	            if(bufferedOpsCount >= config.getCommitAfterNWrites()) {
	                
	                writer.commit();
	                
	                lastCommitTimestamp = System.currentTimeMillis();
	                
	                firstBufferedElementTimestamp = null;
	                
	                commitsCount++;
	                
	                bufferedOpsCount = 0;
	                
	            }
	            
	        }
	        
	        
	    } else {
	        commitsCount++;
	        writer.commit();
	    }
	}
	
	/**
	 * commits any pending buffered changes, returns true if there were any
	 * @return true if there were any pending changes
	 * @throws IOException
	 */
	public boolean forceCommit() throws IOException {
	    
	    if(config.isBufferWrites()) {
	        
	        synchronized (commitsLock) {
                
	            if(bufferedOpsCount > 0) {
	                writer.commit();
	                lastCommitTimestamp = System.currentTimeMillis();
	                firstBufferedElementTimestamp = null;
	                commitsCount++;
	                bufferedOpsCount = 0;
	                return true;
	            }
	            
            }
	        
	    }
	    
	    return false;
	    
	}

	/**
	 * @return last commit timestamp or <code>null</code> if non buffered commits
	 */
	public Long getLastCommitTimestamp() {
        return lastCommitTimestamp;
    }
	
	
	/**
	 * @return the timestamp of first buffered operation or null if buffer empty
	 */
	public Long getFirstBufferedElementTimestamp() {
        return firstBufferedElementTimestamp;
    }

    /**
	 * @return the number of buffered ops, always 0  if non buffered commits 
	 */
    public int getBufferedOpsCount() {
        return bufferedOpsCount;
    }

    //all write methods are synchronized
	public synchronized GraphObject insertOrUpdate(GraphObject object) throws IOException {

		if( isReadOnly() ) {
			throw new IOException("Cannot add object to read-only index: " + getID());
		}
		
		
		Document d = VitalSignsLuceneBridge.get().graphObjectToDocument(object, config.isStoreObjects() && inmemoryStore == null, inmemoryStore != null || config.isStoreNumericFields());
		
		if(TRACKING_INDEX_WRITER) {
		    
		    tiwriter.deleteDocuments(new Term(VitalSignsLuceneBridge.URI_FIELD, object.getURI()));
	        
            tiwriter.addDocument(d);
            
            searcherManager.maybeRefresh();
		    
		} else {
		    
		    writer.deleteDocuments(new Term(VitalSignsLuceneBridge.URI_FIELD, object.getURI()));
		
		    writer.addDocument(d);
		    
		}
		
		commitWriter();
		
		reopenReader();
		
		if(inmemoryStore != null) {
			insertObjectIntoCache(object);
		}
		
//		loadFieldCache();
		
		return object;
		
	}
	
	private void insertObjectIntoCache(GraphObject object) {
		
		GraphObject prev = inmemoryStore.remove(object.getURI());
		
		if(prev != null) {
			deleteObjectFromCache(prev);
		}
		
		inmemoryStore.put(object.getURI(), object);
		
		Class<? extends GraphObject> cls = object.getClass();
		Set<String> objs = typesCache.get(cls);
		if(objs == null) {
			objs = Collections.synchronizedSet(new HashSet<String>());
			typesCache.put(cls, objs);
		}
		objs.add(object.getURI());
		
		if(object instanceof VITAL_Edge) {
			VITAL_Edge e = (VITAL_Edge) object;
			Set<String> srcEdges = srcURI2Edge.get(e.getSourceURI());
			if(srcEdges == null) {
				srcEdges = new HashSet<String>();
				srcURI2Edge.put(e.getSourceURI(), srcEdges);
			}
			srcEdges.add(e.getURI());
			
			Set<String> destEdges = destURI2Edge.get(e.getDestinationURI());
			if(destEdges == null) {
				destEdges = new HashSet<String>();
				destURI2Edge.put(e.getDestinationURI(), destEdges);
			}
			destEdges.add(e.getURI());
		} else if(object instanceof VITAL_HyperEdge) {
			VITAL_HyperEdge he = (VITAL_HyperEdge) object;
			Set<String> srcHyperEdges = srcURI2HyperEdge.get(he.getSourceURI());
			if(srcHyperEdges == null) {
				srcHyperEdges = new HashSet<String>();
				srcURI2HyperEdge.put(he.getSourceURI(), srcHyperEdges);
			}
			srcHyperEdges.add(he.getURI());
			
			Set<String> destHyperEdges = destURI2HyperEdge.get(he.getDestinationURI());
			if(destHyperEdges == null) {
				destHyperEdges = new HashSet<String>();
				destURI2HyperEdge.put(he.getDestinationURI(), destHyperEdges);
			}
			destHyperEdges.add(he.getURI());
		}
		
	}
	
	private void deleteObjectFromCache(GraphObject g) {

		if(g == null) return;
				
		Set<String> s = typesCache.get(g.getClass());
			
		if(s != null) {
			s.remove(g.getURI());
		}
		
		if(g instanceof VITAL_Edge) {
			
			VITAL_Edge e = (VITAL_Edge) g;
			
			synchronized(this) {
				
				clearIndex(srcURI2Edge, e.getURI());
				
				clearIndex(destURI2Edge, e.getURI());
				
			}
			
		} else if(g instanceof VITAL_HyperEdge) {
	
			VITAL_HyperEdge he = (VITAL_HyperEdge) g;
		
			synchronized(this) {
				
				clearIndex(srcURI2HyperEdge, he.getURI());
				
				clearIndex(destURI2HyperEdge, he.getURI());
				
			}
			
		}
		
	}
	
	private void clearIndex(Map<String, Set<String>> index, String eURI) {
		
		for(Iterator<Entry<String,Set<String>>> iterator = index.entrySet().iterator(); iterator.hasNext(); ) {
			
			Entry<String,Set<String>> entry = iterator.next();
			
			Set<String> edges = entry.getValue();
			
			edges.remove(eURI);
			
			if(edges.size() == 0) {
				
				iterator.remove();
				
			}
			
		}
		
	}
	
	public void deleteAll() throws IOException {
		
		if( isReadOnly() ) {
			throw new IOException("Cannot delete object from read-only index: " + getID());
		}
		
		if(TRACKING_INDEX_WRITER) {
		    
		    tiwriter.deleteAll();
		    
		    searcherManager.maybeRefresh();
		    
		} else {
		    
		    writer.deleteAll();
		    
		}
		
		
		commitWriter();
		
		reopenReader();
		
		if(this.inmemoryStore != null) {
			
			this.inmemoryStore.clear();
			
			typesCache.clear();
			srcURI2Edge.clear();
			destURI2Edge.clear();
			srcURI2HyperEdge.clear();
			destURI2HyperEdge.clear();
			
		}
		
	}
	
	public synchronized void delete(String uri) throws IOException {
		
		if( isReadOnly() ) {
			throw new IOException("Cannot delete object from read-only index: " + getID());
		}
		

		if(TRACKING_INDEX_WRITER) {
		    
		    tiwriter.deleteDocuments(new Term(VitalSignsLuceneBridge.URI_FIELD, uri));
		    
		    searcherManager.maybeRefresh();
		    
		} else {
		    
		    writer.deleteDocuments(new Term(VitalSignsLuceneBridge.URI_FIELD, uri));
		    
		}
		
		commitWriter();
		
		reopenReader();
		
		if(this.inmemoryStore != null) {
			
			GraphObject g = this.inmemoryStore.remove(uri);
			
			deleteObjectFromCache(g);
			
		}
		
	}
	
	public synchronized void deleteBatch(Collection<String> uris) throws IOException {
		
		int s = uris.size();
		
		if(s == 0) {
			return;
		} else if(s == 1) {
			delete(uris.iterator().next());
			return;
		}
		
		//delete
		List<Term> terms = new ArrayList<Term>(s);
		
		for(String uri : uris) {
			terms.add(new Term(VitalSignsLuceneBridge.URI_FIELD, uri));
		}

		if(TRACKING_INDEX_WRITER) {
		    
		    tiwriter.deleteDocuments(terms.toArray(new Term[terms.size()]));
		    
		    searcherManager.maybeRefresh();
		    
		} else {
		    
		    writer.deleteDocuments(terms.toArray(new Term[terms.size()]));
		    
		}

		commitWriter();
		
		reopenReader();
		
		if(inmemoryStore != null) {
			for(String u : uris) {
				
				GraphObject g = this.inmemoryStore.remove(u);
				
				deleteObjectFromCache(g);
				
			}
			
		}
		
	}
	
	
	public List<GraphObject> getGraphObjectsBatch(Collection<String> uris) throws IOException {
	    return getGraphObjectsBatch(uris, null);
	}
	
	public List<GraphObject> getGraphObjectsBatch(Collection<String> uris, QueryStats queryStats) throws IOException {
		
		if(!config.isStoreObjects()) throw new IOException("Cannot return graph objects - index is not a store");
		
		if(inmemoryStore != null) {
			List<GraphObject> l = new ArrayList<GraphObject>();
			for(String u : uris) {
				GraphObject g = inmemoryStore.get(u);
				if(g != null) l.add(g);
			}
			return l;
		}
		
		int s = uris.size();
		
		if(s == 0) {
			return Collections.emptyList();
		} else if(s == 1) {
		    long start = System.currentTimeMillis();
			GraphObject g = getGraphObject(uris.iterator().next());
			if(queryStats != null) {
			    long time = queryStats.addObjectsBatchGetTimeFrom(start);
			    if(queryStats.getQueriesTimes() != null) queryStats.getQueriesTimes().add(new QueryTime("batch get " + uris.size(), "batch get " + uris, time));
			}
			if(g == null) return Collections.emptyList();
			List<GraphObject> l = new ArrayList<GraphObject>();
			l.add(g);
			return l;
		}
		
		
		IndexReaderSearcherWrapper wrapper = acquireReaderSearcher();
		
		
		List<GraphObject> objects = new ArrayList<GraphObject>(uris.size());
		
		long batchGetNano = 0L;
		
		try {
		
		    List<String> urisList = null;
            if(uris instanceof List) {
                urisList = (List<String>) uris;
            } else {
                urisList = new ArrayList<String>(uris);
            }
            
            int maxClauseCount = BooleanQuery.getMaxClauseCount();
            
            for(int i = 0 ; i < urisList.size(); i += maxClauseCount) {
                
                List<String> subUrisList = urisList.subList(i, Math.min(urisList.size(), i + maxClauseCount));
                
                BooleanQuery query = new BooleanQuery(true);
                for(String uri : subUrisList) {
                    query.add(new TermQuery(new Term(VitalSignsLuceneBridge.URI_FIELD, uri)), Occur.SHOULD);
                }
                
                long nano = queryStats != null ? System.nanoTime() : 0L;
                
                TopDocs searchRS = wrapper.getSearcher().search(query, uris.size());
                
                if(queryStats != null) {
                    batchGetNano += (System.nanoTime() - nano);
                }
                
                for(ScoreDoc sd : searchRS.scoreDocs) {
            
                    nano = queryStats != null ? System.nanoTime() : 0L;
                    
                    Document doc = wrapper.getDocument(sd.doc);
                    
                    if(queryStats != null) {
                        batchGetNano += (System.nanoTime() - nano);
                    }
                    
                    if(doc != null) {
                        objects.add(VitalSignsLuceneBridge.get().documentToGraphObject(doc));
                    }
                }
                
            }
			
		} finally {
			wrapper.release();
		}
		
		if(queryStats != null) {
		    long time = queryStats.addObjectsBatchGetTimeFrom(batchGetNano / 1000000);
		    if(queryStats.getQueriesTimes() != null) queryStats.getQueriesTimes().add(new QueryTime("batch get " + uris.size(), "batch get " + uris, time));
		}
		
		//sort the results if input is a list
		if(uris instanceof List) {
			
			final List<String> urisList = (List<String>) uris;
			
			Collections.sort(objects, new Comparator<GraphObject>(){

                @Override
                public int compare(GraphObject g1, GraphObject g2) {
                    Integer i1 = urisList.indexOf(g1.getURI());
                    Integer i2 = urisList.indexOf(g2.getURI()); 
                    return i1.compareTo(i2);
                }
			    
			});
			
		}
		
		return objects;
		
	}
	
	
	public boolean containsURI(String uri) throws IOException {
		
		if(inmemoryStore != null) {
			return inmemoryStore.containsKey(uri);
		}
		
		IndexReaderSearcherWrapper wrapper = acquireReaderSearcher();
		
		try {
			
			TopDocs search = wrapper.getSearcher().search(new TermQuery(new Term(VitalSignsLuceneBridge.URI_FIELD, uri)), 1);

			return search.totalHits > 0;
			
		} finally {
			if(wrapper != null) {
				wrapper.release();
			}
		}
		
	}
	
	/**
	 * Returns the number of URIs found in the segment
	 * @throws IOException 
	 */
	public int containsURIs(Collection<String> uris) throws IOException {
		
		if(inmemoryStore != null) {
			int c = 0;
			for(String u : uris) {
				if(inmemoryStore.containsKey(u)) c++;
			}
			return c;
		}
		
		IndexReaderSearcherWrapper wrapper = acquireReaderSearcher();
		
		try {
			
		    List<String> urisList = null;
		    if(uris instanceof List) {
		        urisList = (List<String>) uris;
		    } else {
		        urisList = new ArrayList<String>(uris);
		    }
		    
		    int hitsCount = 0;
		    
		    int maxClauseCount = BooleanQuery.getMaxClauseCount();
		    
		    for(int i = 0 ; i < urisList.size(); i += maxClauseCount) {
		        
		        BooleanQuery q = new BooleanQuery();

		        List<String> subUrisList = urisList.subList(i, Math.min(urisList.size(), i + maxClauseCount));
		        
		        for(String uri : subUrisList) {
	               q.add(new TermQuery(new Term(VitalSignsLuceneBridge.URI_FIELD, uri)), Occur.SHOULD);
		        }
		            
	            TopDocs search = wrapper.getSearcher().search(q, uris.size());

	            hitsCount += search.totalHits;
		        
		    }
		    
		    return hitsCount;
			
		} finally {
			if(wrapper != null) {
				wrapper.release();
			}
		}
		
	}
	
	public List<String> containsURIsList(Collection<String> uris) throws IOException {
		
		List<String> res = new ArrayList<String>();
		
		if(inmemoryStore != null) {
			int c = 0;
			for(String u : uris) {
				if(inmemoryStore.containsKey(u)) {
					res.add(u);
				}
			}
			return res;
		}
		
		IndexReaderSearcherWrapper wrapper = acquireReaderSearcher();
		
		try {
			
		    
		    List<String> urisList = null;
            if(uris instanceof List) {
                urisList = (List<String>) uris;
            } else {
                urisList = new ArrayList<String>(uris);
            }
            
            int maxClauseCount = BooleanQuery.getMaxClauseCount();
            
            for(int i = 0 ; i < urisList.size(); i += maxClauseCount) {
                
                BooleanQuery q = new BooleanQuery();
                
                List<String> subUrisList = urisList.subList(i, Math.min(urisList.size(), i + maxClauseCount));
                
                for(String uri : subUrisList) {
                    q.add(new TermQuery(new Term(VitalSignsLuceneBridge.URI_FIELD, uri)), Occur.SHOULD);
                }
                
                TopDocs search = wrapper.getSearcher().search(q, uris.size());

                Set<String> fields = new HashSet<String>(Arrays.asList(VitalSignsLuceneBridge.URI_FIELD));
                
                for(ScoreDoc sd : search.scoreDocs) {
                    
                    Document doc = wrapper.getDocument(sd.doc, fields);
                    if(doc != null) {
                        String u = doc.get(VitalSignsLuceneBridge.URI_FIELD);
                        if(u != null) {
                            res.add(u);
                        }
                    }
                    
                }
                
            }
		    
		} finally {
			if(wrapper != null) {
				wrapper.release();
			}
		}
		
		return res;
		
	}
	
	public GraphObject getGraphObject(String uri) throws IOException {
		
		if(!config.isStoreObjects()) throw new IOException("Cannot return graph object - index is not a store");
		
		if(inmemoryStore != null) {
			return inmemoryStore.get(uri);
		}
		
		int documentID = -1;

		IndexReaderSearcherWrapper wrapper = acquireReaderSearcher();
		
		try {

			long searchTime = log.isDebugEnabled() ? System.nanoTime() : 0L;
						
			TopDocs search = wrapper.getSearcher().search(new TermQuery(new Term(VitalSignsLuceneBridge.URI_FIELD, uri)), 1);
			
			if(log.isDebugEnabled()) {
				searchTime = System.nanoTime() - searchTime;
			}		
			
			long documentGetTime = log.isDebugEnabled() ? System.nanoTime() : 0L;
			
			if(search.scoreDocs.length > 0) {
						
				documentID = search.scoreDocs[0].doc;
						
				if(log.isDebugEnabled()) {
					documentGetTime = System.nanoTime() - documentGetTime;
				}
				
			}
			
			if(documentID < 0) {
				log.debug("No document found for uri {}", uri);
				return null;
			}
			
			if(log.isDebugEnabled()) {
				log.debug("Found documentID: {} for uri {}", documentID, uri);
			}
			
			Document document = wrapper.getDocument(documentID);
	
			if(document == null) return null;
			
			wrapper.release();
			wrapper = null;
			
			long conversionTime = log.isDebugEnabled() ? System.nanoTime() : 0L;
			GraphObject g = VitalSignsLuceneBridge.get().documentToGraphObject(document);
			
			if(log.isDebugEnabled()) { 
				conversionTime = System.nanoTime() - conversionTime;
				log.debug("Lucene segment time: searchTime: {}ns, getTime: {}ns, conversionTime: {}ns", new Object[]{searchTime, documentGetTime, conversionTime});
			}
			
			return g;
			
		} finally {
			if(wrapper != null) {
				wrapper.release();
			}
		}
			
		
	}
	

	private void reopenReader() throws IOException {
		
	    if(TRACKING_INDEX_WRITER) {
	        return;
	    }
	    
		long start = System.currentTimeMillis();
				
		boolean isNew = false;
				
		if(readerSearcherWrapper != null) {
			
			IndexReader newReader = DirectoryReader.openIfChanged((DirectoryReader) readerSearcherWrapper.getReader(), writer, true);	
			
			 if (newReader != null && newReader != readerSearcherWrapper.getReader() ) {
				
			     isNew = true;
			     
				 // reader was reopened
		//		 searcher.close();
		//	     reader.close();
		//		 searcher = new IndexSearcher(newReader);
		//	     searcher.setDefaultFieldSortScoring(true, false);
						 
						 
				 IndexSearcher indexSearcher = new IndexSearcher(newReader);
//				 indexSearcher.setDefaultFieldSortScoring(true, false);
						 
				 //this should be synchronized to prevent dead access
				 synchronized(readerSearcherWrapperLock) {
					 readerSearcherWrapper.close();
					 readerSearcherWrapper = new IndexReaderSearcherWrapper(newReader, indexSearcher);
				 }
						 
			 }
					 
			 //reader = newReader;
		
		}
				
		long stop = System.currentTimeMillis();
				
		log.debug("Reopening the segment {} took: {}ms, isNew {}", getID(), (stop - start), isNew);

	}

	
	public URIResultList selectQuery(VitalSelectQuery selectQuery) throws IOException {
	    return selectQuery(selectQuery, null);
	}
	
	public URIResultList selectQuery(VitalSelectQuery selectQuery, QueryStats queryStats) throws IOException {
	    return selectQueryWithCollectResolvedFields(selectQuery, queryStats, false);
	}
	
	public URIResultList selectQueryWithCollectResolvedFields(VitalSelectQuery selectQuery, QueryStats queryStats, boolean collectResolvedFields) throws IOException {
	       //inverse the path elements to narrow down the results
        IndexReaderSearcherWrapper wrapper = acquireReaderSearcher();
        
        try {
            
            LuceneSelectQueryHandler handler = new LuceneSelectQueryHandler(selectQuery, this, wrapper.getSearcher(), queryStats);
            handler.setCollectResolvedFields(collectResolvedFields);
            return handler.handleSelectQuery();
                
        } finally {
            wrapper.release();
        }
        
	}
	
	/**
     * Exports to given output stream, compact string format.
     * The output stream is not closed
     * @param outputStream
     * @return status
     */
    public VitalStatus bulkExport(OutputStream outputStream) {
        return bulkExport(outputStream, null);
    }
	
	/**
	 * Exports to given output stream, compact string format.
	 * The output stream is not closed
	 * @param outputStream
	 * @param datasetURI, may be null
	 * @return status
	 */
	public VitalStatus bulkExport(OutputStream outputStream, String datasetURI) {
		
		
		IndexReaderSearcherWrapper wrapper = null;
        try {
            wrapper = acquireReaderSearcher();
        } catch (IOException e1) {
            log.error(e1.getLocalizedMessage());
            return VitalStatus.withError(e1.getLocalizedMessage());
        }
		
		try {
			
		    OutputStreamWriter local = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
			BlockCompactStringSerializer serializer = new BlockCompactStringSerializer(local);
			
			int c = 0;
			
			int skipped = 0;
			
			if(inmemoryStore != null) {
				
				for(GraphObject g : inmemoryStore.values()) {
					
                    if(datasetURI != null) {
                        String thisDataset = (String) g.getRaw(Property_hasProvenance.class);
                        if(thisDataset == null || !datasetURI.equals(thisDataset)) {
                            //skip the object
                            skipped++;
                            continue;
                        }
                    }
				    
					serializer.startBlock();
					serializer.writeGraphObject(g);
					serializer.endBlock();
					c++;
					
				}
				
			} else {
			
				Bits liveDocs = MultiFields.getLiveDocs(wrapper.getReader());
				
				int maxI = liveDocs != null ? liveDocs.length() : wrapper.getReader().numDocs();
				
				for( int i = 0 ; i < maxI; i++ ) {
					
					if(liveDocs == null || liveDocs.get(i)) {
						
						Document doc = wrapper.getDocument(i);
						
						if(datasetURI != null) {
						    String thisDataset = doc.get(VitalCoreOntology.hasProvenance.getURI());
						    if(thisDataset == null || !datasetURI.equals(thisDataset)) {
						        //skip the object
						        skipped++;
						        continue;
						    }
						}
						
						GraphObject g = VitalSignsLuceneBridge.get().documentToGraphObject(doc);
						
						if(g != null) {
							serializer.startBlock();
							serializer.writeGraphObject(g);
							serializer.endBlock();
							c++;
						}
						
					}
					
				}
			}
			
			local.flush();
			
			VitalStatus vs = VitalStatus.withOKMessage("Exported " + c + " object(s)" + (datasetURI != null ? (", filtered out " + skipped + " object(s)") : ""));
			
			vs.setSuccesses(c);
			
			return vs;
			
		} catch(Exception e){
			return VitalStatus.withError(e.getLocalizedMessage());
		} finally {
			wrapper.release();
		}
		
	}
	
	/**
	 * Imports from given input stream, compact string format.
	 * The input stream is not closed
	 * It does not check if objects exist already or not
	 * @param inputStream
     * @return status
	 */
	public synchronized VitalStatus bulkImport(InputStream inputStream) {
	    return bulkImport(inputStream, null);
	}
	
    /**
     * Imports from given input stream, compact string format.
     * The input stream is not closed
     * It does not check if objects exist already or not
     * @param inputStream
     * @param datasetURI, null
     * @return status
     */
    public synchronized VitalStatus bulkImport(InputStream inputStream, String datasetURI) {
        
		IndexReaderSearcherWrapper wrapper = null;
		
		try {
            wrapper = acquireReaderSearcher();
        } catch (IOException e1) {
            log.error(e1.getLocalizedMessage());
            return VitalStatus.withError(e1.getLocalizedMessage());
        }
		
		try {
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		
			BlockIterator blocksIterator = BlockCompactStringSerializer.getBlocksIterator(reader, false);
			
			int c = 0;
			
			
			while(blocksIterator.hasNext()) {
			
				VitalBlock _next = blocksIterator.next();
				
				
				List<Document> docs = new ArrayList<Document>();
				
				GraphObject mainObject = _next.getMainObject();

				if( ! "".equals(datasetURI) ) {
				    mainObject.set(Property_hasProvenance.class, datasetURI);
				}
				
				
                Document d1 = VitalSignsLuceneBridge.get().graphObjectToDocument(mainObject, config.isStoreObjects() && inmemoryStore == null, inmemoryStore != null || config.isStoreNumericFields());
				
				docs.add(d1);
				
				if(inmemoryStore != null) {
					insertObjectIntoCache(_next.getMainObject());
				}
				
				for(GraphObject g : _next.getDependentObjects()) {
					
				    if(! "".equals(datasetURI)) {
				        g.set(Property_hasProvenance.class, datasetURI);
				    }
				    
                    if(inmemoryStore != null) {
                        insertObjectIntoCache(g);
                    }
					
					Document d = VitalSignsLuceneBridge.get().graphObjectToDocument(g, config.isStoreObjects() && inmemoryStore == null, inmemoryStore != null || config.isStoreNumericFields());
					
					docs.add(d);
				
				}
				
				
				if(TRACKING_INDEX_WRITER) {
				    
				    tiwriter.addDocuments(docs);
				    
				    searcherManager.maybeRefresh();
				    
				} else {
				    
				    writer.addDocuments(docs);
				    
				}
			
				
				c += docs.size();
			
			}
		
				
			commitWriter();
		
			reopenReader();
		
		
			VitalStatus vs = VitalStatus.withOKMessage("Imported " + c + "  object(s)");
			vs.setSuccesses(c);
			return vs;
			
		} catch(Exception e) {
            log.error(e.getLocalizedMessage(), e);
			return VitalStatus.withError(e.getLocalizedMessage());
		} finally {
			wrapper.release();
		}
		
	}
			
    public ResultList exportQuery(VitalExportQuery exportQuery) throws IOException {
	    return exportQuery(exportQuery, null);
	}
	
	public ResultList exportQuery(VitalExportQuery exportQuery, QueryStats queryStats) throws IOException {
		
		//inverse the path elements to narrow down the results
		IndexReaderSearcherWrapper wrapper = acquireReaderSearcher();
		
		try {
			
			LuceneExportQueryHandler exportHandler = new LuceneExportQueryHandler(exportQuery, this, wrapper.getSearcher(), queryStats);
			return exportHandler.handle();
			
		} finally {
			wrapper.release();
		}
		
	}
	
	private IndexReaderSearcherWrapper acquireReaderSearcher() throws IOException {
	    if(TRACKING_INDEX_WRITER) {
	        return new TrackingIndexReaderSearcherWrapper(this.searcherManager, this.searcherManager.acquire());
	    } else {
	        synchronized (readerSearcherWrapperLock) {
	            return readerSearcherWrapper.acquire();
	        }
	    }
	}
	
	public URIResultElement getURIResultElement(String URI) throws IOException {

		int documentID = -1;

		IndexReaderSearcherWrapper wrapper = acquireReaderSearcher();
		
//		public String URI
//		
//			/* these properties only set in graph query */
//			public String typeURI
//			
//			public String sourceURI
//			
//			public String destinationURI
//			
//			public double score
		
		try {
					
			TopDocs search = wrapper.getSearcher().search(new TermQuery(new Term(VitalSignsLuceneBridge.URI_FIELD, (String)URI)), 1);
					
			if(search.scoreDocs.length > 0) {
						
				documentID = search.scoreDocs[0].doc;
						
			}
			
			if(documentID < 0) {
				return null;
			}
			
			Document document = wrapper.getDocument(documentID, VitalSignsLuceneBridge.allStaticFieldsToLoad);
	
			wrapper.release();
			wrapper = null;
			
			if(document == null) return null;
			
			URIResultElement el = new URIResultElement();
			el.segment = this;
			el.URI = document.get(VitalSignsLuceneBridge.URI_FIELD);
			el.typeURI = document.get(VitalSignsLuceneBridge.VITAL_TYPE_FIELD);
			
			ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(el.typeURI);
			
			Class<? extends GraphObject> cls = cm.getClazz();
			
			if(VITAL_Edge.class.isAssignableFrom(cls)) {
				el.destinationURI = document.get(VitalSignsLuceneBridge.EDGE_DEST_URI_FIELD);
				el.sourceURI = document.get(VitalSignsLuceneBridge.EDGE_SRC_URI_FIELD);
			} else if(VITAL_HyperEdge.class.isAssignableFrom(cls)) {
				el.destinationURI = document.get(VitalSignsLuceneBridge.EDGE_DEST_URI_FIELD);
				el.sourceURI = document.get(VitalSignsLuceneBridge.EDGE_SRC_URI_FIELD);
			}
			
			return el;
			
		} finally {
			if(wrapper != null) {
				wrapper.release();
			}
		}
				
	}
	
	public void deleteData() throws IOException {
		if(opened) throw new IOException("Cannot delete data of an open index");
		if(config.getType() == LuceneSegmentType.disk && config.getPath() != null) {
			log.info("Deleting disk segment data...");
			FileUtils.deleteDirectory(new File(config.getPath()));
			log.info("Data deleted.");
		}
	}

	@Override
	public Iterator<GraphObject> iterator() {
		if(inmemoryStore == null) throw new RuntimeException("Only in-memory lucene store segments may be iterated!"); 
		return new HashSet<GraphObject>(inmemoryStore.values()).iterator();
//		throw new RuntimeException("Lucene segment does not provide graph object iterator!")
	}

	@Override
	public <T extends GraphObject> Iterator<T> iterator(Class<T> cls,
			boolean strict) {
			
		if(inmemoryStore == null) throw new RuntimeException("Only in-memory lucene store segments may be iterated!"); 
			
		List<T> typedList = new ArrayList<T>();
			
		if(strict) {
				
			Set<String> uris= typesCache.get(cls);
				
			if(uris != null) {
					
				for(String s : uris) {
					GraphObject go = get(s);
					if(go != null) {
						typedList.add((T) go);
					}
				}
					
			}
				
		} else {
			
			//we need to iterate over type list and collect
			
			for(Iterator<Entry<Class<? extends GraphObject>, Set<String>>> iterator = typesCache.entrySet().iterator(); iterator.hasNext(); ) {
					
				Entry<Class<? extends GraphObject>, Set<String>> entry = iterator.next();
					
				Class<? extends GraphObject> _class = entry.getKey();
					
				if(cls.isAssignableFrom(_class)) {
					Set<String> uris = entry.getValue();
					if(uris != null) {
						for(String s : uris) {
							GraphObject go = get(s);
							if(go != null) {
								typedList.add((T) go);
							}
						}
					}
				}
				
			}
			
			
		}
		
		return typedList.iterator();
		
	}

	@Override
	public <T extends GraphObject> Iterator<T> iterator(Class<T> cls) {
		return this.iterator(cls, false);
	}

	@Override
	public GraphObject get(String uri) {
		try {
            return this.getGraphObject(uri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
	}

	@Override
	public boolean isEdgeIndexEnabled() {
		return this.inmemoryStore != null;
	}

	@Override
	public Map<String, Set<String>> getSrcURI2Edge() {
		return this.srcURI2Edge;
	}

	@Override
	public Map<String, Set<String>> getDestURI2Edge() {
		return this.destURI2Edge;
	}

	@Override
	public Map<String, Set<String>> getSrcURI2HyperEdge() {
		return this.srcURI2HyperEdge;
	}

	@Override
	public Map<String, Set<String>> getDestURI2HyperEdge() {
		return this.destURI2HyperEdge;
	}

	public int totalDocs() {
		synchronized(this.readerSearcherWrapperLock) {
			int nd = this.readerSearcherWrapper.getReader().numDocs();
			return  nd;  
		}
		
	}
	
	public int getSegmentSize() throws IOException {
		
		
		//inverse the path elements to narrow down the results
		IndexReaderSearcherWrapper wrapper = acquireReaderSearcher();
		
		try {
			return wrapper.getReader().numDocs();
		} finally {
			wrapper.release();
		}
		
	}

	public Collection<GraphObject> getAllObjects() {
		if(inmemoryStore == null) throw new RuntimeException("Only for inmemory store!");
		return inmemoryStore.values();
	}

	public Map<String, GraphObject> getMap() {
		if(inmemoryStore == null) throw new RuntimeException("Only for inmemory store!");
		return inmemoryStore;
	}

	@Override
	public LuceneSegment getLuceneSegment() {
		return this;
	}

    public LuceneSegmentConfig getConfig() {
        return config;
    }

    public VitalOrganization getOrganization() {
        return organization;
    }

    public VitalApp getApp() {
        return app;
    }
    
    public boolean areAllObjectFieldsStored() {
        return config.isStoreObjects() && inmemoryStore == null;
    }

}
