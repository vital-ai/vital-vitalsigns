package ai.vital.query.querybuilder

import ai.vital.query.ARC_BASE;
import ai.vital.query.ARC_BOOLEAN;
import ai.vital.query.ARC_OR;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class ARC_ORFactory extends ARC_BOOLEANFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		ARC_OR arc = null
		if (attributes != null)
			arc = new ARC_OR(attributes)
		else
			arc = new ARC_OR()

		return arc
	}
}
