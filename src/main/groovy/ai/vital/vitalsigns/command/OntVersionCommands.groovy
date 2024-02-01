package ai.vital.vitalsigns.command

import com.hp.hpl.jena.ontology.OntDocumentManager
import com.hp.hpl.jena.ontology.OntModel
import com.hp.hpl.jena.ontology.Ontology
import com.hp.hpl.jena.rdf.model.ModelFactory
import com.hp.hpl.jena.rdf.model.RDFNode
import com.hp.hpl.jena.rdf.model.Statement
import com.hp.hpl.jena.util.iterator.ExtendedIterator
import com.hp.hpl.jena.ontology.OntModelSpec

import java.nio.file.Files;
import java.util.Map.Entry
import java.util.regex.Matcher
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import owl2vcs.tools.OntologyNormalizer
import owl2vcs.tools.OntologyNormalizer.CommentPattern
import ai.vital.vitalsigns.command.patterns.JarFileInfo;
import ai.vital.vitalsigns.command.patterns.JsonSchemaFileInfo;
import ai.vital.vitalsigns.command.patterns.OwlFileInfo
import ai.vital.vitalsigns.model.DomainOntology
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.rdf.RDFUtils;

class OntVersionCommands {

	public static boolean mockSystemIn = false
	
	public static void purge(File vitalHome, String app) throws Exception {
		
		File domainDir = new File(vitalHome, 'domain-ontology')
		if(!domainDir.exists()) throw new Exception("Domain ontology directory does not exist: ${domainDir.absolutePath}")
		if(!domainDir.isDirectory()) throw new Exception("Domain ontology path does not denote a directory: ${domainDir.absolutePath}")
		
		File domainArchiveDir = new File(domainDir, 'archive')
		domainArchiveDir.mkdir()
		
		File jarDir = new File(vitalHome, 'domain-groovy-jar')
		File jarArchiveDir = new File(jarDir, 'archive')
		jarArchiveDir.mkdir()
		
		File jsonDir = new File(vitalHome, 'domain-json-schema')
		File jsonArchiveDir = new File(jsonDir, 'archive')
		jsonArchiveDir.mkdir()
		
		
		List<File> filesToDelete = []
		//list all files
		for(Iterator<File> iter = FileUtils.iterateFiles(domainDir, null, true); iter.hasNext(); ) {
			
			File f = iter.next()
			
			OwlFileInfo i = null
			
			try { 
				i = OwlFileInfo.fromString(f.name)
			} catch(Exception e){}
			
			if(i != null && i.domain == app) {
				filesToDelete.add(f)
			}
			
		}
		
		for(Iterator<File> iter = FileUtils.iterateFiles(jarDir, null, true); iter.hasNext(); ) {
			
			File f = iter.next()
			
			JarFileInfo i = JarFileInfo.fromStringUnsafe(f.name)
			
			if(i != null && i.domain == app) {
				filesToDelete.add(f)
			}
			
		}
		
		
		for(Iterator<File> iter = FileUtils.iterateFiles(jsonDir, null, true); iter.hasNext(); ) {
			
			File f = iter.next()
			
			JsonSchemaFileInfo i = JsonSchemaFileInfo.fromStringUnsafe(f.name)
			
			if(i != null && i.domain == app) {
				filesToDelete.add(f)
			}
			
		}
		
		if(filesToDelete.size() < 1) throw new Exception("No files to delete")

		for(File f : filesToDelete) {
			println "Deleting: ${f.absolutePath}"
			f.delete()
		}
		
		
		
		if(VitalSignsCommand.gitAvailable(vitalHome)) {
			
			println "Git directory detected - reflecting deleting files changes in staging..."

			for(File f : filesToDelete) {
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', f.absolutePath])
			}
			
		}
		
		println "DONE"
				
		
	}
	
