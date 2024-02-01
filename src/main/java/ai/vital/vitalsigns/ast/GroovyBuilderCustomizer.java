package ai.vital.vitalsigns.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ImportNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.classes.ClassesRegistry;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;

/**
 * A customizer that collects all referenced domains as well as other classes from the input builder code
 */
public class GroovyBuilderCustomizer extends CompilationCustomizer {

    private final static Logger log = LoggerFactory.getLogger(GroovyBuilderCustomizer.class);
    
    ClassLoader cl;
    
    public GroovyBuilderCustomizer(/*ClassLoader cl*/) {
        super(CompilePhase.SEMANTIC_ANALYSIS);
//        this.cl = cl;
    }
    
    /*
    Set<String> notFound = new HashSet<String>();
    
    //strings end with dots 
    Set<String> starPackages = new HashSet<String>();

    Set<String> alreadyResolved = new LinkedHashSet<String>();
    */
    
    private List<String> domainsList;
    
    private List<String> otherClassesList;
    
    @Override
    public void call(SourceUnit su, GeneratorContext arg1, ClassNode arg2) throws CompilationFailedException {
        
        Set<String> allClasses = new HashSet<String>();
        
        ModuleNode ast = su.getAST();
        
        for(ImportNode iNode : ast.getStarImports()) {
            
//            String pkg = iNode.getPackageName();
//            log.debug("Star import : " + pkg);
//            starPackages.add(pkg);
            
        }    
            
        for(ImportNode iNode : ast.getImports()) {
            
            String cls = iNode.getClassName();
            
            log.debug("Checking import : " + cls);
            
            allClasses.add(cls);
            
//            checkClass(cls);
            
        }
        
        for(ImportNode staticImport : ast.getStaticImports().values()) {
            
            String cls = staticImport.getClassName();
            
            log.debug("Checking static import : " + cls);

//            checkClass(cls);
            
            allClasses.add(cls);
            
        }
        
        for(ImportNode staticStarImport : ast.getStaticStarImports().values()) {
            
            String cls = staticStarImport.getClassName();
            
            log.debug("Checking static import : " + cls);

//            checkClass(cls);
            allClasses.add(cls);
            
        }
        
        GroovyBuilderCodeVisitor groovyBuilderCodeVisitor = new GroovyBuilderCodeVisitor();
        
        BlockStatement sb = ast.getStatementBlock();
        sb.visit(groovyBuilderCodeVisitor);
        
        allClasses.addAll( groovyBuilderCodeVisitor.getTypes() );
        
        log.debug("All referenced types: {}", allClasses.size());
        
        ClassesRegistry cr = VitalSigns.get().getClassesRegistry();
        
        Map<String, String> ontologyURI2Package = VitalSigns.get().getOntologyURI2Package();
        
        Set<String> domainsUris = new HashSet<String>();
        
        Set<String> otherClasses = new HashSet<String>();
        
        for(String t : allClasses) {
            
            ClassMetadata cm = cr.getClassMetadata(t);
            
            if(cm == null) {
                otherClasses.add(t);
                continue;
            }
            
            
            Package pkg = cm.getClazz().getPackage();
            String pkgName = null;
            
            if(pkg != null) {
                
                pkgName = pkg.getName();
                
            } else {
                pkgName = cm.getClazz().getCanonicalName(); 
                pkgName = pkgName.substring(0, pkgName.lastIndexOf('.')); 
                
            }
            
            for( Entry<String, String> entry : ontologyURI2Package.entrySet() ) {
                
                if(pkgName.equals( entry.getValue() ) ) {
                    
                    domainsUris.add(entry.getKey());
                    
                }
                
            }
            
        }
        
        for(ImportNode iNode : ast.getStarImports()) {
            
          String pkgName = iNode.getPackageName();
          
          if(pkgName.endsWith(".")) pkgName = pkgName.substring(0, pkgName.length() - 1);
          
          log.debug("Star import : " + pkgName);
          
          for( Entry<String, String> entry : ontologyURI2Package.entrySet() ) {
              
              if(pkgName.equals( entry.getValue() ) ) {
                  
                  domainsUris.add(entry.getKey());
                  
              }
              
          }
          
          
      }    
        
        domainsUris.remove(VitalCoreOntology.ONTOLOGY_IRI);
        domainsUris.remove("http://vital.ai/ontology/vital");
        
        domainsList = new ArrayList<String>(domainsUris);
        Collections.sort(domainsList);
        otherClassesList = new ArrayList<String>(otherClasses);
        Collections.sort(otherClassesList);
        
    }

    public List<String> getDomainsList() {
        if(domainsList == null) throw new RuntimeException("Groovy builder customizer was never called.");
        return domainsList;
    }

    public List<String> getOtherClassesList() {
        if(otherClassesList == null) throw new RuntimeException("Groovy builder customizer was never called.");
        return otherClassesList;
    }
    
    

    /*
    private void checkClass(String cls) {

        if(alreadyResolved.contains(cls)) return;
        
        try {
            cl.loadClass(cls);
            alreadyResolved.add(cls);
            return;
        } catch(ClassNotFoundException e) {
        }
        
        for(String sp : starPackages) {
            String nc = sp + cls;
        }
        
        
    }
    */
    
    
    
}
