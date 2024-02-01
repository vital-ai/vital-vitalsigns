package ai.vital.query.querybuilder

import ai.vital.query.ARC;
import ai.vital.query.CONSTRAINT_BOOLEAN;
import ai.vital.query.HYPER_ARC;
import ai.vital.query.Target;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class TargetFactory extends AbstractFactory{


	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		Target t = null
		if (attributes != null)
			t = new Target(attributes)
		else
			t = new Target()

		return t
	}

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object t) {
			
		Target target = t
		
		if(parent instanceof ARC) {
			((ARC)parent).children.add(target)
		} else if(parent instanceof HYPER_ARC) {
			((HYPER_ARC)parent).children.add(target)
		} else if(parent instanceof CONSTRAINT_BOOLEAN) {
			((CONSTRAINT_BOOLEAN)parent).children.add(target)
		} else {
			throw new RuntimeException("Unexpected parent of ${t.class.canonicalName}: ${parent.class.canonicalName}")
		}
	}


}