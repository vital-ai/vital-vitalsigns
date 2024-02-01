package ai.vital.query.querybuilder

import ai.vital.query.BaseConstraint;
import ai.vital.query.Constraint;
import ai.vital.query.Container;
import ai.vital.query.Query;
import ai.vital.query.SELECT;
import ai.vital.vitalservice.query.VitalGraphQueryTypeCriterion;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;
import ai.vital.vitalsigns.model.VITAL_HyperNode;


class SELECTFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
		SELECT s = null
		if (attributes != null)
			s = new SELECT(attributes)
		else
			s = new SELECT()

		return s
	}

	@Override
	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object s) {
		if(parent instanceof Query) {
			((Query)parent).select = (SELECT) s
		} else {
			throw new RuntimeException("Unexecpted parent of SELECT - ${parent.class}")
		}
	}

	static List<Class<? extends GraphObject>> baseTypes = [
		VITAL_Edge.class,
		VITAL_HyperEdge.class,
		VITAL_HyperNode.class,
		VITAL_Node.class
	]
			
	@Override
	public void onNodeCompleted(FactoryBuilderSupport builder, Object parent,
			Object node) {

		Query query = (Query)parent;
		SELECT select = (SELECT)node;
		
		List<VitalGraphQueryTypeCriterion> tcs = []
		
		collectTypeConstraints(select, tcs);	
		
		//validate type constraints
		VitalGraphQueryTypeCriterion prev = null;
		for(VitalGraphQueryTypeCriterion t : tcs) {
			
			if(prev != null) {
				
				Class<? extends GraphObject> t1 = baseType( t.getType() );
				Class<? extends GraphObject> t2 = baseType( prev.getType() ); 
					
				if( !t1.equals(t2) ) {
					throw new RuntimeException("Incompatible graph types used in a select query: ${t.getType().canonicalName} [${t1.simpleName}]" +
						" and ${prev.getType().canonicalName} [${t2.simpleName}]")
				}
				
			}
			
			prev = t;
		}

	}
			
	private Class<? extends GraphObject> baseType(Class<? extends GraphObject> t1) {
		for(Class<? extends GraphObject> x : baseTypes) {
			if(x.isAssignableFrom(t1)) {
				return x;
			}
		}
		throw new RuntimeException("No base graph type found for class: ${t1.canonicalName}")
	}

	private void collectTypeConstraints(Container select, List<VitalGraphQueryTypeCriterion> tcs) {
		
		for(Object ch : select.children) {
			
			if(ch instanceof Container) {
			
				collectTypeConstraints((Container) ch, tcs)
					
			} else if(ch instanceof BaseConstraint) {
			
				BaseConstraint bc = (BaseConstraint) ch
				if(bc.getCriterion() instanceof VitalGraphQueryTypeCriterion) {
					tcs.add(bc.getCriterion())
				}
			
			} else if(ch instanceof Constraint) {
			
				Constraint c = (Constraint) ch
			
				if( c.getCriterion() != null && c.getCriterion() instanceof VitalGraphQueryTypeCriterion) {
					
					tcs.add(c.getCriterion())
					
				}
				
			}
		
			
		}
		
	}	
				
}
