package ai.vital.vitalsigns.command;

import java.io.File;

import org.apache.commons.io.FileUtils;

import ai.vital.vitalsigns.command.patterns.JarFileInfo;
import ai.vital.vitalsigns.command.patterns.JsonSchemaFileInfo;
import ai.vital.vitalsigns.command.patterns.OwlFileInfo;
import ai.vital.vitalsigns.utils.StringUtils;

public class DeployUndeployCommand {

    private static class ImplData {

//        File domainGroovyJarDir;
//        File domainJsonSchemaDir;
//        File domainOntologyDir;

        File deployedGroovyJarDir;
        File deployedJsonSchemaDir;
        File deployedOntologyDir;
        
        File inputJar = null;
        File inputJsonSchema = null;
        File inputOntology = null;
     
        File targetJar = null;
        File targetJson = null;
        File targetOntology = null;
        
    }
    
    static void o(String m) { System.out.println(m); }
    
    public static void deploy(File vitalHome, String organizationID, String appID, String domainOntology, String domainJar, String domainJsonSchema) throws Exception {
        
        checkInput(domainOntology, domainJar, domainJsonSchema);

        ImplData d = getState(vitalHome, organizationID, appID, domainOntology, domainJar, domainJsonSchema);
        
        if(d.targetJar != null) {
            d.targetJar.getParentFile().mkdirs();
            o("Copying " + d.inputJar.getAbsolutePath() + " -> " + d.targetJar.getAbsolutePath());
            FileUtils.copyFile(d.inputJar, d.targetJar);
        } else {
            o("No jar to deploy");
        }
        
        
        if(d.targetOntology != null) {
            d.targetOntology.getParentFile().mkdirs();
            o("Copying " + d.inputOntology.getAbsolutePath() + " -> " + d.targetOntology.getAbsolutePath());
            FileUtils.copyFile(d.inputOntology, d.targetOntology);
        } else {
            o("No ontology to deploy");
        }
        
        if(d.targetJson != null) {
            d.targetJson.getParentFile().mkdirs();
            o("Copying " + d.inputJsonSchema.getAbsolutePath() + " -> " + d.targetJson.getAbsolutePath());
            FileUtils.copyFile(d.inputJsonSchema, d.targetJson);
        } else {
            o("No json to deploy");
        }
        
    }
    
