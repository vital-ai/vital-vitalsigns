package ai.vital.vitalsigns.ontology;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class DomainJarAnalyzer {

    public static class DomainJarModel {
        
        public String ontologyURI;
        
        public Model model;
        
        public String md5Hash;
        
    }
    
    public static DomainJarModel analyzeDomainJar(File domainJar) throws Exception {
        
        FileInputStream fis = null;
        
        try {
            fis = new FileInputStream(domainJar);
            return analyzeDomainJar(fis);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }
    
    public static DomainJarModel analyzeDomainJar(InputStream inputStream) throws Exception {
        
        DomainJarModel domainJarModel = new DomainJarModel();
        
        ZipInputStream zis = null;
        
        Model model = null;
        
        String md5Hash = null;
        
        try {
            zis = new ZipInputStream(inputStream);
            
            ZipEntry next = null;
            
            
            while( ( next = zis.getNextEntry() ) != null ) {
                
                String name = next.getName();
                
                if(name.endsWith(".owl")) {
                    
                    if(model != null) throw new RuntimeException("More than 1 owl file found in domain jar");
                    model = ModelFactory.createDefaultModel();
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    IOUtils.copy(zis, os);
                    
                    byte[] owlBytes = os.toByteArray();
                    
                    md5Hash = DigestUtils.md5Hex(owlBytes);
                    
                    model.read(new ByteArrayInputStream(owlBytes), null);
                    
                }
                
            }
            
        } finally {
            IOUtils.closeQuietly(zis);
            IOUtils.closeQuietly(inputStream);
        }
        
        if(model == null) throw new RuntimeException("No owl file found in domain jar");
        
       Resource ontologyRes = null;
        
        for( ResIterator resIter = model.listSubjectsWithProperty(RDF.type, OWL.Ontology); resIter.hasNext(); ) {
            
            if(ontologyRes != null) throw new RuntimeException("More than 1 ontology found in source model!");
            
            Resource o = resIter.next();
            
            ontologyRes = o;
            
        }
        
        if(ontologyRes == null) throw new Exception("No ontology resource found in owl file");
        if(!ontologyRes.isURIResource()) throw new Exception("Ontology is not a URI resource");
        
        domainJarModel.model = model;
        
        domainJarModel.md5Hash = md5Hash;
        
        domainJarModel.ontologyURI = ontologyRes.getURI();
        
        return domainJarModel;
        
    }
    
}
