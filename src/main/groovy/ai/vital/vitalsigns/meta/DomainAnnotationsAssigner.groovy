package ai.vital.vitalsigns.meta

import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.property.IProperty
import ai.vital.vitalsigns.model.property.URIProperty
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.properties.PropertyMetadata
import ai.vital.vitalsigns.properties.PropertyTrait;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Property
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import java.lang.reflect.Method

class DomainAnnotationsAssigner {

	public static domainAnnotationsInterceptor() {

		GraphObject.metaClass.static.getClassAnnotations = { boolean subclasses = true ->
			// println "delegate: ${delegate} subclasses ? ${subclasses}"
			// return getClassAnnotations(delegate, subclasses)
			return AnnotationsImplementation.getClassAnnotations(delegate, subclasses)
		}

        PropertyTrait.metaClass.static.getPropertyAnnotations = { subclasses = true ->
            return AnnotationsImplementation.getPropertyAnnotations(delegate, subclasses)
        } 
        
	}

	
	public static List<DomainPropertyAnnotation> getPropertyAnnotations(Class<? extends GraphObject> cls, String propertyName) {
		
		PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getPropertyByShortName(cls, propertyName)
		
		if(pm == null) throw new RuntimeException("Class: ${cls} property: ${propertyName} not found.")
		
		return getPropertyAnnotations(pm)
		
	}
	
	public static List<DomainPropertyAnnotation> getPropertyAnnotations(PropertyMetadata pm) {
		
		OntModel model = VitalSigns.get().getOntologyModel()
		
		OntProperty ontProp = model.getOntProperty(pm.getURI())
		
		List<DomainPropertyAnnotation> annotations = []
		
		for( StmtIterator iter = ontProp.listProperties(); iter.hasNext(); ) {
				
			Statement stmt = iter.nextStatement()
						
			Property predicate = stmt.getPredicate()
						
			AnnotationProperty ap = model.getAnnotationProperty(predicate.getURI())
						
			if(ap == null) continue
						
			RDFNode node = stmt.getObject()
					
			Object val = null
				
			if(node.isURIResource()) {
				val = new URIProperty(node.asResource().getURI());
			} else if(node.isLiteral()) {
				val = node.asLiteral().getValue();
			}
				
			annotations.add(new DomainPropertyAnnotation(URI: predicate.getURI(), value: val, propertyInterface: pm))
				
		}
		
		return annotations
	}
	
	public static annotateDomain(String _package, String namespace) {
		
		// iterate over all data properties and inject statc propertyXXX accessors
		// VitalSigns.get().getModel().listOntProperties()
		
	}
}
