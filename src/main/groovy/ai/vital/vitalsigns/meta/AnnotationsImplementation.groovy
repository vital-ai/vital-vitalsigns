package ai.vital.vitalsigns.meta

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.properties.PropertyTrait;
import ai.vital.vitalsigns.rdf.RDFSerialization;
import ai.vital.vitalsigns.rdf.RDFUtils;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

public class AnnotationsImplementation {

	public static List<DomainPropertyAnnotation> listAnnotatedProperties(Property annotation, IProperty filter) {
	
		RDFNode n = null;
		
		if(filter != null) {
			n = RDFSerialization.toRDFNode(filter);
		}
		
		List<DomainPropertyAnnotation> propertyWithDomains = new ArrayList<DomainPropertyAnnotation>();
	
		OntModel model = VitalSigns.get().getOntologyModel();
		
		for(StmtIterator iter = model.listStatements((null), annotation, n); iter.hasNext(); ) {
		
			Statement stmt = iter.next();
		
			String pURI = stmt.getSubject().getURI();
			
			PropertyMetadata property = VitalSigns.get().getPropertiesRegistry().getProperty(pURI);
			
			if(property == null) continue;
			
			DomainPropertyAnnotation dpa = new DomainPropertyAnnotation();
			dpa.propertyInterface = property;
			dpa.URI = pURI;
			RDFNode rdfNode = stmt.getObject();
			if(rdfNode.isURIResource()) {
				dpa.value = new URIProperty(rdfNode.asResource().getURI());
			} else if(rdfNode.isLiteral()) {
			    Literal l = rdfNode.asLiteral();
				dpa.value = l.getValue();
				dpa.lang = l.getLanguage();
			}
			propertyWithDomains.add(dpa);
			
		}
		
		return propertyWithDomains;
		
	}
	
	
	@SuppressWarnings("unchecked")
	public static List<DomainClassAnnotation> getClassAnnotations(Object i, boolean subclasses) {

		Class<? extends GraphObject> graphObjectCls = null;
		

		if(i instanceof Class) {
			if( ! GraphObject.class.isAssignableFrom((Class<?>) i)) throw new RuntimeException("Only subclasses of GraphObject are supported");
			graphObjectCls = (Class<? extends GraphObject>) i;
		} else if(i instanceof GraphObject) {
			graphObjectCls = ((GraphObject)i).getClass();
		} else {
			throw new RuntimeException("Only graphobject class or instances have class annnotations accessor");
		}
		
		if(graphObjectCls.equals(GraphObject.class)) throw new RuntimeException("Cannot get annotations of GraphObject class - it's abstract");

		String clsURI = VitalSigns.get().getClassesRegistry().getClassURI(graphObjectCls);
		
		if(clsURI == null) throw new RuntimeException("No class URI found - " + graphObjectCls.getCanonicalName());
		
		OntModel model = VitalSigns.get().getOntologyModel();
		OntClass ontClass = model.getOntClass(clsURI);

		List<DomainClassAnnotation> annotations = new ArrayList<DomainClassAnnotation>();
		
		List<OntClass> allClasses = new ArrayList<OntClass>(Arrays.asList(ontClass));
		
		if(subclasses) {
			RDFUtils.collectAllSubclasses(ontClass, allClasses);
		}
		
		for(OntClass c : allClasses) {
			
			ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(c.getURI());
			if( cm == null) continue;
			
			for( StmtIterator iter = c.listProperties(); iter.hasNext(); ) {
				
				Statement stmt = iter.nextStatement();
						
				Property predicate = stmt.getPredicate();
						
				AnnotationProperty ap = model.getAnnotationProperty(predicate.getURI());
						
				if(ap == null) continue;
						
				RDFNode node = stmt.getObject();
						
				Object val = null;
						
				if(node.isURIResource()) {
					val = new URIProperty(node.asResource().getURI());
				} else if(node.isLiteral()) {
					val = node.asLiteral().getValue();
				}
				
				DomainClassAnnotation a = new DomainClassAnnotation();
				a.URI = predicate.getURI();
				a.value = val;
				a.clazz = cm.getClazz();
				
				annotations.add(a);
				
			}
		}
		
		return annotations;

	}

	static Map<String, String> propURI2commonName = new HashMap<String, String>();
	
	static {
	    propURI2commonName.put(RDFS.label.getURI(), RDFS.label.getLocalName());
	    propURI2commonName.put(RDFS.comment.getURI(), RDFS.comment.getLocalName());
	    propURI2commonName.put(RDFS.isDefinedBy.getURI(), RDFS.isDefinedBy.getLocalName());
	}
	

    public static Map<String, List<DomainPropertyAnnotation>> getPropertyAnnotations(Class<? extends PropertyTrait> vitalProperty, boolean subclasses) {

        Map<String, List<DomainPropertyAnnotation>> m = new HashMap<String, List<DomainPropertyAnnotation>>();
    
        OntModel model = VitalSigns.get().getOntologyModel();
        
        PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getPropertyByTrait(vitalProperty);
        
        if(pm == null) throw new RuntimeException("Property not found in vitalsigns: " + vitalProperty.getCanonicalName());
        
        OntProperty rootOntProperty = model.getOntProperty(pm.getURI());
        if(rootOntProperty == null) throw new RuntimeException("OntProperty not found: " + pm.getURI());
        
        List<OntProperty> allProperties = new ArrayList<OntProperty>(Arrays.asList(rootOntProperty));
        
        if(subclasses) {
            RDFUtils.collectAllSubproperties(rootOntProperty, allProperties);
        }
        
//        model.getResource(vitalProperty)
        
        for(OntProperty ontProperty : allProperties) {
            
            for(StmtIterator iter = model.listStatements(ontProperty, null, (RDFNode) null); iter.hasNext(); ) {
            
                Statement stmt = iter.next();
    
                DomainPropertyAnnotation dpa = new DomainPropertyAnnotation();
                dpa.propertyInterface = pm;
                String pURI = stmt.getPredicate().getURI();
                dpa.URI = pURI;
                RDFNode rdfNode = stmt.getObject();
                if(rdfNode.isURIResource()) {
                    dpa.value = new URIProperty(rdfNode.asResource().getURI());
                } else if(rdfNode.isLiteral()) {
                    Literal l = rdfNode.asLiteral();
                    dpa.value = l.getValue();
                    dpa.lang = l.getLanguage();
                }
                
                List<DomainPropertyAnnotation> v = m.get(pURI);
                if(v == null) {
                    v = new ArrayList<DomainPropertyAnnotation>();
                    m.put(pURI, v);
                }
                v.add(dpa);
                
            }
            
        }   
        
        for(Entry<String, String> e : propURI2commonName.entrySet()) {
            
            List<DomainPropertyAnnotation> list = m.get(e.getKey());
            if(list != null) {
                m.put(e.getValue(), list);
            }
            
        }
        
        return m;
        
    }
}
