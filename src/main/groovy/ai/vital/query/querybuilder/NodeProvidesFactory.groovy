package ai.vital.query.querybuilder

import ai.vital.query.ARC;
import ai.vital.query.NodeProvides;
import ai.vital.query.ProvidesProperty;
import ai.vital.query.Target;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import org.codehaus.groovy.runtime.GStringImpl

class NodeProvidesFactory extends BaseProvidesFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		NodeProvides p = null
		if (attributes != null)
			p = new NodeProvides(attributes)
		else
			p = new NodeProvides()

		return p
	}

	@Override
	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object p) {
		if(parent instanceof ARC) {
			((ARC)parent).children.add((NodeProvides)p)
		} else if(parent instanceof Target) {
			((Target)parent).provides = (NodeProvides)p
		} else {
			throw new RuntimeException("Unexpected ${getName()} parent: ${parent.class}")
		}
	}

	@Override
	protected String getName() {
		return 'node_provides';
	}

}
