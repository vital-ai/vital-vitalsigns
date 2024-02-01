package ai.vital.query.querybuilder

import java.util.Map

import ai.vital.query.ARC;
import ai.vital.query.ARC_BASE;
import ai.vital.query.EndPath;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class EndPathFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {

		EndPath sp = null
		if(attributes != null) {
			sp = new EndPath(attributes)
		} else {
			sp = new EndPath()
		}
		
		if(value != null) {
			sp.name = value
		} 
		
		return sp;
	}

	@Override
	public boolean isLeaf() {
		return true
	}
			
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		// ? ARC_BASE
		if(parent instanceof ARC) {
			((ARC)parent).children.add(child)
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class}: ${parent.class}")
		}
	}

	
			
}