	public static void checkin(File vitalHome, ontologyFile) throws Exception {

		File domainDir = new File(vitalHome, 'domain-ontology')
		if(!domainDir.exists()) throw new Exception("Domain ontology directory does not exist: ${domainDir.absolutePath}")
		if(!domainDir.isDirectory()) throw new Exception("Domain ontology path does not denote a directory: ${domainDir.absolutePath}")
		
		File domainArchiveDir = new File(domainDir, 'archive')
		domainArchiveDir.mkdir()
		
		File jarDir = new File(vitalHome, 'domain-groovy-jar')
		File jarArchiveDir = new File(jarDir, 'archive')
		jarArchiveDir.mkdir()
		
		File jsonDir = new File(vitalHome, 'domain-json-schema')
		File jsonArchiveDir = new File(jsonDir, 'archive')
		jsonArchiveDir.mkdir()
		
		File ontFile = new File(ontologyFile)
		
		println "Ont file: ${ontFile.absolutePath}"
		
		if(!ontFile.exists()) throw new Exception("Ontology file does not exist: ${ontFile.absolutePath}")
		
		if(!ontFile.isFile()) throw new Exception("Ontology file path does not denote a path: ${ontFile.absolutePath}")

		
		File parentFile = ontFile.getParentFile()
		if(parentFile == null) throw new Exception("Cannot get parent directory of file: ${ontFile.absolutePath}")
		
		if(parentFile.getCanonicalPath().equals(domainDir.getCanonicalPath()) || parentFile.getCanonicalPath().equals(domainArchiveDir.getCanonicalPath())) throw new Exception("Cannot check in ontology file from \$VITAL_HOME/domain-ontology")
		
		
		
		LoadedOntology l = readOntology(ontFile);
		
		//once loaded, check current version if exists
		
		File currentOntologyF = null
		
		OwlFileInfo ci = null
		
		for(File f : domainDir.listFiles()) {
			
			OwlFileInfo i = OwlFileInfo.fromStringUnsafe(f.name)
			
			if(i == null) continue
			
			String app = i.domain
			
			if(l.app != app) continue
			
			if(currentOntologyF != null) throw new Exception("Expected exactly one ontology file for app: ${app}, paths: ${currentOntologyF.absolutePath}, ${f.absolutePath}")
			
			currentOntologyF = f
			
			ci = i
			
		}
		
		File archivedOntologyF = null
		
		File currentJarF = null
		
		File archivedJarF = null
		
		
		File currentJsonF = null
		
		File archivedJsonF = null
		
		
		boolean gitAvailable = VitalSignsCommand.gitAvailable(vitalHome)
		
		if(currentOntologyF != null) {
			
			DomainOntology current = new DomainOntology(l.domainOntology.uri, ci.toVersionNumberString())

			if(current.compareTo(l.domainOntology) >= 0) throw new Exception("Cannot check in a version of existing ontology with lower or equal version, current: ${current.toVersionString()}, new: ${l.domainOntology.toVersionString()}")
		
			println "Archiving current ontology file... ${currentOntologyF.absolutePath}"
			
			//archive
			archivedOntologyF = new File(domainArchiveDir, currentOntologyF.name)
			
			FileUtils.deleteQuietly(archivedOntologyF);
			FileUtils.moveFile(currentOntologyF, archivedOntologyF)
			
			JarFileInfo jfi = JarFileInfo.fromOwlInfo(ci)
			
			currentJarF = new File(jarDir, jfi.toFileName())
			
			if(currentJarF.exists()) {
				
				println "Archiving current domain jar... ${currentJarF.absolutePath}"

				JarFileInfo ajfi = jfi.clone()
				if(gitAvailable) {
					ajfi.useremail = GitUtils.getGitUserEmail(vitalHome)
				}
								
				archivedJarF = new File(jarArchiveDir, ajfi.toFileName())
				
				FileUtils.deleteQuietly(archivedJarF)
				FileUtils.moveFile(currentJarF, archivedJarF)
				
			} else {
			
				currentJarF = null
				
			}
			
			
			JsonSchemaFileInfo jsfi = JsonSchemaFileInfo.fromOwlInfo(ci)
			
			currentJsonF = new File(jsonDir, jsfi.toFileName())
			
			if(currentJsonF.exists()) {
				
				println "Archiving current domain json schema ... ${currentJsonF.absolutePath}"
				
				JsonSchemaFileInfo ajsfi = jsfi.clone()
				if(gitAvailable) {
					ajsfi.useremail = GitUtils.getGitUserEmail(vitalHome)
				}
				
				archivedJsonF = new File(jsonArchiveDir, ajsfi.toFileName())
				
				FileUtils.deleteQuietly(archivedJsonF)
				FileUtils.moveFile(currentJsonF, archivedJsonF)
				
			} else {
			
				currentJsonF = null
				
			}
							
		}
		
		File destFile = new File(domainDir, ontFile.name)
		
		
		FileUtils.copyFile(ontFile, destFile)
		
		
		
		if(gitAvailable) {
			
			println "Git directory detected - adding changes to staging..."

			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', destFile.absolutePath])
			
			if(currentOntologyF != null) {
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', currentOntologyF.absolutePath])
				
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', archivedOntologyF.absolutePath])
				
				
			}
			
			if(currentJarF != null) {
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', currentJarF.absolutePath])
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', archivedJarF.absolutePath])
				
			}
			
			if(currentJsonF != null) {
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', currentJsonF.absolutePath])
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', archivedJsonF.absolutePath])
				
			}
			
			
		}
		
		println "DONE"
		
	}
	
	
	public static void checkIfOntologyInOrder(File ontF) throws Exception {
		
		OntologyNormalizer norm1 = new OntologyNormalizer(ontF, CommentPattern.parseFromParam("ICCCCCCC"));
		if( ! norm1.isOntologyInOrder() ) {
			throw new Exception("OWL file is not in order: " + norm1.getOntologyOrderError() + ' - ' + ontF.absolutePath)
		}
		if(norm1.hasMergeComments()) {
			throw new Exception("OWL file has merge comments - please review the changes - " + ontF.absolutePath)
		}
		
	}
	
