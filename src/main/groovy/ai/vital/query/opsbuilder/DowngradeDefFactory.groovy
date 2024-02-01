package ai.vital.query.opsbuilder

import java.util.Map

import org.apache.commons.lang3.RandomStringUtils;

import ai.vital.vitalsigns.uri.URIGenerator;
import ai.vital.query.ops.DOWNGRADE;
import ai.vital.query.ops.DowngradeDef;
import ai.vital.vitalsigns.model.GraphObject
import groovy.lang.Closure;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class DowngradeDefFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {

		DowngradeDef op = attributes != null ? new DowngradeDef(attributes) : new DowngradeDef()
		
//		if(value == null) throw new InstantiationException("No ${name} node value, expected class")
//		
//		if(!(value instanceof Class)) throw new InstantiationException("${name} value must be a subclass of GraphObject class, got: ${value.getClass().getCanonicalName()}") 
//
//		if(!GraphObject.class.isAssignableFrom(value)) throw new InstantiationException("${name} value must be a subclass of GraphObject class, got: ${value.getClass().getCanonicalName()}")
//		
//		op.clazz = value
//			
		return op;
	}

	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		DowngradeDef dd = child
			
		if(parent instanceof DOWNGRADE) {
            
			DOWNGRADE d = (DOWNGRADE)parent 

            d.downgradeDefs.add(dd)
            			
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
			
	}
	
	@Override
	public boolean isHandlesNodeChildren() {
		return true
	}

	@Override
	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node,
			Closure childContent) {

		DowngradeDef dd = (DowngradeDef)node
		
//		GraphObject instance = dd.clazz.newInstance()
//		
//		dd.instance = instance.generateURI(dd.getParent().getAppObj())
		
        dd.closure = childContent
        
		return false
		
	}

	
}
