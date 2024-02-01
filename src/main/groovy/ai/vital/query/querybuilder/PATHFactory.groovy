package ai.vital.query.querybuilder

import ai.vital.query.PATH;
import ai.vital.query.Query;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class PATHFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
		Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
	
		return attributes != null ? new PATH(attributes) : new PATH() 	
	
	}
	
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		if(parent instanceof Query) {
			((Query)parent).path = (PATH) child
		} else {
			throw new RuntimeException("Unexecpted parent of PATH - ${parent.class}")
		}
			
	}
			
	@Override
	public void onNodeCompleted(FactoryBuilderSupport builder, Object parent,
			Object node) {

		//validate PATH
		PATH path = node
		
		Object md = path.maxdepth
		
		if( md == null ) throw new RuntimeException("maxdepth of a PATH node not set")
		
		if(md instanceof String) {
			
			if(md == '*') {
				path.maxdepth = 0
			} else {
				throw new RuntimeException("maxdepth as a string may only be set to '*'")
			}
			 
		} else if(md instanceof Integer) {
		
			if(md.intValue() < 0) throw new RuntimeException("maxdepth must not be < 0, 0 means '*'")	
		
		} else {
			throw new RuntimeException("Only '*' string or integer allowed as maxdepth property");
		}
		
		if(path.arcs.size() < 1) throw new RuntimeException("No PATH ARCs set")
			
	}
}
