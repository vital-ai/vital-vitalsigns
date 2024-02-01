package ai.vital.query.querybuilder

import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.query.SELECT;
import ai.vital.query.SortBy;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.property.URIProperty;

class SortByFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {
	
		SortBy s = null
		
		if (attributes != null) {
			s = new SortBy(attributes)
		} else {
			s = new SortBy()
		}
		
		if(value == null) throw new RuntimeException(name + ' requires value - property URI or instance')
		
		IProperty prop = null
		
		if(value instanceof String) {
			
			prop = VitalSigns.get().getProperty(value);
			
			if(prop == null) {
				prop = VitalSigns.get().getProperty(new URIProperty(value))
			}
			
			if(prop == null) throw new RuntimeException("Sort property not found (or ambiguous): " + value);
			
		} else if(value instanceof IProperty) {
			prop = VitalSigns.get().getProperty( new URIProperty(((IProperty)value).getURI()) )
			if(prop == null) throw new RuntimeException("Sort property not found: " + value.getURI());
		} else if(value instanceof VitalGraphQueryPropertyCriterion) {
			String propURI = ((VitalGraphQueryPropertyCriterion)value).getPropertyURI();
			prop = VitalSigns.get().getProperty(new URIProperty(propURI))
			if(prop == null) throw new RuntimeException("Sort property not found: " + propURI);
		}
		
		s.propertyURI = prop.getURI()
		
		return s
	}

	@Override
	public boolean isLeaf() {
		return true
	}
	
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		if(parent instanceof SELECT) {
			((SELECT)parent).children.add(child)
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}");
		}
			
	}

}
