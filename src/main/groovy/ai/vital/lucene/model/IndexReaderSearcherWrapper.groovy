package ai.vital.lucene.model;

import java.io.IOException
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

public class IndexReaderSearcherWrapper {

	protected IndexReader reader;
	
	protected IndexSearcher searcher;

	public int references;
	
	public boolean closed;
	
	public IndexReaderSearcherWrapper(IndexReader reader, IndexSearcher searcher) {
		super();
		this.reader = reader;
		this.searcher = searcher;
		this.references = 0;
	}

	public synchronized void release() {
		references--;
		cleanup();
		
	}
	
	public synchronized IndexReaderSearcherWrapper acquire() {
		references++;
		return this;
	}
	
	private void cleanup() {
		
		if(closed && references == 0 && reader != null) {
			try {
				reader.close();
                reader = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	public synchronized void close() {

		//to be closed
		closed = true;

		cleanup();
		
	}

    public IndexReader getReader() {
        return reader;
    }

    public IndexSearcher getSearcher() {
        return searcher;
    }

    public Document getDocument(int doc) {
        return reader.document(doc);
    }

    public Document getDocument(int doc, Set<String> fields) {
        return reader.document(doc, fields);
    }

}
