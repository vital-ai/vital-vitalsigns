package ai.vital.vitalsigns.csv

import java.util.Iterator
import java.util.List
import java.util.ServiceLoader

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.vitalsigns.model.GraphObject;

public class ToCSVHandler {

    private final static Logger log = LoggerFactory.getLogger(ToCSVHandler.class)
    
    private static ToCSVProvider provider
    
    public static void toCSV(GraphObject graphObject, List<String> targetList) {
        
        initProvider()
        
        provider.toCSV(graphObject, targetList)
        
    }
    
    public static String getHeaders() {
        
        initProvider()
        
        return provider.getHeaders();
    }

    private static void initProvider() {

        if(provider == null) {
            
            synchronized (ToCSVHandler.class) {
                
                if(provider == null) {
                    
                    for( Iterator<ToCSVProvider> iterator = ServiceLoader.load(ToCSVProvider.class).iterator(); iterator.hasNext(); ) {
                        
                        ToCSVProvider next = iterator.next();
                        
                        if(provider != null) {
                            log.warn("ToCSVProvider implementation already found: " + provider.getClass().getCanonicalName() + " vs " + next.getClass().getCanonicalName());
                        } else {
                            provider = next;
                        }
                        
                        
                    }
                    
                    if(provider == null) throw new RuntimeException("No ToCSVProvider implementation found");
                    
                }
                
            }
            
        }
        
        
    }
    
}
