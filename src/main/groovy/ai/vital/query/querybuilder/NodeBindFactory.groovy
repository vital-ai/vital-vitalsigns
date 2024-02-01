package ai.vital.query.querybuilder

import groovy.util.FactoryBuilderSupport

import java.util.Map;

import ai.vital.query.NodeBind;

class NodeBindFactory extends BaseBindFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {

		NodeBind nb = null
		
		if(attributes != null) {
			nb = new NodeBind(attributes)
		} else {
			nb = new NodeBind()
		}
			
		return nb;
		
	}

}
