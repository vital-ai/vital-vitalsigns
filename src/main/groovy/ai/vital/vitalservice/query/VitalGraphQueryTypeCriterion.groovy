package ai.vital.vitalservice.query

import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion.Comparator

public class VitalGraphQueryTypeCriterion extends VitalGraphQueryPropertyCriterion {

	private static final long serialVersionUID = 2010L;
	
	boolean expandTypes = false
	
	private Class<? extends GraphObject> type;
	
    VitalGraphQueryTypeCriterion() {}
    
	public VitalGraphQueryTypeCriterion(Class<? extends GraphObject> clazz) {
		super(null, getTypesProperty(), clazz != null ? getClazzURI(clazz) : null, Comparator.EQ, false);
		type = clazz
	}
	
	public VitalGraphQueryTypeCriterion(GraphElement symbol, Class<? extends GraphObject> clazz) {
		super(symbol, getTypesProperty(), clazz != null ? getClazzURI(clazz) : null, Comparator.EQ, false);
		type = clazz
	}
	
	private static IProperty getTypesProperty() {
		return VitalSigns.get().getProperty(new URIProperty(VitalCoreOntology.NS + "types"))
	}
	
	private static URIProperty getClazzURI(Class<? extends GraphObject> clazz) {
		String uri = VitalSigns.get().getClassesRegistry().getClassURI(clazz)
		if(!uri) throw new RuntimeException("Class URI not found: ${clazz}")
		return new URIProperty(uri)
	}

	public Class<? extends GraphObject> getType() {
		return type;
	}
	
	public boolean isExpandTypes() {
        return expandTypes;
    }

    public void setExpandTypes(boolean expandTypes) {
        this.expandTypes = expandTypes;
    }

    public void setType(Class<? extends GraphObject> type) {
        this.type = type;
    }

    public VitalGraphQueryTypeCriterion expandTypes(boolean flag) {
		this.expandTypes = flag
		return this
	}
    
    @Override
    public String toString() {
        return "${this.class.simpleName} ${this.comparator} expand types ? ${this.expandTypes} type: ${type} value: ${this.value}"
    }
	
}
