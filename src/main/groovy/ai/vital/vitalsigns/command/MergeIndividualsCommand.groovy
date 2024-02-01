package ai.vital.vitalsigns.command

import ai.vital.vitalsigns.command.patterns.OwlFileInfo;

import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.rdf.model.RDFNode
import com.hp.hpl.jena.rdf.model.ResIterator
import com.hp.hpl.jena.rdf.model.Resource
import com.hp.hpl.jena.rdf.model.ResourceFactory
import com.hp.hpl.jena.vocabulary.OWL
import com.hp.hpl.jena.vocabulary.RDF

import java.util.regex.Matcher

import org.apache.commons.io.FileUtils

import owl2vcs.tools.OntologyNormalizer
import owl2vcs.tools.OntologyNormalizer.CommentPattern;

class MergeIndividualsCommand {


	public static void mergeIndividuals(File vitalHome, File sourceOWLFile, String ontologyFileName, boolean mergeNotRemove) throws Exception {
		
		File domainOntologyDir = new File(vitalHome, 'domain-ontology')
		
		File domainArchiveOntologyDir = new File(domainOntologyDir, 'archive')
		
		domainArchiveOntologyDir.mkdirs()
		
		File targetOWLFile = new File(ontologyFileName)
		
		if(!targetOWLFile.isFile()) throw new Exception("Domain ontology file not found: ${targetOWLFile.absolutePath}")
		
		Resource NamedIndividual = ResourceFactory.createResource(OWL.NS + 'NamedIndividual')
		
		if(!sourceOWLFile.isFile()) throw new Exception("Source OWL path does not exist or is not a file: ${sourceOWLFile.absolutePath}")
		
		if(!targetOWLFile.isFile()) throw new Exception("Target OWL path does not exist or is not a fiel: ${targetOWLFile.absolutePath}")
		
		
		println "Loading source model..."
		Model source = ModelFactory.createDefaultModel()
		source.read(new FileInputStream(sourceOWLFile), null, "RDF/XML")
		
		
		List<Resource> srcOntResources = source.listSubjectsWithProperty(RDF.type, OWL.Ontology).toList()
		if(srcOntResources.size() > 1) throw new Exception("More than 1 ontology found in file: ${targetOWLFile.absolutePath}")
		
		if(srcOntResources.size() > 0) {
			println "Source file is an ontology, validating ..."
			OntologyNormalizer norm1 = new OntologyNormalizer(sourceOWLFile, CommentPattern.parseFromParam("ICCCCCCC"));
			if( ! norm1.isOntologyInOrder() ) {
				throw new Exception("Source OWL file is not in order: " + norm1.getOntologyOrderError() + ' - ' + sourceOWLFile.absolutePath)
			}
			if(norm1.hasMergeComments()) {
				throw new Exception("Source OWL file has merge comments - please review the changes - " + sourceOWLFile.absolutePath)
			}
		} else {
			println "Source file is just a regular RDF file, no validation."
		}
		
		
		OntologyNormalizer norm2 = new OntologyNormalizer(targetOWLFile, CommentPattern.parseFromParam("ICCCCCCC"));
		if( ! norm2.isOntologyInOrder() ) {
			throw new Exception("Target OWL file is not in order: " + norm2.getOntologyOrderError() + ' - ' + targetOWLFile.absolutePath)
		}
		if(norm2.hasMergeComments()) {
			throw new Exception("Target OWL file has merge comments - please review the changes - " + targetOWLFile.absolutePath)
		}
		
		
		Matcher matcher = OwlFileInfo.domain_version.matcher(targetOWLFile.name)
		if(!matcher.matches()) throw new Exception("Domain OWL file does not match pattern: ${OwlFileInfo.domain_version.pattern()}")
		
		OwlFileInfo i = OwlFileInfo.fromString(targetOWLFile.name)
		
		//archived version info
		OwlFileInfo ai = i.clone()
		
		//increment patch
		i.patch = i.patch + 1
		
		
		
		println "Loading target model..."
		Model target = ModelFactory.createDefaultModel()
		target.read(new FileInputStream(targetOWLFile), null, "RDF/XML")
		
		Resource ontologyResource = null;
		List<Resource> ontResources = target.listSubjectsWithProperty(RDF.type, OWL.Ontology).toList()
		if(ontResources.size() > 1) throw new Exception("More than 1 ontology found in file: ${targetOWLFile.absolutePath}")
		if(ontResources.size() < 1) throw new Exception("No ontology found in file: ${targetOWLFile.absolutePath}")

		//increment ontology version		
		
		ontologyResource = ontResources[0]
		target.removeAll(ontologyResource, OWL.versionInfo, null)
		
		ontologyResource.addProperty(OWL.versionInfo, (String)i.toVersionNumberString())
		
		List<Resource> sourceIndividuals = source.listSubjectsWithProperty(RDF.type, NamedIndividual).toList()
		
		println "Source model individuals count: ${sourceIndividuals.size()}"
		
		if(sourceIndividuals.size() < 1) throw new Exception("No individuals found in source file: ${sourceOWLFile.absolutePath}")
		
		
		for(Resource si : sourceIndividuals) {
			if(!si.isURIResource()) throw new Exception("Individual without URI detected in source OWL: ${sourceOWLFile.absolutePath} - ${si}")
		}
		
		
		List<Resource> targetIndividuals = target.listSubjectsWithProperty(RDF.type, NamedIndividual).toList()
				
		println "Target model individuals count: ${targetIndividuals.size()}"
		
		if(mergeNotRemove) {
			
			println "Inserting ..."
			
			int inserted = 0;
			
			for(Resource si : sourceIndividuals) {
				
				for(Resource ti : targetIndividuals) {
					
					if(si.getURI() == ti.getURI()) {
						throw new Exception("Individual with URI ${si.getURI()} already exists in target OWL: ${targetOWLFile.absolutePath}")
					}
				}
				
				target.add( source.listStatements(si, null, (RDFNode) null) )
				
				inserted++
				
			}
			
			println "Inserted: ${inserted}"
			
		} else {
		
		
			println "Removing ..."
			
			int removed = 0;
			int notFound = 0;
			
			for(Resource ti : targetIndividuals) {
				
				boolean found = false
				
				for(Resource si : sourceIndividuals) {
					
					if(si.getURI() == ti.getURI()) {
						found = true
						break
					}
				}
				
				if(found) {
					
					target.removeAll(ti, null, null)
				
					removed++
						
				} else {
				
					notFound++
				
				}
				
				
			}
			
			println "Removed: ${removed} / Not Found: ${notFound}"
			
			if(removed < 1) {
				println "NO CHANGES - exiting..."
				return
			}
		
		}
		
		
		boolean gitAvailable = VitalSignsCommand.gitAvailable(vitalHome)
		
		
		if(gitAvailable) {
			
			ai.useremail = GitUtils.getGitUserEmail(vitalHome)
			
		}
		
		File archivedFile = new File(domainArchiveOntologyDir, ai.toFileName())
		println "Archiving current owl version, ${targetOWLFile.absolutePath} -> ${archivedFile.absolutePath}"
		//
		
		
		if(archivedFile.exists()) {
			FileUtils.deleteQuietly(archivedFile)
		}
		
		FileUtils.moveFile(targetOWLFile, archivedFile)
		
		if(gitAvailable) {
			
			println "Adding archved OWL file to staging ..."
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', archivedFile.absolutePath])
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', targetOWLFile.absolutePath])
			
		}
		
		File newVersion = new File(domainOntologyDir, i.toFileName());
		
		println "Persisting ${newVersion.absolutePath} ..."
		
		FileOutputStream fos = new FileOutputStream(newVersion)
		target.write(fos, "RDF/XML")
		fos.close()
		
		VitalSignsCommand.rewriteOntology(newVersion)
		
		if(gitAvailable) {
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', newVersion.absolutePath])
			
		}
		
		println "DONE"
		
	}
}
