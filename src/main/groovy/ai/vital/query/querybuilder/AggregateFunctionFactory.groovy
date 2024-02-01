package ai.vital.query.querybuilder

import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.query.Distinct;
import ai.vital.query.Query;
import ai.vital.query.SELECT;
import ai.vital.query.AggregateFunction;
import ai.vital.query.AggregateFunction.Average;
import ai.vital.query.AggregateFunction.Count;
import ai.vital.query.AggregateFunction.CountDistinct;
import ai.vital.query.AggregateFunction.Max;
import ai.vital.query.AggregateFunction.Min;
import ai.vital.query.AggregateFunction.Sum;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.property.URIProperty;


class AggregateFunctionFactory extends AbstractFactory {

	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {

		AggregateFunction f = null
	
		if( "AVEREAGE".equals(name) ) {
			f = attributes != null ? new Average(attributes) : new Average()
		} else if("COUNT".equals(name)) {
			f = attributes != null ? new Count(attributes) : new Count()
		} else if("COUNT_DISTINCT".equals(name)) {
			f = attributes != null ? new CountDistinct(attributes) : new CountDistinct()
		} else if("MIN".equals(name)) {
			f = attributes != null ? new Min(attributes) : new Min()
		} else if("MAX".equals(name)) {
			f = attributes != null ? new Max(attributes) : new Max()
		} else if("SUM".equals(name)) {
			f = attributes != null ? new Sum(attributes) : new Sum()
		} else {
			throw new InstantiationException("Unexpected node name: ${name}")
		}
		
		
		if(value == null) throw new RuntimeException(name + ' requires value - property URI or instance')
		
		IProperty prop = null
		
		if(value instanceof String) {
			
			prop = VitalSigns.get().getProperty(value);
			
			if(prop == null) {
				prop = VitalSigns.get().getProperty(new URIProperty(value))
			}
			
			if(prop == null) throw new RuntimeException("Aggregation property not found (or ambiguous): " + value);
			
		} else if(value instanceof IProperty) {
			prop = VitalSigns.get().getProperty( new URIProperty(((IProperty)value).getURI()) )
			if(prop == null) throw new RuntimeException("Aggregation property not found: " + value.getURI());
		} else if(value instanceof VitalGraphQueryPropertyCriterion) {
			String propURI = ((VitalGraphQueryPropertyCriterion)value).getPropertyURI();
			prop = VitalSigns.get().getProperty(new URIProperty(propURI))
			if(prop == null) throw new RuntimeException("Aggregation property not found: " + propURI);
		}
		
		f.propertyURI = prop.getURI()
		
		return f;
	}
	
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		if(parent instanceof Query) {
			
		} else if(parent instanceof SELECT) {
			((SELECT)parent).children.add(child)
		} else {
			throw new RuntimeException("Unexpected parent of ${child.getClass().name}");
		}
			
	}

	@Override
	public boolean isLeaf() {
		return true
	}
			
	/*
			
	@Override
	public boolean isHandlesNodeChildren() {
		return true
	}

	@Override
	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node,
			Closure childContent) {

		AggregateFunction af = (AggregateFunction)node
			
		Object ev = childContent.call(node)
			
		if(ev instanceof String || ev instanceof GString) {
			
			String var = ev
			if(var.startsWith("?")) {
				af.variableName = var.substring(1)
			} else {
				af.propertyURI = var;
			}
			
		} else if(ev instanceof VitalGraphQueryPropertyCriterion) {
		
			af.propertyURI = ((VitalGraphQueryPropertyCriterion)ev).getPropertyURI()
			
//		} else if(ev instanceof Distinct) {
//		
//			if(!( af instanceof Count)) {
//				throw new RuntimeException("Only COUNT aggregation function may accept DISTINCT element");
//			}
//			
//			((Count)af).distinct = (Distinct)ev
			
		} else {
			throw new RuntimeException("Aggregate function must evaluate to String or ${VitalGraphQueryPropertyCriterion.class.canonicalName}, got: ${ev?.class?.canonicalName}, COUNT may also accept DISTINCT")
		}
			
		return false
		
	}
			
			*/
	
}
