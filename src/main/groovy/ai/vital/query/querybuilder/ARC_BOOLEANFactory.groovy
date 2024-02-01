package ai.vital.query.querybuilder

import ai.vital.query.ARC_BASE;
import ai.vital.query.ARC_BOOLEAN;
import groovy.util.FactoryBuilderSupport;

abstract class ARC_BOOLEANFactory extends AbstractFactory {

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object child) {

		if(parent instanceof ARC_BASE || parent instanceof ARC_BOOLEAN) {
			parent.children.add(child);
		} else {
			throw new RuntimeException("Unexpected ${child.class.simpleName} parent: ${parent.class}")
		}
	}
}
