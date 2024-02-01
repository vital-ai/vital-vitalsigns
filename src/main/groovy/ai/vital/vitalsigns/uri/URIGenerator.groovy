package ai.vital.vitalsigns.uri


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalOrganization;
import ai.vital.vitalsigns.model.properties.Property_hasAppID;
import ai.vital.vitalsigns.model.properties.Property_hasOrganizationID;

import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The class responsible for generating valid URIs and validating them
 *
 */

public class URIGenerator {

	public static boolean useCounter = true;
	
	// static DecimalFormat sdf = new DecimalFormat("000000");
	
	static Random r = new Random();
	
	static AtomicInteger counter = new AtomicInteger(r.nextInt(Integer.MAX_VALUE));
	
	public static Pattern uriPattern = Pattern.compile("^https?://(?<domain>[^/]+)/(?<organization>[^/]+)/(?<app>[^/]+)/(?<clazz>[^/]+)/(?<transient>transient/)?(?<random>[^/])+\$");
	
	
	public static int incrementAndGet() {
        return counter.incrementAndGet();
    }

    public static int getValue() {
        return counter.get();
    }
	
	@Deprecated
	public static String generateURI(String appID, Class<?> clazz) {
				
		String localName = clazz.getSimpleName();
		
		//sdf.format(r.nextInt(1000000)))
		return "http://uri.vital.ai/" + appID + "/" + localName + "_" + System.currentTimeMillis() + "_" + (useCounter ? incrementAndGet() :  r.nextInt(1000000) );
				
	}
		
	public static String generateURI(VitalApp app, Class<?> clazz) {
		return generateURI(app, clazz, false, null);
	}
	
	public static String generateURI(VitalApp app, Class<?> clazz, boolean _transient) {
		return generateURI(app, clazz, _transient, null);
	}
	
	public static String generateURI(VitalApp app, Class<?> clazz, String randomPart) {
		return generateURI(app, clazz, false, randomPart);
	}
	
	public static String generateURI(VitalApp app, Class<?> clazz, boolean _transient, String randomPart) {

	    String appPart = app != null ? (String) app.getRaw(Property_hasAppID.class) : "default";
	    
		String localName = clazz.getSimpleName();
		
		String rp = randomPart != null ? randomPart : System.currentTimeMillis() + "_" + (useCounter ? incrementAndGet() :  r.nextInt(1000000));
		
		VitalOrganization org = VitalSigns.get().getOrganization();
		String orgPart = null;
		
		if(org == null) {
		    orgPart = "default";
		} else {
		    orgPart = (String) org.getRaw(Property_hasOrganizationID.class); 
		}
		
        return VitalSigns.get().getConfig().uriBase + "/" + orgPart + "/" + appPart + "/" + localName + "/" + (_transient ? "transient/" : "") + rp; 
		
	}
	
	public static Class<? extends GraphObject> getClassFromURI(String uri) {
		
		Matcher m = uriPattern.matcher(uri);
		
		if(m.matches()) {
			
			String app = m.group("app");
			
			String ns = VitalSigns.get().getApp2ns().get(app);
			
//			List<String> packages = new ArrayList<String>();
			
			List<String> namespaces = new ArrayList<String>(); 
			
			
			String classShortName = m.group("clazz");
			
			if(ns != null) {
				
				namespaces.add(ns);
				
				List<String> imports = VitalSigns.get().getOntologyURI2ImportsTree().get(ns);
				
				if(imports != null) {
					
					for(String imp : imports) {

						namespaces.add(imp);
						
					}
					
				}
				
			}
			
			if(!namespaces.contains("http://vital.ai/ontology/vital")) {
				namespaces.add("http://vital.ai/ontology/vital");
			}
			
			if(!namespaces.contains(VitalCoreOntology.ONTOLOGY_IRI)) {
				namespaces.add(VitalCoreOntology.ONTOLOGY_IRI);
			}

			for(String namespace : namespaces) {
				
				Class clazz = null;
				
				String pkg = VitalSigns.get().getNs2Package().get(namespace);
				
				String fullClassName = pkg + '.' + classShortName;
			
				ClassLoader l = VitalSigns.get().getNs2ClassLoader().get(namespace);
				
				
				if(l != null) {
					
					try {
						clazz = Class.forName(fullClassName, false, l);
					} catch (ClassNotFoundException e1) {
						// TODO Auto-generated catch block
					}
					
					if(clazz != null) return clazz;
					
				}
				
				
				if(VitalSigns.get().getCustomClassLoader() != null) {
					
					try {
						clazz = Class.forName(fullClassName, false, VitalSigns.get().getCustomClassLoader());
					} catch (ClassNotFoundException e1) {
						// TODO Auto-generated catch block
					}
					
					if(clazz != null) return clazz;
					
				}
				
				try {
					clazz = Class.forName(fullClassName);
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
				}

				if(clazz != null) return clazz;
				
			}
		}
		
		return null;
		
	}
	
	
	public static URIResponse validateURI(String uri) {
		
		Matcher matcher = uriPattern.matcher(uri);
		
		URIResponse r = new URIResponse();
		
		if(matcher.matches()) {
			
			String transientGroup = matcher.group("transient");
			
			r._transient = transientGroup != null;
			
			r.domain = matcher.group("domain");
			r.valid = true;
			r.organizationID = matcher.group("organization");
			r.appID = matcher.group("app");
			r.classShortName = matcher.group("clazz");
			r.randomPart = matcher.group("random");
		}
		
		return r;
		
	}
	
	public static class URIResponse {
		
		public boolean valid = false;
		
		public boolean _transient = false; 
		
		public String domain;
		public String organizationID;
		public String appID;
		public String classShortName;
		public String randomPart;

	}
}
