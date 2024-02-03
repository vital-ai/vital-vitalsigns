package ai.vital.query.querybuilder

import ai.vital.query.ARC;
import ai.vital.query.CONSTRAINT_BOOLEAN;
import ai.vital.query.EdgeConstraint;
import ai.vital.query.SELECT;
import ai.vital.query.Target;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Edge


class EdgeConstraintFactory extends BaseConstraintFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		EdgeConstraint c = null
		if (attributes != null)
			c = new EdgeConstraint(attributes)
		else
			c = new EdgeConstraint()

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
			((Target)parent).constraint = (EdgeConstraint)c
		} else {
			throw new RuntimeException("Unexpected parent of ${getName()}: ${parent.class}");
		}

	}

	@Override
	protected Class<? extends GraphObject> getClazz() {
		return VITAL_Edge.class;
	}

	@Override
	protected String getName() {
		return "edge_constraint";
	}

}
