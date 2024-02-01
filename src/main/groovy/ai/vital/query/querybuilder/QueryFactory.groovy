package ai.vital.query.querybuilder

import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import ai.vital.query.Query;


class QueryFactory extends AbstractFactory {
	
	public Object newInstance(FactoryBuilderSupport builder,
	  Object name, Object value, Map attributes
	  ) throws InstantiationException, IllegalAccessException {
		Query q = null
		if (attributes != null)
		  q = new Query(attributes)
		else
		  q = new Query()
		return q
	}
  
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {
		throw new RuntimeException("Query's setParent shouldn't be called")
	}
	
	  
}
	
