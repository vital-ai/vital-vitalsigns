package ai.vital.query.querybuilder

import java.util.Map

import ai.vital.query.ARC;
import ai.vital.query.BaseBind;
import groovy.lang.Closure;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

abstract class BaseBindFactory extends AbstractFactory {

	@Override
	public boolean isHandlesNodeChildren() {
		return true
	}

	@Override
	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node,
			Closure childContent) {

		BaseBind b = (BaseBind)node

		Object ev = childContent.call()

		if(ev instanceof String || ev instanceof GString) {
			b.name = ev.toString()
		} else {
			throw new RuntimeException("Bind element must evaluate to String, got: ${ev?.class?.canonicalName}")
		}
	}

	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {
	
		if(parent instanceof ARC) {
		
			((ARC)parent).children.add(child)
				
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
		
	}
}
