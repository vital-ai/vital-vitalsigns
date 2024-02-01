package ai.vital.query.querybuilderimport ai.vital.query.ARC_BASE;import ai.vital.query.CONSTRAINT_BOOLEANimport ai.vital.query.SELECT;import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;
abstract class CONSTRAINT_BOOLEANFactory extends AbstractFactory {
	public boolean isLeaf() {		return false	}	public void setParent(FactoryBuilderSupport builder,			Object parent, Object child) {		if(parent instanceof ARC_BASE || parent instanceof SELECT || parent instanceof CONSTRAINT_BOOLEAN) {			parent.children.add(child)		} else {			throw new RuntimeException("Unexpected ${child.class.simpleName} parent: ${parent.class}")		}	}}
