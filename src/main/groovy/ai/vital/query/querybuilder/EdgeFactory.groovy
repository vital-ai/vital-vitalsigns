package ai.vital.query.querybuilder

import ai.vital.query.Constraint;
import ai.vital.query.Edge;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class EdgeFactory extends TypeFactory {

	public boolean isLeaf() {
		return false
	}

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		Edge e = null
		if (attributes != null)
			e = new Edge(attributes)
		else
			e = new Edge()

		return e
	}
}
