package ai.vital.lucene.query;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopFieldDocs;

import ai.vital.lucene.model.LuceneSegment;
import ai.vital.lucene.model.VitalSignsLuceneBridge;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.query.QueryStats;
import ai.vital.vitalservice.query.QueryTime;
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalExportQuery;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;

public class LuceneExportQueryHandler {

	private VitalExportQuery exportQuery;
	private LuceneSegment segment;
	private IndexSearcher searcher;
    private QueryStats queryStats;

	public LuceneExportQueryHandler(VitalExportQuery exportQuery, LuceneSegment segment, IndexSearcher searcher, QueryStats queryStats) {
		this.exportQuery = exportQuery;
		this.segment = segment;
		this.searcher = searcher;
		this.queryStats = queryStats;
	}
	
	public ResultList handle() throws IOException  {
		
		final int offset = exportQuery.getOffset();
		final int limit = exportQuery.getLimit();
		
		long start = System.currentTimeMillis();
		Query q = null;
		if(exportQuery.getDatasetURI() != null) {
		    q = new TermQuery(new Term(VitalCoreOntology.hasProvenance.getURI(), exportQuery.getDatasetURI()));
		} else {
		    q = new MatchAllDocsQuery();
		}
        TopFieldDocs rs = searcher.search(q, offset + limit, Sort.INDEXORDER);
		if(queryStats != null) {
		    long time = queryStats.addDatabaseTimeFrom(start);
		    if(queryStats.getQueriesTimes() != null) queryStats.getQueriesTimes().add(new QueryTime(exportQuery.debugString(), "match all docs query, topN" + (offset+limit), time));
		}
		
		int maxIndex = Math.min(offset + limit, rs.scoreDocs.length);
		
		ResultList res = new ResultList();
		res.setLimit(limit);
		res.setOffset(offset);
		res.setStatus(VitalStatus.withOK());
		res.setQueryStats(queryStats);
		
		long batchGetTimeNano = 0;
		
		int total = 0 ;
		for(int i = offset; i < maxIndex; i++) {
			
		    total++;
		    
			ScoreDoc sd = rs.scoreDocs[i];
//			sd.doc

			long nano = queryStats != null ? System.nanoTime() : 0L;
			
			Document doc = searcher.doc(sd.doc);
			
			if(queryStats != null) {
			    batchGetTimeNano += ( System.nanoTime() - nano );
			}
			
			GraphObject g = VitalSignsLuceneBridge.get().documentToGraphObject(doc);
			
			res.getResults().add(new ResultElement(g, 1D));
			
		}
		
		if(queryStats != null) {
		    long time = queryStats.addObjectsBatchGetTimeFrom(batchGetTimeNano / 1000000L);
		    if(queryStats.getQueriesTimes() != null) {
		        queryStats.getQueriesTimes().add(new QueryTime(total + " lucene documents get time", total + " lucene documents get time", time));
		    }
		}
		
		return res;
		
		
		/*
		searcher.search(new MatchAllDocsQuery(), new Collector() {
			
			int index = 0;
			
			int docBase = 0;
			
			AtomicReader reader;
			
			@Override
			public void setScorer(Scorer arg0) throws IOException {
			}
			
			@Override
			public void setNextReader(AtomicReaderContext arg0) throws IOException {
				docBase = arg0.docBase;
				reader = arg0.reader();
			}
			
			@Override
			public void collect(int arg0) throws IOException {

				
				
				
			}
			
			@Override
			public boolean acceptsDocsOutOfOrder() {
				return false;
			}
		});
		*/
		
	}
	
}
