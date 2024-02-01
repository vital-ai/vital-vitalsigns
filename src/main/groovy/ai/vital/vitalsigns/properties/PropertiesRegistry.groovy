package ai.vital.vitalsigns.properties;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.PropertyFactory;
import ai.vital.vitalsigns.rdf.RDFUtils;

public class PropertiesRegistry {

	//URI -> property pattern
	private Map<String, PropertyMetadata> propertiesMap = new HashMap<String, PropertyMetadata>();

	private Map<String, List<PropertyMetadata>> shortnameMap = new HashMap<String, List<PropertyMetadata>>();
	
	private Map<String, Map<String, PropertyMetadata>> classCName2Properties = new HashMap<String, Map<String, PropertyMetadata>>();
	
	//special case for annotation properties that are added for new classes
	private Map<Class<? extends GraphObject>, Map<String, String>> classCName2AnnotationPropertyShortName2URI = new HashMap<Class<? extends GraphObject>, Map<String, String>>();

//	public static PropertyMetadata typesProperty = null;
//	
//	static {
//		
//		IProperty pattern = PropertyFactory.createInstance(URIProperty.class, TypesProperty.class);
//		
//		typesProperty = new PropertyMetadata(pattern, URIProperty.class, TypesProperty.class, new ArrayList<ClassMetadata>());
//		typesProperty.setMultipleValues(true);
//	}
	
	public PropertyMetadata getProperty(String uri) {
		return propertiesMap.get(uri);
	}
	
	public PropertyMetadata getProperty(GraphObject g, String propertyURI) throws Exception {
		
		//gets the property but also validates if the domain range 
		PropertyMetadata pm = propertiesMap.get(propertyURI);
		if(pm == null) throw new Exception("Property not found: " + propertyURI);
		
		
		boolean rangeOK = checkPropertyDomain(pm, g.getClass());
		
		if(rangeOK) return pm;
		
		String classes = "[";
		for(ClassMetadata cm : pm.getDomains()) {
			if(classes.length() > 1) classes += ", ";
			classes += cm.getClass().getCanonicalName();
		}
		classes += "]";
		
		throw new Exception("Property " + propertyURI + " found, but class " + g.getClass().getCanonicalName() + " does not match domain: " + classes);
		
	}

	private boolean checkPropertyDomain(PropertyMetadata pm, Class<? extends GraphObject> targetClass) {

		for(ClassMetadata cm : pm.getDomains()) {
			
			Class<? extends GraphObject> c = cm.getClazz();
			
			while( c != null) {
				
				if(c.isAssignableFrom(targetClass)) {
					return true;
				}
				
				
				Class parentCls = c.getSuperclass();
				
				if(GraphObject.class.isAssignableFrom(parentCls)) {
					
					c = parentCls;
					
				} else {
					
					c = null;
							
				}
				
			}
			
		}
		
		return false;

		
	}

	public void addClass(ClassMetadata clazz) throws Exception {
		
		Map<String, PropertyMetadata> map = classCName2Properties.get(clazz.getCanonicalName());
		
		if(map != null) throw new Exception("Class already added to properties registry: " + clazz.getURI());
		
		
		ClassMetadata parentClass = clazz.getParentClass();
		
		if(parentClass != null) {

			Map<String, PropertyMetadata> m = classCName2Properties.get(parentClass.getCanonicalName());
			
			if(m == null) throw new RuntimeException("Parent class not found in properties registry: " + parentClass.getURI());
			
			//it is important to copy all properties from parent
			map = new HashMap<String, PropertyMetadata>(m);
			
		} else {
			
			//if not parent class initlize it with static type property
			map = new HashMap<String, PropertyMetadata>();
			
		}
		
		classCName2Properties.put(clazz.getCanonicalName(), map);
		
		
	}
	
