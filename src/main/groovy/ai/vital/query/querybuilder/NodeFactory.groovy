package ai.vital.query.querybuilder

import ai.vital.query.Node;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class NodeFactory extends TypeFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		Node n = null
		if (attributes != null)
			n = new Node(attributes)
		else
			n = new Node()

		return n
	}
}
