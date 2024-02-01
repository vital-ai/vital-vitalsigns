package ai.vital.vitalsigns.command

import java.io.File;
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import ai.vital.vitalsigns.command.patterns.OwlFileInfo;
import owl2vcs.changeset.FullChangeSet;
import owl2vcs.render.ChangeFormat;
import owl2vcs.tools.DiffVital;
import owl2vcs.tools.DiffVital.DiffSettings2;
import owl2vcs.utils.OntologyManagerThatDoesNotLoadImports;
import owl2vcs.utils.OntologyUtils;
import owl2vcs.utils.TimeTracker;

class DiffCommand {


	public static void diff(File vitalHome, String ont1, String ont2, Integer history) throws Exception {
		
		boolean gitAvailable = VitalSignsCommand.gitAvailable(vitalHome)
		
		String useremail = null
		
		if(gitAvailable) {
			
			useremail = GitUtils.getGitUserEmail(vitalHome)
			
		}
		
		
		File ont1F = null
		File ont2F = null
		
		File domainDir = new File(vitalHome, 'domain-ontology')
		File domainArchiveDir = new File(domainDir, 'archive')
		
		if(ont1F == null) {
			
			File c = new File(ont1)
			if(c.exists()) {
				ont1F = c
			} else {
				throw new Exception("Ontology file 1: ${ont1} not found")
			}
		}
		
		
		if(ont2) {
			
			if(ont2F == null) {
				
				File c = new File(ont2)
				if(c.exists()) {
					ont2F = c
				} else {
					throw new Exception("Ontology file 2: ${ont2} not found")
				}
			}
			

			if(ont1F.equals(ont2F)) throw new Exception("Cannot compare the same file: ${ont1F.absolutePath}")
			
		} else {
		
			if(history == null) throw new Exception("History param required when single ont version")
			if(history <= 0) throw new Exception("History param must be greater or equals 1")
			
			//list previous version in archive
			OwlFileInfo ofi1 = OwlFileInfo.fromString(ont1F.name)
			
			List<OwlFileInfo> previousVersions = []
			
			if(domainArchiveDir.isDirectory()) {
				for(File f : domainArchiveDir.listFiles()) {
					
					OwlFileInfo o = OwlFileInfo.fromStringUnsafe(f.name)
					if(o == null) continue
					if(o.useremail != useremail) continue
					
					if(ofi1.compareTo(o) <= 0) continue
					
					previousVersions.add(o)
					
				}
			}
			
			//sort previous versions ascendingly 
			Collections.sort(previousVersions)
			
			OwlFileInfo ofi2 = null
			
			if(previousVersions.size() >= history) {
				
				ofi2 = previousVersions.get(previousVersions.size() - history)
				
			}
			
			if(ofi2 == null) {
				throw new Exception("version requested not found")
			}
			
			
			ont2F = new File(domainArchiveDir, ofi2.toFileName())
		
		}
		
		
		
		
		DiffSettings2 settings = new DiffSettings2()
		settings.changes = true;
		settings.entities = true;
		settings.format = ChangeFormat.INDENTED;
//		settings.iriFormat = IriFormat.FULL;
		settings.prefixes = true;
		
		DiffVital.showFilesDiff(ont1F.absolutePath, ont2F.absolutePath, settings)
		
		
	}
	
	public static boolean checkIfVersionHasChanged(File owlF, File currentVersionJar) throws Exception {
		
		ZipInputStream zis = null
		try {
			
			zis = new ZipInputStream(new FileInputStream(currentVersionJar))
			
			ZipEntry ze = null
			
			ZipEntry owlEntry = null;
			
			while( ( ze = zis.getNextEntry()) != null ) {
				
				if(ze.getName().endsWith(".owl")) {
					
					owlEntry = ze
			
					break
							
					
				}
				
			}
			
			if(owlEntry == null) throw new Exception("No owl file found in domain jar: ${currentVersionJar.absolutePath}")
			
//			final TimeTracker t = new TimeTracker();
			
			StreamDocumentSource parentSource = new StreamDocumentSource(zis)
			
			FileDocumentSource childSource = new FileDocumentSource(owlF);
            // binary compare
//            final Boolean areIdenticalSources = binaryCompareSources(
//                    parentSource, childSource);
//            if (areIdenticalSources) {
//                System.err.println("Binary identical files");
//                return 0;
//            }
            // when no options provided, assume user wants raw changes
//            if (!settings.changes && !settings.entities)
//                settings.changes = true;

//            t.start("total");
//            t.start("load");
            final OWLOntology o1 = loadOntologyFromStream(parentSource); 
			//OntologyUtils.loadOntology(parentSource);
            final OWLOntology o2 = OntologyUtils.loadOntology(childSource);
//            t.end("load");

//            t.start("diff");
            final FullChangeSet cs = new FullChangeSet(o1, o2);
			
			if( cs.isEmpty() ) return false
			
			return true
			
			
//            t.end("diff");
			
		} finally {
			IOUtils.closeQuietly(zis);
		}
		
		
	}

	
	public static OWLOntology loadOntologyFromStream(final StreamDocumentSource source) throws OWLOntologyCreationException {

		final OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration();
		config.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
		final OWLOntologyManager manager = OntologyManagerThatDoesNotLoadImports.create();
		final OWLOntology ontology = manager.loadOntologyFromOntologyDocument(source, config);
		return ontology;
	}
		
}
