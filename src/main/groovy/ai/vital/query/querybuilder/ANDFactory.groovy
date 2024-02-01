package ai.vital.query.querybuilder

import ai.vital.query.AND;
import ai.vital.query.ARC_BASE;
import ai.vital.query.ARC_BOOLEAN;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class ANDFactory extends CONSTRAINT_BOOLEANFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		AND and = null
		if (attributes != null)
			and = new AND(attributes)
		else
			and = new AND()

		return and
	}
}
