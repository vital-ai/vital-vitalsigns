package ai.vital.vitalsigns.command

import groovy.cli.picocli.CliBuilder

import java.nio.file.Path
import java.nio.file.Paths;
import java.util.jar.Attributes
import java.util.jar.JarEntry;
import java.util.jar.JarFile
import java.util.jar.Manifest;
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.SystemUtils

import ai.vital.vitalsigns.VitalManifest;
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.command.patterns.JarFileInfo
import ai.vital.vitalsigns.maven.VitalJarMavenUtil
import ai.vital.vitalsigns.maven.VitalJarMavenUtil.LocalArtifactFiles;
import ai.vital.vitalsigns.maven.VitalJarMavenUtil.MavenArtifact;
import ai.vital.vitalsigns.model.DomainOntology
import ai.vital.vitalsigns.ontology.DomainJarAnalyzer
import ai.vital.vitalsigns.ontology.OntologyProcessor
import ai.vital.vitalsigns.ontology.DomainJarAnalyzer.DomainJarModel

class VitalMavenDeployCommand {
    
    static final String VMD = "vitalmavendeploy"

    static final String CMD_HELP = 'help'
    
    static final String CMD_INSTALL_VITAL_CORE = 'installvitalcore'
     
    static final String CMD_INSTALL_VITAL_DOMAIN = 'installvitaldomain' 
    
    //installs all modules
    static final String CMD_INSTALL_VDK_MODULES = 'installvdkmodules'
    
    static final String CMD_INSTALL_VDK_MODULE = 'installvdkmodule'
    
    //install all jars
    static final String CMD_INSTALL_DOMAIN_JARS = 'installdomainjars'
    
    //install particular jar
    static final String CMD_INSTALL_DOMAIN_JAR = 'installdomainjar'
    
    static Map cmd2CLI = [:];
    
    static Set allCommands = null;
    
    static {

        //installs vitalsigns and vital-domain jars        
        def cmdInstallVitalCoreCLI = new CliBuilder(usage: "${VMD} ${CMD_INSTALL_VITAL_CORE}", stopAtNonOption: false)
        cmd2CLI[CMD_INSTALL_VITAL_CORE] = cmdInstallVitalCoreCLI
        
        def cmdInstallVitalDomainCLI = new CliBuilder(usage: "${VMD} ${CMD_INSTALL_VITAL_DOMAIN}", stopAtNonOption: false)
        cmd2CLI[CMD_INSTALL_VITAL_DOMAIN] = cmdInstallVitalDomainCLI
        
        def cmdInstallVdkModulesCLI = new CliBuilder(usage: "${VMD} ${CMD_INSTALL_VDK_MODULES}", stopAtNonOption: false)
        cmd2CLI[CMD_INSTALL_VDK_MODULES] = cmdInstallVdkModulesCLI
        
        def cmdInstallVdkModuleCLI = new CliBuilder(usage: "${VMD} ${CMD_INSTALL_VDK_MODULE}", stopAtNonOption: false)
        cmdInstallVdkModuleCLI.with {
            j longOpt: "jar", "module jar path, must be located in \$VITAL_HOME/", args: 1, required: true
        }
        cmd2CLI[CMD_INSTALL_VDK_MODULE] = cmdInstallVdkModuleCLI
        
        def cmdInstallDomainJarsCLI = new CliBuilder(usage: "${VMD} ${CMD_INSTALL_DOMAIN_JARS}", stopAtNonOption: false)
        cmdInstallDomainJarsCLI.with {
            od longOpt: "output-poms", "optional output directory for pom files, must not exist, use --overwrite option otherwise", args: 1, required: false
            ow longOpt: "overwrite", "overwrite output pom files directory", args:0, required: false
        }
        cmd2CLI[CMD_INSTALL_DOMAIN_JARS] = cmdInstallDomainJarsCLI
        
        def cmdInstallDomainJarCLI = new CliBuilder(usage: "${VMD} ${CMD_INSTALL_DOMAIN_JAR}", stopAtNonOption: false)
        cmdInstallDomainJarCLI.with {
            j longOpt: "jar", "domain jar path, must be located in \$VITAL_HOME/domain-groovy-jar/", args: 1, required: true
            od longOpt: "output-poms", "optional output directory for pom file, must not exist, use --overwrite option otherwise", args: 1, required: false
            ow longOpt: "overwrite", "overwrite output pom files directory", args:0, required: false
        }                   
        cmd2CLI[CMD_INSTALL_DOMAIN_JAR] = cmdInstallDomainJarCLI
        
        allCommands = new LinkedHashSet(cmd2CLI.keySet())
        allCommands.add(CMD_HELP)
    }
    
