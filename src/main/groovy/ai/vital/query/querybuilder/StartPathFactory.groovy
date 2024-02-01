package ai.vital.query.querybuilder

import java.util.Map

import ai.vital.query.ARC;
import ai.vital.query.ARC_BASE;
import ai.vital.query.StartPath;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class StartPathFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {

		StartPath sp = null
		if(attributes != null) {
			sp = new StartPath(attributes)
		} else {
			sp = new StartPath()
		} 
		
		if(value != null) {
			sp.name = value	
		}
		
		
		if(sp.maxLength != null && sp.minLength != null) {
			if(sp.minLength > sp.maxLength) {
				throw new InstantiationException("start_path minLength (${sp.minLength}) must be <= maxLength (${sp.maxLength})")
			}
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

		//? base
		if(parent instanceof ARC) {
			((ARC)parent).children.add(child)
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class}: ${parent.class}")
		}
	}

	
			
}
