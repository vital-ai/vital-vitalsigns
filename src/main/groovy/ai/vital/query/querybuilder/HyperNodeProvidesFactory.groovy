package ai.vital.query.querybuilder

import ai.vital.query.HYPER_ARC;
import ai.vital.query.HyperNodeProvides;
import ai.vital.query.ProvidesProperty;
import ai.vital.query.Target;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import org.codehaus.groovy.runtime.GStringImpl

class HyperNodeProvidesFactory extends BaseProvidesFactory {


	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		HyperNodeProvides p = null
		if (attributes != null)
			p = new HyperNodeProvides(attributes)
		else
			p = new HyperNodeProvides()

		return p
	}

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object p) {
		if(parent instanceof HYPER_ARC) {
			((HYPER_ARC)parent).children.add((HyperNodeProvides)p)
		} else if(parent instanceof Target) {
			((Target)parent).provides = (HyperNodeProvides)p
		} else {
			throw new RuntimeException("Unexpected parent of ${p.class} - ${parent.class}")
		}
	}

	@Override
	protected String getName() {
		return 'hypernode_provides';
	}

	
	
			
}
