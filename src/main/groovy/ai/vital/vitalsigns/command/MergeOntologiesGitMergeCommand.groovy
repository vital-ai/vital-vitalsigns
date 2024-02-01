package ai.vital.vitalsigns.command

import java.io.File;

import org.apache.commons.io.FileUtils;

import owl2vcs.tools.GitMergeTool;

//import ai.vital.vitalsigns.command.OntVersionCommands;
import ai.vital.vitalsigns.command.OntVersionCommands.LoadedOntology;
import ai.vital.vitalsigns.command.patterns.JarFileInfo;
import ai.vital.vitalsigns.command.patterns.JsonSchemaFileInfo;
import ai.vital.vitalsigns.command.patterns.OwlFileInfo;

/**
 * new command based on ontologies merger tool
 * it outputs 
 *
 */
class MergeOntologiesGitMergeCommand {

	File vitalHome

	File domainOntDir
	
	File domainOntArchiveDir
	
	File domainJarDir
	
	File domainJarArchiveDir
	
	File domainJsonDir
	
	File domainJsonArchiveDir
	
	boolean gitAvailable
	
	String useremail
	
	public MergeOntologiesGitMergeCommand(File vitalHome) throws Exception {
		
		this.vitalHome = vitalHome
		//check
		this.domainOntDir = new File(vitalHome, 'domain-ontology')
		
		this.domainOntArchiveDir = new File(domainOntDir, 'archive')

		
		this.domainJarDir = new File(vitalHome, 'domain-groovy-jar')
		
		this.domainJarArchiveDir = new File(domainJarDir, 'archive')
		
		
		this.domainJsonDir = new File(vitalHome, 'domain-json-schema')
		
		this.domainJsonArchiveDir = new File(domainJsonDir, 'archive')
		
		
		if(!domainOntDir.isDirectory()) throw new Exception("Domain ontology dir not found: ${domainOntDir.absolutePath}")
		if(!domainJarDir.isDirectory()) throw new Exception("Domain jar dir not found: ${domainJarDir.absolutePath}")
		if(!domainJsonDir.isDirectory()) throw new Exception("Domain json dir not found: ${domainJsonDir.absolutePath}")
		
		gitAvailable = VitalSignsCommand.gitAvailable(vitalHome)
		
		if(gitAvailable) {
			useremail = GitUtils.getGitUserEmail(vitalHome)
		}
		
	}	

