package ai.vital.query.querybuilder

import ai.vital.query.ARCElement;
import ai.vital.query.ARC_BASE;
import ai.vital.query.CONSTRAINT_BOOLEAN;
import ai.vital.query.Constraint;
import ai.vital.query.SELECT;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion
import ai.vital.vitalservice.query.VitalGraphQueryTypeCriterion
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import org.codehaus.groovy.runtime.GStringImpl

class ConstraintFactory extends AbstractFactory {


	public boolean isLeaf() {
		return false
	}

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		Constraint c = null
		if (attributes != null)
			c = new Constraint(attributes)
		else
			c = new Constraint()

		return c
	}

	//constraints are only allowed within ARC_BASE or poropertyt NORMAL boolean con
	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object c) {

		if(parent instanceof ARC_BASE || parent instanceof CONSTRAINT_BOOLEAN || parent instanceof SELECT) {
			parent.children.add(c)
		} else if(parent instanceof ARCElement) {
			((ARCElement)parent).rawConstraint = c
		} else {
			throw new RuntimeException("Unexpected parent of constraint: " + parent.class)
		}

	}

	public boolean isHandlesNodeChildren() {
		return true
	}

	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node, Closure childContent) {

		Constraint c = (Constraint) node;

		Integer pagesize = null
		Integer page = null

		Object o = childContent.call()


		if(o instanceof java.lang.Class) {

			c.criterion = new VitalGraphQueryTypeCriterion(null, o);

		} else if(o instanceof GString || o instanceof java.lang.String) {

			c.eval = o.toString()

			//evaluate property now
			//also Type Criterion
		} else if(o instanceof VitalGraphQueryPropertyCriterion ) {

			c.criterion = o;

		} else {

			throw new RuntimeException("Unexpected constraint value: ${o}")

		}

		return false


	}

}
