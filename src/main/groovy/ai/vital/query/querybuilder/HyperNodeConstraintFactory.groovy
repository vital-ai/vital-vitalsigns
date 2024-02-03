package ai.vital.query.querybuilder

import ai.vital.query.CONSTRAINT_BOOLEAN;
import ai.vital.query.HYPER_ARC;
import ai.vital.query.HyperNodeConstraint;
import ai.vital.query.SELECT;
import ai.vital.query.Target;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_HyperNode


class HyperNodeConstraintFactory extends BaseConstraintFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		HyperNodeConstraint n = null
		if (attributes != null)
			n = new HyperNodeConstraint(attributes)
		else
			n = new HyperNodeConstraint()

		return n
	}

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object n) {
			
		if(parent instanceof SELECT) {
			((SELECT)parent).children.add(n)
		} else if(parent instanceof HYPER_ARC) {
			((HYPER_ARC)parent).children.add((HyperNodeConstraint)n)
		} else if(parent instanceof CONSTRAINT_BOOLEAN) {
			((CONSTRAINT_BOOLEAN)parent).children.add(n)
		} else if(parent instanceof Target) {
			((Target) parent).constraint = (HyperNodeConstraint)n;
		} else {
			throw new RuntimeException("Unexpected parent of ${n.class} - ${parent.class}")
		}
	}

	@Override
	protected Class<? extends GraphObject> getClazz() {
		return VITAL_HyperNode.class;
	}

	@Override
	protected String getName() {
		return 'hypernode_constraint';
	}

}
