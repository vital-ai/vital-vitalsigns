package ai.vital.vitalsigns.properties;

import java.util.ArrayList;
import java.util.List;
import ai.vital.vitalsigns.meta.PropertyDefinitionProcessor.RestrictionValue;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.PropertyFactory;
import ai.vital.vitalsigns.classes.ClassMetadata;

public class PropertyMetadata {

    private String URI;
    
	private IProperty pattern;
	
	private boolean multipleValues = false;

	private List<PropertyMetadata> children = new ArrayList<PropertyMetadata>();

	private List<ClassMetadata> domains;

	private Class<? extends PropertyTrait> traitClass;

	private Class<? extends IProperty> baseClass;
	
	private String shortName = null;

	private boolean internalStore = false;
	
	private boolean dontIndex = false;
	
	private boolean transientProperty = false;
	
	private boolean analyzedNoEquals = false;
	
	private PropertyMetadata parent;
	
	
	
	public List<RestrictionValue> minimumValuesExclusive = null;	
	public List<RestrictionValue> minimumValuesInclusive = null;
		
	public List<RestrictionValue> maximumValuesExclusive = null;
	public List<RestrictionValue> maximumValuesInclusive = null;
	
	
	
	public PropertyMetadata(PropertyMetadata parent, String URI, IProperty pattern, Class<? extends IProperty> basePropertyTrait, Class<? extends PropertyTrait> propertyTrait, List<ClassMetadata> domains, String shortName) {
		super();
		this.URI = URI;
		this.parent = parent;
		this.pattern = pattern;
		this.baseClass = basePropertyTrait;
		this.traitClass = propertyTrait;
		this.domains = domains;
		this.shortName = shortName;
	}

	public IProperty getPattern() {
	    if(pattern == null) {
	        pattern = PropertyFactory.createInstance(baseClass, traitClass);
	    }
		return pattern;
	}

	public void setPattern(IProperty pattern) {
		this.pattern = pattern;
	}

	public boolean isMultipleValues() {
		return multipleValues;
	}

	public void setMultipleValues(boolean multipleValues) {
		this.multipleValues = multipleValues;
	}

	public List<PropertyMetadata> getChildren() {
		return children;
	}

	public void setChildren(List<PropertyMetadata> children) {
		this.children = children;
	}

	public List<ClassMetadata> getDomains() {
		return domains;
	}

	public void setDomains(List<ClassMetadata> domains) {
		this.domains = domains;
	}

	public Class<? extends PropertyTrait> getTraitClass() {
		return traitClass;
	}

	public void setTraitClass(Class<? extends PropertyTrait> traitClass) {
		this.traitClass = traitClass;
	}

	public Class<? extends IProperty> getBaseClass() {
		return baseClass;
	}

	public void setBaseClass(Class<? extends IProperty> baseClass) {
		this.baseClass = baseClass;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public void setInternalStore(boolean internalStore) {
		this.internalStore = internalStore;
	}

	public boolean isInternalStore() {
		return internalStore;
	}

	public boolean isDontIndex() {
		return dontIndex;
	}

	public void setDontIndex(boolean dontIndex) {
		this.dontIndex = dontIndex;
	}

	public boolean isTransientProperty() {
		return transientProperty;
	}

	public void setTransientProperty(boolean transientProperty) {
		this.transientProperty = transientProperty;
	}

	public boolean isAnalyzedNoEquals() {
		return analyzedNoEquals;
	}

	public void setAnalyzedNoEquals(boolean analyzedNoEquals) {
		this.analyzedNoEquals = analyzedNoEquals;
	}

	public PropertyMetadata getParent() {
		return parent;
	}

	public String getURI() {
		return URI;
	}

}