	public void mergeOntologies(String ont1Path, String ont2Path) throws Exception {
		
		
		File ont1 = new File(ont1Path)
		
		File ont2 = new File(ont2Path)
		
		println "Ont1: ${ont1.absolutePath}"
		println "Ont2: ${ont2.absolutePath}"
		
		//throw exception if ont1 does not match pattern
		OwlFileInfo ofi1 = OwlFileInfo.fromString(ont1.name)
		
		List<OwlFileInfo> candidates = [ofi1]
		
		//validation only
		LoadedOntology l1 = OntVersionCommands.readOntology(ont1);		
		
		OwlFileInfo ofi2 = OwlFileInfo.fromStringUnsafe(ont2.name)
		
		if(ofi2 != null) {
			if( ofi2.domain != ofi1.domain) {
				println "WARN Ont2 file is from different domain, ${ofi2.domain}, ont1: ${ofi1.domain}"
				ofi2 = null
			}
		} else {
			if(ofi2 == null && (ont2.getParentFile().equals(domainOntDir)|| ont2.getParentFile().equals(domainOntArchiveDir))) {
				throw new Exception("Ont2 file name in domain ontology directory or archive must follow vitalsigns convention:" + OwlFileInfo.archivePattern);
			}
			println "WARN: Ont2 file does not follow vitalsigns naming convention - its versionInfo will be ignored"
		}
		
		
		if(ofi2 != null) {
			LoadedOntology l2 = OntVersionCommands.readOntology(ont2)		
			candidates.add(ofi2)
		}
		
		
		for(File f : domainOntDir.listFiles()) {
			if(!f.isFile()) continue
			OwlFileInfo ofi = OwlFileInfo.fromStringUnsafe(f.name)

			if(ofi != null && ofi.domain == ofi1.domain) {
				candidates.add(ofi)
			}
						
		}
		
		
		
		Collections.sort(candidates)
		
		OwlFileInfo highestVersion = candidates.get(candidates.size() - 1)
		
		OwlFileInfo nextVersion = highestVersion.clone()
		nextVersion.useremail = null
		nextVersion.down = null
		nextVersion.patch = nextVersion.patch + 1
		
		println ("New version: ${nextVersion.toVersionNumberString()}")

		File outFile = new File(domainOntDir, nextVersion.toFileName());
		
		println "Merging ${ont1.absolutePath}, ${ont2.absolutePath} -> ${outFile.getAbsolutePath()} ..."
		
		new GitMergeTool().merge(vitalHome, ont1Path, ont1Path, ont2Path, outFile.getAbsolutePath())
		
		//fixing version to match the new one
		String owl = FileUtils.readFileToString(outFile, "UTF-8")
		owl = owl.replace("versionInfo>${ofi1.toVersionNumberString()}<", "versionInfo>${nextVersion.toVersionNumberString()}<")
		
		FileUtils.writeStringToFile(outFile, owl, "UTF-8")
		
		
		if(ont1.getParentFile().equals(domainOntDir)) {
			
			moveAndArchive(ont1, ofi1)
			
		}
		
		if(ont2.getParentFile().equals(domainOntDir)) {
			
			moveAndArchive(ont2, ofi2)
			
		}
		
		if(gitAvailable) {
			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', outFile.absolutePath])
		}
		
		
	}
	
	
	void moveAndArchive(File ont, OwlFileInfo ofi) {
		
		OwlFileInfo ofi1Archive = ofi.clone()
		ofi1Archive.useremail = useremail
		
		domainOntArchiveDir.mkdir();
		File ont1Archived = new File(domainOntArchiveDir, ofi1Archive.toFileName())

		FileUtils.deleteQuietly(ont1Archived)
		FileUtils.moveFile(ont, ont1Archived)
		
		if(gitAvailable) {
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', ont.absolutePath])
			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', ont1Archived.absolutePath])
			
		}
		
		JarFileInfo jarFileInfo = JarFileInfo.fromOwlInfo(ofi)
		jarFileInfo.useremail = null
		jarFileInfo.down = null
		
		File jarFile = new File(domainJarDir, jarFileInfo.toFileName())
		if(jarFile.exists()) {
			
			domainJarArchiveDir.mkdir()
			
			JarFileInfo archiveJFI = jarFileInfo.clone();
			archiveJFI.down = null
			archiveJFI.useremail = useremail
			
			File jarFileArchived = new File(domainJarArchiveDir, archiveJFI.toFileName())
			
			FileUtils.deleteQuietly(jarFileArchived)
			FileUtils.moveFile(jarFile, jarFileArchived)
			
			if(gitAvailable) {
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', jarFile.absolutePath])
				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', jarFileArchived.absolutePath])
				
			}
			
		}
		
		
		JsonSchemaFileInfo jsonFileInfo = JsonSchemaFileInfo.fromOwlInfo(ofi)
		File jsonFile = new File(domainJsonDir, jsonFileInfo.toFileName())
		
		if(jsonFile.exists()) {
			
			domainJsonArchiveDir.mkdir()
			
			JsonSchemaFileInfo archiveJSFI = jsonFileInfo.clone()
			archiveJSFI.down = null
			archiveJSFI.useremail = useremail
			
			File jsonSchemaFileArchived = new File(domainJsonArchiveDir, archiveJSFI.toFileName())
			
			FileUtils.deleteQuietly(jsonSchemaFileArchived)
			FileUtils.moveFile(jsonFile, jsonSchemaFileArchived)
			
			if(gitAvailable) {
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', jsonFile.absolutePath])
				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', jsonSchemaFileArchived.absolutePath])
				
			}
			
			
		}
		
		
	}
}
