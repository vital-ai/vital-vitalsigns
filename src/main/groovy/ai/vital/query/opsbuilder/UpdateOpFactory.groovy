package ai.vital.query.opsbuilder

import ai.vital.query.ops.UPDATE;
import ai.vital.query.ops.UpdateOp;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class UpdateOpFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
		Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {

	UpdateOp op = attributes != null ? new UpdateOp(attributes) : new UpdateOp()
	
	if(value != null) throw new InstantiationException("Raw Value not allowed in ${name} node")

	if(op.uri == null) throw new InstantiationException("${name} requires uri String attribute")
		
	return op;
}

@Override
public void setParent(FactoryBuilderSupport builder, Object parent,
		Object child) {

	UpdateOp up = child
		
	if(parent instanceof UPDATE) {
		UPDATE upd = (UPDATE)parent
		
		if(up.provides) {
			for(UpdateOp op : upd.updates) {
				if(op.provides == up.provides) {
					throw new RuntimeException("Provided name can only be used once: ${up.provides}")
				}
			}
		}
		
		upd.updates.add(up)
		up.parent = upd
		
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

	UpdateOp op = (UpdateOp)node
	
//	GraphObject instance = op.clazz.newInstance()
	
//	instance.URI = RandomStringUtils.randomAlphanumeric(16)
	
	Expando expando = new Expando()
	
//	op.instance = expando
	
	//use expando for blind call - verification or refs mainly
	expando.URI = op.uri
	
	childContent.call(expando)
	
	op.closure = childContent
	
	return false
	
}
	
}
