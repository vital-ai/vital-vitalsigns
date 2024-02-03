package ai.vital.query.querybuilder

import ai.vital.query.ARC_BASE;
import ai.vital.query.ARC_BOOLEAN;
import ai.vital.query.Capture
import ai.vital.query.GRAPH;
import ai.vital.query.HYPER_ARC;
import ai.vital.query.Query;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class HYPER_ARCFactory extends AbstractFactory {


	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		HYPER_ARC arc = null
		if (attributes != null)
			arc = new HYPER_ARC(attributes)
		else
			arc = new HYPER_ARC()

		return arc
	}

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object hyperarc) {

	    HYPER_ARC ha = hyperarc
        
		//may not be used as top arc!
        if(parent instanceof GRAPH) {
            ((GRAPH)parent).topArc = ha
        } else 
		/*if(parent instanceof Query) {
			((Query)parent).topArc = hyperarc
		} else */
			if(parent instanceof ARC_BASE || parent instanceof ARC_BOOLEAN) {
			if(ha.capture == null) ha.capture = Capture.BOTH
			parent.children.add(hyperarc)
		} else {
			throw new RuntimeException("Unexpected HYPER_ARC parent: " + parent.getClass())
		}
	}

}
