package ai.vital.vitalsigns.command

import java.nio.file.attribute.PosixFilePermission
import java.nio.file.Files
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

class GitDiffCommands {

	public final static String config_diff_line0  = '[diff "vitalsignsdiff"]'
	public final static String config_diff_line1  = 'command = vitalsigns diff'
	
	public final static String config_merge_line0 = '[merge "vitalsignsmerge"]'
	public final static String config_merge_line1 = 'name = vitalsigns merge driver' 
	public final static String config_merge_line2 = 'driver = vitalsigns gitmergetool -b %O -l %A -r %B -o %A'
	
	public final static String config_jar_merge_line0 = '[merge "vitalsignsjarmerge"]'
	public final static String config_jar_merge_line1 = 'name = vitalsigns jar merge driver' 
	public final static String config_jar_merge_line2 = 'driver = vitalsigns gitjarmerge -b %O -l %A -r %B -o %A'
	
	public static void gitEnable(File vitalHome) {
		
		_handle(vitalHome, true)
		
	}
	
	public static void gitDisable(File vitalHome) {
		
		_handle(vitalHome, false)
		
	}
	
	private static void _handle(File vitalHome, boolean enableNotDisable) {
		
		File attrFile = getAttributesFile(vitalHome)
		if(attrFile == null) return
		
		File configFile = getConfigFile(vitalHome)
		if(configFile == null) return
		
		//check if not already enabled
		
		/*
		boolean diffAttribFound = false
		
		boolean mergeAttribFound = false
		
		boolean jarmergeAttribFound = false
		
		List<String> attribLines = FileUtils.readLines(attrFile, 'UTF-8')
		
		for(Iterator<String> iter = attribLines.iterator(); iter.hasNext(); ) {
		
			String line = iter.next()
			
//			*.owl	diff=vitalsignsdiff
			if(line.contains('diff=vitalsignsdiff')) {
				
				diffAttribFound = true
				
				if(enableNotDisable) {
					
				} else {
				
					iter.remove()
				
				}
			}
			
//			*.owl	merge=vitalsignsmerge
			if(line.contains('merge=vitalsignsmerge')) {

				mergeAttribFound = true				
				
				if(enableNotDisable) {
					
				} else {
				
					iter.remove()
					
				}
				
				
			}
			
			if(line.contains('merge=vitalsignsjarmerge')) {
				
				jarmergeAttribFound = true
				
				if(enableNotDisable) {
					
				} else {
				
					iter.remove()
				
				}
				
			}
				
		}
		
		if(enableNotDisable) {
			
			if(diffAttribFound && mergeAttribFound && jarmergeAttribFound) {
				
				println "Attributes file already contains the vitalsigns entries, ${attrFile.absolutePath}"
				
			} else {
			
				println "Appending attribute config to ${attrFile.absolutePath}"
				
				if(!diffAttribFound) {
					attribLines.add('*.owl	diff=vitalsignsdiff')
				}
				
				if(!mergeAttribFound) {
					attribLines.add('*.owl	merge=vitalsignsmerge')
				}
				
				if(!jarmergeAttribFound) {
					attribLines.add('domain-groovy-jar/*.jar	merge=vitalsignsjarmerge')
				}
			
				FileUtils.writeLines(attrFile, 'UTF-8', attribLines, '\n', false)
				
			}
			 
		} else {
		
			if(diffAttribFound || mergeAttribFound || jarmergeAttribFound) {
				
				println "Removing config from attributes file ${attrFile.absolutePath}"
				
				FileUtils.writeLines(attrFile, 'UTF-8', attribLines, '\n', false)
				
			} else {
			
				println "Attributes file already up-to-date - no vitalsigns entries, ${attrFile.absolutePath}"
			
			}
		
		}
		
		
		boolean diffConfigFound = false
		
		boolean mergeConfigFound = false
		
		boolean jarmergeConfigFound = false
		
		List<String> configLines = FileUtils.readLines(configFile, 'UTF-8')
		
		List<String> newList = new ArrayList<String>()
		
		for(int i = 0 ; i < configLines.size(); i++) {
			
			String line = configLines.get(i)
			
			boolean skipLine = false
			
			boolean confPart = false
			
			int linesToSkip = 0
			
			if(line.trim() == config_diff_line0) {
				
				if(diffConfigFound) {
					println "SEVERE: more than one section '${config_diff_line0}' found"
					return
				}
			
				
				String nextLine = i < configLines.size() -1 ? configLines.get(i+1).trim() : null
				
				if(nextLine == null || nextLine != config_diff_line1) {
					
					println "SEVERE: incomplete git config, line ${i+2} expected to be '${config_diff_line1}'"
					return
					
				} 
				
				diffConfigFound = true
				
				confPart = true
				
				linesToSkip = 1
				
			}	
			
			
			if(line.trim() == config_merge_line0) {
				
				if(mergeConfigFound) {
					println "SEVERE: more than one section '${config_merge_line0}' found"
					return
				}
				
				String nextLine1 = i < configLines.size() - 1 ? configLines.get(i+1).trim() : null
				
				String nextLine2 = i < configLines.size() - 2 ? configLines.get(i+2).trim() : null
				
				//        name = feel-free merge driver
//        driver = filfre %O %A %B
//        recursive = binary
				
				//https://www.kernel.org/pub/software/scm/git/docs/gitattributes.html
				
				//The �merge.*.driver` variable�s value is used to construct a command to run 
				//to merge ancestor�s version (%O), 
				//current version (%A) 
				//and the other branches� version (%B). 

				
				if(nextLine1 == null || nextLine1 != config_merge_line1) {
					println "SEVERE: incomplete git config, line ${i + 2} expected to be '${config_merge_line1}'"
					return 
				}
				
				//			b longOpt: 'base', "base owl file path", args: 1, required: true
//			l longOpt: 'local', "local owl file path", args: 1, required: true
//			r longOpt: 'remote', "remote owl file path", args: 1, required: true
//			o longOpt: 'output', "output merged owl file path", args: 1, required: true
				
				if(nextLine2 == null || nextLine2 != config_merge_line2 ) {
					println "SEVERE: incomplete git config, line ${i + 2} expected to be '${config_merge_line2}'"
					return
				}
				
				mergeConfigFound = true
				
				confPart = true
				
				linesToSkip = 2
				
			}
			
			if(line.trim() == config_jar_merge_line0) {
				
				if(jarmergeConfigFound) {
					println "SEVERE: more than one section '${config_jar_merge_line0}' found"
					return
				}
				
				String nextLine1 = i < configLines.size() - 1 ? configLines.get(i+1).trim() : null
				
				String nextLine2 = i < configLines.size() - 2 ? configLines.get(i+2).trim() : null
				
				if(nextLine1 == null || nextLine1 != config_jar_merge_line1) {
					println "SEVERE: incomplete git config, line ${i + 2} expected to be '${config_jar_merge_line1}'"
					return
				}
				
				if(nextLine2 == null || nextLine2 != config_jar_merge_line2 ) {
					println "SEVERE: incomplete git config, line ${i + 2} expected to be '${config_jar_merge_line2}'"
					return
				}
				
				jarmergeConfigFound = true
				
				confPart = true
				
				linesToSkip = 2
				
			}
			
			if(confPart) {
				
				
				if(enableNotDisable) {
						
					//when config is found do nothing 
						
				} else {
					
					//erase this and next line(s)
					skipLine = true
						
					i = i + linesToSkip
					
				}
					
				
			}
			
			if(!skipLine) {
				newList.add(line)
			}
			
		}
		
		
		if(enableNotDisable) {
		
			if(diffConfigFound && mergeConfigFound && jarmergeConfigFound) {
				
				println "Config file already contains vitalsigns entries, ${configFile.absolutePath}"
				
			} else {
			
				println "Appending config entry(ies) to ${configFile.absolutePath}"
				
				if(!diffConfigFound) {
					newList.add(config_diff_line0)
					newList.add('\t' + config_diff_line1)
				}
				
				if(!mergeConfigFound) {
					newList.add(config_merge_line0)
					newList.add('\t' + config_merge_line1)
					newList.add('\t' + config_merge_line2)
				}
				
				if(!jarmergeConfigFound) {
					newList.add(config_jar_merge_line0)
					newList.add('\t' + config_jar_merge_line1)
					newList.add('\t' + config_jar_merge_line2)
				}
				
				FileUtils.writeLines(configFile, 'UTF-8', newList, '\n', false)
				
			}
				
		} else {
		
			if(diffConfigFound || mergeConfigFound || jarmergeConfigFound) {
				
				println "Removing config entries from ${configFile.absolutePath}"
				
				FileUtils.writeLines(configFile, 'UTF-8', newList, '\n', false)
				
			} else {
			
				println "Config file already up-to-date - no vitalsigns entries, ${attrFile.absolutePath}"
			
			}
		
		}
		*/
		
		//handle git ignore file
		
		handleFileUpdate(configFile, 'config', enableNotDisable)
		
		handleFileUpdate(attrFile, 'attributes', enableNotDisable)
		
		File gitIgnoreFile = getGitignoreFile(vitalHome)
		
		handleFileUpdate(gitIgnoreFile, 'gitignore.txt', enableNotDisable)

		
		File gitDir = VitalSignsCommand.gitDir(vitalHome)
		
		if(gitDir == null) throw new RuntimeException("Git dir not found")
		
		File preCommitFile = new File(gitDir, "hooks/pre-commit")
		
		File postMergeFile = new File(gitDir, "hooks/post-merge")
		
		handleFileUpdate(preCommitFile, 'pre-commit', enableNotDisable)
		
		if(enableNotDisable) {
			
			addExecFlag(preCommitFile)
		}
				
		handleFileUpdate(postMergeFile, 'post-merge', enableNotDisable)
		
		if(enableNotDisable) {

			addExecFlag(postMergeFile)
		}		
		
	}
	
