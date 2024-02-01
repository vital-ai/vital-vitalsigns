package ai.vital.query.querybuilder

import ai.vital.query.ARC_BASE;
import ai.vital.query.OR;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class ORFactory extends CONSTRAINT_BOOLEANFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		OR or = null
		if (attributes != null)
			or = new OR(attributes)
		else
			or = new OR()

		return or
	}

}