	public static void upVersion(File vitalHome, String ontologyFile) throws Exception {
		
		println "Ontology file: ${ontologyFile}"
		
		File domainDir = new File(vitalHome, 'domain-ontology')
		if(!domainDir.exists()) throw new Exception("Domain ontology directory does not exist: ${domainDir.absolutePath}")
		if(!domainDir.isDirectory()) throw new Exception("Domain ontology path does not denote a directory: ${domainDir.absolutePath}")
		
		File ontFile = null
		
		File domainArchiveDir = new File(domainDir, 'archive')
		domainArchiveDir.mkdir()
		
		File jarDir = new File(vitalHome, 'domain-groovy-jar')
		File jarArchiveDir = new File(jarDir, 'archive')
		jarArchiveDir.mkdir()
		
		File jsonDir = new File(vitalHome, 'domain-json-schema')
		File jsonArchiveDir = new File(jsonDir, 'archive')
		jsonArchiveDir.mkdir()
		
		File p = new File(ontologyFile)

		if(!p.exists()) throw new Exception("Ontology file not found: ${p.absolutePath}")

		if( ! p.getParentFile().getCanonicalFile().equals(domainDir.getCanonicalFile()) ) {
			throw new Exception("Ontology file is not located in \$VITAL_HOME/domain-ontology directory: ${p.absolutePath}, expected: ${domainDir.absolutePath}/${p.name}")
		}
			
		ontFile = p
			
		
		if(!ontFile.exists()) throw new Exception("Ontology file does not exist: ${ontFile.absolutePath}")
		
		if(!ontFile.isFile()) throw new Exception("Ontology file path does not denote a path: ${ontFile.absolutePath}")
		
		checkIfOntologyInOrder(ontFile)
		
		LoadedOntology l = readOntology(ontFile)
		
		boolean gitAvailable = VitalSignsCommand.gitAvailable(vitalHome)
		
		//move the ont file into archive
		
		OwlFileInfo i = OwlFileInfo.fromString(ontFile.name)
		
		OwlFileInfo ai = i.clone()
		
		if(gitAvailable) {
			ai.useremail = GitUtils.getGitUserEmail(vitalHome)
		}
		
		File archivedOwlF = new File(domainArchiveDir, ai.toFileName());
		
		FileUtils.deleteQuietly(archivedOwlF)
		FileUtils.moveFile(ontFile, archivedOwlF)
		
		
		Integer highestPatch = i.patch
		
		List<File> allFiles = new ArrayList<File>(Arrays.asList(domainDir.listFiles()))
		allFiles.addAll(domainArchiveDir.listFiles())
		
		for(File f : allFiles) {
			
			if(f.isDirectory()) continue
			
			OwlFileInfo di = OwlFileInfo.fromStringUnsafe(f.name)
			
			if(di == null) continue
			
			if(di.domain != i.domain) continue
			
			if(di.major != i.major || di.minor != i.minor) continue
			
			if(highestPatch < di.patch) {
				highestPatch = di.patch
			}
			
		}
		
		
		OwlFileInfo newI = i.clone()
		newI.patch = highestPatch + 1
		newI.useremail = null
		newI.down = null
		
		File newOntFile = new File(domainDir, newI.toFileName());
		
		l.ontology.setVersionInfo(newI.toVersionNumberString())
		
		//persist into file
		
		FileOutputStream fos = new FileOutputStream(newOntFile)
		l.model.write(fos, "RDF/XML")
		fos.close()
			
		VitalSignsCommand.rewriteOntology(newOntFile)
		
		
		JarFileInfo jfi = JarFileInfo.fromOwlInfo(i)
		jfi.down = null
		jfi.useremail = null
		
		File currentJarF = new File(jarDir, jfi.toFileName())
		
		File archivedJarF = null
		
		if(currentJarF.exists()) {
			
			//archive current jar
		
			JarFileInfo ajfi = jfi.clone()
			
			if(gitAvailable) {
				ajfi.useremail = GitUtils.getGitUserEmail(vitalHome)
			}	
			
			archivedJarF = new File(jarArchiveDir, ajfi.toFileName())
			
			println "Moving current jar file into archive... ${currentJarF.absolutePath} -> ${archivedJarF.absolutePath}"
			
			FileUtils.deleteQuietly(archivedJarF)
			FileUtils.moveFile(currentJarF, archivedJarF)
			
		}
		
		JsonSchemaFileInfo jsfi = JsonSchemaFileInfo.fromOwlInfo(i)
		jsfi.down = null
		jsfi.useremail = null
		
		File currentJsonF = new File(jsonDir, jsfi.toFileName())
		
		File archivedJsonF = null
		
		if(currentJsonF.exists()) {
			
			//archive current json
			
			JsonSchemaFileInfo ajsfi = jsfi.clone()
			
			if(gitAvailable) {
				ajsfi.useremail = GitUtils.getGitUserEmail(vitalHome) 
			}
			
			archivedJsonF = new File(jsonArchiveDir, ajsfi.toFileName())
			
			println "Moving current json schema file into archive... ${currentJsonF.absolutePath} -> ${archivedJsonF.absolutePath}"
			
			FileUtils.deleteQuietly(archivedJsonF)
			FileUtils.moveFile(currentJsonF, archivedJsonF)
			
		}
		
		if(gitAvailable) {
			
			println "Adding archved OWL file, new version to staging, removing current version from git ..."
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', archivedOwlF.absolutePath])
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', newOntFile.absolutePath])
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', ontFile.absolutePath])
			
			if(archivedJarF != null) {
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', archivedJarF.absolutePath])
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', currentJarF.absolutePath])
			}
			
			if(archivedJsonF != null) {

				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', archivedJsonF.absolutePath])
				
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', currentJsonF.absolutePath])
				
			}
			
		}
		
		println "DONE"
		
	}

	
	public static void downVersion(File vitalHome, String ontologyFile, String version) {

		OntVersionCommands.mockSystemIn = true
		
		println "Ontology file: ${ontologyFile}"
		
		File domainDir = new File(vitalHome, 'domain-ontology')
		if(!domainDir.exists()) throw new Exception("Domain ontology directory does not exist: ${domainDir.absolutePath}")
		if(!domainDir.isDirectory()) throw new Exception("Domain ontology path does not denote a directory: ${domainDir.absolutePath}")
		
		File ontFile = null
		
		File domainArchiveDir = new File(domainDir, 'archive')
		domainArchiveDir.mkdir()
		
		File jarDir = new File(vitalHome, 'domain-groovy-jar')
		File jarArchiveDir = new File(jarDir, 'archive')
		jarArchiveDir.mkdir()
		
		File jsonDir = new File(vitalHome, 'domain-json-schema')
		File jsonArchiveDir = new File(jsonDir, 'archive')
		jsonArchiveDir.mkdir()
		
			
		File p = new File(ontologyFile)

		if(!p.exists()) throw new Exception("Ontology file not found: ${p.absolutePath}")

		if( ! p.getParentFile().getCanonicalFile().equals(domainDir.getCanonicalFile()) ) {
			throw new Exception("Ontology file is not located in \$VITAL_HOME/domain-ontology directory: ${p.absolutePath}, expected: ${domainDir.absolutePath}/${p.name}")
		}
			
		ontFile = p
			
			
		if(!ontFile.exists()) throw new Exception("Ontology file does not exist: ${ontFile.absolutePath}")
		
		if(!ontFile.isFile()) throw new Exception("Ontology file path does not denote a path: ${ontFile.absolutePath}")
		
		checkIfOntologyInOrder(ontFile)
		
		boolean gitAvailable = VitalSignsCommand.gitAvailable(vitalHome)
		
		String user = null
		
		if(gitAvailable) {
			
			user = GitUtils.getGitUserEmail(vitalHome)
			
		}
		
		//current ont file located, checking archive now
		
		OwlFileInfo i = OwlFileInfo.fromString(ontFile.name)
		
		String app = i.domain
		
		DomainOntology currentVersion = new DomainOntology(i.domain, i.toVersionNumberString())
		
		DomainOntology versionSought = null
		
		if(version) {
			
			//validates version string
			versionSought = new DomainOntology(i.domain, version)
			
			int c = currentVersion.compareTo(versionSought)
			
			if(c <= 0) throw new Exception("Current version ${currentVersion.toVersionString()} is equal or lower than target downgraded version: ${versionSought.toVersionString()}")
			
		}
		
		Map<DomainOntology, List<OwlFileInfo>> versionToFiles = [:]
		
		Map<DomainOntology, List<OwlFileInfo>> versionToDownFiles = [:]
		
		for(File f : domainArchiveDir.listFiles()) {
			
			String fname = f.name
			
			OwlFileInfo di = OwlFileInfo.fromStringUnsafe(fname)
			
			if(di == null || di.domain != app) continue
			
			if(di.down != null) {
				
				Integer index = di.down - 1;
				
				if( ( user != null && di.useremail != user) || (user == null && di.useremail != null)) {
					//skip non user down version files
					
				} else {
				
					DomainOntology d = new DomainOntology(di.domain, di.toVersionNumberString())
					
					List<OwlFileInfo> l = null
						
					for(Entry<DomainOntology, List<OwlFileInfo>> e : versionToDownFiles.entrySet()) {
							
						if(e.key.equals(d)) {
							l = e.value
							break
								
						}
							
					}
						
					if(l == null) {
						l = []
						versionToDownFiles.put(d, l)
					}
						
					while(l.size() <= index ) {
						l.add(null)
					}
						
					l.set(index, f)
					
				}
									
			} 

				
			DomainOntology d = new DomainOntology(app, di.toVersionNumberString())
				
			List<OwlFileInfo> versionFiles = null
				
			for(Entry<DomainOntology, List<OwlFileInfo>> e :versionToFiles.entrySet()) {
					
				if(e.key.equals(d)) {
					versionFiles = e.value
					break
				}
					
			}
				
			if(versionFiles == null) {
				versionFiles = []
				versionToFiles.put(d, versionFiles)
			}
				
			versionFiles.add(di)
				
		}
		
		DomainOntology archivedVersion = null
		
		OwlFileInfo archivedOwlFile = null
		
		List<OwlFileInfo> filesList = null
		
		if(versionSought != null) {
			
			println "Looking for particular version: ${version} ..."
			
			//validates version string
			
			for(Entry<DomainOntology, List<OwlFileInfo>> e : versionToFiles.entrySet()) {
				if(e.key.equals(versionSought)) {
					filesList = e.value
					break
				}
			}
			
			if(filesList == null || filesList.size() < 1) {
				throw new Exception("No archived version found: ${version}")
			}
			
			archivedVersion = versionSought
			
		} else {
		
			List<DomainOntology> allVersions = new ArrayList<DomainOntology>(versionToFiles.keySet())
			
			
			if(allVersions.size() < 1) throw new Exception("No archived versions available")
			
			Collections.sort(allVersions)
			
			DomainOntology latest = allVersions.get(allVersions.size()-1)
			
			filesList = versionToFiles.get(latest)
			
			if(filesList == null || filesList.size() < 1) {
				throw new Exception("No previous version found")
			}
			
			archivedVersion = latest
			
			println "Latest version: ${archivedVersion.toVersionString()}"
		
		}
		
		
		
		if(filesList.size() == 1) {
			
			archivedOwlFile = filesList[0]
			
			println "Exactly 1 archived version file found: ${archivedOwlFile.toFileName()}"
			
		} else {
		
			//sort the list, user must be first!
		
			filesList.sort { OwlFileInfo f1, OwlFileInfo f2 ->
			
				String u1 = f1.useremail ? f1.useremail : ''
				String u2 = f2.useremail ? f2.useremail : ''
				
				if(user) {
				
					
					if(u1 == user) return -1
					if(u2 == user) return 1
					
					//sort by name ?
					return u1.compareTo(u2)
						
				} else {
				
					if(u1 == null) return -1
					if(u2 == null) return 1
				
					return u1.compareTo(u2)
				}
				
			}
			
			println "Found ${filesList.size()} archive files matching version: ${archivedVersion.toVersionString()}, please select one:"
			
			
			for(int l = 0 ; l < filesList.size(); l++) {
				
				OwlFileInfo fi = filesList[l]
				
				println "${l+1}. ${fi.toFileName()}"
				
			}
			
			int choice = 0
			
			if(mockSystemIn) {
				
				choice = 1
				
			} else {
			
				choice = Integer.parseInt(new String([System.in.read()] as char[]));
				
			}
			
			if(choice < 1 || choice > filesList.size()) {
				throw new Exception("Unknown choice, expected: [1-${filesList.size()}]")
			}
			
			archivedOwlFile = filesList.get(choice-1)
			
			
			println("Selected: ${choice} - ${archivedOwlFile.toFileName()}")
		
		}
		
		File archivedVersionFile = new File(domainArchiveDir, archivedOwlFile.toFileName())
		
		
		int newIndex = 1
		
		//get the down index
		for(Entry<DomainOntology, List<File>> e : versionToDownFiles.entrySet()) {
			if(e.key.equals(currentVersion)) {
				newIndex = ((List<File>)e.value).size() + 1
			}
		}
		
		//move the current ontFile -> downFile
		
		OwlFileInfo downI = i.clone()
		downI.down = newIndex
		downI.useremail = user
		
		File downFile = new File(domainArchiveDir, downI.toFileName())
		
		println "Moving ${ontFile.absolutePath} -> ${downFile.absolutePath}"
		FileUtils.deleteQuietly(downFile)
		FileUtils.moveFile(ontFile, downFile)
		
		
		OwlFileInfo newOntInfo = archivedOwlFile.clone()
		newOntInfo.useremail = null
		newOntInfo.down = null
		
		File newOnt = new File(domainDir, newOntInfo.toFileName())
		println "Moving ${archivedVersionFile.absolutePath} -> ${newOnt.absolutePath}"
		FileUtils.deleteQuietly(newOnt)
		FileUtils.moveFile(archivedVersionFile, newOnt)
		
		
		//jar
		JarFileInfo currentJarInfo = JarFileInfo.fromOwlInfo(i)
		
		//check if corresponding jars exist
		File currentJar = new File(jarDir, currentJarInfo.toFileName())
		
		File downgradedJar = null
		JarFileInfo downgradedJarInfo = null
		
		JarFileInfo archivedJarInfo = JarFileInfo.fromOwlInfo(archivedOwlFile)
		
		File archivedJar = new File(jarArchiveDir, archivedJarInfo.toFileName())
		
		if(currentJar.exists()) {
		
			//downgrade current jar too!
			println "Downgrading current domain jar too: ${currentJar.absolutePath} ..."
			
			//list all downgraded files to get the highest index
			
			int newJarIndex = 1
			
			for(File f : jarArchiveDir.listFiles()) {
				
				JarFileInfo m = JarFileInfo.fromStringUnsafe(f.name)
				
				if(m == null || m.domain != app || m.down == null || (user != null && user != m.useremail) || (user == null && m.useremail != null)) continue
				
				Integer v = m.down
				
				if(v >= newJarIndex) {
					newJarIndex = v + 1
				}
				
			}
			
			downgradedJarInfo = JarFileInfo.fromOwlInfo(i)
			downgradedJarInfo.down = newJarIndex
			downgradedJarInfo.useremail = user
			
			downgradedJar = new File(jarArchiveDir, downgradedJarInfo.toFileName())
			
			FileUtils.deleteQuietly(downgradedJar)
			FileUtils.moveFile(currentJar, downgradedJar)
			
		} else {
			currentJar = null
		}
		
		JarFileInfo restoredJarInfo = null
		File restoredJarF = null
		
		if(archivedJar.exists()) {
			
			println "Corresponding archived version found - restoring: ${archivedJar.absolutePath}"
			
			//move the archived jar back to domain jar dir
			restoredJarInfo = archivedJarInfo.clone()
			restoredJarInfo.down = null
			restoredJarInfo.useremail = null
			restoredJarF = new File(jarDir, restoredJarInfo.toFileName())	
			
			FileUtils.deleteQuietly(restoredJarF)
			FileUtils.moveFile(archivedJar, restoredJarF)
			
		} else {
		
			archivedJar = null
		}
		
		
		
		//json
		JsonSchemaFileInfo currentJsonInfo = JsonSchemaFileInfo.fromOwlInfo(i)
		
		//check if corresponding json exist
		File currentJson = new File(jsonDir, currentJsonInfo.toFileName())
		
		File downgradedJson = null
		JsonSchemaFileInfo downgradedJsonInfo = null
		
		JsonSchemaFileInfo archivedJsonInfo = JsonSchemaFileInfo.fromOwlInfo(archivedOwlFile)
		
		File archivedJson = new File(jsonArchiveDir, archivedJsonInfo.toFileName())
		
		if(currentJson.exists()) {
		
			//downgrade current json too!
			println "Downgrading current domain json schema too: ${currentJson.absolutePath} ..."
			
			//list all downgraded files to get the highest index
			
			int newJsonIndex = 1
			
			for(File f : jsonArchiveDir.listFiles()) {
				
				JsonSchemaFileInfo m = JsonSchemaFileInfo.fromStringUnsafe(f.name)
				
				if(m == null || m.domain != app || m.down == null || (user != null && user != m.useremail) || (user == null && m.useremail != null)) continue
				
				Integer v = m.down
				
				if(v >= newJsonIndex) {
					newJsonIndex = v + 1
				}
				
			}
			
			downgradedJsonInfo = JsonSchemaFileInfo.fromOwlInfo(i)
			downgradedJsonInfo.down = newJsonIndex
			downgradedJsonInfo.useremail = user
			
			downgradedJson = new File(jsonArchiveDir, downgradedJsonInfo.toFileName())
			
			FileUtils.deleteQuietly(downgradedJson)
			FileUtils.moveFile(currentJson, downgradedJson)
			
		} else {
			currentJson = null
		}
		
		JsonSchemaFileInfo restoredJsonInfo = null
		File restoredJsonF = null
		
		if(archivedJson.exists()) {
			
			println "Corresponding archived version found - restoring: ${archivedJson.absolutePath}"
			
			//move the archived json back to domain json dir
			restoredJsonInfo = archivedJsonInfo.clone()
			restoredJsonInfo.down = null
			restoredJsonInfo.useremail = null
			restoredJsonF = new File(jsonDir, restoredJsonInfo.toFileName())
			
			FileUtils.deleteQuietly(restoredJsonF)
			FileUtils.moveFile(archivedJson, restoredJsonF)
			
		} else {
		
			archivedJson = null
		}
		
		
		if(gitAvailable) {
			
			println "Git directory detected - adding changes to staging..."

			VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', archivedVersionFile.absolutePath])
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', newOnt.absolutePath])
						
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', ontFile.absolutePath])
			
			VitalSignsCommand.runProcess(vitalHome, ['git', 'add', downFile.absolutePath])

			if(currentJar != null) {
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', currentJar.absolutePath])
			}
			
			if(downgradedJar != null) {
				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', downgradedJar.absolutePath])
			}
			
			if(archivedJar != null) {
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', archivedJar.absolutePath])
			}
			
			if(restoredJarF != null) {
				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', restoredJarF.absolutePath])
			}
			
			
			if(currentJson != null) {
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', currentJson.absolutePath])
			}
			
			if(downgradedJson != null) {
				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', downgradedJson.absolutePath])
			}
			
			if(archivedJson != null) {
				VitalSignsCommand.runProcess(vitalHome, ['git', 'rm', archivedJson.absolutePath])
			}
			
			if(restoredJsonF != null) {
				VitalSignsCommand.runProcess(vitalHome, ['git', 'add', restoredJsonF.absolutePath])
			}
		}
		
		
		println "DONE"
		
	}
		
	
	public static class LoadedOntology {
		