    static String mvnCommand() {
        if( SystemUtils.IS_OS_WINDOWS ) {
            return 'mvn.cmd'
        }
        return 'mvn'
    }
    
    def static main(args) {
        
        String VITAL_HOME = System.getenv(VitalSigns.VITAL_HOME);
        if(!VITAL_HOME) error("VITAL_HOME environment variable not set.");
        
        File vitalHome = new File(VITAL_HOME);
        if(!vitalHome.exists()) error("$VITAL_HOME path does not exist: ${VITAL_HOME} ");
        if(!vitalHome.isDirectory()) error("VITAL_HOME does not denote a directory: ${VITAL_HOME}");
        
        if(args.length == 0 || args[0] == CMD_HELP) {
            println "Usage: ${VMD} <command> [options]"
            println "   where command is one of: ${allCommands}"
            
            println "\n"
            
            for(def e : cmd2CLI.entrySet() ) {
                String cmd = e.key
                def cli = e.value
                cli.usage();
//                println "\n"
            }
            
            return;
        }
        
        String cmd = args[0];
        
        def cli = cmd2CLI[cmd];
        
        if(cli == null) {
            println "Unknown command: ${cmd}"
            println "available commands: ${allCommands}"
            return
        }
        
        List params = [];
        for(int i = 1 ; i < args.length; i++) {
            params.add(args[i]);
        }
        
        println "command: ${cmd}"
        
        def options = cli.parse(params.toArray(new String[params.size()]))
        
        if(!options) return
        
        print "checking maven command... "
                
        //test maven command
        StringBuilder sb = new StringBuilder()
        int status = ProcessUtils.runProcess(vitalHome, [mvnCommand(), '-v'], false, sb)
        if(status != 0) {
            throw new Exception("mavenCommand Command error: ${status} - ${sb.toString()}")
        }
        
        println "ok"
        
        
        if(cmd == CMD_INSTALL_VITAL_CORE) {
            
            installVitalCore(vitalHome, true)
            
        } else if(cmd == CMD_INSTALL_VITAL_DOMAIN) {
            
            installVitalDomain(vitalHome, true)
            
        } else if(cmd == CMD_INSTALL_DOMAIN_JAR || cmd == CMD_INSTALL_DOMAIN_JARS) {
            
            Boolean overwrite = options.ow ? true : false
            
            String outputDirectory = options.od ? options.od : null
            
            File outputDir = outputDirectory != null ? new File(outputDirectory) : null
            
            if(outputDir != null) {
                
                println "pom files output directory: ${outputDir.absolutePath}"
                println "overwrite ? ${overwrite}"
                if(outputDir.exists()) {
                    if(!overwrite.booleanValue()) {
                        error("output directory alredy exists, use --overwrite option, ${outputDir.absolutePath}")
                        return
                    }
                    if(!outputDir.isDirectory()) {
                        error("Output path is not a directory: ${outputDir.absolutePath}")
                        return
                    }
                    FileUtils.deleteDirectory(outputDir)
                }
                
                outputDir.mkdirs()
                
                
            }
            
            if(cmd == CMD_INSTALL_DOMAIN_JAR) {
               
                String jarPath = options.j
                
                File jar = new File(jarPath)
            
                installDomainJars(vitalHome, jar, outputDir, true)
                    
            }
            
            if(cmd == CMD_INSTALL_DOMAIN_JARS) {
                
                installDomainJars(vitalHome, null, outputDir, true)
                
            }
            
        } else if(cmd == CMD_INSTALL_VDK_MODULE) {
        
            File modFile = new File(options.j)
        
            println "Module jar file: ${modFile.absolutePath}"
            
            installVDKModules(vitalHome, modFile, true)
            
        } else if(cmd == CMD_INSTALL_VDK_MODULES) {
        
            installVDKModules(vitalHome, null, true)
            
        } else {
            error("Unhandled command: ${cmd}")
        }
        
    }
    
