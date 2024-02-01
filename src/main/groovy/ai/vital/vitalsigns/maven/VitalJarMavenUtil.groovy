package ai.vital.vitalsigns.maven

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.apache.commons.io.FileUtils
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

import ai.vital.vitalsigns.maven.VitalJarMavenUtil.MavenArtifact

import org.w3c.dom.Node;

/**
 * A simple utility that creates jar pom resources for projects that are not maven projects yet
 *
 */

class VitalJarMavenUtil {

    static class MavenArtifact {
        
        String artifactId
        
        String groupId
        
        String version
        
        public String toMavenArtifact() {
            return "${groupId}:${artifactId}:${version}"
        }
        
    }
    
    static File getLocalMavenRepo() {
        
        File mavenLocalRepo = new File(System.getProperty("user.home"), '.m2/repository')

        if(!mavenLocalRepo.exists()) throw new Exception("Local maven repo not found: ${mavenLocalRepo.absolutePath}")
        if(!mavenLocalRepo.isDirectory()) throw new Exception("Local maven repo not a directory: ${mavenLocalRepo.absolutePath}")
                
        return mavenLocalRepo
    }
    
    static File getLocalArtifactDir(MavenArtifact ma) {
        
        File localRepo = getLocalMavenRepo()
        
        File dir = new File(localRepo, "${ma.groupId.replace('.', '/')}/${ma.artifactId}/${ma.version}") 
        
        if(dir.exists() && !dir.isDirectory()) throw new Exception("Not a directory: ${dir.absolutePath}")
        
        return dir
        
    }
    
    static class LocalArtifactFiles {
        File jar
        File pom
    }
    
    static LocalArtifactFiles getLocalArtifactFiles(MavenArtifact ma) {

        File localRepo = getLocalMavenRepo()
        
        File dir = new File(localRepo, "${ma.groupId.replace('.', '/')}/${ma.artifactId}/${ma.version}")
        
        File jar = new File(dir, "${ma.artifactId}-${ma.version}.jar")
        
        if(jar.exists() && !jar.isFile()) throw new Exception("Artifact jar path not a file: ${jar.absolutePath}")
        
        File pom = new File(dir, "${ma.artifactId}-${ma.version}.pom")
        if(pom.exists() && !pom.isFile()) throw new Exception("Artifact pom path not a file: ${pom.absolutePath}")
        
        return new LocalArtifactFiles(jar: jar, pom: pom)
        
    }
    
    public static MavenArtifact getPomArtifact(File inputPomFile, String pom) {
        
        String targetMsg = inputPomFile != null ? inputPomFile.absolutePath : "(source pom string)"
        
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = docBuilderFactory.newDocumentBuilder();

        Document doc = documentBuilder.parse(new ByteArrayInputStream(pom.getBytes('UTF-8')));
        
        Element projectEl = doc.getDocumentElement()
        
        if( projectEl.getTagName() != 'project' ) throw new Exception("Not a pom file: ${targetMsg}")
        
        NodeList nl = projectEl.getChildNodes()
        
        String groupId = null
        String artifactId = null
        String version = null
        
        for(int i = 0 ; i < nl.getLength(); i++) {
            Node n = nl.item(i)
            if(!(n instanceof Element)) continue
            Element el = n
            String tn = el.getTagName()
            if(tn == 'groupId') {
                groupId = el.getTextContent()
            } else if(tn == 'artifactId') {
                artifactId = el.getTextContent()
            } else if(tn == 'version') {
                version = el.getTextContent()
            }
        }
        
        if(!groupId) throw new Exception("No project groupId in ${targetMsg}")
        if(!artifactId) throw new Exception("No project artifactId in ${targetMsg}")
        if(!version) throw new Exception("No project version in ${targetMsg}")
     
        return new MavenArtifact(artifactId: artifactId, groupId: groupId, version: version)   
    }
    
    def static main(args) {
        
        if(args.length != 3) {
            System.err.println "usage: VitalMavenUtils <inputPomFile> <version> <outputDirectory>"
            System.exit(1)
            return
        }
        
        File inputPomFile = new File(args[0])
   
        String version = args[1]
        
        File outputDirectory = new File(args[2])     
        
        
        String pom = FileUtils.readFileToString(inputPomFile, 'UTF-8')
        
        pom = pom.replace("__VERSION__", version)
        
        MavenArtifact artifact = getPomArtifact(inputPomFile, pom)
        
        String groupId = artifact.groupId
        String artifactId = artifact.artifactId
        
        File metaInfMavenDir = new File(outputDirectory, "META-INF/maven/${groupId}/${artifactId}")
        metaInfMavenDir.mkdirs()
        
        File outputPomPropertiesFile = new File(metaInfMavenDir, "pom.properties")
        FileUtils.writeStringToFile(outputPomPropertiesFile, """\
#Generated by vital software
#${new Date().toString()}
version=${version}
groupId=${groupId}
artifactId=${artifactId}

""", 'UTF-8')
        
        File outputPomFile = new File(metaInfMavenDir, "pom.xml")
        FileUtils.writeStringToFile(outputPomFile, pom, 'UTF-8')
        
    }
    
}
