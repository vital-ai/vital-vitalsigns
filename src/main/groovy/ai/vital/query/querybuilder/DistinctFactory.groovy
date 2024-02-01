package ai.vital.query.querybuilder

import java.util.Map

import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.query.Distinct;
import ai.vital.query.FirstLast;
import ai.vital.query.SELECT;
import ai.vital.query.AggregateFunction.Count;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.property.URIProperty;
import groovy.lang.Closure;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;


class DistinctFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
	
		Distinct d = null
		
		if (attributes != null) {
			d = new Distinct(attributes)
		} else {
			d = new Distinct()
		}
		
		if(value == null) throw new RuntimeException(name + ' requires value - property URI or instance')
		
		IProperty prop = null
		
		if(value instanceof String) {
			
			prop = VitalSigns.get().getProperty(value);
			
			if(prop == null) {
				prop = VitalSigns.get().getProperty(new URIProperty(value))
			}
			
			if(prop == null) throw new RuntimeException("Distinct property not found (or ambiguous): " + value);
			
		} else if(value instanceof IProperty) {
			prop = VitalSigns.get().getProperty( new URIProperty(((IProperty)value).getURI()) )
			if(prop == null) throw new RuntimeException("Distinct property not found: " + value.getURI());
		} else if(value instanceof VitalGraphQueryPropertyCriterion) {
			String propURI = ((VitalGraphQueryPropertyCriterion)value).getPropertyURI();
			prop = VitalSigns.get().getProperty(new URIProperty(propURI))
			if(prop == null) throw new RuntimeException("Distinct property not found: " + propURI);
		}
		
		d.propertyURI = prop.getURI()
		
		return d
	}

	@Override
	public boolean isHandlesNodeChildren() {
		return false
	}
	
	@Override
	public boolean isLeaf() {
		return true;
	}
	


	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		Distinct d = (Distinct)child
			
		if(parent instanceof SELECT) {
			((SELECT)parent).children.add(child)
//		} else if(parent instanceof Count) {
//			((Count)parent).distinct = d
		} else if(parent instanceof FirstLast) {
			((FirstLast) parent).distinct = d
		} else {
			throw new RuntimeException("Unexpected parent of a ${d.class.simpleName} element: ${parent.class.simpleName}")
		}
			
	}

    /*
	@Override
	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node,
			Closure childContent) {
	
		Distinct distinct = (Distinct) node
		
		def current = builder.getCurrent();
		
		builder.current = distinct;
		
		def newCurrent = builder.current;
		
		
		def del = childContent.delegate;
		def own = childContent.getOwner();
		
		Object ev = childContent.call()
			
		if(ev instanceof String || ev instanceof GString) {
			
			String var = ev
			if(var.startsWith("?")) {
				distinct.variableName = var.substring(1)
			} else {
				distinct.propertyURI = var;
			}
		} else if(ev instanceof VitalGraphQueryPropertyCriterion) {
			distinct.propertyURI = ((VitalGraphQueryPropertyCriterion)ev).getPropertyURI()
		} else {
			throw new RuntimeException("Distinct must evaluate to String or ${VitalGraphQueryPropertyCriterion.class.canonicalName}, got: ${ev?.class?.canonicalName}")
		}
		
		return true;
		
	}
			
	*/
	
	

}
