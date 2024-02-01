package ai.vital.query.opsbuilder

import java.util.Map

import org.apache.commons.lang3.RandomStringUtils;

import ai.vital.vitalsigns.uri.URIGenerator;
import ai.vital.query.ops.UPGRADE;
import ai.vital.query.ops.UpgradeDef;
import ai.vital.vitalsigns.model.GraphObject
import groovy.lang.Closure;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class UpgradeDefFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {

		UpgradeDef op = attributes != null ? new UpgradeDef(attributes) : new UpgradeDef()
		
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

		UpgradeDef dd = child
			
		if(parent instanceof UPGRADE) {
            
			UPGRADE d = (UPGRADE)parent 

            d.upgradeDefs.add(dd)
            			
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

		UpgradeDef ud = (UpgradeDef)node
		
//		GraphObject instance = dd.clazz.newInstance()
//		
//		dd.instance = instance.generateURI(dd.getParent().getAppObj())
		
        ud.closure = childContent
        
		return false
		
	}

	
}