    static void installVitalCore(File vitalHome, boolean installNoUninstall) {
        
        String version = VitalSigns.VERSION
        
        File vitalsignsJar = new File(vitalHome, "vitalsigns/command/VitalSigns-${version}.jar")
        if(!vitalsignsJar.exists()) throw new Exception("VitalSigns jar not found: ${vitalsignsJar.absolutePath}")
        
        String pomFileContents = IOUtils.toString(VitalMavenDeployCommand.class.getResource("vitalsigns.pom"))
        pomFileContents = pomFileContents.replace("__VERSION__", version)
        
        if(!installNoUninstall) {
        
            MavenArtifact ma = VitalJarMavenUtil.getPomArtifact(null, pomFileContents)
            
            File mavenRepo = VitalJarMavenUtil.getLocalMavenRepo()
            
            File localArtifactDir = VitalJarMavenUtil.getLocalArtifactDir(ma)
            
            if(!localArtifactDir.exists()) error("Directory not found: ${localArtifactDir.absolutePath}")
            //check if file exists
            
            FileUtils.deleteDirectory(localArtifactDir)
            
            println "vitalsigns jar uninstalled: ${localArtifactDir.absolutePath}"
            //get 
            
            return    
        }
        
        
        //write contents to temp file
        File pomFile = File.createTempFile("vitalsigns", ".xml")
        pomFile.deleteOnExit()
        FileUtils.writeStringToFile(pomFile, pomFileContents, "UTF-8")
        
        StringBuilder sb = new StringBuilder()
        ProcessUtils.runProcess(vitalHome, [mvnCommand(), 'install:install-file', '"-Dfile=' + vitalsignsJar.absolutePath + '"', '"-DpomFile=' + pomFile.absolutePath + '"'], true, sb)
        
        println "vitalsigns jar installed"
        
    }
    
    static void installVitalDomain(File vitalHome, boolean installNoUninstall) {
        
        String version = VitalSigns.VERSION
        
        File vitalDomainJar = new File(vitalHome, "vital-domain/VitalDomain-groovy-${version}.jar")
        if(!vitalDomainJar.exists()) throw new Exception("VitalDomain jar not found: ${vitalDomainJar.absolutePath}")
        
        String pomFileContents = IOUtils.toString(VitalMavenDeployCommand.class.getResource("vital-domain.pom"))
        pomFileContents = pomFileContents.replace("__VERSION__", version)

        if(!installNoUninstall) {
            
            MavenArtifact ma = VitalJarMavenUtil.getPomArtifact(null, pomFileContents)
            
            File mavenRepo = VitalJarMavenUtil.getLocalMavenRepo()
            
            File localArtifactDir = VitalJarMavenUtil.getLocalArtifactDir(ma)
            
            if(!localArtifactDir.exists()) error("Directory not found: ${localArtifactDir.absolutePath}")
            //check if file exists
            
            FileUtils.deleteDirectory(localArtifactDir)
            
            println "vital-domain jar uninstalled: ${localArtifactDir.absolutePath}"
            //get
            
            return
            
        }
        
        File pomFile = File.createTempFile("vital-domain", ".xml")
        pomFile.deleteOnExit()
        FileUtils.writeStringToFile(pomFile, pomFileContents, "UTF-8")
        
        StringBuilder sb = new StringBuilder()
        ProcessUtils.runProcess(vitalHome, [mvnCommand(), 'install:install-file', '"-Dfile=' + vitalDomainJar.absolutePath + '"', '"-DpomFile=' + pomFile.absolutePath + '"'], true, sb)
        
        println "vital-domain jar installed"

        
    }

    static class DomainJarData {
        DomainJarModel djm
        File f
        DomainOntology _do
    }
    
    static FilenameFilter fnFilter = new FilenameFilter(){
        public boolean accept(File f, String n) {
            return n.endsWith(".jar")
        }
    }
    
