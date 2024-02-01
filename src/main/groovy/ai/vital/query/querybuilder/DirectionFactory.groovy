package ai.vital.query.querybuilder;

import java.util.Map;

import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

public class DirectionFactory extends AbstractFactory {

	@Override
	public boolean isLeaf() {
		return true;
	}
	
	@Override
	public Object newInstance(FactoryBuilderSupport arg0, Object arg1,
			Object arg2, Map arg3) throws InstantiationException,
			IllegalAccessException {

		throw new RuntimeException("XXX");
	}

}
