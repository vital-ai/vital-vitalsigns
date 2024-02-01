package ai.vital.query.querybuilder

import ai.vital.query.EdgeBind;
import groovy.util.FactoryBuilderSupport

import java.util.Map;

class EdgeBindFactory extends BaseBindFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {

		EdgeBind eb = null
		
		if(attributes != null) {
			eb = new EdgeBind(attributes)	
		} else {
			eb = new EdgeBind()
		}
		return eb;
	}

}
