package ai.vital.service.lucene.model;

import ai.vital.lucene.model.LuceneSegmentType;

public class LuceneSegmentConfig {

    private LuceneSegmentType type;
    
    private boolean storeObjects;
    
    /**
     * This setting is only used if storeObjects==false.
     * Based on this setting aggregation functions will be enabled or disabled.
     * <code>false</code> by default
     */
    private boolean storeNumericFields = false;

    private String path;
    
    
    /**
     * only for disk based segment, when enabled the operations will be committed after every N operations 
     */
    private boolean bufferWrites = false;
    
    private int commitAfterNWrites = 1;
    
    public LuceneSegmentConfig(LuceneSegmentType type, boolean storeObjects,
            boolean storeNumericFields, String path) {
        super();
        this.type = type;
        this.storeObjects = storeObjects;
        this.storeNumericFields = storeNumericFields;
        this.path = path;
    }

    public LuceneSegmentType getType() {
        return type;
    }

    public void setType(LuceneSegmentType type) {
        this.type = type;
    }

    public boolean isStoreObjects() {
        return storeObjects;
    }

    public void setStoreObjects(boolean storeObjects) {
        this.storeObjects = storeObjects;
    }

    public boolean isStoreNumericFields() {
        return storeNumericFields;
    }

    public void setStoreNumericFields(boolean storeNumericFields) {
        this.storeNumericFields = storeNumericFields;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isBufferWrites() {
        return bufferWrites;
    }

    public void setBufferWrites(boolean bufferWrites) {
        this.bufferWrites = bufferWrites;
    }

    public int getCommitAfterNWrites() {
        return commitAfterNWrites;
    }

    public void setCommitAfterNWrites(int commitAfterNWrites) {
        this.commitAfterNWrites = commitAfterNWrites;
    }

}
