package ai.vital.query.opsbuilder

import java.util.Map

import ai.vital.query.Query;
import ai.vital.query.ops.UPDATE;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class UPDATEFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
	
		if(attributes != null) {
			return new UPDATE(attributes)
		} else {
			return new UPDATE()	
		}
	}

	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {
		if(parent instanceof Query) {
			((Query)parent).update = (UPDATE)child
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
	}
	
}
