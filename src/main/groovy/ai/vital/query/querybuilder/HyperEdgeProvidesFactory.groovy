package ai.vital.query.querybuilder

import ai.vital.query.Connector;
import ai.vital.query.HYPER_ARC;
import ai.vital.query.HyperEdgeProvides;
import ai.vital.query.ProvidesProperty;
import ai.vital.query.Target;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import org.codehaus.groovy.runtime.GStringImpl

class HyperEdgeProvidesFactory extends BaseProvidesFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		HyperEdgeProvides p = null
		if (attributes != null)
			p = new HyperEdgeProvides(attributes)
		else
			p = new HyperEdgeProvides()

		return p
	}

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object p) {
//		if(parent instanceof HYPER_ARC) {
//			((HYPER_ARC)parent).children.add((HyperEdgeProvides)p)
		if(parent instanceof Target) {
			((Target)parent).provides = (HyperEdgeProvides)p
		} else if(parent instanceof Connector) {
			((Connector)parent).provides = (HyperEdgeProvides)p 
		} else {
			throw new RuntimeException("Unexpected parent of ${p.class} - ${parent.class}")
		}
	}

	@Override
	protected String getName() {
		return 'hyperedge_provides';
	}
}
