package ai.vital.query.opsbuilder

import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import ai.vital.query.ops.DELETE;
import ai.vital.query.ops.DeleteOp;

class DeleteOpFactory extends AbstractFactory {

	
	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
	
		DeleteOp delOp = attributes != null ? new DeleteOp(attributes) : new DeleteOp()
		
		if(delOp.uri == null) throw new InstantiationException("No uri set in delete node")
		
		return delOp
		
	}
	
	@Override
	public boolean isLeaf() {
		return true
	}
	
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {
		DeleteOp delOp = child

		if(parent instanceof DELETE) {
			((DELETE)parent).deleteOps.add(delOp)	
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
		
	}
	
}
