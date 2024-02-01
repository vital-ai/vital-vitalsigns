package ai.vital.query.querybuilder

import ai.vital.query.ARC;
import ai.vital.query.CONSTRAINT_BOOLEAN
import ai.vital.query.HYPER_ARC
import ai.vital.query.Source;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class SourceFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {

		Source src = null
		
		if(attributes != null) {
			src = new Source(attributes)
		} else {
			src = new Source()
		}
		
		return src
			
	}
			
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,Object child) {
        
        Source src = child;
            
        if(parent instanceof ARC) {
            ((ARC)parent).children.add(src)
        } else if(parent instanceof HYPER_ARC) {
            ((HYPER_ARC)parent).children.add(src)
        } else if(parent instanceof CONSTRAINT_BOOLEAN) {
            ((CONSTRAINT_BOOLEAN)parent).children.add(src)
        } else {
            throw new RuntimeException("Unexpected parent of ${child.class.canonicalName}: ${parent.class.canonicalName}")
        }

	}			

}
