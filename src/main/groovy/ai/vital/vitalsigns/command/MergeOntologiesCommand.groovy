package ai.vital.vitalsigns.command

import ai.vital.vitalsigns.command.patterns.OwlFileInfo;
import ai.vital.vitalsigns.model.DomainOntology;
import java.util.regex.Matcher

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.io.FileDocumentSource
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom
import org.semanticweb.owlapi.model.AddOntologyAnnotation
import org.semanticweb.owlapi.model.OWLAnnotation
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyChange
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource

import owl2vcs.tools.OntologyNormalizer
import owl2vcs.tools.OntologyNormalizer.CommentPattern
import owl2vcs.utils.OntologyUtils

import com.hp.hpl.jena.vocabulary.OWL

class MergeOntologiesCommand {

	public static void mergeOntologies(File vitalHome, String ont, String mergingOntPath) throws Exception {
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		File domainOntDir = new File(vitalHome, 'domain-ontology')
		
		File domainOntArchiveDir = new File(domainOntDir, 'archive')
		
		if(!domainOntDir.isDirectory()) throw new Exception("Domain ontology dir not found: ${domainOntDir.absolutePath}")
		
		File ontF = new File(ont)
		
		if(!ontF.isFile()) throw new Exception("Input ontology file not found: ${ontF.absolutePath}")
		
		File merginOntF = new File(mergingOntPath)
		if(!merginOntF.isFile()) throw new Exception("Merging ontology file not found: ${merginOntF.absolutePath}")
		
		
		OntologyNormalizer norm1 = new OntologyNormalizer(ontF, CommentPattern.parseFromParam("ICCCCCCC"));
		if( ! norm1.isOntologyInOrder() ) {
			throw new Exception("Input OWL file is not in order: " + norm1.getOntologyOrderError() + ' - ' + ontF.absolutePath)
		}
		if(norm1.hasMergeComments()) {
			throw new Exception("Input OWL file has merge comments - please review the changes - " + ontF.absolutePath)
		}
		
		OntologyNormalizer norm2 = new OntologyNormalizer(merginOntF, CommentPattern.parseFromParam("ICCCCCCC"));
		if( ! norm2.isOntologyInOrder() ) {
			throw new Exception("Merging OWL file is not in order: " + norm2.getOntologyOrderError() + ' - ' + merginOntF.absolutePath)
		}
		if(norm2.hasMergeComments()) {
			throw new Exception("Merging OWL file has merge comments - please review the changes - " + merginOntF.absolutePath)
		}
		
		
		
		DomainOntology baseDomain = VitalSignsCommand.readOntologyFile(ontF, true)

		
		OwlFileInfo i = OwlFileInfo.fromString(ontF.name)
		
		
		OwlFileInfo targetInfo = i.clone();
		targetInfo.useremail = null
		targetInfo.down = null
		targetInfo.patch = targetInfo.patch + 1
		
		
		OwlFileInfo ai = i.clone();
		
		
		DomainOntology mergingDomain = VitalSignsCommand.readOntologyFile(merginOntF, true)
		
		if(baseDomain.uri != mergingDomain.uri) throw new Exception("Input and mergin ontology IRI don't match: ${baseDomain.uri} vs. ${mergingDomain.uri}")
		
		FileDocumentSource baseDomainSource = new FileDocumentSource(ontF);
		
		OWLOntology targetOntology = OntologyUtils.loadOntology(baseDomainSource);
		
		FileDocumentSource mergingSource = new FileDocumentSource(merginOntF)
		
		OWLOntology merginghOntology = OntologyUtils.loadOntology(mergingSource)
		
//		if(targetOntology.equals(merginghOntology)) { 
//			
//			println "Ontologies are equal, nothing to merge."
//			return
//			
//		}
		
		
		
		
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		
		// move the axioms
		for (OWLAxiom ax : merginghOntology.getAxioms()) {
			changes.add(new AddAxiom(targetOntology, ax));
		}

		// move ontology annotations
		for (OWLAnnotation annot : merginghOntology.getAnnotations()){
			changes.add(new AddOntologyAnnotation(targetOntology, annot));
		}

		/*
		if (!targetOntology.getOntologyID().isAnonymous()){
					// move ontology imports
					for (OWLImportsDeclaration decl : ont.getImportsDeclarations()){
						if (ontologies.contains(ont.getOWLOntologyManager().getImportedOntology(decl))) {
							continue;
						}
						if (!decl.getIRI().equals(targetOntology.getOntologyID().getDefaultDocumentIRI())){
							changes.add(new AddImport(targetOntology, decl));
						}
						else{
							logger.warn("Merge: ignoring import declaration for ontology " + targetOntology.getOntologyID() +
										" (would result in target ontology importing itself).");
						}
					}
				}
		*/
		//archive older version
		RDFXMLOntologyFormat format = new RDFXMLOntologyFormat()
		format.setAddMissingTypes(false)
		
		
		
//		FileUtils.deleteQuietly(archivedOntF)
//		manager.saveOntology(targetOntology, format, new FileOutputStream(archivedOntF))

		
		boolean gitAvailable = VitalSignsCommand.gitAvailable(vitalHome)
		
		if(gitAvailable) {
			ai.useremail = GitUtils.getGitUserEmail(vitalHome)
		}
				
		File archivedOntF = new File(domainOntArchiveDir, ai.toFileName())
		
		File outputFile = new File(domainOntDir, targetInfo.toFileName())
		
		
		manager.applyChanges(changes);
		
		FileOutputStream fos1 = null
		try {
			fos1 = new FileOutputStream(outputFile);
			manager.saveOntology(targetOntology, format, fos1)
		} finally {
			IOUtils.closeQuietly(fos1)
		}
		
		if(archivedOntF.exists()) {
			FileUtils.deleteQuietly(archivedOntF)
		}
		
		FileUtils.moveFile(ontF, archivedOntF)
		FileUtils.deleteQuietly(ontF)
		
		if(gitAvailable) {
			
			println "Adding archved OWL file to staging ..."
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', archivedOntF.absolutePath])
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', ontF.absolutePath])
			
		}
		
		//update output file version info
		Model m = ModelFactory.createDefaultModel()
		m.read(new FileInputStream(outputFile), null, "RDF/XML")
		Resource ontRes = m.getResource(baseDomain.uri)
		m.removeAll(ontRes, OWL.versionInfo, null)
		ontRes.addProperty(OWL.versionInfo, (String) targetInfo.toVersionNumberString())
		FileOutputStream fos = new FileOutputStream(outputFile)
		m.write(fos, "RDF/XML")
		fos.close()
		
		
		//rewrite the model again ?
//		OWLOntology loaded = OntologyUtils.loadOntology(new FileDocumentSource(outputFile))
//		manager.saveOntology(loaded, format, new FileOutputStream(outputFile));
		VitalSignsCommand.rewriteOntology(outputFile)
		
		if(gitAvailable) {
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', outputFile.absolutePath])
			
		}
		
		
		println "DONE"
//		manager.save(targetOntology, OWLOntologydo);
		
	}
	/*
	public void mergeOntologies() {
		List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
		for (OWLOntology ont : ontologies) {
			if (!ont.equals(targetOntology)){

				// move the axioms
				for (OWLAxiom ax : ont.getAxioms()) {
					changes.add(new AddAxiom(targetOntology, ax));
				}

				// move ontology annotations
				for (OWLAnnotation annot : ont.getAnnotations()){
					changes.add(new AddOntologyAnnotation(targetOntology, annot));
				}

				if (!targetOntology.getOntologyID().isAnonymous()){
					// move ontology imports
					for (OWLImportsDeclaration decl : ont.getImportsDeclarations()){
						if (ontologies.contains(ont.getOWLOntologyManager().getImportedOntology(decl))) {
							continue;
						}
						if (!decl.getIRI().equals(targetOntology.getOntologyID().getDefaultDocumentIRI())){
							changes.add(new AddImport(targetOntology, decl));
						}
						else{
							logger.warn("Merge: ignoring import declaration for ontology " + targetOntology.getOntologyID() +
										" (would result in target ontology importing itself).");
						}
					}
				}
			}
		}
		try {
			owlOntologyManager.applyChanges(changes);
		}
		catch (OWLOntologyChangeException e) {
			ErrorLogPanel.showErrorDialog(e);
		}
	}*/
	
}