	public void addAnnotationProperty(List<ClassMetadata> domains, String annotationPropertyURI, String shortname) {
	    if(domains == null || domains.isEmpty()) throw new NullPointerException("Domains list cannot be null or empty");
	    for(ClassMetadata cm : domains) {
	        Map<String, String> map = classCName2AnnotationPropertyShortName2URI.get(cm.getClazz());
	        if(map == null) {
	            map = new HashMap<String, String>();
	            classCName2AnnotationPropertyShortName2URI.put(cm.getClazz(), map);
	        }
	        map.put(shortname, annotationPropertyURI);
	    }
	}
	
	public void removeAnnotationProperties(List<ClassMetadata> domains) {
       for(ClassMetadata cm : domains) {
           classCName2AnnotationPropertyShortName2URI.remove(cm.getClazz());
       }
	}
	
	/**
	 * properties may be added more than once (refreshing them)
	 * @param parentProperty
	 * @param propertyMeta
	 * @throws Exception
	 */
	public void addProperty(PropertyMetadata parentProperty,
			PropertyMetadata propertyMeta) throws Exception {

	    
		String pURI = propertyMeta.getURI();
//		try {
//			Class<? extends IProperty> class1 = propertyMeta.getPattern().getClass();
//			Method method = class1.getMethod("getURI");
//			pURI = (String) method.invoke(propertyMeta.getPattern());
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
		
		String shortName = RDFUtils.getPropertyShortName(pURI);
		List<PropertyMetadata> shlist = shortnameMap.get(shortName);
		if(shlist == null) {
		    shlist = new ArrayList<PropertyMetadata>();
		    shortnameMap.put(shortName, shlist);
		}
		
		PropertyMetadata previous = propertiesMap.remove(pURI);
		if(previous != null) {
		    
		    if(previous.getParent() != null) {
		        previous.getParent().getChildren().remove(previous);
		    }
		    
		    shlist.remove(previous);
		    
		    //remove it from cls,shname,prop structure
		    
		    for( Iterator<Entry<String, Map<String, PropertyMetadata>>> iter1 = classCName2Properties.entrySet().iterator(); iter1.hasNext(); ) {
		        
		        Entry<String, Map<String, PropertyMetadata>> e1 = iter1.next();
		        
		        Map<String, PropertyMetadata> m = e1.getValue();
                for( Iterator<Entry<String, PropertyMetadata>> iter2 = m.entrySet().iterator(); iter2.hasNext();) {
                    
                    if( iter2.next().getValue() == previous ) {
                        
                        iter2.remove();
                        
                    }
		            
		        }
		        
		    }
		    
//		    List<PropertyMetadata> parentProperties = getParentProperties(propertyMeta, false);
//		    for(PropertyMetadata pm : parentProperties) {
//		    }
//		    List<ClassMetadata> allDomains = new ArrayList<ClassMetadata>(propertyMeta.getDomains());
		}
		
		
		propertiesMap.put(pURI, propertyMeta);
		
		
		
		shlist.add(propertyMeta);
		
		//collect all domains, starting at explicit domains, property domains bubble up - subproperty extends to superproperty domains
		List<ClassMetadata> allDomains = new ArrayList<ClassMetadata>(propertyMeta.getDomains());
		
		
		if(parentProperty != null) {
			
			parentProperty.getChildren().add(propertyMeta);
			
		}
		
		List<PropertyMetadata> parentProperties = getParentProperties(propertyMeta, false);
		
		for(PropertyMetadata pm : parentProperties) {
			
			for(ClassMetadata d : pm.getDomains()) {
				if( !allDomains.contains(d) ) {
					allDomains.add(d);
				}
			}
			
		}
		
		
		//now insert it to <class,shortname,propertyMeta> structure
		for( ClassMetadata domain : allDomains ) {
			
			List<ClassMetadata> subclasses = VitalSigns.get().getClassesRegistry().getSubclasses(domain, true);
			
			for(ClassMetadata sc : subclasses) {
				
				Map<String, PropertyMetadata> map = classCName2Properties.get(sc.getCanonicalName());
				
				if(map == null) throw new RuntimeException("Class not added to properties registry: " + sc.getURI());
				
				PropertyMetadata propertyMetadata = map.get(shortName);
				
				if(propertyMetadata != null) {
					
					if( ! propertyMetadata.getURI().equals(pURI) ) {
						throw new Exception("Property conflict detected, class " + sc.getURI() + " has two different properties with same short name " + shortName + ": " + pURI + " and " + propertyMetadata.getURI() );
					}
					
				} else {
					
					map.put(shortName, propertyMeta);
					
				}
				
				
			}
			
		}
		
		
	}
	
