package ai.vital.query.opsbuilder

import java.util.Map

import ai.vital.query.Query;
import ai.vital.query.ops.INSTANTIATE;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class INSTANTIATEFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
	
		if(attributes != null) {
			return new INSTANTIATE(attributes)
		} else {
			return new INSTANTIATE()	
		}
	}

	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {
		if(parent instanceof Query) {
            Query q = parent
            if(q.instantiates == null)
			q.instantiates = []
            q.instantiates.add((INSTANTIATE)child)
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
	}
}
