package ai.vital.query.opsbuilder

import java.util.Map

import ai.vital.query.Query;
import ai.vital.query.ops.DELETE;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class DELETEFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
	
		if(attributes != null) {
			return new DELETE(attributes)
		} else {
			return new DELETE()	
		}
	}
	
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {
		if(parent instanceof Query) {
			((Query)parent).delete = (DELETE)child
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
	}
}