	public List<PropertyMetadata> getParentProperties(
			PropertyMetadata propertyMeta, boolean includeThis) {

		List<PropertyMetadata> l = new ArrayList<PropertyMetadata>();
		
		if(includeThis) l.add(propertyMeta);
		
		PropertyMetadata currentParent = propertyMeta;
		
		while(currentParent != null) {
			
			currentParent = currentParent.getParent();
			
			if(currentParent != null) l.add(currentParent);
			
		}
		
		return l;
		
	}

//	public List<Class<? extends GraphObject>> getSubClasses

	public PropertyMetadata getPropertyByShortName(
			Class<? extends GraphObject> cls, String pname) throws NoSuchFieldException {

//		//common for all classes
//		if(VitalCoreOntology.types.getLocalName().equals( pname ) ) {
//			return typesProperty;
//		}
		
		Map<String, PropertyMetadata> map = classCName2Properties.get(cls.getCanonicalName());
		
		if(map == null) throw new RuntimeException("Class not added to properties registry: " + cls.getCanonicalName()); 
		
		PropertyMetadata match = map.get(pname);
		
		if(match == null) throw new NoSuchFieldException("No property with name " + pname + " found for class: " + cls.getCanonicalName() );
		
		return match;
		
	}
	
	/**
	 * Returns annotation property URI or <code>null</code>
	 * @param cls
	 * @param pname
	 * @return annotation property URI or <code>null</code>
	 */
	public String getAnnotationPropertyURIByShortName(Class<? extends GraphObject> cls, String pname) {
	    for(Entry<Class<? extends GraphObject>, Map<String, String>> e : classCName2AnnotationPropertyShortName2URI.entrySet()) {
	        if(e.getKey().isAssignableFrom(cls)) {
	            Map<String, String> map = e.getValue();
	            if(map != null) {
	                String uri = map.get(pname);
	                if(uri != null) return uri;
	            }
	        }
	    }
	    return null;
	}

	public List<PropertyMetadata> getClassProperties(Class<? extends GraphObject> t) {

		Map<String, PropertyMetadata> m = classCName2Properties.get(t.getCanonicalName());
		
		if(m == null) throw new RuntimeException("No properties of class found: " + t.getCanonicalName());
		
		return new ArrayList<PropertyMetadata>(m.values());
		
	}

