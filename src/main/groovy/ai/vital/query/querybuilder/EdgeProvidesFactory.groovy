package ai.vital.query.querybuilder

import ai.vital.query.ARC;
import ai.vital.query.EdgeProvides;
import ai.vital.query.ProvidesProperty;
import ai.vital.query.Target;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import org.codehaus.groovy.runtime.GStringImpl

class EdgeProvidesFactory extends BaseProvidesFactory {


	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		EdgeProvides p = null
		if (attributes != null)
			p = new EdgeProvides(attributes)
		else
			p = new EdgeProvides()

		return p
	}

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object p) {

		if(parent instanceof ARC) {
			((ARC)parent).children.add((EdgeProvides)p)
		} else if(parent instanceof Target) {
			((Target)parent).provides = (EdgeProvides)p
		} else {
			throw new RuntimeException("Unexpected ${getName()} parent: ${parent.class}")
		}

	}

	@Override
	protected String getName() {
		return 'edge_provides';
	}
			
		
}
