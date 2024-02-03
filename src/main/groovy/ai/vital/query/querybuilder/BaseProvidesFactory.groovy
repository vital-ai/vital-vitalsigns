package ai.vital.query.querybuilder

import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

import com.sun.org.apache.bcel.internal.generic.ISUB;

import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.query.BaseProvides;
import ai.vital.query.Provides;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.property.URIProperty;

abstract class BaseProvidesFactory extends AbstractFactory {
	
	@Override
	public boolean isLeaf() {
		return false
	}
	
	@Override
	public boolean isHandlesNodeChildren() {
		return true
	}

	protected abstract String getName();
	
	@Override
	public boolean onNodeChildren(FactoryBuilderSupport builder, Object node, Closure childContent) {
		
		BaseProvides provides = node
		
		Object o = childContent.call();
		
		if(o instanceof Provides) {
			
			Provides ch = (Provides)o;
			
			//copy the properties
			provides.alias = ch.alias
			provides.property = ch.property
            provides.propertyURI = ch.propertyURI 
			
		} else if(o instanceof String || o instanceof GString) {
		
			String s = o.toString().trim()
			
			//evaluate ?
			try {
				String[] cols = s.split("\\s+")

				if(cols.length != 3) {
					throw new Exception("Expected 3 columns")
				}
				
				String alias = cols[0] 
				String eqSign = cols[1]
				String pname = cols[2]
				
				if(!eqSign.equals('=')) throw new Exception("column 2 should be =")
				
				IProperty property = null
                String propertyURI = null
				boolean isURI = false
				if(pname == 'URI') {
					isURI = true
				} else if(pname.contains(':')) {
//					if(property == nu)
					property = VitalSigns.get().getProperty(new URIProperty(pname))
			        //external properties allowed
//					if(property == null) throw new Exception("Property not found: ${pname}")
                    if(property == null) {
                        propertyURI = pname
                    }
				} else {
					property = VitalSigns.get().getProperty(pname)
					if(property == null) throw new Exception("Property not found or ambiguous: ${pname}")
				}
				
				provides.alias = alias
				provides.property = property
                provides.propertyURI = propertyURI
				provides.uri = isURI 
								
			} catch(Exception e) {
				throw new RuntimeException("Error when parsing ${getName()} value: ${e.localizedMessage} - string: ${s} - expected pattern: <alias> = <property_name>")
			}
		
		} else {
			throw new RuntimeException("Unexpected value of ${getName()} node")
		}
		
		
		return false
		
	}

}
