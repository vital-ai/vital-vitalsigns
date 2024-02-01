package ai.vital.lucene.model.impl;

import java.util.Iterator;
import java.util.List;

import ai.vital.lucene.model.LuceneSegment;
import ai.vital.vitalservice.query.QueryStats;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.query.graph.Arc;
import ai.vital.vitalsigns.query.graph.BindingEl;
import ai.vital.vitalsigns.query.graph.GraphQueryImplementation.ResultsProvider;

public class LuceneResultsProvider implements ResultsProvider {

	List<LuceneSegment> luceneSegments;
    private QueryStats queryStats;
	
	public LuceneResultsProvider(List<LuceneSegment> luceneSegments, QueryStats queryStats) {
		super();
		this.luceneSegments = luceneSegments;
		this.queryStats = queryStats;
	}
	
	@Override
	public Iterator<BindingEl> getIterator(Arc arc, GraphObject parent) {

		return new LuceneBindingElementIterator(luceneSegments, arc, parent, queryStats);
	}

}
