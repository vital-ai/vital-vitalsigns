package ai.vital.query.querybuilder

import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import ai.vital.query.BaseConstraint;
import ai.vital.query.Eval;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;

class EvalFactory extends AbstractFactory {


	public boolean isHandlesNodeChildren() {
		return true
	}


	public boolean isLeaf() {
		return false
	}

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		Eval e = null
		if (attributes != null)
			e = new Eval(attributes)
		else
			e = new Eval()

		return e
	}
	
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {
//		Eval eval = child;
//		
//		if(parent instanceof BaseConstraint)
		
		super.setParent(builder, parent, child);
	}


	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node, Closure childContent) {

		if(childContent == null) throw new RuntimeException("Eval node must not be null")

		Eval eval = node;
		
		Object childResult = childContent.call();

		
		if(childResult instanceof String  || childResult instanceof GString) {

			VitalGraphQueryPropertyCriterion criterion = null
			try {
				criterion = BaseConstraintFactory.evaluate(childResult.toString())
				if(criterion == null) throw new RuntimeException("string must not evaluate to null");
			} catch(Exception e) {
				throw new RuntimeException("Error when evaluating a string to crierion: " + e.localizedMessage)
			}
			
			eval.criterion = criterion
			
		} else {
		
			throw new RuntimeException("Eval content result not a string: ${childResult?.class}")
		
		}
		

		return false
	}
}
