package ai.vital.query.querybuilder

import ai.vital.query.ARC;
import ai.vital.query.CONSTRAINT_BOOLEAN;
import ai.vital.query.Connector;
import ai.vital.query.HYPER_ARC;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class ConnectorFactory extends AbstractFactory {

	  public Object newInstance(FactoryBuilderSupport builder,
		Object name, Object value, Map attributes
		) throws InstantiationException, IllegalAccessException {
		  Connector c = null
		  if (attributes != null)
			c = new Connector(attributes)
		  else
			c = new Connector()
			
		  return c
	  }
	
	  public void setParent(FactoryBuilderSupport builder,
		Object parent, Object c) {
		
		Connector conn = c
		
		if(parent instanceof ARC) {
			((ARC)parent).children.add(conn)
		} else if(parent instanceof HYPER_ARC) {
			((HYPER_ARC)parent).children.add(conn)
		} else if(parent instanceof CONSTRAINT_BOOLEAN) {
			((CONSTRAINT_BOOLEAN)parent).children.add(conn)
		} else {
			throw new RuntimeException("Unexpected parent of ${c.class.canonicalName}: ${parent.class.canonicalName}")
		}
		
	  }
	
}
