package ai.vital.lucene.model;

import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ReferenceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackingIndexReaderSearcherWrapper extends IndexReaderSearcherWrapper {

    private final static Logger log = LoggerFactory.getLogger(TrackingIndexReaderSearcherWrapper.class);
    
    private ReferenceManager<IndexSearcher> searchManager;

    public TrackingIndexReaderSearcherWrapper(ReferenceManager<IndexSearcher> searchManager,
            IndexSearcher searcher) {
        super(searcher.getIndexReader(), searcher);
        this.searchManager = searchManager;
    }
    
    @Override
    public synchronized void release() {
        try {
            searchManager.release(this.searcher);
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    
    
}
