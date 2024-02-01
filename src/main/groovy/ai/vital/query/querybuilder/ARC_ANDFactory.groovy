package ai.vital.query.querybuilder

import ai.vital.query.ARC_AND;
import ai.vital.query.ARC_BASE;
import ai.vital.query.ARC_BOOLEAN;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class ARC_ANDFactory extends ARC_BOOLEANFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		ARC_AND arc = null
		if (attributes != null)
			arc = new ARC_AND(attributes)
		else
			arc = new ARC_AND()

		return arc
	}
}