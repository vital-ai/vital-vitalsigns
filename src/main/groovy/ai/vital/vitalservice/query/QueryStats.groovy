package ai.vital.vitalservice.query

import java.io.Serializable

class QueryStats implements Serializable {

    private static final long serialVersionUID = 1L;
    
    long databaseTimeMS = 0;
    int databaseQueriesCount = 0
    
    long probingTimeMS = 0L;
    int probingQueriesCount = 0;
    
    long objectsBatchGetTimeMS = 0L;
    int objectsBatchGetCount = 0;
    
    //if long properties are not smooshed
    long objectsResolvingTimeMS = 0L;
    int objectsResolvingCount = 0;
    
    //if partial object attributes are returned
    long attrDataGetTimeMS = 0L;
    int attrDataGetCount = 0;
    
    //overall query execution time
    long queryTimeMS = 0L;
    
    //available only in detailed logging
    List<QueryTime> queriesTimes = null
    
    public long addDatabaseTimeFrom(long startTimestamp)  {
        long diff = System.currentTimeMillis() - startTimestamp; 
        databaseTimeMS += diff;
        databaseQueriesCount++;
        return diff;
        
    }
    
    public long addProbingTimeFrom(long startTimestamp) {
        long diff = System.currentTimeMillis() - startTimestamp;
        probingTimeMS += diff;
        probingQueriesCount++;
        return diff;
    }
    
    public long addObjectsBatchGetTimeFrom(long startTimestamp) {
        long diff = System.currentTimeMillis() - startTimestamp;
        objectsBatchGetTimeMS += diff;
        objectsBatchGetCount++;
        return diff;
    }
    
    public long addObjectsResolvingTimeFrom(long startTimestamp) {
        long diff = System.currentTimeMillis() - startTimestamp;
        objectsResolvingTimeMS += diff;
        objectsResolvingCount++;
        return diff;
    }
    
    public long addAttrDataGetTimeFrom(long startTimestamp) {
        long diff = System.currentTimeMillis() - startTimestamp;
        attrDataGetTimeMS += diff;
        attrDataGetCount++;
        return diff;
    }
    
    public long getTotalDatabaseTime() {
        return databaseTimeMS + probingTimeMS + objectsBatchGetTimeMS + objectsResolvingTimeMS + attrDataGetTimeMS
    }

    
}
