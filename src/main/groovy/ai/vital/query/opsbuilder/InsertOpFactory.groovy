package ai.vital.query.opsbuilder

import java.util.Map

import org.apache.commons.lang3.RandomStringUtils;

import ai.vital.vitalsigns.uri.URIGenerator;
import ai.vital.query.ops.INSERT;
import ai.vital.query.ops.InsertOp;
import ai.vital.vitalsigns.model.GraphObject
import groovy.lang.Closure;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class InsertOpFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {

		InsertOp op = attributes != null ? new InsertOp(attributes) : new InsertOp()
		
		if(value == null) throw new InstantiationException("No ${name} node value, expected class")
		
		if(!(value instanceof Class)) throw new InstantiationException("${name} value must be a subclass of GraphObject class, got: ${value.getClass().getCanonicalName()}") 

		if(!GraphObject.class.isAssignableFrom(value)) throw new InstantiationException("${name} value must be a subclass of GraphObject class, got: ${value.getClass().getCanonicalName()}")
		
		op.clazz = value
			
		return op;
	}

	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		InsertOp io = child
			
		if(parent instanceof INSERT) {
			INSERT ins = (INSERT)parent 
			
			if(io.provides) {
				for(InsertOp op : ins.insertions) {
					if(op.provides == io.provides) {
						throw new RuntimeException("Provided name can only be used once: ${io.provides}")
					}
				}
			} 
			
			ins.insertions.add(io)
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

		InsertOp op = (InsertOp)node
		
		GraphObject instance = op.clazz.newInstance()
		
		op.instance = instance.generateURI()
		
		childContent.call(instance)
		
		return false
		
	}

	
}
