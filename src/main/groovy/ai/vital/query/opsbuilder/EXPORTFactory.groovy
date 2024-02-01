package ai.vital.query.opsbuilder

import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import ai.vital.query.Query
import ai.vital.query.ops.EXPORT;

class EXPORTFactory extends AbstractFactory {

	
	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
	
		if(name != "EXPORT") throw new InstantiationException("Expected EXPORT node name: ${name}")
	
		EXPORT _export = attributes != null ? new EXPORT(attributes) : new EXPORT()
		return _export
		
	}
	
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		if(parent instanceof Query) {
			Query q = parent
			if(q.export) throw new RuntimeException("Exactly 1 EXPORT element allowed")
			q.export = (EXPORT)child
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
			
	}

}
