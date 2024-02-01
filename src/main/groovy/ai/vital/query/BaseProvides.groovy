package ai.vital.query

import ai.vital.vitalsigns.model.property.IProperty;

abstract class BaseProvides {

	String alias
	
	IProperty property
	
    String propertyURI
    
	boolean uri = false
}