    static void installDomainJars(File vitalHome, File singleDomainJar, File outputDir, boolean installNoUninstall) {
    
        File domainJarsDir = new File(vitalHome, "domain-groovy-jar")
        
        if(singleDomainJar != null) {
            if(!singleDomainJar.exists()) throw new Exception("Domain jar file not found: ${singleDomainJar.absolutePath}")

            File p1 = domainJarsDir.getCanonicalFile()
            File p2 = singleDomainJar.getParentFile().getCanonicalFile()
            
            if( ! p2.equals(p1) ) throw new Exception("Domain jar ${singleDomainJar.absolutePath} is not located in VITAL_HOME/domain-groovy-jar (${domainJarsDir.absolutePath})")
        }
        
        Map<String, DomainJarData> jarsMap = [:]
        Map<String, DomainJarData> domainsJarsMap = [:]
        
        for(File dj : domainJarsDir.listFiles(fnFilter)) {
    
            DomainJarModel m = DomainJarAnalyzer.analyzeDomainJar(dj)
            DomainOntology _do = OntologyProcessor.getOntologyMetaData(m.model);
            DomainJarData d = new DomainJarData(djm: m, f: dj.getCanonicalFile(), _do: _do)
            domainsJarsMap.put(m.ontologyURI, d)
            
        }
        
        jarsMap.putAll(domainsJarsMap)
        
        
        if(installNoUninstall) {
            

            //also load vital domains into map
            for(File vj : new File(vitalHome, "vital-domain").listFiles(fnFilter)) {
                
                DomainJarModel m = DomainJarAnalyzer.analyzeDomainJar(vj)
                DomainOntology _do = OntologyProcessor.getOntologyMetaData(m.model)
                DomainJarData d = new DomainJarData(djm: m, f: vj.getCanonicalFile(), _do: _do)
                jarsMap.put(m.ontologyURI ,d )
                
            }
        
        }
        
        
        int ok = 0
        int withIncompleteDeps = 0
        
        int c = 0
        
        for(DomainJarData d : domainsJarsMap.values()) {
            
            if(singleDomainJar != null && !d.f.equals(singleDomainJar.getCanonicalFile())) continue
            
            c++
            
            if(installNoUninstall && singleDomainJar == null) {
                println "Installing domain jar ${c} of ${domainsJarsMap.size()}: ${d.f.absolutePath}"
            } else if(!installNoUninstall && singleDomainJar == null) {
                println "Uninstalling domain jar ${c} of ${domainsJarsMap.size()}"
            }
            
            DomainOntology _do = d._do

            if(!_do.defaultArtifactId) {
                println "WARN: no defaultArtifactId annotation in domain: ${d.f.absolutePath} - ${d.djm.ontologyURI}"
                continue
            }
            
            if(!_do.defaultGroupId) {
                println "WARN: no defaultGroupId annotation in domain: ${d.f.absolutePath} - ${d.djm.ontologyURI}"
                continue
            }

            
            if(installNoUninstall) {
                  
                String dependenciesString = ''
                
                
                boolean hasIncompleteDeps = false
                
                for(String directParent : _do.parentOntologies) {
                    
                    String artifactId = null
                    String groupId = null
                    String version = null
    
                    DomainJarData parent = jarsMap.get(directParent)
                    if(parent == null) {
                        println "WARN: parent domain of ${d.djm.ontologyURI} not found: ${directParent}"
                        hasIncompleteDeps = true
                        continue
                    }
                
                    artifactId = parent._do.defaultArtifactId
                    if(!artifactId) {
                        println "WARN parent domain of ${d.djm.ontologyURI} does not have defaultArtifactId annotation"
                        hasIncompleteDeps = true
                    }
                    
                    groupId = parent._do.defaultGroupId
                    if(!groupId) {
                        println "WARN parent domain of ${d.djm.ontologyURI} does not have defautlGroupId annotation"
                        hasIncompleteDeps = true
                    }
                    
                    version = parent._do.toVersionString()    
                    
                    if(hasIncompleteDeps) {
                        withIncompleteDeps++
                        continue
                    }
                    
                    //static artifact
                    dependenciesString += """\

    <dependency>
      <groupId>${groupId}</groupId>
      <artifactId>${artifactId}</artifactId>
      <version>${version}</version>
      <scope>compile</scope>
    </dependency>

"""
                }
            
            
                String pomFileContents = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>${_do.defaultGroupId}</groupId>
  <artifactId>${_do.defaultArtifactId}</artifactId>
  <version>${_do.toVersionString()}</version>
  <description>POM was generated with ${VMD} command</description>
  
  <dependencies>
    ${dependenciesString}
  </dependencies>
  
</project>

"""
                
                MavenArtifact ma = new MavenArtifact(artifactId: _do.defaultArtifactId, groupId: _do.defaultGroupId, version: _do.toVersionString())
                LocalArtifactFiles localArtifactFiles = VitalJarMavenUtil.getLocalArtifactFiles(ma)
                File localPomFile = localArtifactFiles.pom
                boolean repoJarChanged = ! FileUtils.contentEquals(d.f, localArtifactFiles.jar)
                boolean repoPomChanged = ! localPomFile.exists() || ! FileUtils.readFileToString(localPomFile, "UTF-8").equals(pomFileContents)
                
                boolean repoJarOrPomChanged = repoJarChanged || repoPomChanged
                
                if(!repoJarOrPomChanged) {
                    if( outputDir == null ) {
                        println("domain jar and pom in local repo in sync - ${d.f.absolutePath} - nothing to be done")
                        continue
                    } else {
                        println("domain jar and pom in local repo in sync - ${d.f.absolutePath}")
                        ok--
                    }
                }
                
                JarFileInfo jfi = JarFileInfo.fromString(d.f.name) 
                
                File pomFile = null
                
                if(outputDir != null) {
                    pomFile = new File(outputDir, "${jfi.domain}-groovy-${jfi.toVersionNumberString()}-pom.xml")
                } else {
                    if(repoJarOrPomChanged) {
                        pomFile = File.createTempFile("${jfi.domain}-groovy-${jfi.toVersionNumberString()}-pom", ".xml")
                        pomFile.deleteOnExit()
                    }
                }
                
                if(pomFile != null) {
                    FileUtils.writeStringToFile(pomFile, pomFileContents, "UTF-8")
                }
                
                if(repoJarOrPomChanged) {
                    StringBuilder sb = new StringBuilder();
                    ProcessUtils.runProcess(vitalHome, [mvnCommand(), 'install:install-file', '"-Dfile=' + d.f.absolutePath + '"', '"-DpomFile=' + pomFile.absolutePath + '"'], true, sb)
                }
                
            } else {
            
                //uninstall
            
                MavenArtifact ma = new MavenArtifact(artifactId: _do.defaultArtifactId, groupId: _do.defaultGroupId, version: _do.toVersionString())
                
                File localArtifactDir = VitalJarMavenUtil.getLocalArtifactDir(ma)
                
                if(!localArtifactDir.exists()) { 
                    println("NOT INSTALLED: ${ma.toMavenArtifact()}")
                    continue
                }
                //check if file exists
                
                try {
                    
                    FileUtils.deleteDirectory(localArtifactDir)
                    
                } catch(Exception e) {
                    System.err.println(e.localizedMessage)
                    continue
                }
                
                println "domain jar uninstalled: ${localArtifactDir.absolutePath}"
                //get
                
            }
            
            ok ++
                        
        }
        
        if(installNoUninstall) {
            
            if(singleDomainJar != null) {
                
                if(ok > 0) {
                    if(withIncompleteDeps == 0) {
                        println "Domain jar installed successully: ${singleDomainJar.absolutePath}"
                    } else {
                        println "Domain jar installed but with missing dependencies: ${singleDomainJar.absolutePath}"
                    }
                } else {
                    println "Domain jar NOT installed"
                }
                
            } else {
                int skipped = domainsJarsMap.size() - ok
                        
                println "Domains installed: ${ok} ${withIncompleteDeps > 0 ? '(' + withIncompleteDeps + ' with incomplete dependencies)': ''}, ${skipped} skipped"
            }
            
        } else {
        
            if(singleDomainJar == null) {
                
                int skipped = domainsJarsMap.size() - ok
                
                println "Domains uninstalled: ${ok}, ${skipped} skipped"
                
            }    
        
        }
        
        
        
        
    }
    
    static Pattern pattern = Pattern.compile("META-INF/maven/([^/]+)/([^/]+)/pom.xml")
    
    static void installVDKModules(File vitalHome, File modFile, boolean installNoUninstall) {
        
        List<File> mainJarFiles = []
        
        if(modFile != null) {
        
            if(!modFile.exists()) error("File not found: ${modFile.absolutePath}")
            
            if(!fnFilter.accept(modFile.getParentFile(), modFile.getName())) error("File not a jar: ${modFile.absolutePath}")
            
            File parentFile = modFile.getParentFile()
            
            boolean ok = false
            
            int c = 0
            
            while(parentFile != null && c < 128) {
                
                c++
                
                if(parentFile.equals(vitalHome)) {
                    ok = true
                    break
                }
                
                parentFile = parentFile.getParentFile()

            }
            
            if(!ok) {
                error("Jar: ${modFile.absolutePath} is not located in \$VITAL_HOME: ${vitalHome.absolutePath}")
            }
            
            mainJarFiles.add(modFile)
            
            //find parent
                
        } else {
        
            for(File d : vitalHome.listFiles() ) {
                
                if(!d.isDirectory()) continue
                
                if(d.name == 'vitalsigns' || d.name == 'vital-domain' || d.name.startsWith('domain-groovy-jar')) continue
                
                for(File jarFile : d.listFiles(fnFilter)) {
                    
                    mainJarFiles.add(jarFile)
                }
                
                File commandDir = new File(d, 'command')
                if(commandDir.isDirectory()) {
                    for(File jarFile: commandDir.listFiles(fnFilter)) {
                        mainJarFiles.add(jarFile)
                    }
                }
                
            }
               
        }         
                
        for(File jarFile : mainJarFiles) {
            
            JarFile jf = new JarFile(jarFile)
            Manifest manifest = jf.getManifest()
            if(manifest == null) {
                if(modFile != null) error("No manifest found in file: ${modFile.absolutePath}")
                continue
            }
            Attributes attributes = manifest.getMainAttributes()
            String moduleName = attributes != null ? attributes.getValue(VitalManifest.VITAL_MODULE_NAME) : null
            if(!moduleName) {
                if(modFile != null) error("No ${VitalManifest.VITAL_MODULE_NAME} manifest attribute in file: ${modFile.absolutePath}")
                continue
            }
            
            String moduleVersion = attributes != null ? attributes.getValue(VitalManifest.VITAL_MODULE_VERSION) : null
            
            print "${installNoUninstall ? 'I' : 'Uni'}nstalling module: ${moduleName} ${moduleVersion} ... "
            
            String pomContents = null
            
            for( JarEntry entry : jf.entries() ) {
                
                String n = entry.getName()
                
                Matcher matcher = pattern.matcher(n)
                
                if( ! matcher.matches()) continue
                
                if(pomContents != null) {
                    
                    println "ERROR"
                    System.err.println("More than 1 pom file detected in jar: ${jarFile.absolutePath}")
                    pomContents = null
                    break
                                            
                }
                
                InputStream inputStream = null
                try {
                    inputStream = jf.getInputStream(entry)
                    pomContents = IOUtils.toString(inputStream)
                } catch(Exception e) {
                    println "ERROR"
                    System.err.println(e.localizedMessage)
                    pomContents = null
                    break
                } finally {
                    IOUtils.closeQuietly(inputStream)
                }
                
            }
            
            if(pomContents) {

                MavenArtifact ma = VitalJarMavenUtil.getPomArtifact(jarFile, pomContents)
                
                if(installNoUninstall) {
                    
                    File tempPomFile = File.createTempFile(moduleName + "-pom", ".xml")
                    tempPomFile.deleteOnExit()
                    
                    //install from temp file
                    FileUtils.writeStringToFile(tempPomFile, pomContents, 'UTF-8')
    
                    StringBuilder sb = new StringBuilder()
                    ProcessUtils.runProcess(vitalHome, [mvnCommand(), 'install:install-file', '"-Dfile=' + jarFile.absolutePath + '"', '"-DpomFile=' + tempPomFile.absolutePath + '"'], true, sb)

                    println "OK"
                    
                } else {
                
                
                    File mavenRepo = VitalJarMavenUtil.getLocalMavenRepo()
                    
                    File localArtifactDir = VitalJarMavenUtil.getLocalArtifactDir(ma)
                    
                    if(!localArtifactDir.exists()) {
                        println("NOT INSTALLED: ${ma.toMavenArtifact()}")
                        continue
                    }
                    //check if file exists
                    try {
                        FileUtils.deleteDirectory(localArtifactDir)
                    } catch(Exception e) {
                        println "ERROR"
                        System.err.println(e.localizedMessage)
                        continue
                    }
                    
                    println "OK"
                    //get
                
                
                }
                                    
            } else {
                
                println "NO POM FILE IN JAR"
            
            }
            
            //check if pom exists
            
        }
        
    }
    
    static void error(String m) {
        System.err.println(m);
        System.exit(-1);
    }
}
