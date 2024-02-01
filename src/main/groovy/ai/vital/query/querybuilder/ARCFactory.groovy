package ai.vital.query.querybuilder

import ai.vital.query.ARC;
import ai.vital.query.ARC_AND;
import ai.vital.query.ARC_BASE;
import ai.vital.query.ARC_BOOLEAN;
import ai.vital.query.ARC_OR;
import ai.vital.query.Capture;
import ai.vital.query.GRAPH;
import ai.vital.query.PATH;
import ai.vital.query.Query;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class ARCFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		ARC arc = null
		if (attributes != null)
			arc = new ARC(attributes)
		else
			arc = new ARC()

		arc.root = name == 'ROOT'
			
		return arc
	}

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object arc) {

		ARC arcX = arc
		
		if(arcX.root) {
			
			if(parent instanceof PATH) {
				((PATH)parent).root = arcX
			} else {
				throw new RuntimeException("Unexpected ROOT parent: " + parent.getClass().simpleName)
			}
			
		} else {
		
			if(parent instanceof GRAPH) {
				GRAPH g = (GRAPH)parent
						if(g.topArc != null) throw new RuntimeException("GRAPH may only have a single ARC element")
				g.topArc = arc
				if(arcX.capture == null) {
					arcX.capture = Capture.SOURCE
				} else if(arcX.capture != Capture.SOURCE && arcX.capture != Capture.NONE) {
					throw new RuntimeException("Top arc may only have capture set to SOURCE or NONE, current: " + arcX.capture)
				}
			} else if(parent instanceof ARC_BASE || parent instanceof ARC_BOOLEAN) {
			
				if(parent instanceof ARC && parent.pathArc == true) {
					throw new RuntimeException("Cannot nest arcs in a path query") 
				}
				
				if(arcX.capture == null) {
					arcX.capture = Capture.BOTH
				} else if(arcX.capture == Capture.SOURCE) {
					throw new RuntimeException("Capture.SOURCE only allowed in top arc");
				}
				parent.children.add(arc)
			} else if(parent instanceof PATH) {
				PATH path = (PATH) parent;
				arcX.pathArc = true
				path.arcs.add(arcX)
			} else {
				throw new RuntimeException("Unexpected ARC parent: " + parent.getClass())
			}
			
		}
	}
}