		String app
		
		OntModel model
		
		Ontology ontology
		
		String defaultPackage
		
		DomainOntology domainOntology
        
	}
	
	public static LoadedOntology readOntology(File ontFile) throws Exception {
		
		OwlFileInfo ofi = OwlFileInfo.fromString(ontFile.name);
		
		String app = ofi.domain
		Integer file_majorVersion = ofi.major
		Integer file_minorVersion = ofi.minor
		Integer file_patchVersion = ofi.patch
		
		OntDocumentManager manager = new OntDocumentManager();
		boolean processImports = manager.getProcessImports();
		manager.setProcessImports(false);
		manager.setCacheModels(false)
		
		OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);
		spec.setDocumentManager(manager);

		OntModel m  = ModelFactory.createOntologyModel(spec, null );
		
		//			OntModel m = ModelFactory.createOntologyModel();
		m.read(new FileInputStream(ontFile), null);
		
		Ontology ontology = null;
		
		for( ExtendedIterator<Ontology> iterator = m.listOntologies(); iterator.hasNext(); ) {
			if(ontology != null) throw new Exception("More than 1 ontology detected in owl file: ${ontFile.absolutePath}")
			ontology = iterator.next();
		}
		
		if(ontology == null) {
			throw new Exception("No ontology found in file: ${ontFile.absolutePath}")
		}
		
