package ai.vital.query.querybuilder

import ai.vital.query.ARC;
import ai.vital.query.CONSTRAINT_BOOLEAN;
import ai.vital.query.NodeConstraint;
import ai.vital.query.SELECT;
import ai.vital.query.Target;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Node


class NodeConstraintFactory extends BaseConstraintFactory {


	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		NodeConstraint c = null
		if (attributes != null)
			c = new NodeConstraint(attributes)
		else
			c = new NodeConstraint()

		return c
	}

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object c) {
		if(parent instanceof SELECT) {
			((SELECT)parent).children.add(c)
		} else if(parent instanceof ARC) {
			((ARC)parent).children.add(c)
		} else if(parent instanceof CONSTRAINT_BOOLEAN) {
			((CONSTRAINT_BOOLEAN)parent).children.add(c)
		} else if(parent instanceof Target) {
			((Target)parent).constraint = (NodeConstraint)c
		} else {
			throw new RuntimeException("Unexpected parent of ${getName()}: ${parent.class}");
		}
	}

	@Override
	protected Class<? extends GraphObject> getClazz() {
		return VITAL_Node.class;
	}

	@Override
	protected String getName() {
		return "node_constraint";
	}


}
