package ai.vital.query.ops;

import java.util.ArrayList;
import java.util.List;

import ai.vital.query.Query;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.VitalBlock;
import ai.vital.vitalsigns.model.GraphObject;

public class ToBlockImplementation {

    private Query query;

    public ToBlockImplementation(Query query) {
        this.query = query;
    }
    
    public List<VitalBlock> toBlock() {
        
        if(query.getInstantiates() == null || query.getInstantiates().size() < 1) throw new NullPointerException("Expected at least 1 INSTANTIATE element");
        
        List<INSTANTIATE> instantiates = query.getInstantiates();
        
        List<VitalBlock> r = new ArrayList<VitalBlock>(instantiates.size());
        
        for(INSTANTIATE instantiate : instantiates) {
            
            List<GraphObject> g = new ArrayList<GraphObject>();
            for(InstanceOp i : instantiate.getInstances()) {
                g.add(i.getInstance());
            }
            r.add(new VitalBlock(g));
        }
        
        return r;
        
        
    }
    
}