	private static void addExecFlag(File f) {
		
		try {
			
			Set<PosixFilePermission> pcf = new HashSet<PosixFilePermission>(Files.getPosixFilePermissions(f.toPath()))
			
			int initialSize = pcf.size()
					
			pcf.add(PosixFilePermission.OWNER_EXECUTE)
			pcf.add(PosixFilePermission.GROUP_EXECUTE)
			pcf.add(PosixFilePermission.OTHERS_EXECUTE)
					
			if(pcf.size() != initialSize) {
				println "Adding exec permissions to file: ${f.absolutePath}"
				Files.setPosixFilePermissions(f.toPath(), pcf)
			} else {
				println "File ${f.absolutePath} exec permissions ok"
			}
			
		} catch(UnsupportedOperationException e) {
			println "Cannot update file permission - not supported on this platform"
		}
		
	}
				
	private static handleFileUpdate(File targetFile, String contentFile, boolean enableNotDisable) {
		
		
		String startMarker = '### VITALSIGNS MANAGED START ###'
		
		String endMarker   = '### VITALSIGNS MANAGED END   ###'
		
		try {
			
				String content = ""
				
				if(targetFile.exists()) {
					
					content = FileUtils.readFileToString(targetFile, "UTF-8")
					
				}
				
				int start = content.indexOf(startMarker);
				int end = content.indexOf(endMarker);
				
				boolean sectionExists = false;
				
				if(start >= 0 && end >= 0) {
					
					if(end < start) {
						throw new Exception("messed ${targetFile.name} file, vitalsigns end marker occurs before start marker")
					}
					
					sectionExists = true
					
					println "${targetFile.name} already contains the vitalsigns section"
					
				} else if( start >= 0 ) {
				
					throw new Exception("messed ${targetFile.name} file, it contains start marker only")
				
				} else if( end >= 0 ) {
				
					throw new Exception("messed ${targetFile.name} file, it contains end marker only")
					
				}
	
				
				boolean save = false
				
				if(enableNotDisable) {
				
					if(!sectionExists) {
						
						String b = IOUtils.toString(GitDiffCommands.class.getResourceAsStream(contentFile), 'UTF-8')
						
						content = content + "\n" + startMarker + "\n\n" + b + "\n\n" + endMarker
						
						save = true
						
					}
						
				} else {
				
					if(sectionExists) {
						
						content = content.substring(0, start) + content.substring(end + endMarker.length())
						
						save = true
					}
				
				}
				
				if(save) {
					
					FileUtils.writeStringToFile(targetFile, content)
					
					println "updated ${targetFile.name} file - ${targetFile.absolutePath}"
					
				} else {
				
					println "No ${targetFile.name} changes"
				}
							
			
			} catch(Exception e) {
				System.err.println("${targetFile.name} update failed: ${e.localizedMessage}")
			}
		
	}
	