	public void deregisterDomain(String ontologyURI, String _package) {

		for( Iterator<Entry<String, PropertyMetadata>> iter = propertiesMap.entrySet().iterator(); iter.hasNext(); ) {
			
			Entry<String, PropertyMetadata> e = iter.next();
			
			String ontURI = RDFUtils.getOntologyPart(e.getKey());
			
			PropertyMetadata pm = e.getValue();
			
			if(!ontURI.equals(ontologyURI)) {
			    
			    //just remove parent property domains
			    
			    for(Iterator<ClassMetadata> iter2 = pm.getDomains().iterator(); iter2.hasNext(); ) {

			        ClassMetadata domain = iter2.next();
			        
			        if( RDFUtils.getOntologyPart(domain.getURI()).equals(ontologyURI) ) {
			            
			            iter2.remove();
			            
			        }
			        
			    }
			    
			    continue;
			    
			}
			
			
			PropertyFactory.clearProperty(pm.getTraitClass());
			
			String shortName = pm.getShortName();

			List<PropertyMetadata> list = shortnameMap.get(shortName);
			if(list != null) {
				list.remove(pm);
				if(list.size() == 0) shortnameMap.remove(shortName);
			}
			
			
			//also remove it from inherited domains
			List<ClassMetadata> allDomains = new ArrayList<ClassMetadata>(pm.getDomains());
			
			for(PropertyMetadata p : getParentProperties(pm, false)) {
				for(ClassMetadata c : p.getDomains()) {
					if( ! allDomains.contains(c) ) allDomains.add(c);
				}
			}
			
			//remove it 
			for( ClassMetadata domain : allDomains) {
				
				List<ClassMetadata> subclasses = VitalSigns.get().getClassesRegistry().getSubclasses(domain, true);
				
				for(ClassMetadata sc : subclasses) {
					
					Map<String, PropertyMetadata> map = classCName2Properties.get(sc.getCanonicalName());
					
					if(map == null) continue;
					
					map.remove(shortName);
					
				}
				
			}
			
			
			iter.remove();
			
		}
		
		for(Iterator<Entry<String, Map<String, PropertyMetadata>>> iter = classCName2Properties.entrySet().iterator(); iter.hasNext(); ) {
			
			Entry<String, Map<String, PropertyMetadata>> next = iter.next();
			
			String cname = next.getKey();
			
			String _thisPkg = cname.substring(0, cname.lastIndexOf('.'));
			
			if(!_package.equals( _thisPkg ) ) continue;
			
			iter.remove();
			
		}
		
		for(Iterator<Entry<Class<? extends GraphObject>, Map<String, String>>> iter = classCName2AnnotationPropertyShortName2URI.entrySet().iterator(); iter.hasNext(); ) {
		    
		    Entry<Class<? extends GraphObject>, Map<String, String>> next = iter.next();
		    String cname = next.getKey().getCanonicalName();
		    
		    String _thisPkg = cname.substring(0, cname.lastIndexOf('.'));
		    
		    if(!_package.equals(_thisPkg)) continue;
		    
		    iter.remove();
		    
		}
		
		
	}

	public String getPropertyURI(Class<? extends PropertyTrait> cls) {


		for(Entry<String, PropertyMetadata> entry : propertiesMap.entrySet()) {
			
			PropertyMetadata value = entry.getValue();
			
			if( cls.equals( value.getTraitClass() ) ) {
				return entry.getKey();
			}
			
		}
		
		return null;
	}

	public List<PropertyMetadata> listPropertiesWithShortName(String name) {

		List<PropertyMetadata> list = shortnameMap.get(name);
		if(list == null) return Collections.emptyList();
		return new ArrayList<PropertyMetadata>(list);
		
	}

	public PropertyMetadata getPropertyByTrait(
			Class<? extends PropertyTrait> traitClass) {

		for( PropertyMetadata p : propertiesMap.values() ) {
			if(p.getTraitClass().equals(traitClass)) {
				return p;
			}
		}
		
		return null;
	}
	
	
	public List<PropertyMetadata> getSubProperties(PropertyMetadata domain, boolean includeThis) {

		List<PropertyMetadata> r = new ArrayList<PropertyMetadata>();
		
		if(includeThis) r.add(domain);
		
		processSubproperties(r, domain);
		
		return r;
	}

	private void processSubproperties(List<PropertyMetadata> r,
			PropertyMetadata domain) {

		List<PropertyMetadata> children = domain.getChildren();
		
		if(children == null) return;
		
		for(PropertyMetadata child : children) {
			
			r.add(child);
			
			processSubproperties(r, child);
			
		}
		
	}

    public List<PropertyMetadata> listAllProperties() {
        return new ArrayList<PropertyMetadata>(propertiesMap.values());
    }
	
}
