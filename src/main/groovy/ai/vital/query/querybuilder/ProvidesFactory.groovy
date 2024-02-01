package ai.vital.query.querybuilder

import ai.vital.query.ARC_BASE;
import ai.vital.query.Provides;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class ProvidesFactory extends AbstractFactory {

	
	public boolean isLeaf() {
		return false
	  }
	
	  public Object newInstance(FactoryBuilderSupport builder,
		Object name, Object value, Map attributes
		) throws InstantiationException, IllegalAccessException {
		  Provides p = null
		  if (attributes != null)
			p = new Provides(attributes)
		  else
			p = new Provides()
			
		  return p
	  }
	
	  public void setParent(FactoryBuilderSupport builder,
		Object parent, Object p) {
		if(parent instanceof ARC_BASE) {
			((ARC_BASE)parent).children.add((Provides)p)
		} else {
			throw new RuntimeException("Unexpected parent of provides: ${parent.class}")
		}
	  }
	
	
}