		String ontologyURI = ontology.getURI()
		println "Ontology URI: ${ontologyURI}"
		
		//if(ontologyURI.contains("_")) throw new Exception("Ontology IRI cannot contain underscores: ${ontologyURI}")
		
		if(!ontologyURI) throw new Exception("No ontology prefix found.");
		if(ontologyURI.endsWith("#")) ontologyURI = ontologyURI.substring(0, ontologyURI.length() - 1 );
		
		String versionInfo = ontology.getVersionInfo()
		if(!versionInfo) throw new Exception("Ontology ${ontFile.absolutePath} URI: ${ontologyURI} does not have versionInfo annotation!");
		
		Matcher versionMatcher = DomainOntology.versionPattern.matcher(versionInfo)
		
		if(!versionMatcher.matches()) throw new Exception("Ontology owl:versionInfo annotation does not match pattern: ${DomainOntology.versionPattern} - ${versionInfo}")
		
		Integer ont_majorVersion = Integer.parseInt(versionMatcher.group(1))
		Integer ont_minorVersion = Integer.parseInt(versionMatcher.group(2))
		Integer ont_patchVersion = Integer.parseInt(versionMatcher.group(3))
		
		println "Ontology major version: " + ont_majorVersion
		println "Ontology minor version: " + ont_minorVersion
		println "Ontology patch version: " + ont_patchVersion
		
