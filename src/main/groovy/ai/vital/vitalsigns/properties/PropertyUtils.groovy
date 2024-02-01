package ai.vital.vitalsigns.properties;

import ai.vital.vitalsigns.rdf.RDFUtils;

public class PropertyUtils {

	private static final String PROPERTY_PREFIX = "Property_";

	public static String getShortName(Class<?> clazz) {
		
		String name = clazz.getSimpleName();
		
		if(name.startsWith(PROPERTY_PREFIX)) name = name.substring(PROPERTY_PREFIX.length());
		
		return RDFUtils.getPropertyShortName(name);
		
	}
	
}
