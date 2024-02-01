package ai.vital.vitalsigns.command

import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import com.hp.hpl.jena.vocabulary.RDF
import com.hp.hpl.jena.vocabulary.OWL

class ListIndividualsCommand {

	public static listIndividuals(File ontologyFile, boolean listProperties) {
		
		Model model = ModelFactory.createDefaultModel()
		
		model.read(new FileInputStream(ontologyFile), null, "RDF/XML")
		
		Resource NamedIndividualRes = ResourceFactory.createResource(OWL.NS + "NamedIndividual")
		
		int c = 0;
		
		for( ResIterator iter = model.listSubjectsWithProperty(RDF.type, NamedIndividualRes); iter.hasNext(); ) {
		
			Resource individual = iter.nextResource();
			
			if(individual.anon) continue
		
			
			List<Statement> statements = new ArrayList<Statement>(individual.listProperties().toList());

			Resource otherType = null;
			
			for(Statement stmt : statements) {
				if(stmt.predicate == RDF.type && stmt.object != NamedIndividualRes) {
					otherType = stmt.object
				}
			}
			
			println "#${c+1} ${individual.URI}   type: ${otherType}"
			
			c++
			
			statements.sort { Statement s1, Statement s2 ->
				
				if(s1.predicate == RDF.type) {
					return -1
				} 
				if(s2.predicate == RDF.type) {
					return 1
				}
				
				return s1.predicate.getURI().compareToIgnoreCase(s2.predicate.getURI())
				
			}
			
			if(listProperties) {
				for(Statement stmt : statements) {
					if(stmt.object == NamedIndividualRes) continue
					if(stmt.predicate == RDF.type) continue
					println "     ${stmt.predicate.getURI()} ${stmt.object.toString()}"
				}
			}
						
				
		}
		
		println "Total ${c}"
		
		
	}
}
