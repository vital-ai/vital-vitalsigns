package ai.vital.vitalservice.query

import java.io.Serializable

class QueryTime implements Serializable {

    private static final long serialVersionUID = 1L;
    
    String vitalQueryString
    
    String innerQueryString
    
    long time

    public QueryTime(String vitalQueryString, String innerQueryString,
            long time) {
        super();
        this.vitalQueryString = vitalQueryString;
        this.innerQueryString = innerQueryString;
        this.time = time;
    }
    
    
}
