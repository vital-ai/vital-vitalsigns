package ai.vital.vitalsigns.model;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.properties.PropertyMetadata;

/**
 * Base class to access class properties.
 * There should be a single helper class for each generated graph object class.
 * GraphObject.props() 'dynamic' static method returns an instance of this helper.
 * 
 * Subclasses contain dynamically generated helper class
 *
 */
public abstract class ClassPropertiesHelper extends GroovyObjectSupport {

	public ClassPropertiesHelper(String classURI){
		ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(classURI);
		if(cm == null) throw new RuntimeException("Class for URI not found:" + classURI);
		this.clazz = cm.getClazz();
	}
	
	public ClassPropertiesHelper(Class<? extends GraphObject> clazz) {
		super();
		this.clazz = clazz;
	}

	protected Class<? extends GraphObject> clazz;
	
	protected VitalGraphQueryPropertyCriterion _implementation(String pname) {
		
		PropertyMetadata pm = null;
		try {
			pm = VitalSigns.get().getPropertiesRegistry().getPropertyByShortName(clazz, pname);
		} catch (NoSuchFieldException e) {
		}
		
		if(pm == null) throw new RuntimeException("Property not found for class: " + clazz.getCanonicalName() + " name: " + pname);
		
		VitalGraphQueryPropertyCriterion c = new VitalGraphQueryPropertyCriterion(null, pm.getPattern(), null);
		
		return c;
		
	}

	/**
	 * Special property criterion for graph object's URI
	 * @return
	 */
	public VitalGraphQueryPropertyCriterion getURI() {
		VitalGraphQueryPropertyCriterion c  = new VitalGraphQueryPropertyCriterion(VitalGraphQueryPropertyCriterion.URI);
		return c;
	}
	
	@Override
	public Object getProperty(String property) {
	    try {
	        return super.getProperty(property);
	    } catch(MissingPropertyException e) {
	        return _implementation(property);
	    }
	}
	
	@Override
	public Object invokeMethod(String name, Object args) {

		try {
			
			return super.invokeMethod(name, args);
			
		} catch(MissingMethodException e ) {

			if(name.startsWith("get") && name.length() > 3) {
				
				//dynamic access
				String shortName = name.substring(3, 4).toLowerCase() + name.substring(4, name.length());
				
				return _implementation(shortName);
				
			}
			
			throw e;
			
		}
		
	}
	
}
