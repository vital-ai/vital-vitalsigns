package ai.vital.query.querybuilder

import ai.vital.query.ARCElement;
import ai.vital.query.Bind;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class BindFactory extends AbstractFactory {

	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		Bind b = (Bind)child
			
		if(parent instanceof ARCElement) {
			((ARCElement)parent).bind = b
		} else {
			throw new RuntimeException("Unexpected parent of Bind: ${parent.class.canonicalName}")
		}
		
	}

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {

		Bind b = null
		
		if(attributes != null) {
			b = new Bind(attributes)
		} else {
			b = new Bind()
		}
				
		return b;
	}
	
	@Override
	public boolean isHandlesNodeChildren() {
		return true
	}

	@Override
	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node,
			Closure childContent) {

		Bind b = (Bind)node
		
		Object ev = childContent.call()
		
		if(ev instanceof String || ev instanceof GString) {
			b.name = ev.toString()
		} else {
			throw new RuntimeException("Bind element must evaluate to String, got: ${ev?.class?.canonicalName}")
		}
			
	}

}
