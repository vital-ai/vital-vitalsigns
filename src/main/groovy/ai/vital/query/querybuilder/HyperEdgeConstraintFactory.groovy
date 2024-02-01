package ai.vital.query.querybuilder

import ai.vital.query.CONSTRAINT_BOOLEAN
import ai.vital.query.Connector;
import ai.vital.query.HYPER_ARC;
import ai.vital.query.HyperEdgeConstraint;
import ai.vital.query.SELECT;
import ai.vital.query.Target;
import ai.vital.vitalsigns.model.GraphObject

class HyperEdgeConstraintFactory extends BaseConstraintFactory {


	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		HyperEdgeConstraint c = null
		if (attributes != null)
			c = new HyperEdgeConstraint(attributes)
		else
			c = new HyperEdgeConstraint()

		return c
	}

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object c) {

		if(parent instanceof SELECT) {
			((SELECT)parent).children.add(c)
		} else if(parent instanceof HYPER_ARC) {
			((HYPER_ARC)parent).children.add(c)
        } else if(parent instanceof CONSTRAINT_BOOLEAN) {
            ((CONSTRAINT_BOOLEAN)parent).children.add(c)
		} else if(parent instanceof Connector) {
			((Connector) parent).constraint = (HyperEdgeConstraint)c
		} else if(parent instanceof Target) {
			((Target) parent).constraint = (HyperEdgeConstraint)c;
		} else {
			throw new RuntimeException("Unexpected parent of ${getName()} - ${parent.class}")
		}
	}

	@Override
	protected Class<? extends GraphObject> getClazz() {
		return VITAL_HyperEdge.class;
	}

	@Override
	protected String getName() {
		return 'hyperedge_constraint';
	}

}
