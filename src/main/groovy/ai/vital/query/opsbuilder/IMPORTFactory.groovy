package ai.vital.query.opsbuilder

import ai.vital.query.Query
import ai.vital.query.ops.IMPORT
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class IMPORTFactory extends AbstractFactory {

	
	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
	
		if(name != "IMPORT") throw new InstantiationException("Expected IMPORT node name: ${name}")
	
		IMPORT _import = attributes != null ? new IMPORT(attributes) : new IMPORT()
		return _import
		
	}
	
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		if(parent instanceof Query) {
			Query q = parent
			if(q._import) throw new RuntimeException("Exactly 1 IMPORT element allowed")
			q._import = (IMPORT)child
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
			
	}
	
}
