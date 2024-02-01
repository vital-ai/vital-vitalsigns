package ai.vital.vitalsigns.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.properties.PropertyTrait

public class HierarchyAccessAssigner {

	private final static Logger log = LoggerFactory.getLogger(HierarchyAccessAssigner.class)

	public static void heirarchyAccessInterceptor() {
		
		GraphObject.metaClass.static.'getSubClasses' = { ->
			
			def del = delegate
			
			//static accessors
			return HierarchyAccessImplementation.handleGetSubClasses(del)
			
		}
		
		GraphObject.metaClass.static.'getSuperClasses' = { ->
			
			def del = delegate
			return HierarchyAccessImplementation.handleGetSuperClasses(del)
		
		}
		
		GraphObject.metaClass.static.'isSubClassOf' = { Class<? extends GraphObject> other ->
			
			def del = delegate
			
			return HierarchyAccessImplementation.handleIsSubClassOf(del, other)
			
		}
		
		GraphObject.metaClass.static.'isSuperClassOf' = { Class<? extends GraphObject> other ->
			
			def del = delegate
			
			return HierarchyAccessImplementation.handleIsSuperClassOf(del, other)
			
		}
		
		
		GraphObject.metaClass.static.'expandSubclasses' = { Boolean flag ->
			
			def del = delegate
			
			return HierarchyAccessImplementation.handleExpandSubclasses(del, flag)
			
		}
		
		PropertyTrait.metaClass.static.'getSubProperties' = { ->
			
			def del = delegate
			
			return HierarchyAccessImplementation.handleGetSubProperties(del)
			
		}
		
		PropertyTrait.metaClass.static.'getSuperProperties' = { ->
			
			def del = delegate
			
			return HierarchyAccessImplementation.handleGetSuperProperties(del)
			
		}
		
		PropertyTrait.metaClass.static.'isSubPropertyOf' = { Class<? extends PropertyTrait> other ->
			
			def del = delegate 
			
			return HierarchyAccessImplementation.handleIsSubPropertyOf(del, other)
			
		}
		
		PropertyTrait.metaClass.static.'isSuperPropertyOf' = { Class<? extends PropertyTrait> other ->
		
			def del = delegate
		
			return HierarchyAccessImplementation.handleIsSuperPropertyOf(del, other)
				
		}
		
		
		
		
	}
	
	/*
		
	public static void annotateDomain(String namespace) {
		
		//list classes and register
		
		OntModel model = VitalSigns.get().getModel();
		
//		OntClass nodeClass = model.getOntClass(VitalCoreOntology.VITAL_Node.getURI());
//		OntClass edgeClass = model.getOntClass(VitalCoreOntology.VITAL_Edge.getURI());
//		OntClass hypernodeClass = model.getOntClass(VitalCoreOntology.VITAL_HyperEdge.getURI());
//		OntClass hyperedgeClass = model.getOntClass(VitalCoreOntology.VITAL_HyperNode.getURI());
		
		//collect all subclasses for given
		
		//graph object is a special case
		
		for(ExtendedIterator<OntClass> classes = model.listClasses(); classes.hasNext(); ) {
			
			OntClass cls = classes.next();
			
			String ns = cls.getNameSpace();
			
			if(ns == null) continue;
			
			if(ns.endsWith("#")) ns = ns.substring(0, ns.length() - 1);
			
			if(!ns.equals(namespace)) continue;
			
			Class<? extends GraphObject> gClass = VitalSigns.get().getGroovyClass(cls);
			
			if(gClass == null) continue;
		
			annotateClass(gClass)	
		
		}
		
	}

	public static annotateClass(Class<? extends GraphObject> gClass) {
		
		gClass.metaClass.'static'.'getSubClasses' = { ->
			
			def del = delegate
			
			//static accessors
			return handleGetSubClasses(del)
			
		}
		
		gClass.metaClass.'static'.'getSuperClasses' = { ->
			
			def del = delegate
			return handleGetSuperClasses(del)
		
		}
		
		gClass.metaClass.'static'.'isSubClassOf' = { Class<? extends GraphObject> other ->
			
			def del = delegate
			
			return handleIsSubClassOf(del, other)
			
		}
		
		gClass.metaClass.'static'.'isSuperClassOf' = { Class<? extends GraphObject> other ->
			
			def del = delegate
			
			return handleIsSuperClassOf(del, other)
			
		}
		
	}
	*/
	
	

	
}
