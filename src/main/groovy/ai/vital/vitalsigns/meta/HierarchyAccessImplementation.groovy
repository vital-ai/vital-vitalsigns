package ai.vital.vitalsigns.meta;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

import ai.vital.vitalservice.query.VitalGraphQueryTypeCriterion;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.properties.PropertyTrait;

public class HierarchyAccessImplementation {

	protected static void checkInference() {
		
		// log.warn "inference check disabled"
		
		if( ! VitalSigns.get().isInferenceEnabled() ) {
			throw new RuntimeException("No inference enabled in vitalsigns - cannot use this method");
		}
		
	}
	
	
	public static List<Class<? extends PropertyTrait>> handleGetSubProperties(Class<? extends PropertyTrait> cls) {
		return handleHierarchyPropertiesList(cls, true);
	}
	
	public static List<Class<? extends PropertyTrait>> handleGetSuperProperties(Class<? extends PropertyTrait> cls) {
		return handleHierarchyPropertiesList(cls, false);
	}
	
	protected static List<Class<? extends PropertyTrait>> handleHierarchyPropertiesList(Class<? extends PropertyTrait> cls, boolean subNotSuper) {
	
		if(cls == null) throw new NullPointerException("Class argument cannot be null");
		
		checkInference();
		
		OntModel model = VitalSigns.get().getOntologyModel();
		
		String ontTraitURI = VitalSigns.get().getPropertiesRegistry().getPropertyURI(cls);
		
		if(ontTraitURI == null) throw new RuntimeException("No OWL property URI found for " + cls.getCanonicalName());
		
		OntProperty ontProperty = model.getOntProperty(ontTraitURI);
		
		if(ontProperty == null) throw new RuntimeException("No OWL property found with URI: " + ontTraitURI);
		
		Query queryObj = subNotSuper ?
				QueryFactory.create("SELECT DISTINCT ?p WHERE { ?p <" + RDFS.subPropertyOf.getURI() + "> <" + ontTraitURI + "> }")
				:
				QueryFactory.create("SELECT DISTINCT ?p WHERE { <" + ontTraitURI + "> <" + RDFS.subPropertyOf.getURI() + "> ?p }");
		
		QueryExecution execution = QueryExecutionFactory.create(queryObj, model);
		
		ResultSet rs = execution.execSelect();
				
		List<Class<? extends PropertyTrait>> res = new ArrayList<Class<? extends PropertyTrait>>();
		
		while( rs.hasNext() ) {
			
			QuerySolution solution = rs.nextSolution();
			
			Resource propertyR = solution.getResource("p");
			
			if(!propertyR.isURIResource()) continue;
			
			PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(propertyR.getURI());
			
			if(pm != null) {
				
				res.add(pm.getTraitClass());
				
			}
		}
		
		return res;
		
	}
	
	
	public static boolean handleIsSubPropertyOf(Class<? extends PropertyTrait> cls, Class<? extends PropertyTrait> otherClass) {
		return handlePropertyHierarchyCheck(cls, otherClass, true);
	}
	
	public static boolean handleIsSuperPropertyOf(Class<? extends PropertyTrait> cls, Class<? extends PropertyTrait> otherClass) {
		return handlePropertyHierarchyCheck(cls, otherClass, false);
	}
	
	protected static boolean handlePropertyHierarchyCheck(Class<? extends PropertyTrait> thisClass, Class<? extends PropertyTrait> otherClass, boolean subNotSuper) {
		
		if(thisClass == null) throw new NullPointerException("First property trait argument cannot be null");
		
		if(otherClass == null) throw new NullPointerException("Second property trait argument cannot be null");
		
		checkInference();
		
		OntModel model = VitalSigns.get().getOntologyModel();
		
		String thisOntPropertyURI = VitalSigns.get().getPropertiesRegistry().getPropertyURI(thisClass);
			
		if(thisOntPropertyURI == null) throw new RuntimeException("No OWL property URI found for " + thisClass.getCanonicalName());
			
		OntProperty thisOntProperty = model.getOntProperty(thisOntPropertyURI);
		
		if(thisOntProperty == null) throw new RuntimeException("No OWL property found with URI: " + thisOntPropertyURI);
		
		
		String otherOntPropertyURI = VitalSigns.get().getPropertiesRegistry().getPropertyURI(otherClass);
		
		if(otherOntPropertyURI == null) throw new RuntimeException("No OWL property URI found for " + otherClass.getCanonicalName());
		
		OntProperty otherOntProperty = model.getOntProperty(otherOntPropertyURI);
		
		if(otherOntProperty == null) throw new RuntimeException("No OWL property found with URI: " + otherOntPropertyURI);
		
		Query queryObj =
			subNotSuper ?
				QueryFactory.create("ASK { <" + thisOntPropertyURI + "> <" + RDFS.subPropertyOf.getURI() + "> <" + otherOntPropertyURI + "> }")
				:
				QueryFactory.create("ASK { <" + otherOntPropertyURI + "> <" + RDFS.subPropertyOf.getURI() + "> <" + thisOntPropertyURI + "> }");
	
		QueryExecution execution = QueryExecutionFactory.create(queryObj, model);
		
		return execution.execAsk();
		
	}
	

	
	public static List<Class<? extends GraphObject>> handleGetSubClasses(Class<? extends GraphObject> cls) {
		return handleHierarchyList(cls, true);
	}
	
