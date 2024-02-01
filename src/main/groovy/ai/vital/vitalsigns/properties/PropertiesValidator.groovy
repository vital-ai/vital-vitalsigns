package ai.vital.vitalsigns.properties;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.GraphObject;

public class PropertiesValidator {

	/**
	 * validates the graph object properties list after types list update
	 */
	public static void validateProperties(List<Class<? extends GraphObject>> types, Map<String, IProperty> properties, boolean deleteInvalidProperties) {
		
		
		Set<String> validProperties = new HashSet<String>();
		
		for(Class<? extends GraphObject> t : types) {
			
			List<PropertyMetadata> props = VitalSigns.get().getPropertiesRegistry().getClassProperties(t);
			
			for(PropertyMetadata p : props) {
				validProperties.add(p.getURI());
			}
			
			
		}
		
		for(Iterator<Entry<String, IProperty>> i = properties.entrySet().iterator(); i.hasNext(); ) {
			
			Entry<String, IProperty> e = i.next();
			
			//check if it's an external property ?
			if( VitalSigns.get().getPropertiesRegistry().getProperty(e.getKey()) == null ) continue;
			
			if(!validProperties.contains(e.getKey())) {
				if(deleteInvalidProperties) {
					i.remove();
				} else {
					throw new RuntimeException("Property no longer valid: " + e.getKey());
				}
			}
			
			
		}
		
		
	}
}
