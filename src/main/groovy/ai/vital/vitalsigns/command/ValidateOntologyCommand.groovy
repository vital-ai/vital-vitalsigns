package ai.vital.vitalsigns.command

import owl2vcs.tools.OntologyNormalizer;
import owl2vcs.tools.OntologyNormalizer.CommentPattern;
import ai.vital.vitalsigns.command.OntVersionCommands.LoadedOntology;

/*
 * prints ok if ontology is valid or errors otherwise
 */
class ValidateOntologyCommand {
	
	public static int validateOntology(File vitalHome, String path) {
		
		try {
			
			File f = new File(path)
			
			LoadedOntology lo = OntVersionCommands.readOntology(f)
			
			OntologyNormalizer norm1 = new OntologyNormalizer(f, CommentPattern.parseFromParam("ICC"));
			
			String s = norm1.checkOtherOntologiesClasses()
			
			if(s) throw new Exception(s);
			
			if(lo.defaultPackage != null && lo.defaultPackage.startsWith("ai.vital.")) throw new Exception("Ontology's default package mustn't start with ai.vital.")
			
			if(norm1.hasMergeComments()) throw new Exception("Merge comments detected, please review the changes")
			
			boolean sorted = norm1.isOntologyInOrder()
			
			if(!sorted) {
//				throw new Exception("Ontology out of order: " + norm1.getOntologyOrderError())
				println "Ontology not sorted - sorting..."
				norm1.normalizeOntology(f)
			}
			
			println "OK"
			
			return 0
			
		} catch(Exception e) {
		
			println "ERROR: " + e.getLocalizedMessage()
			
			return 1
			
		}
		
	}
	
}
