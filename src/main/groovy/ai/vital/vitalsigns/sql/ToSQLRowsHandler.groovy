package ai.vital.vitalsigns.sql;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalsigns.model.GraphObject;

public class ToSQLRowsHandler {

    private static ToSQLRowsProvider provider;
    
    private final static Logger log = LoggerFactory.getLogger(ToSQLRowsHandler.class);
    
    public static List<String> toSqlRows(GraphObject graphObject) {

        initProvider();
        
        return provider.toSQLRows(graphObject);
    }
    
    public static List<String> getColumns() {
        
        initProvider();
        
        return provider.getColumns();
        
    }

    private static void initProvider() {

        if(provider == null) {
            
            synchronized (ToSQLRowsHandler.class) {
                
                if(provider == null) {
                    
                    for( Iterator<ToSQLRowsProvider> iterator = ServiceLoader.load(ToSQLRowsProvider.class).iterator(); iterator.hasNext(); ) {
                        
                        ToSQLRowsProvider next = iterator.next();
                        
                        if(provider != null) {
                            log.warn("ToSQLRowsProvider implementation already found: " + provider.getClass().getCanonicalName() + " vs " + next.getClass().getCanonicalName());
                        } else {
                            provider = next;
                        }
                        
                        
                    }
                    
                    if(provider == null) throw new RuntimeException("No ToSQLRowsProvider implementation found");
                    
                }
                
            }
            
        }
        
    }

}
