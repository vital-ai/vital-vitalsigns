package ai.vital.lucene.model.impl;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import ai.vital.lucene.model.LuceneSegment;
import ai.vital.service.lucene.impl.LuceneServiceQueriesImpl;
import ai.vital.vitalservice.query.QueryStats;
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalsigns.model.GraphObject;

public class LuceneResultsIterator implements Iterator<GraphObject> {

	static int PAGE_SIZE = 100;
	
	private List<LuceneSegment> luceneSegments;
	private VitalSelectQuery sq;

    private QueryStats queryStats;

	public LuceneResultsIterator(List<LuceneSegment> luceneSegments, VitalSelectQuery sq, QueryStats queryStats) {
		this.luceneSegments = luceneSegments;
		this.sq = sq;
		this.sq.setLimit(PAGE_SIZE);
		this.queryStats = queryStats;
		nextPage();
	}

	int offset = 0;
	
	ResultList page = null;
	
	int pageIndex = -1;
	
	private void nextPage() {

		this.sq.setOffset(offset);
		
		page = LuceneServiceQueriesImpl.selectQuery(luceneSegments, sq, queryStats);
		
		pageIndex = 0;
	
		offset += PAGE_SIZE;
		
	}

	
	@Override
	public boolean hasNext() {

		//index points at next solution
		
		if( pageIndex < page.getResults().size() ) {
			return true;
		}
		
		return false;
	}

	@Override
	public GraphObject next() {

		if(!hasNext()) throw new NoSuchElementException("No more results!");
		
		ResultElement resultElement = page.getResults().get(pageIndex);
		
		pageIndex++;
		
		if(pageIndex == page.getResults().size()) {
			
			if(pageIndex == PAGE_SIZE) {
				
				//get next page
				
				nextPage();
				
			} else {
				
				//end of results!
				
			}
			
		}
		
		return resultElement.getGraphObject();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException(LuceneResultsIterator.class.getSimpleName() + " does not support removals");
	}

}