		if(file_majorVersion != ont_majorVersion || file_minorVersion != ont_minorVersion || file_patchVersion != ont_patchVersion) {
			throw new Exception("File and ontology version numbers don't match: ${file_majorVersion}.${file_minorVersion}.${file_patchVersion} vs ${ont_majorVersion}.${ont_minorVersion}.${ont_patchVersion}")
		}
		
		String _defaultPackage = RDFUtils.getStringPropertySingleValue(ontology, VitalCoreOntology.hasDefaultPackage);
		
		if(_defaultPackage != null) {
			if(_defaultPackage.isEmpty()) throw new Exception("Ontology ${ontFile.absolutePath} URI: ${ontologyURI} vital-core:hasDefaultPackage literal must be a non empty string")
		}
        
		String backVersion = RDFUtils.getStringPropertySingleValue(ontology, VitalCoreOntology.hasBackwardCompatibilityVersion)
        
        if(backVersion != null) {

            Matcher backMatcher = DomainOntology.versionPattern.matcher(backVersion)
            if(!backMatcher.matches()) throw new Exception("Ontology vital-core:${VitalCoreOntology.hasBackwardCompatibilityVersion.getLocalName()} annotation does not match pattern: ${DomainOntology.versionPattern} - ${backVersion}")
            
        }
        
        Set<String> preferredImportVersions = RDFUtils.getStringPropertyValues(ontology, VitalCoreOntology.hasPreferredImportVersion);

		
		LoadedOntology l = new LoadedOntology()
		l.app = app
		l.model = m
		l.domainOntology = new DomainOntology(ontology.URI, versionInfo, backVersion)
        l.domainOntology.defaultPackage = _defaultPackage
        l.domainOntology.preferredImportVersions = preferredImportVersions
		l.ontology = ontology
		l.defaultPackage = _defaultPackage
		
		return l
		
		
	}
	
}