	private static File getConfigFile(File vitalHome) {
		
		//it is assumed  that .git dir already verified
		
		File gitDir = VitalSignsCommand.gitDir(vitalHome)

		if(gitDir == null) {
			println "\$VITAL_HOME is not in a git repository: ${vitalHome.absolutePath}"
			return null
		}
				
		File configFile = new File(gitDir, 'config')
		
		if(!configFile.exists()) {
			println "SEVERE .git/config file does not exist, path: ${configFile.absolutePath}"
			return null
		} 
		
		if(!configFile.isFile()) {
			println "SEVERE .git/config path does not denote a file, path: ${configFile.absolutePath}"
			return null
		}
		
		return configFile
	}
	
	private static File getGitignoreFile(File vitalHome) {
		
		return new File(vitalHome, '.gitignore')
		
	}
	
	private static File getAttributesFile(File vitalHome) {
	
		File gitDir = VitalSignsCommand.gitDir(vitalHome)
		
		if(gitDir == null) {
			println "\$VITAL_HOME is not in a git repository: ${vitalHome.absolutePath}"
			return null
		}
		

		File infoDir = new File(gitDir, 'info')
		if(!infoDir.exists()) {
			if(!infoDir.mkdir()) {
				println "SEVERE: couldn't create .git/info directory, path: ${infoDir.absolutePath}"
				return null
			}
		}
		
		if(!infoDir.isDirectory()) {
			println "SEVERE: .git/info is not a directory, path: ${infoDir.absolutePath}"
			return null
		}
		
		File attributesFile = new File(infoDir, 'attributes')
		
		if(!attributesFile.exists()) {
			
			try {
				FileUtils.touch(attributesFile)
			} catch(Exception e) {
				println "SEVERE: couldn't create attributes file, path: ${attributesFile.absolutePath} - ${e.localizedMessage}"
				return null
			}
			
		} else if(!attributesFile.isFile()) {
		
			println "SEVERE: attributes file path is not a file, path: ${attributesFile.absolutePath}"
			return null
		}
		
		return attributesFile
					
	}
	
}
