package ai.vital.vitalsigns.scripts;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.model.VITAL_Node;

public class TaxonomyEdgesVerifier {

	static void println(Object o) {System.out.println(o);}
	
	public static void main(String[] args) throws Exception {
		
		println("Verifying taxonomy edges...");
		
		VitalSigns signs = VitalSigns.get();
		
		for( ClassMetadata cm : signs.getClassesRegistry().listAllClasses() ) {
			
			if(VITAL_Node.class.isAssignableFrom(cm.getClazz())) {
				
				println("Checking: " + cm.getClazz().getCanonicalName() + " ...");
				
				signs.getClassesRegistry().getPaths(cm.getClazz(), true);
				signs.getClassesRegistry().getPaths(cm.getClazz(), false);
				
			}			
			
		}
				
		println("Done");
		
		
	}
	
}
