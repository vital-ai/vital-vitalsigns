package ai.vital.vitalsigns.rdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.LRUMap;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.XSD;

public class RDFUtils {

	public static final String PROPERTIES_PACKAGE = "properties";
	
	public static final String ONTOLOGY_PACKAGE = "ontology";

	public static String uriFromNamespace(String ns) {

		int ind = ns.indexOf('#');
		
		if(ind < 0) return ns;
		
		return ns.substring(0, ind);
		
	}
	

	public static String getPropertyClassName(String localName) {
		return "Property_" + localName;
	}
	
	public static String getLocalURIPart(String uri) {
	    
	    int lastSlash = uri.lastIndexOf('/');
	    
	    if(lastSlash > 0) {
	        
	        return uri.substring(lastSlash + 1);
	        
	    }
	    
	    return uri;
	    
	}


	/**
	 * Returns a list of ont properties [ (parent ->)*, [ this ] ]
	 * @param dp
	 * @param includeParameter
	 * @return
	 */
	public static List<OntProperty> getPropertyParents(OntProperty dp, boolean includeParameter) {

		List<OntProperty> r = new ArrayList<OntProperty>();
		
		if(includeParameter) {
			r.add(dp);
		}
		
		OntProperty parent = dp.getSuperProperty();
		
		while(parent != null) {
		
			r.add(0, parent);
			
			parent = parent.getSuperProperty();
			
		}
		
		return r;
		
	}


	public static List<OntClass> getClassParents(OntClass oc, boolean includeParameter) {

		List<OntClass> r = new ArrayList<OntClass>();
		
		if(includeParameter) {
			r.add(oc);
		}
		
		OntClass parent = oc.getSuperClass();
		
		while(parent != null && !OWL.Thing.getURI().equals(parent.getURI())) {
			
			r.add(0, parent);
			
			parent = parent.getSuperClass();
		}
		
		return r;
	}


	@SuppressWarnings("unchecked")
	static Map<String, String> propertyURI2ShortName = Collections.synchronizedMap( new LRUMap(10000) );

	public static String getPropertyShortName(String propertyURI) {
		
		String shortName = (String) propertyURI2ShortName.get(propertyURI);
		
		if(shortName != null) return shortName;
		
		String pName = null;
		
		int i = propertyURI.indexOf('#');
		if(i < 0) {
//			throw new RuntimeException("Not a property URI: ${propertyURI} - missing #");
			pName = propertyURI;
		} else {
			pName = propertyURI.substring(i+1);
		}
		
		
		if(pName.startsWith("has")) {
			
			pName = pName.substring(3, 4).toLowerCase() + pName.substring(4);
			
		} else if(pName.startsWith("is")) {
		
			pName = pName.substring(2, 3).toLowerCase() + pName.substring(3);
		
		} else {

			//don't trim
			
		}
		
		propertyURI2ShortName.put(propertyURI, pName);
	
		return pName;
		
	}
	
	
	public static Boolean getBooleanPropertySingleValue(Resource r, Property p) {
		
		List<Statement> list = r.listProperties(p).toList();
		
		if(list.size() == 0) return null;
		
		if(list.size() > 1) throw new RuntimeException("Resource " + r.getURI() + " has more than 1 " + p.getURI() + " property");
		
		String dt = list.get(0).getLiteral().getDatatypeURI();
		if(!XSD.xboolean.getURI().equals( dt)) throw new RuntimeException("Resource " + r.getURI() + " property " + p.getURI() + " value expected to be an xsd:boolean, got: " + dt );
		
		return list.get(0).getBoolean();
		
	}


	public static String getStringPropertySingleValue(Resource r,
			Property p) {

		List<Statement> list = r.listProperties(p).toList();
		
		if(list.size() == 0) return null;
		
		if(list.size() > 1) throw new RuntimeException("Resource " + r.getURI() + " has more than 1 " + p.getURI() + " property");
		
		String dt = list.get(0).getLiteral().getDatatypeURI();
		
		if(dt == null || XSD.xstring.getURI().equals(dt) ) {
			return list.get(0).getString();
		} else {
			throw new RuntimeException("Resource " + r.getURI() + " property " + p.getURI() + " value expected to be a plain literal or xsd:string, got: " + (dt != null ? dt : "plain" ));
		}
		
	}
	
	public static Set<String> getStringPropertyValues(Resource r, Property p) {
	    
	    Set<String> v = null;
	    
	    for( StmtIterator listProperties = r.listProperties(p); listProperties.hasNext(); ) {
	        
	        Statement stmt = listProperties.nextStatement();
	        
	        Literal literal = stmt.getLiteral();
	        
	        if(literal == null) throw new RuntimeException("Property " + p.getURI() + " value is not a literal: " + stmt.getObject());

	        String dt = literal.getDatatypeURI();

	        if(dt == null || XSD.xstring.getURI().equals(dt) ) {
	            if(v == null) v = new HashSet<String>();
	            v.add(literal.getString());
	        } else {
	            throw new RuntimeException("Resource " + r.getURI() + " property " + p.getURI() + " value expected to be a plain literal or xsd:string, got: " + (dt != null ? dt : "plain" ));
	        }

	    }
	    
	    return v;
	    
	}


	public static String getOntologyPart(String propertyURI) {
		
		String o = null;
		
		int i = propertyURI.indexOf('#');
		if(i < 0) {
//			throw new RuntimeException("Not a property URI: ${propertyURI} - missing #");
			o = propertyURI;
		} else {
			o = propertyURI.substring(0, i);
		}
		
		return o;
		
	}

	
	public static void collectAllSubclasses(OntClass rootNodeClass, List<OntClass> subclasses) {
		
		for(ExtendedIterator<OntClass> iter = rootNodeClass.listSubClasses(true); iter.hasNext(); ) {
			
			OntClass subclass = iter.next();
			
			if(subclasses.indexOf(subclass) < 0) {
				subclasses.add(subclass);
				collectAllSubclasses(subclass, subclasses);
			}
			
		}
		
	}
	
	public static void collectAllSubproperties(OntProperty rootPropertyClass, List<OntProperty> subproperties) {
	    
	    for(ExtendedIterator<? extends OntProperty> iter = rootPropertyClass.listSubProperties(true); iter.hasNext(); ) {
	        
	        OntProperty subproperty = iter.next();
	        
	        if(subproperties.indexOf(subproperty) < 0) {
	            subproperties.add(subproperty);
	            collectAllSubproperties(subproperty, subproperties);
	        }
	        
	    }
	    
	}
	
	//collect all super classes
	public static void collectAllSuperClasses(OntClass rootClass, List<OntClass> superClasses, OntClass stopAtClass) {
		
		for(ExtendedIterator<OntClass> iter = rootClass.listSuperClasses(true); iter.hasNext(); ){
			
			OntClass superClass = iter.next();
			
			if(stopAtClass != null && stopAtClass.equals(superClass)) continue;
			
			if(superClasses.indexOf(superClass) < 0) {
				
				superClasses.add(superClass);
				collectAllSuperClasses(superClass, superClasses, stopAtClass);
				
			}
			
		}
		
	}


	public static String getGetterName(String shName) {

		String uc = shName.substring(0, 1).toUpperCase();
		
		return "get" + uc + shName.substring(1) ;
	} 
}
 