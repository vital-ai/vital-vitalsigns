package ai.vital.vitalservice.query

import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.properties.PropertyMetadata

import java.io.Serializable

/**
 * URI is a special value indicating the graph object itself 
 *
 */
class VitalGraphValue implements Serializable {

	private static final long serialVersionUID = 2015L;
	
	public final static String URI = "URI"
	
	GraphElement symbol;
	
	String propertyURI;
	
	public VitalGraphValue() {}
    
	public VitalGraphValue(GraphElement symbol, Class<?> cls, String shortPropertyName) {
		
		PropertyMetadata pm =VitalSigns.get().getPropertiesRegistry().getPropertyByShortName(cls, shortPropertyName)
		if(pm == null) throw new RuntimeException("Property '${shortPropertyName}' not found for class: '${cls.getCanonicalName()}'");
		
		this.propertyURI = pm.getPattern().getURI();
		
		this.symbol = symbol;
	
	}
	
	public VitalGraphValue(GraphElement symbol, IProperty property) {
		
		this.symbol = symbol;
		this.propertyURI = property.getURI();
		
	}
	
	public VitalGraphValue(GraphElement symbol, String propertyURI) {
		this.propertyURI = propertyURI;
		this.symbol = symbol;
	}
	
}
