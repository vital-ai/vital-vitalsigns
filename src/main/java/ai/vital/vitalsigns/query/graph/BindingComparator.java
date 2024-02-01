package ai.vital.vitalsigns.query.graph;

import java.util.Comparator;
import java.util.List;

import ai.vital.vitalservice.query.GraphElement;
import ai.vital.vitalservice.query.SortStyle;
import ai.vital.vitalservice.query.VitalSortProperty;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.query.graph.QueryAnalysis.ProvidesValueParent;
import ai.vital.vitalsigns.rdf.RDFUtils;

/**
 * The comparator has to work in reversed logic - used in a priority queue
 *
 */
public class BindingComparator implements Comparator<Binding> {

    List<ProvidesValueParent> pvps;
    
    List<VitalSortProperty> sortProperties;

    private SortStyle sortStyle;
    
    
    public BindingComparator(List<ProvidesValueParent> pvps,
            List<VitalSortProperty> sortProperties, SortStyle sortStyle) {
        super();
        this.pvps = pvps;
        this.sortProperties = sortProperties;
        this.sortStyle = sortStyle;
    }


    @Override
    public int compare(Binding bind1, Binding bind2) {

        if(sortStyle == SortStyle.inOrder) {
            return inOrderImplementation(bind1, bind2);
        } else {
            return mergedImplementation(bind1, bind2);
        }
        
    }
    
    private int mergedImplementation(Binding bind1, Binding bind2) {

        Object p1 = null;
        Object p2 = null;
        
        for(int i = 0 ; i < pvps.size(); i++) {
            ProvidesValueParent pvp = pvps.get(i);
            p1 = getValue(pvp, bind1);
            if(p1 != null) break;
        }
        
        for(int i = 0 ; i < pvps.size(); i++) {
            ProvidesValueParent pvp = pvps.get(i);
            p2 = getValue(pvp, bind2);
            if(p2 != null) break;
        }
        
        return compareValues(p1, p2, sortProperties.get(0).isReverse());
    }


    private int inOrderImplementation(Binding bind1, Binding bind2) {
        
        for(int i = 0 ; i < pvps.size(); i++) {
            
            ProvidesValueParent pvp = pvps.get(i);
            
            Object p1 = getValue(pvp, bind1);
            
            Object p2 = getValue(pvp, bind2);
            
            int c = compareValues(p1, p2, sortProperties.get(i).isReverse());
            
            if(c != 0) {
                return c;
            }
            
        }

        return 0;
        
    }
    
    private int compareValues(Object p1, Object p2, boolean reverse) {
        if(p1 == null && p2 == null) {
            return 0;
        }
        
        if(p1 == null) {
            return reverse ? -1 : +1;
        }
        
        if(p2 == null) {
            return reverse ? +1 : -1;
        }
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        int c = ((Comparable)p2).compareTo((Comparable)p1);
        
        if(c != 0 ) {
            return reverse ? (-1 * c) : c;
        }
        
        return 0;
    }
    
    private Object getValue(ProvidesValueParent pvp, Binding b) {
        
        BindingEl b1 = b.getBindingElForArc(pvp.arc);
        
        GraphElement symbol = pvp.value.getSymbol();
        GraphObject g1 = null;
        if(symbol == GraphElement.Connector) {
            if(pvp.arc.isTopArc()) throw new RuntimeException("Cannot use provided connector value in top arc");
            g1 = b1 != null ? b1.getConnector() : null;
        } else {
            g1 = b1 != null ? b1.getEndpoint() : null;
        }
        
        Object p1 = null;
        
        if(pvp.value.getPropertyURI().equals("URI") || pvp.value.getPropertyURI().equals(VitalCoreOntology.URIProp.getURI())) {

            p1 = g1 != null ? g1.getURI() : null;
            
        } else {
            
            p1 = g1 != null ? g1.getProperty(RDFUtils.getPropertyShortName(pvp.value.getPropertyURI())) : null;
            
        }
        
        return p1;
        
    }
    
}
