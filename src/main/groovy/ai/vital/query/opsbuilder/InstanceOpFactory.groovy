package ai.vital.query.opsbuilder

import java.util.Map

import org.apache.commons.lang3.RandomStringUtils;

import ai.vital.vitalsigns.uri.URIGenerator;
import ai.vital.query.ops.INSTANTIATE;
import ai.vital.query.ops.InstanceOp;
import ai.vital.vitalsigns.model.GraphObject
import groovy.lang.Closure;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class InstanceOpFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {

		InstanceOp op = attributes != null ? new InstanceOp(attributes) : new InstanceOp()
		
		if(value == null) throw new InstantiationException("No ${name} node value, expected class")
		
		if(!(value instanceof Class)) throw new InstantiationException("${name} value must be a subclass of GraphObject class, got: ${value.getClass().getCanonicalName()}") 

		if(!GraphObject.class.isAssignableFrom(value)) throw new InstantiationException("${name} value must be a subclass of GraphObject class, got: ${value.getClass().getCanonicalName()}")
		
		op.clazz = value
			
		return op;
	}

	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		InstanceOp io = child
			
		if(parent instanceof INSTANTIATE) {
			INSTANTIATE ins = (INSTANTIATE)parent 
			
			if(io.provides) {
				for(InstanceOp op : ins.instances) {
					if(op.provides == io.provides) {
						throw new RuntimeException("Provided name can only be used once: ${io.provides}")
					}
				}
			} 
			
			ins.instances.add(io)
			io.parent = ins
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

		InstanceOp op = (InstanceOp)node
		
		GraphObject instance = op.clazz.newInstance()
		
		op.instance = instance.generateURI(op.getParent().getAppObj())
		
		childContent.call(instance)
		
		return false
		
	}

	
}
