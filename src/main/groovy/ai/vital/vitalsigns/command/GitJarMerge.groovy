package ai.vital.vitalsigns.command

import org.apache.commons.io.FileUtils;

/**
 * Simple domain jar git merge tool - it automatically copies local file into output - using "ours" version
 * 
 *
 */
class GitJarMerge {

	public String merge(File vitalHome, String base, String local, String remote, String output) {
		
		File localFile = new File(local)
		println "Local file: ${localFile.absolutePath}"
		File outputFile = new File(output)
		println "Output file: ${outputFile.absolutePath}"
		
		try {
			FileUtils.copyFile(localFile, outputFile)
		} catch(Exception e) {
			return e.localizedMessage
		}
		
	}
}
