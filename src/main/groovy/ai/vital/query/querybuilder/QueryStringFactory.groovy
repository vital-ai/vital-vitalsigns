package ai.vital.query.querybuilder

import ai.vital.query.QueryString;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class QueryStringFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport arg0, Object arg1,
			Object arg2, Map arg3) throws InstantiationException,
			IllegalAccessException {
		QueryString s = new QueryString()
		return s;
	}
			
	@Override
	public boolean isHandlesNodeChildren() {
		return true
	}

	@Override
	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node,
			Closure childContent) {

		QueryString qs = (QueryString) node;
			
		Object obj = childContent.call();
		
		if(obj instanceof String) {
			
			qs.query = obj
			
		} else if(obj instanceof GString) {
		
			qs.query = obj.toString()
		
		} else {
		
			throw new RuntimeException("QueryString may only accept String or GString object, got: " + obj)
			
		}
			
		return false
	}
	
	

}
