package ai.vital.vitalsigns.command;

import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.List
import java.util.Set

import org.apache.commons.io.IOUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import ai.vital.vitalsigns.command.patterns.JarFileInfo;
import ai.vital.vitalsigns.command.patterns.JsonSchemaFileInfo;
import ai.vital.vitalsigns.command.patterns.OwlFileInfo;
import ai.vital.vitalsigns.model.DomainOntology;
import ai.vital.vitalsigns.ontology.OntologyProcessor;
import ai.vital.vitalsigns.rdf.RDFUtils;

public class VitalSignsCommandHelper {

    static void o(Object o){ println(o) }
    
    public static void printOntologies(File vitalHome) throws Exception {
        
        File domainOntologiesDir = new File(vitalHome, "domain-ontology")
        
        File domainJarsDir = new File(vitalHome, "domain-groovy-jar")
        
        File domainJsonDir = new File(vitalHome, "domain-json-schema")
        
        if(!domainOntologiesDir.isDirectory()) throw new RuntimeException("domain-ontology does not exist or not a directory: " + domainOntologiesDir.getAbsolutePath());
        if(!domainJarsDir.isDirectory()) throw new RuntimeException("domain-groovy-jar does not exist or not a directory: " + domainJarsDir.getAbsolutePath());
        if(!domainJsonDir.isDirectory()) throw new RuntimeException("domain-json-schema does not exist or not a directory: " + domainJsonDir.getAbsolutePath());
     
        File[] files = domainOntologiesDir.listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File f) {
                return f.isFile() && f.getName().endsWith(".owl")
            }
            
        })
        
        List<File> l = new ArrayList<File>(Arrays.asList(files))
        
        Collections.sort(l)
        
        o("Domain ontologies count: " + l.size())

        int i = 1
        
        for(File o : l) {
            
            FileInputStream fis = null

            o(i + ". Domain ontology file: " + o.getAbsolutePath())
            
            OwlFileInfo ofi = OwlFileInfo.fromString(o.getName())
            
            JarFileInfo jfi = JarFileInfo.fromOwlInfo(ofi)
            
            File groovyJar = new File(domainJarsDir, jfi.toFileName())
            
            o("Domain jar file " + ( groovyJar.exists() ? "present" : " does not exist" ) + " - " + groovyJar.getAbsolutePath())
            
            JsonSchemaFileInfo jsfi = JsonSchemaFileInfo.fromOwlInfo(ofi)
            
            File json = new File(domainJsonDir, jsfi.toFileName())
            
            o("Domain json schema file " + ( json.exists() ? "present" : " does not exist" ) + " - " + json.getAbsolutePath())
            
            
            Model m = ModelFactory.createDefaultModel()

            
            try {
                
                fis = new FileInputStream(o)
                
                m.read(fis, null)
                
            } finally {
                IOUtils.closeQuietly(fis)
            }
            
            
            DomainOntology d = OntologyProcessor.getOntologyMetaData(m)
            
            o("URI:" + d.getUri())

            o("version: " + d.toVersionString())
            
            if(d.getBackwardCompatibleVersion() != null) {
                o("backward compatible version: " + d.getBackwardCompatibleVersion().toVersionString())
            } else {
                o("(no backward compatible version set)")
            }
            
            if(d.getDefaultPackage() != null) {
                o("default package: " + d.getDefaultPackage())
            } else {
                o("(no default package)")
            }

            Set<String> parentOntologies = d.getParentOntologies()

            if(parentOntologies == null) parentOntologies = new HashSet<String>()
            
            o("parent ontologies: " + parentOntologies.size())
            
            Set<String> preferredImportVersions = d.getPreferredImportVersions()
            
            for(String p : parentOntologies) {
            
                String preferredVersion = null
                
                String app = RDFUtils.getLocalURIPart(p)
                
                if(app.endsWith(".owl")) app = app.substring(0, app.length() - 4)
                
                if(preferredImportVersions != null) {
                    
                    for(String pv : preferredImportVersions) {
                        
                        OwlFileInfo pfi = OwlFileInfo.fromString(pv)
                        
                        if(pfi.getDomain().equals(app)) {
                            preferredVersion = pfi.toVersionNumberString()
                        }
                        
                        
                    }
                    
                }
                
                o("\t" + p + " , preferred version: " + (preferredVersion != null ? preferredVersion : "none"))
                
                
            }

            
            o("")
            
            i++
            
        }
    }
}
