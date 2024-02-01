package ai.vital.lucene.model.impl;

import java.util.List;

import ai.vital.lucene.model.LuceneSegment;
import ai.vital.service.lucene.impl.LuceneServiceQueriesImpl;
import ai.vital.vitalservice.query.QueryStats;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalsigns.query.graph.ref.ReferenceGraphQueryImplementation.Executor;

public class LuceneGraphQueryExecutor implements Executor {

	List<LuceneSegment> luceneSegments;
    private QueryStats queryStats;
	
	public LuceneGraphQueryExecutor(List<LuceneSegment> luceneSegments, QueryStats queryStats) {
		super();
		this.luceneSegments = luceneSegments;
		this.queryStats = queryStats;
	}



	@Override
	public ResultList selectQuery(VitalSelectQuery sq) {

		return LuceneServiceQueriesImpl.selectQuery(luceneSegments, sq, queryStats);
		
	}

}