    private static ImplData getState(File vitalHome, String organizationID, String appID, String domainOntology, String domainJar, String domainJsonSchema) throws Exception {
        
        File domainGroovyJarDir = new File(vitalHome, "domain-groovy-jar");
        File domainJsonSchemaDir = new File(vitalHome, "domain-json-schema");
        File domainOntologyDir = new File(vitalHome, "domain-ontology");
        
        File deployedGroovyJarDir = new File(vitalHome, "domain-groovy-jar-deployed");
        File deployedJsonSchemaDir = new File(vitalHome, "domain-json-schema-deployed");
        File deployedOntologyDir = new File(vitalHome, "domain-ontology-deployed");
        
        File inputJar = null;
        File inputJsonSchema = null;
        File inputOntology = null;
        
        if(!StringUtils.isEmpty(domainJar)) {
            
            inputJar = new File(domainJar);
            
            if(!inputJar.isFile()) throw new Exception("Input domain groovy jar path does not exist or not a file: " + inputJar.getAbsolutePath());
            
            if(!inputJar.getParentFile().equals(domainGroovyJarDir)) throw new Exception("Input domain groovy jar is not located in development directory: " + domainGroovyJarDir.getAbsolutePath() + " - " + inputJar.getAbsolutePath());

            JarFileInfo jfi = JarFileInfo.fromString(inputJar.getName());
            
            inputOntology = new File(domainOntologyDir, OwlFileInfo.fromJarInfo(jfi).toFileName());
            
            inputJsonSchema = new File(domainJsonSchemaDir, JsonSchemaFileInfo.fromJarInfo(jfi).toFileName());
            
        }
        
        if(!StringUtils.isEmpty(domainOntology)) {
            
            inputOntology = new File(domainOntology);
            
            if(!inputOntology.isFile()) throw new Exception("Input domain ontology path does not exist or not a file: " + inputOntology.getAbsolutePath());
            
            if(!inputOntology.getParentFile().equals(domainOntologyDir)) throw new Exception("Input domain ontology is not located in development directory: " + domainOntologyDir.getAbsolutePath() + " - " + inputOntology.getAbsolutePath());
         
            OwlFileInfo ofi = OwlFileInfo.fromString(inputOntology.getName());
            
            inputJar = new File(domainGroovyJarDir, JarFileInfo.fromOwlInfo(ofi).toFileName());
            
            inputJsonSchema = new File(domainJsonSchemaDir, JsonSchemaFileInfo.fromOwlInfo(ofi).toFileName());
            
        }
        
        if(!StringUtils.isEmpty(domainJsonSchema)) {
            
            inputJsonSchema = new File(domainJsonSchema);
            
            if(!inputJsonSchema.isFile()) throw new Exception("Input json schema path does not exist or not a file: " + inputJsonSchema.getAbsolutePath());
            
            if(!inputJsonSchema.getParentFile().equals(domainJsonSchemaDir)) throw new Exception("Input domain json schema is not located in development directory: " + domainJsonSchemaDir.getAbsolutePath() + " - " + inputJsonSchema.getAbsolutePath());
            
            JsonSchemaFileInfo jfi = JsonSchemaFileInfo.fromString(inputJsonSchema.getName());
            
            inputJar = new File(domainGroovyJarDir, JarFileInfo.fromJsonSchemaInfo(jfi).toFileName());
            
            inputOntology = new File(domainOntologyDir, OwlFileInfo.fromJsonSchemaInfo(jfi).toFileName());
            
            
        }
        
        File targetJar = null;
        File targetJson = null;
        File targetOntology = null;
        
        if(inputJar.exists()) {

            JarFileInfo jfi = JarFileInfo.fromString(inputJar.getName());
            
            //check if already installed under different version ?
            for(File f : FileUtils.listFiles(deployedGroovyJarDir, new String[]{"jar"}, true)) {
                
                JarFileInfo j = JarFileInfo.fromString(f.getName());
                
                if(j.getDomain().equals(jfi.getDomain())) {
                    
                    String a = f.getParentFile().getName();
                    String o = f.getParentFile().getParentFile().getName();

                    if( ! o.equals(organizationID) ) throw new Exception("Domain jar already deployed in different organization '" + o + "' - " + f.getAbsolutePath());
                    
                    if( ! a.equals(appID) ) throw new Exception("Domain jar already deployed in different app '" + a + "' - " + f.getAbsolutePath() );
                    
                    if(j.compareTo(jfi) != 0) throw new Exception("Existing domain jar with different version already deployed, undeploy it first: " + f.getAbsolutePath());
                    
                    checkUserEmail(jfi.getUseremail(), j.getUseremail());
                    
                }
                
            }
            
            targetJar = new File(deployedGroovyJarDir, organizationID + "/" + appID + "/" + jfi.toFileName());
            
        }
        
        if(inputOntology.exists()) {
            
            OwlFileInfo ofi = OwlFileInfo.fromString(inputOntology.getName());
            
            for(File f : FileUtils.listFiles(deployedOntologyDir, new String[]{"owl"}, true)) {
                
                OwlFileInfo _o = OwlFileInfo.fromString(f.getName());
                
                if(_o.getDomain().equals(ofi.getDomain())) {
                    
                    String a = f.getParentFile().getName();
                    String o = f.getParentFile().getParentFile().getName();
                 
                    if( ! o.equals(organizationID) ) throw new Exception("Domain owl already deployed in different organization '" + o + "' - " + f.getAbsolutePath());
                    
                    if( ! a.equals(appID) ) throw new Exception("Domain owl already deployed in different app '" + a + "' - " + f.getAbsolutePath() );
                    
                    if(_o.compareTo(ofi) != 0) throw new Exception("Existing domain owl with different version already deployed, undeploy it first: " + f.getAbsolutePath());
                    
                    checkUserEmail(ofi.getUseremail(), _o.getUseremail());
                    
                }
                
            }
            
            targetOntology = new File(deployedOntologyDir, organizationID + "/" + appID + "/" + ofi.toFileName());
            
        }
        
        if(inputJsonSchema.exists()) {
            
            JsonSchemaFileInfo jfi = JsonSchemaFileInfo.fromString(inputJsonSchema.getName());
            
            //check if already installed under different version ?
            for(File f : FileUtils.listFiles(deployedJsonSchemaDir, new String[]{"js"}, true)) {
                
                JsonSchemaFileInfo j = JsonSchemaFileInfo.fromString(f.getName());
                
                if(j.getDomain().equals(jfi.getDomain())) {
                    
                    String a = f.getParentFile().getName();
                    String o = f.getParentFile().getParentFile().getName();

                    if( ! o.equals(organizationID) ) throw new Exception("Domain json schema already deployed in different organization '" + o + "' - " + f.getAbsolutePath());
                    
                    if( ! a.equals(appID) ) throw new Exception("Domain json schema already deployed in different app '" + a + "' - " + f.getAbsolutePath() );
                    
                    if(j.compareTo(jfi) != 0) throw new Exception("Existing domain json schema with different version already deployed, undeploy it first: " + f.getAbsolutePath());
                    
                    checkUserEmail(jfi.getUseremail(), j.getUseremail());
                    
                }
                
            }
            
            targetJson = new File(deployedJsonSchemaDir, organizationID + "/" + appID + "/" + jfi.toFileName());
            
        }
        
        
        ImplData d = new ImplData();
        
//        d.domainGroovyJarDir = domainGroovyJarDir;
//        d.domainJsonSchemaDir = domainJsonSchemaDir;
//        d.domainOntologyDir = domainOntologyDir;
            
        d.deployedGroovyJarDir = deployedGroovyJarDir;
        d.deployedJsonSchemaDir = deployedJsonSchemaDir;
        d.deployedOntologyDir = deployedOntologyDir;
            
        d.inputJar = inputJar;
        d.inputJsonSchema = inputJsonSchema;
        d.inputOntology = inputOntology;
         
        d.targetJar = targetJar;
        d.targetJson = targetJson;
        d.targetOntology = targetOntology;

        return d;
        
    }
    