	public static List<Class<? extends GraphObject>> handleGetSuperClasses(Class<? extends GraphObject> cls) {
		return handleHierarchyList(cls, false);

	}
	
	protected static List<Class<? extends GraphObject>> handleHierarchyList(Class<? extends GraphObject> cls, boolean subNotSuper) {
			
		if(cls == null) throw new NullPointerException("Class argument cannot be null");
		
		checkInference();
		
		OntModel model = VitalSigns.get().getOntologyModel();
		
		String ontClassURI = VitalSigns.get().getClassesRegistry().getClassURI(cls);
			
		if(ontClassURI == null) throw new RuntimeException("No OWL class URI found for " + cls.getCanonicalName());
			
		OntClass ontClass = model.getOntClass(ontClassURI);
		
		if(ontClass == null) throw new RuntimeException("No OWL class found with URI: " + ontClassURI);
		
		Query queryObj =
			subNotSuper ?
				QueryFactory.create("SELECT DISTINCT ?c WHERE { ?c <" + RDFS.subClassOf.getURI() + "> <" + ontClassURI +"> }")
				:  
				QueryFactory.create("SELECT DISTINCT ?c WHERE { <" + ontClassURI + "> <" + RDFS.subClassOf.getURI() + "> ?c }");
		
		QueryExecution execution = QueryExecutionFactory.create(queryObj, model);
		
		ResultSet rs = execution.execSelect();
		
		List<Class<? extends GraphObject>> res = new ArrayList<Class<? extends GraphObject>>();
		
		while( rs.hasNext() ) {
			
			QuerySolution solution = rs.nextSolution();
		
			Resource classR = solution.getResource("c");
			
			if(!classR.isURIResource()) continue;
			
			ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(classR.getURI());
			
			if(cm != null) {
				res.add(cm.getClazz());
			}
			
		}
		
		return res;
		
	}
	
	protected static boolean handleHierarchyCheck(Class<? extends GraphObject> thisClass, Class<? extends GraphObject> otherClass, boolean subNotSuper) {
		
		if(thisClass == null) throw new NullPointerException("First class argument cannot be null");
		
		if(otherClass == null) throw new NullPointerException("Second class argument cannot be null");
		
		checkInference();
		
		OntModel model = VitalSigns.get().getOntologyModel();
		
		String thisOntClassURI = VitalSigns.get().getClassesRegistry().getClassURI(thisClass);
			
		if(thisOntClassURI == null) throw new RuntimeException("No OWL class URI found for " + thisClass.getCanonicalName());
			
		OntClass thisOntClass = model.getOntClass(thisOntClassURI);
		
		if(thisOntClass == null) throw new RuntimeException("No OWL class found with URI: " + thisOntClassURI);
		
		ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClassMetadata(otherClass);
		
		if(cm == null) throw new RuntimeException("No OWL class URI found for " + otherClass.getCanonicalName());
		
		String otherOntClassURI = cm.getURI(); 
		
		if(otherOntClassURI == null) throw new RuntimeException("No OWL class URI found for " + otherClass.getCanonicalName());
		
		OntClass otherOntClass = model.getOntClass(otherOntClassURI);
		
		if(otherOntClass == null) throw new RuntimeException("No OWL class found with URI: " + otherOntClassURI);
		
		Query queryObj =
			subNotSuper ?
				QueryFactory.create("ASK { <" + thisOntClassURI + "> <" + RDFS.subClassOf.getURI() + "> <" + otherOntClassURI + "> }")
				:
				QueryFactory.create("ASK { <" + otherOntClassURI + "> <" + RDFS.subClassOf.getURI() + "> <" + thisOntClassURI + "> }");
	
		QueryExecution execution = QueryExecutionFactory.create(queryObj, model);
		
		return execution.execAsk();
		
	}
							
	public static boolean handleIsSubClassOf(Class<? extends GraphObject> thisClass, Class<? extends GraphObject> otherClass) {
		
		return handleHierarchyCheck(thisClass, otherClass, true);
		
	}
	
	public static boolean handleIsSuperClassOf(Class<? extends GraphObject> thisClass, Class<? extends GraphObject> otherClass) {
		
		return handleHierarchyCheck(thisClass, otherClass, false);
		 		
	}
	

	public static VitalGraphQueryTypeCriterion handleExpandSubclasses(Class<? extends GraphObject> thisClass, boolean flag) {
		VitalGraphQueryTypeCriterion t = new VitalGraphQueryTypeCriterion(null, thisClass);
		t.setExpandTypes(flag);
		return t;
	}
}
