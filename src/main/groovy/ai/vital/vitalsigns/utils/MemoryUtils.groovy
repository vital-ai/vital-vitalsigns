package ai.vital.vitalsigns.utils;

public class MemoryUtils {

    static int mb = 1024*1024;
    
    public static void printMemoryUsage() {
    
        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();
        
        System.out.print("##### Heap utilization stats [MB]: ");
        
        //Print used memory
        System.out.print(" Used Memory:" 
            + (runtime.totalMemory() - runtime.freeMemory()) / mb);
    
        //Print free memory
        System.out.print(" Free Memory:" 
            + runtime.freeMemory() / mb);
        
        //Print total available memory
        System.out.print(" Total Memory:" + runtime.totalMemory() / mb);
    
        //Print Maximum available memory
        System.out.print(" Max Memory:" + runtime.maxMemory() / mb);
        
        System.out.print("\n");
        
    }
    
}
