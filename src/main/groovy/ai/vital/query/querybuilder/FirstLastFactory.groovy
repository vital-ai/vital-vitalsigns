package ai.vital.query.querybuilder

import java.util.Map

import ai.vital.query.FirstLast;
import ai.vital.query.SELECT;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class FirstLastFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
		) throws InstantiationException, IllegalAccessException {

		FirstLast fl = new FirstLast()
		
		if(name == 'FIRST') {
			fl.firstNotLast = true	
		} else if(name == 'LAST'){
			fl.firstNotLast = false
		} else {
			throw new RuntimeException("Unexpected node: ${name}")
		}
		
		return fl;
	}
		
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		FirstLast fl = child;
			
		if(parent instanceof SELECT) {
			((SELECT) parent).children.add(fl)
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
	}

}
