package ai.vital.vitalservice.query

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.vitalsigns.model.property.PropertyFactory;
import ai.vital.vitalsigns.model.property.StringProperty
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.properties.PropertyTrait;


class VitalSortProperty implements Serializable {

	private static final long serialVersionUID = -1313355462244296283L;

	public static final String RELEVANCE = "RELEVANCE";
	
	public static final String INDEXORDER = "INDEXORDER";
	
	private String propertyURI;
	
	private String providedName;
		
	private boolean reverse;
	
	private boolean expandProperty = false
	
	public VitalSortProperty() {
        
	}
    
    /**
	 * property used for sorting	
	 * @param property
	 * @param providedName required
	 * @param reverse
	 */
    @Deprecated
	public VitalSortProperty(IProperty property, String providedName, boolean reverse) {
			
		this.propertyURI = property.getURI();
			
		this.providedName = providedName;
			
		this.reverse = reverse
			
	}
    
	@Deprecated
	public VitalSortProperty(IProperty property, String providedName) {
		this(property, providedName, false)
	}
    
	public VitalSortProperty(IProperty property, boolean reverse) {
        
        this.propertyURI = property.getURI();
        this.reverse = reverse
        
	}
	
    @Deprecated
	public VitalSortProperty(String propertyURI, String providedName,
			boolean reverse) {
		super();
		this.propertyURI = propertyURI;
		this.providedName = providedName;
		this.reverse = reverse;
	}
            
    public VitalSortProperty(String providedName, boolean reverse) {
        this.providedName = providedName;
        this.reverse = reverse;
    }    

	public String getPropertyURI() {
		return propertyURI;
	}
	
	public boolean isReverse() {
		return reverse;
	}
	
	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

	public boolean isExpandProperty() {
		return expandProperty;
	}

	public void setExpandProperty(boolean expandProperty) {
		this.expandProperty = expandProperty;
	}
	
    @Deprecated
    //this method is just for backward compatibility
    public static VitalSortProperty get(Class<? extends PropertyTrait> traitClass, String providedName, boolean reverse) {
        
        IProperty property = VitalSigns.get().getPropertyByTrait(traitClass)
        
        if(property == null) throw new RuntimeException("Property not found for trait class: " + traitClass.getCanonicalName());
        
        return new VitalSortProperty(property, providedName, reverse);
        
    }
    
    public static VitalSortProperty get(Class<? extends PropertyTrait> traitClass, boolean reverse) {
        
        IProperty property = VitalSigns.get().getPropertyByTrait(traitClass)
        
        if(property == null) throw new RuntimeException("Property not found for trait class: " + traitClass.getCanonicalName());
        
        return new VitalSortProperty(property, reverse);
    }
    
    public static VitalSortProperty get(String providedName, boolean reverse) {
        
        return new VitalSortProperty(providedName, reverse);
    }
    
    public static VitalSortProperty get(String providedName) {
        return get(providedName, false)
    }
    
    public static VitalSortProperty get(Class<? extends PropertyTrait> traitClass) {
        
        return get(traitClass, false)
    }
    
    public static VitalSortProperty getByPropertyName(String propertyNameOrURI, boolean reverse) {
        
        IProperty prop = propertyNameOrURI.contains(":") ? VitalSigns.get().getProperty(URIProperty.withString(propertyNameOrURI)) : VitalSigns.get().getProperty(propertyNameOrURI)
        if(prop == null) throw new RuntimeException("Sort Property with name: ${propertyNameOrURI} not found or ambiguous")
        
        return new VitalSortProperty(prop, reverse)
        
    }
    
    public static VitalSortProperty withPropertyURI(String propertyURI, boolean reverse) {
        VitalSortProperty vsp = new VitalSortProperty()
        vsp.propertyURI = propertyURI
        vsp.reverse = reverse
        return vsp
    }
    
    public static VitalSortProperty withProvidedName(String providedName, boolean reverse) {
        VitalSortProperty vsp = new VitalSortProperty()
        vsp.providedName = providedName
        vsp.reverse = reverse
        return vsp
    } 

    public String getProvidedName() {
        return providedName;
    }

    public void setProvidedName(String providedName) {
        this.providedName = providedName;
    }

    public void setPropertyURI(String propertyURI) {
        this.propertyURI = propertyURI;
    }


    @Override
    public String toString() {
        return "SORT propURI: ${propertyURI} expand? ${expandProperty} reverse: ${reverse} name: ${providedName}"
    }    	
}