    public static void undeploy(File vitalHome, String domainOntology, String domainJar, String domainJsonSchema) throws Exception {
        
        
        checkInput(domainOntology, domainJar, domainJsonSchema);
        
        File deployedGroovyJarDir = new File(vitalHome, "domain-groovy-jar-deployed");
        File deployedJsonSchemaDir = new File(vitalHome, "domain-json-schema-deployed");
        File deployedOntologyDir = new File(vitalHome, "domain-ontology-deployed");
        
        File targetOntology = null;
        
        File targetJson = null;
        
        File targetJar = null;
        
        if(!StringUtils.isEmpty(domainOntology)) {
            
            targetOntology = new File(domainOntology);

            String app = targetOntology.getParentFile().getName();
            
            String org = targetOntology.getParentFile().getParentFile().getName();
            
            if( ! targetOntology.getParentFile().getParentFile().getParentFile().equals(deployedOntologyDir) ) throw new Exception("domain ontology file must be located in deployed ontologies/org/app directory: " + deployedOntologyDir.getAbsolutePath());

            if(!targetOntology.exists()) throw new Exception("Deployed domain ontology file not found: " + targetOntology.getAbsolutePath());
            
            OwlFileInfo ofi = OwlFileInfo.fromString(targetOntology.getName());
            
            targetJson = new File(deployedJsonSchemaDir, org + "/" + app + "/" + JsonSchemaFileInfo.fromOwlInfo(ofi).toFileName());
            
            targetJar = new File(deployedGroovyJarDir, org + "/" + app + "/" + JarFileInfo.fromOwlInfo(ofi).toFileName());
            
        }
        
        
        if(!StringUtils.isEmpty(domainJar)) {
         
            
            targetJar = new File(domainJar);

            String app = targetJar.getParentFile().getName();
            
            String org = targetOntology.getParentFile().getParentFile().getName();
            
            if( ! targetJar.getParentFile().getParentFile().getParentFile().equals(deployedGroovyJarDir) ) throw new Exception("domain jar file must be located in deployed jars/org/app directory: " + deployedGroovyJarDir.getAbsolutePath());

            if(!targetJar.exists()) throw new Exception("Deployed domain jar file not found: " + targetJar.getAbsolutePath());
            
            JarFileInfo jfi = JarFileInfo.fromString(targetJar.getName());
            
            targetJson = new File(deployedJsonSchemaDir, org + "/" + app + "/" + JsonSchemaFileInfo.fromJarInfo(jfi).toFileName());
            
            targetOntology = new File(deployedOntologyDir, org + "/" + app + "/" + OwlFileInfo.fromJarInfo(jfi).toFileName());
            
        }
        
        
        if(!StringUtils.isEmpty(domainJsonSchema)) {
            
            
            targetJson= new File(domainJsonSchema);
            
            String app = targetJson.getParentFile().getName();
            
            String org = targetOntology.getParentFile().getParentFile().getName();
            
            if( ! targetJson.getParentFile().getParentFile().getParentFile().equals(deployedJsonSchemaDir) ) throw new Exception("domain json schema file must be located in deployed json schemas/org/app directory: " + deployedJsonSchemaDir.getAbsolutePath());
            
            if(!targetJson.exists()) throw new Exception("Deployed domain json schema file not found: " + targetJson.getAbsolutePath());
            
            JsonSchemaFileInfo jfi = JsonSchemaFileInfo.fromString(targetJson.getName());
            
            targetJar = new File(deployedGroovyJarDir, org + "/" + app + "/" + JarFileInfo.fromJsonSchemaInfo(jfi).toFileName());
            
            targetOntology = new File(deployedOntologyDir, org + "/" + app + "/" + OwlFileInfo.fromJsonSchemaInfo(jfi).toFileName());
            
        }
        
        
        if(targetJar.exists()) {
            o("Undeploying " + targetJar.getAbsolutePath());
            FileUtils.deleteQuietly(targetJar);
        } else {
            o("No jar to undeploy");
        }
        
        if(targetOntology.exists()) {
            o("Undeploying " + targetOntology.getAbsolutePath());
            FileUtils.deleteQuietly(targetOntology);
        } else {
            o("No owl to undeploy");
        }
        
        if(targetJson.exists()) {
            o("Undeploying " + targetJson.getAbsolutePath());
            FileUtils.deleteQuietly(targetJson);
        } else {
            o("No json schema to undeploy");
        }
        
    }

    private static void checkUserEmail(String u1, String u2) throws Exception {
        if(u1 == null && u2 == null) return;
        if(u1 == null || u2 == null || !u1.equals(u2)) throw new Exception("domain user email does not match, expected: " + u1 + ", found: " + u2);
    }
    
    private static void checkInput(String domainOntology, String domainJar,
            String domainJsonSchema) throws Exception {

        int c = 0;
        
        if(!StringUtils.isEmpty(domainOntology)) {
            c++;
        }
        
        if(!StringUtils.isEmpty(domainJar)) {
            c++;
        }
        
        if(!StringUtils.isEmpty(domainJsonSchema)) {
            c++;
        }
        
        if(c == 0) throw new Exception("Exactly one of ontology, jar json-schema param expected, no input");
        if(c >  1) throw new Exception("Exactly one of ontology, jar json-schema param expected, more than 1 given");
        
    }
    
}
