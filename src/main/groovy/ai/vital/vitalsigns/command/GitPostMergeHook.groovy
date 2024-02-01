package ai.vital.vitalsigns.command

import ai.vital.vitalsigns.command.patterns.JarFileInfo;
import ai.vital.vitalsigns.command.patterns.OwlFileInfo;
import ai.vital.vitalsigns.model.DomainOntology;

import java.util.Map.Entry

/**
 * This hook is intended to be called after successful git pull
 * It checks whether there are no simultaneous active ontologies
 * git hook: post-merge
 *
 */
class GitPostMergeHook {

	public static List<String> postMerge(File vitalHome) {
		
		List<String> errors = []
		
		File domainOntDir = new File(vitalHome, 'domain-ontology')
		if(!domainOntDir.isDirectory()) throw new RuntimeException("No VITAL_HOME/domain-ontology directory: ${domainOntDir.absolutePath}");

		File domainJarDir = new File(vitalHome, 'domain-groovy-jar')
		if(!domainJarDir.isDirectory()) throw new RuntimeException("No VITAL_HOME/domain-groovy-jar directory: ${domainJarDir.absolutePath}");

		
		Map<String, List<OwlFileInfo>> activeOwlVersions = [:]
		
		for(File f : domainOntDir.listFiles()) {
			
			if(!f.isFile()) continue
			
			OwlFileInfo i = OwlFileInfo.fromStringUnsafe(f.name)
			if(i == null) continue
			
			List<OwlFileInfo> l = activeOwlVersions.get(i.domain)
			if(l == null) {
				l = []
				activeOwlVersions.put(i.domain, l)
			}
			
			l.add(i)
			
		}
		
		for(Entry<String, List<OwlFileInfo>> e : activeOwlVersions.entrySet()) {
			
			String app = e.key
			
			List<OwlFileInfo> versions = e.value
			
			if(versions.size() > 1) {
				
				String vs = ''
				
				List<DomainOntology> dos = []
				for(OwlFileInfo v : versions) {
					dos.add(new DomainOntology(app, v.toVersionNumberString()))
				}
				
				Collections.sort(dos)
				
				for(DomainOntology d : dos) {
					if(vs.length() > 0) vs += ', '
					vs += d.toVersionString()
				}
				
				errors.add("Multiple [${versions.size()}] active ontologies for app: ${app} - ${vs}")
				
			}
			
		}
		
		
		Map<String, List<JarFileInfo>> activeJarVersions = [:]
		
		for(File f : domainJarDir.listFiles()) {
			
			if(!f.isFile()) continue
			
			JarFileInfo i = JarFileInfo.fromStringUnsafe(f.name)
			
			if(i == null) continue
			
			List<JarFileInfo> l = activeJarVersions.get(i.domain)
			if(l == null) {
				l = []
				activeJarVersions.put(i.domain, l)
			}
			
			l.add(i)
			
		}
		
		
		for(Entry<String, List<JarFileInfo>> e : activeJarVersions.entrySet()) {
			
			String app = e.key
			
			List<JarFileInfo> versions = e.value
			
			if(versions.size() > 1) {
				
				String vs = ''
				
				List<DomainOntology> dos = []
				for(JarFileInfo v : versions) {
					dos.add(new DomainOntology(app, v.toVersionNumberString()))
				}
				
				Collections.sort(dos)
				
				for(DomainOntology d : dos) {
					if(vs.length() > 0) vs += ', '
					vs += d.toVersionString()
				}
				
				errors.add("Multiple [${versions.size()}] active domain jars for app: ${app} - ${vs}")
				
			}
			
		}
		
		return errors
				
	}
	
}
