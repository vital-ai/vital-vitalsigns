package ai.vital.lucene.model;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VitalIndexWriter extends IndexWriter {

    private final static Logger log = LoggerFactory.getLogger(VitalIndexWriter.class);
    
    private int flushesCount = 0;
    
    public VitalIndexWriter(Directory d, IndexWriterConfig conf)
            throws IOException {
        super(d, conf);
    }

    @Override
    protected void doAfterFlush() throws IOException {
        super.doAfterFlush();
//        log.info("After flush");
        flushesCount++;
    }

    @Override
    protected void doBeforeFlush() throws IOException {
        super.doBeforeFlush();
//        log.info("Before flush");
    }

    public int getFlushesCount() {
        // TODO Auto-generated method stub
        return flushesCount;
    }
    
    

}
