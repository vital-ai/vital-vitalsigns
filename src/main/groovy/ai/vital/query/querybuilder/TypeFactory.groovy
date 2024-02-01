package ai.vital.query.querybuilder

import ai.vital.query.Constraint;
import ai.vital.query.Edge;
import ai.vital.query.Type;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

abstract class TypeFactory extends AbstractFactory {


	public boolean isHandlesNodeChildren() {
		return true
	}


	public boolean isLeaf() {
		return false
	}

	public void setParent(FactoryBuilderSupport builder,
			Object parent, Object e) {
		if(parent instanceof Constraint) {
			((Constraint)parent).type = (Type) e;
		} else {
			throw new RuntimeException("Unexpected parent of edge element: ${parent.class}")
		}

	}

	public void onNodeCompleted(FactoryBuilderSupport builder,
			Object parent, Object t) {
		//invoice.save()
	}


	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node, Closure childContent) {

		println "TypeFactory: " + childContent

		println "TypeFactory: " + childContent.call()
		return false
		
	}
}
