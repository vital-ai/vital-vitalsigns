package ai.vital.vitalsigns.domains


import java.nio.file.Files
import java.util.Map.Entry

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalservice.VitalService
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.admin.VitalServiceAdmin
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.command.patterns.JarFileInfo;
import ai.vital.vitalsigns.command.patterns.JsonSchemaFileInfo;
import ai.vital.vitalsigns.command.patterns.OwlFileInfo;
import ai.vital.vitalsigns.conf.VitalSignsConfig;
import ai.vital.vitalsigns.conf.VitalSignsConfig.DomainsStrategy;
import ai.vital.vitalsigns.conf.VitalSignsConfig.DomainsSyncLocation;
import ai.vital.vitalsigns.conf.VitalSignsConfig.DomainsSyncMode;
import ai.vital.vitalsigns.conf.VitalSignsConfig.DomainsVersionConflict;
import ai.vital.vitalsigns.model.DomainModel;
import ai.vital.vitalsigns.model.Edge_hasChildDomainModel;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_Container;

import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.properties.Property_hasAppID;
import ai.vital.vitalsigns.model.properties.Property_hasDomainOWLHash;

import ai.vital.vitalsigns.model.properties.Property_isActive;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.URIProperty;

import ai.vital.vitalsigns.model.VITAL_Node;


public class DomainsSyncImplementation {

    private VitalSignsConfig cfg;
    
    private VitalService service;

    private VitalServiceAdmin serviceAdmin;

    private VitalApp currentApp;
    
    private final static Logger log = LoggerFactory.getLogger(DomainsSyncImplementation.class);
    
    public final static String DOMAIN_MANAGER_DATASCRIPT = "commons/scripts/DomainsManagerScript";

    public final static boolean USE_FILE_ENDPOINT = true;
    
    public DomainsSyncImplementation() {
        
        this.cfg = VitalSigns.get().getConfig();
        this.service = VitalSigns.get().getVitalService();
        this.serviceAdmin = VitalSigns.get().getVitalServiceAdmin();
        this.currentApp = VitalSigns.get().getCurrentApp();
    }
    
    //for 
    public void doSync() throws Exception {
        
        if(service == null && serviceAdmin == null) throw new Exception("No active VitalService or vitalServiceAdmin");
        
        if(cfg.domainsStrategy != DomainsStrategy.dynamic) throw new Exception("Sync is only allowed in dynamic mode");
        
        if(cfg.loadDeployedJars) throw new Exception("Sync must not be used in loadDeployedJars mode");
        
        if(!cfg.autoLoad) throw new Exception("Sync may only be used in autoLoad mode");
        
        DomainsSyncMode m = cfg.domainsSyncMode;
        
        if(m == DomainsSyncMode.none || m == null) {
            log.info("DomainsSyncMode == none  or null - domains synchronization skipped");
            return;
        }
        
        log.info("Domains sync mode: {}", m);
        
        impl(m, cfg.domainsSyncLocation, cfg.domainsVersionConflict);
        
    }
    
    

    public VitalService getService() {
        return service;
    }

    public void setService(VitalService service) {
        this.service = service;
    }

    public VitalServiceAdmin getServiceAdmin() {
        return serviceAdmin;
    }

    public void setServiceAdmin(VitalServiceAdmin serviceAdmin) {
        this.serviceAdmin = serviceAdmin;
    }

    public VitalApp getCurrentApp() {
        return currentApp;
    }

    public void setCurrentApp(VitalApp currentApp) {
        this.currentApp = currentApp;
    }

    public void impl(DomainsSyncMode sm, DomainsSyncLocation sl, DomainsVersionConflict vc) throws Exception {
        
        Map<String, Object> jarsArgs = new HashMap<String, Object>();
        jarsArgs.put("action", "listDomainJars");
        
        ResultList domainsJarRL = null;
        if(service != null) {
            domainsJarRL = service.callFunction(DOMAIN_MANAGER_DATASCRIPT, jarsArgs);
        } else if(serviceAdmin != null) {
            domainsJarRL = serviceAdmin.callFunction(currentApp, DOMAIN_MANAGER_DATASCRIPT, jarsArgs);
        } else {
            throw new Exception("No active service or serviceAdmin");
        }
        
        checkRL(domainsJarRL);
        
//        List<DomainModel> currentModels = VitalSigns.get().getDomainModels(); 
//        log.info("current models count: {}", currentModels.size());
        
        
        Map<String, DomainModel> remoteModels = new HashMap<String, DomainModel>();
        
        List<DomainModel> remoteUnloadedModels = new ArrayList<DomainModel>();
        
//        Map<String, DomainModel> 
        
        //returned sorted
        for(GraphObject g : domainsJarRL) {
            
            DomainModel m = (DomainModel) g;
            
            JarFileInfo mjfi = JarFileInfo.fromString(m.get(Property_hasName.class).toString());
            
            String u1 = mjfi.getUseremail();
            
            IProperty isActive = (IProperty)m.get(Property_isActive.class);
            
            if(isActive == null || ( boolean) isActive.rawValue() ) {
                
                remoteModels.put(m.getURI(), m);
                
                boolean deploy = true;
                
                File existing = null;
                
                
                List<DomainModel> childModels = new ArrayList<DomainModel>();
                Map<String, byte[]> domainModelToCachedJar = new HashMap<String, byte[]>();
                
                if(sm == DomainsSyncMode.both || sm == DomainsSyncMode.pull) {
                    
                    List<DomainModel> currentModels = VitalSigns.get().getDomainModels(); 
                    log.info("current models count: {}", currentModels.size());
                    for(DomainModel current : currentModels) {
                        
                        JarFileInfo cjfi = JarFileInfo.fromString(current.get(Property_hasName.class).toString()); 
                
                        
                        if(!mjfi.getDomain().equals(cjfi.getDomain())) continue;
                        
                        existing = new File(VitalSigns.get().getVitalHomePath(), "domain-groovy-jar/" + cjfi.toFileName());
                        
                        String u2 = cjfi.getUseremail();
                        
                        if(u1 == null && u2 == null) {
                        } else if(u1 == null || u2 == null || !u1.equals(u2)) throw new Exception("domain user email does not match, expected: " + u1 + ", found: " + u2);
                     
                        int c = mjfi.compareTo(cjfi);
                        
                        boolean same = c == 0;
                        
                        String hashMsg = "";
                        
                        if(same) {
                            
                            log.info("Current domain and remote domain version match: " + cjfi.toFileName() + " " + mjfi.toFileName() + ", checking owl hash");
                            
                            IProperty currentHash = (IProperty) current.get(Property_hasDomainOWLHash.class);
                            
                            if(currentHash == null) {
                                log.warn("Local domain model owl hash not set: " + cjfi.toFileName());
                            } else {
                                
                                IProperty remoteHash = (IProperty) m.get(Property_hasDomainOWLHash.class);
                                
                                if(remoteHash == null) {
                                    
                                    log.warn("Remote domain model owl hash not set: " + mjfi.toFileName());
                                    
                                } else {
                                    
                                    if( ! currentHash.toString().equals(remoteHash.toString())) {
                                     
                                        log.warn("Domain jars owl hash values are different, local: {}, remote: {}", currentHash.toString(), remoteHash.toString());
                                        
                                        hashMsg = ( ", local hash: " + currentHash.toString() + " remote hash: " + remoteHash.toString() );
                                        
                                        same = false;
                                        
                                    }
                                    
                                }
                                
                            }


                        } 
                        
                        if(same) {
                            
                            deploy = false;
                            
                        } else {
                            
                            
                            boolean deregister = false;
                            
                            if( vc == DomainsVersionConflict.server ) {
                            
                                log.info("Remote domain version or owl hash is different than current, replacing with remote version, remote: " + mjfi.toFileName() + " local: " + cjfi.toFileName() + hashMsg);
                                
                                if(cfg.domainsStrategy == DomainsStrategy.dynamic) {
//                                    VitalSigns.get().deregisterOntology(current.getURI());
                                    deregister = true;
                                    
                                } else {
                                    log.info("classpath domains strategy - unloading skipped");
                                }
                                
                                //try deleting it 
                                deploy = true;
                                
                            } else {
                                
                                log.warn("Remote domain version or owl hash is different but versionconflict=unload, just unloading current version: remote: " + mjfi.toFileName() + " local: " + cjfi.toFileName() + hashMsg);
                                
                                if(cfg.domainsStrategy == DomainsStrategy.dynamic) {
//                                    VitalSigns.get().deregisterOntology(current.getURI());
                                    deregister = true;
                                } else {
                                    log.info("classpath domains strategy - unloading skipped");
                                }
                                
                                deploy = false;
                                
                            }
                            
                            if(deregister) {
                                
                                //look for
                                List<GraphObject> modelsWithEdges = VitalSigns.get().getDomainModelsWithEdges();
                                VITAL_Container container = null;
                                
                                try {
                                    container = new VITAL_Container();
                                        
                                    container.putGraphObjects(modelsWithEdges);
                                    
                                    GraphObject currentDomain = container.get(current.getURI());
                                    if(currentDomain == null) throw new Exception("Internal error: current domain not found: " + current.getURI());
                                    List<String> sources = Arrays.asList(currentDomain.getURI());
                                    
                                    
                                    while(sources.size() > 0) {
                                        
                                        List<String> newSources = new ArrayList<String>();
                                        
                                        for(Iterator<Edge_hasChildDomainModel> iter = container.iterator(Edge_hasChildDomainModel.class, true); iter.hasNext(); ) {
                                            
                                            Edge_hasChildDomainModel e = iter.next();
                                            
                                            if(sources.contains(e.getSourceURI())) {
                                                
                                                DomainModel child = (DomainModel) container.get(e.getDestinationURI());
                                                if(child == null) throw new Exception("Child domain of " + e.getSourceURI() + " not found: " + e.getDestinationURI());
                                        
                                                if(!childModels.contains(child)) {
                                                    
                                                    childModels.add(child);
                                                    
                                                    byte[] domainJar = VitalSigns.get().getDomainJarBytes(e.getDestinationURI());
                                                    
                                                    if(domainJar == null) throw new Exception("No domain jar bytes for domain: " + e.getDestinationURI());
                                                    
                                                    //cache it locally
                                                    domainModelToCachedJar.put(e.getDestinationURI(), domainJar);
                                                    
                                                    newSources.add(e.getDestinationURI());
                                                    
                                                }
                                        
                                                
                                            }
                                            
                                        }
                                        
                                        sources = newSources;
                                        
                                    }

                                } finally {
                                    IOUtils.closeQuietly(container);
                                }
                                
                                for(int i = childModels.size() - 1; i >= 0; i--) {
                                    log.info("Deregistering dependent domain : " + childModels.get(i).getURI());
                                    VitalSigns.get().deregisterOntology(childModels.get(i).getURI());
                                }
                                
                                log.info("Deregistering domain being replaced: " + current.getURI());
                                VitalSigns.get().deregisterOntology(current.getURI());
                                
                            }
                            
                        }
                        
                    }
                    
                } else {
                    deploy = false;
                }
                
                if(!deploy) continue;
                
                Map<String, Object> getParams = new HashMap<String, Object>();
                getParams.put("action", "getDomainJar");
                getParams.put("jarName", m.get(Property_hasName.class).toString());
                
                //domain model not has uri
                
                byte[] jarBytes = null;
                
                
                if(USE_FILE_ENDPOINT) {
                    
                    jarBytes = downloadFile(m);
                    
                } else {
                    
                    ResultList getRL = null;
                    if(service != null) {
                        getRL = service.callFunction(DOMAIN_MANAGER_DATASCRIPT, getParams);
                    } else {
                        VitalApp appParam = null;
                        if(currentApp != null) {
                            //get it from domain jar
                            appParam = currentApp;
                        } else {
                            appParam = VitalApp.withId((String) m.getRaw(Property_hasAppID.class));
                        }
                        getRL = serviceAdmin.callFunction(appParam, DOMAIN_MANAGER_DATASCRIPT, getParams);
                    }
                    checkRL(getRL);
                    
                    VITAL_Node content = (VITAL_Node) getRL.first();
                    jarBytes = Base64.decodeBase64(content.get(Property_hasName.class).toString());
                    
                }
                
                File targetJar = null;
                
                if(sl == DomainsSyncLocation.inmemory) {
                    
                    File tempDir = Files.createTempDirectory("domainjar").toFile();
                    
                    tempDir.deleteOnExit();
                    
                    targetJar = new File(tempDir, mjfi.toFileName());
                    
                    targetJar.deleteOnExit();
                    
                    FileUtils.writeByteArrayToFile(targetJar, jarBytes);
                    
                    
                } else {
                    
                    
                    if(existing != null) {
                        FileUtils.deleteQuietly(existing);
                    }
                    
                    //check if a jar is already there, persist
                    targetJar = new File(VitalSigns.get().getVitalHomePath(), "domain-groovy-jar/" + mjfi.toFileName());
                    FileUtils.writeByteArrayToFile(targetJar, jarBytes);
                    
                }
                
                
                if(cfg.domainsStrategy == DomainsStrategy.dynamic) {
                    log.info("Loading domain: " + m.getURI() + " " + mjfi.toFileName());
                    VitalSigns.get().registerOntology(targetJar.toURI().toURL());
                } else {
                    log.info("classpath domains strategy - loading skipped");
                }
                
                
                for(DomainModel childModel : childModels) {
                
                    byte[] domainJar = domainModelToCachedJar.get(childModel.getURI());
                    
                    File tempDir = Files.createTempDirectory("domainjar").toFile();
                    
                    tempDir.deleteOnExit();
                    
                    targetJar = new File(tempDir, (String)childModel.getRaw(Property_hasName.class));
                    
                    targetJar.deleteOnExit();
                    
                    FileUtils.writeByteArrayToFile(targetJar, domainJar);
                    
                    if(cfg.domainsStrategy == DomainsStrategy.dynamic) {
                        log.info("Loading cached child domain: " + childModel.getURI() + " " + childModel.getRaw(Property_hasName.class));
                        VitalSigns.get().registerOntology(targetJar.toURI().toURL());
                    } else {
                        log.info("classpath domains strategy - loading skipped");
                    }
                    
                }
                
            } else {
                remoteUnloadedModels.add(m);
            }
            
        }
        
        if(sm == DomainsSyncMode.push || sm == DomainsSyncMode.both && (service != null || currentApp != null)) {
            
            List<DomainModel> currentModels = VitalSigns.get().getDomainModels(); 
            log.info("current models count: {}", currentModels.size());
            
            for(DomainModel currentDomain : currentModels) {
                
                DomainModel remoteModel = remoteModels.get(currentDomain.getURI());
                
                if(remoteModel != null) {
                    log.info("Remote model already loaded : " + currentDomain.getURI());
                    continue;
                }
                
                JarFileInfo jfi = JarFileInfo.fromString(currentDomain.get(Property_hasName.class).toString());
                
                DomainModel remoteExistsUnloaded = null;
                
                for(DomainModel remoteUnloaded : remoteUnloadedModels) {
                    
                    JarFileInfo r = JarFileInfo.fromString(remoteUnloaded.get(Property_hasName.class).toString());
                    
                    if(jfi.getDomain().equals(r.getDomain())) {

                        remoteExistsUnloaded = remoteUnloaded;
                        break;
                        
                    }
                    
                    
                }
                
                if(remoteExistsUnloaded != null) {
                    log.info("Existing unloaded remote model found: " + remoteExistsUnloaded.get(Property_hasName.class) );
                    continue;
                }
                
                log.info("Pushing domain model into remote service: " + currentDomain.getURI());
                    
                File domainJar = new File(VitalSigns.get().getVitalHomePath(), "domain-groovy-jar/" + jfi.toFileName());
                    
//                    DomainModel remoteUnoadedJar = null;

                Map<String, Object> saveParams = new HashMap<String, Object>();
                saveParams.put("action", "saveDomainJar");
                saveParams.put("jarName", jfi.toFileName());
                saveParams.put("content", FileUtils.readFileToByteArray(domainJar));
                
                ResultList saveRL = null; 
                if(service != null) {
                    saveRL = service.callFunction(DOMAIN_MANAGER_DATASCRIPT, saveParams);
                } else {
                    
                    VitalApp appParam = currentApp;
                    saveRL = serviceAdmin.callFunction(appParam, DOMAIN_MANAGER_DATASCRIPT, saveParams);
                    
                }
                checkRL(saveRL);

            }
            
        }
        
        
        //ontologies
        Map<String, Object> owlsArgs = new HashMap<String, Object>();
        owlsArgs.put("action", "listDomainOntologies");
        
        Set<String> currentOnts = new HashSet<String>();
        Map<String, File> currentOntsFiles = new HashMap<String, File>();
        Map<String, DomainModel> remoteOnts = new HashMap<String, DomainModel>();
        
        ResultList domainsOntologiesRL = null;
        
        if(service != null) {
            domainsOntologiesRL = service.callFunction(DOMAIN_MANAGER_DATASCRIPT, owlsArgs);
        } else {
            domainsOntologiesRL = serviceAdmin.callFunction(currentApp, DOMAIN_MANAGER_DATASCRIPT, owlsArgs);
        }
        checkRL(domainsOntologiesRL);
        
        for(GraphObject g : domainsOntologiesRL) {
            DomainModel m = (DomainModel) g;
            remoteOnts.put(m.get(Property_hasName.class).toString(), m);
        }
        
        for(File f : new File(VitalSigns.get().getVitalHomePath(), "domain-ontology").listFiles()) {
            String n = f.getName();
            if(f.isFile() && n.endsWith(".owl")) {
                currentOnts.add(n);
                currentOntsFiles.put(n, f);
            }
        }
        
        
        //only overwrite if sync location is domains directory
        if(sl == DomainsSyncLocation.domainsDirectory && ( sm == DomainsSyncMode.pull || sm == DomainsSyncMode.both) ) {
            
            for(Entry<String, DomainModel> r : remoteOnts.entrySet()) {
                
                OwlFileInfo rofi = OwlFileInfo.fromString(r.getKey());
                
                boolean pull = true;
                
                File existing = null;
                
                for(String cu : currentOnts) {
                    
                    OwlFileInfo cofi = OwlFileInfo.fromString(cu);
                    
                    if(!rofi.getDomain().equals(cofi.getDomain())) continue;
                        
                    int c = rofi.compareTo(cofi);
                    
                    boolean same = c == 0;
                    
                    String hashMsg = "";
                    
                    if(same) {
                        
                        log.info("Current domain and remote domain owl version match: " + cofi.toFileName() + " " + rofi.toFileName() + ", checking hash...");
                        
                        DomainModel dm = r.getValue();
                        
                        IProperty hashValue = (IProperty) dm.get(Property_hasDomainOWLHash.class);
                        if(hashValue != null) {
                            
                            File ontFile = currentOntsFiles.get(cu);
                            String hash = DigestUtils.md5Hex(FileUtils.readFileToByteArray(ontFile));
                            
                            if(!hashValue.toString().equals(hash)) {
                                
                                log.warn("Domain ontologies owl Hash values are different, local: {}, remote: {}", hash, hashValue.toString());
                                
                                hashMsg = (", local hash: " + hash + " remote hash: " + hashValue.toString());
                                
                                same = false;
                                
                            }
                            
                        } else {
                            
                            log.warn("No owl model MD5 hash in remote domain model object: " + r.getKey());
                            
                        }
                        
                    } 
                    
                    if(same) {
                        
                        pull = false;
                        
                    } else {
                        
                        if( vc == DomainsVersionConflict.server ) {
                            
                            log.info("Remote domain owl version or owl hash is different than current, replacing with remote version, local: " + rofi.toFileName() + " remote: " + cofi.toFileName() + hashMsg);
                            
                            existing = new File(VitalSigns.get().getVitalHomePath(), "domain-ontology/" + cofi.toFileName());
                            
                            //try deleting it 
                            pull = true;
                            
                        } else {
                            
                            log.info("Remote domain version or owl hash is different but versionconflict=unload, just unloading current version: " + rofi.toFileName() + " remote: " + cofi.toFileName() + hashMsg);
                            
                            pull = false;
                            
                        }
                    }
                        
                }
                
                if(pull/* && cfg.domainsSyncLocation == DomainsSyncLocation.domainsDirectory*/) {
                    
                    if(existing != null) {
                        FileUtils.deleteQuietly(existing);
                    }
                    
                    byte[] owlBytes = null;
                    
                    if(USE_FILE_ENDPOINT) {
                        
                        owlBytes = downloadFile(r.getValue());
                        
                    } else {
                        
                        Map<String, Object> getParams = new HashMap<String, Object>();
                        getParams.put("action", "getDomainOwl");
                        getParams.put("owlName", r.getKey());
                        
                        ResultList getRL = null;
                        
                        if(service != null) {
                            getRL = service.callFunction(DOMAIN_MANAGER_DATASCRIPT, getParams);
                        } else {
                            
                            VitalApp appParam = null;
                            if(currentApp != null) {
                                //get it from domain jar
                                appParam = currentApp;
                            } else {
                                appParam = new VitalApp();
                                appParam.set(Property_hasAppID.class, r.getValue().get(Property_hasAppID.class));
                            }
                            
                            getRL = serviceAdmin.callFunction(appParam, DOMAIN_MANAGER_DATASCRIPT, getParams);
                            
                        }                    
                        checkRL(getRL);
                    
                        VITAL_Node content = (VITAL_Node) getRL.first();
                        owlBytes = Base64.decodeBase64(content.get(Property_hasName.class).toString());
                    }
                    
                    //check if a jar is already there, persist
                    FileUtils.writeByteArrayToFile(new File(VitalSigns.get().getVitalHomePath(), "domain-ontology/" + rofi.toFileName()), owlBytes);
                    
                }
                
            }
            
        }
        
        if(sm == DomainsSyncMode.push || sm == DomainsSyncMode.both && (service != null || currentApp != null)) {
            
            for(String currentOwl : currentOnts) {
                
                OwlFileInfo co = OwlFileInfo.fromString(currentOwl);
                
                boolean doSave = true;
                
                for(String remoteOwl : remoteOnts.keySet()) {
                    
                    OwlFileInfo ro = OwlFileInfo.fromString(remoteOwl);
                    
                    if(co.getDomain().equals(ro.getDomain())) {

                        doSave = false;
                            
                        break;
                        
                    }
                    
                }
                
                if(doSave) {
                    
                    File owlFile = new File(VitalSigns.get().getVitalHomePath(), "domain-ontology/" + co.toFileName());
                    Map<String, Object> saveParams = new HashMap<String, Object>();
                    saveParams.put("action", "saveDomainOntology");
                    saveParams.put("owlName", co.toFileName());
                    saveParams.put("content", FileUtils.readFileToByteArray(owlFile));
                    
                    ResultList saveRL = null;
                    if(service != null) {
                        saveRL = service.callFunction(DOMAIN_MANAGER_DATASCRIPT, saveParams);
                    } else {
                        saveRL = serviceAdmin.callFunction(currentApp, DOMAIN_MANAGER_DATASCRIPT, saveParams);
                    }
                    checkRL(saveRL);

                    
                }
                
            }
            
        }
        
        
        
        
        //json schemas
        Map<String, Object> jsonArgs = new HashMap<String, Object>();
        jsonArgs.put("action", "listJsonSchemas");
        
        Set<String> currentJsons = new HashSet<String>();
        Map<String, DomainModel> remoteJsons = new HashMap<String, DomainModel>();
        
        
        ResultList domainsJsonRL = null;
        if(service != null) {
            domainsJsonRL = service.callFunction(DOMAIN_MANAGER_DATASCRIPT, jsonArgs);
        } else {
            domainsJsonRL = serviceAdmin.callFunction(currentApp, DOMAIN_MANAGER_DATASCRIPT, jsonArgs);
        }
        checkRL(domainsJsonRL);
        
        for(GraphObject g : domainsJsonRL) {
            DomainModel m = (DomainModel) g;
            remoteJsons.put(m.get(Property_hasName.class).toString(), m);
        }
        
        for(File f : new File(VitalSigns.get().getVitalHomePath(), "domain-json-schema").listFiles()) {
            String n = f.getName();
            if(f.isFile() && n.endsWith(".js")) {
                currentJsons.add(n);
            }
        }
        
        
        //only overwrite if sync location is domains directory
        if(sl == DomainsSyncLocation.domainsDirectory && ( sm == DomainsSyncMode.pull || sm == DomainsSyncMode.both ) ) {
            
            for(Entry<String, DomainModel> r : remoteJsons.entrySet()) {
                
                JsonSchemaFileInfo rjfi = JsonSchemaFileInfo.fromString(r.getKey());
                
                boolean pull = true;
                
                File existing = null;
                
                for(String cu : currentJsons) {
                    
                    JsonSchemaFileInfo cjfi = JsonSchemaFileInfo.fromString(cu);
                    
                    if(!rjfi.getDomain().equals(cjfi.getDomain())) continue;
                        
                    int c = rjfi.compareTo(cjfi);
                    
                    if(c == 0) {
                        
                        log.info("Current domain and remote domain json schema version match: " + cjfi.toFileName() + " " + rjfi.toFileName());
                        
                        pull = false;
                        
                    } else {
                        
                        if( vc == DomainsVersionConflict.server ) {
                            
                            log.info("Remote domain json schema version is different than current, replacing with remote version: " + rjfi.toFileName() + " != " + cjfi.toFileName());
                            
                            existing = new File(VitalSigns.get().getVitalHomePath(), "domain-json-schema/" + cjfi.toFileName());
                            
                            //try deleting it 
                            pull = true;
                            
                        } else {
                            
                            log.info("Remote domain json schema version is different but versionconflict=unload, just unloading current version: " + rjfi.toFileName() + " != " + cjfi.toFileName());
                            
                            pull = false;
                            
                        }
                        
                    }
                        
                }
                
                if(pull/* && cfg.domainsSyncLocation == DomainsSyncLocation.domainsDirectory*/) {
                    
                    if(existing != null) {
                        FileUtils.deleteQuietly(existing);
                    }
                    
                    byte[] jsonBytes = null;
                    
                    if(USE_FILE_ENDPOINT) {
                        
                        jsonBytes = downloadFile(r.getValue());
                        
                    } else {
                        
                        Map<String, Object> getParams = new HashMap<String, Object>();
                        getParams.put("action", "getDomainJsonSchema");
                        getParams.put("jsonSchemaName", r.getKey());
                        
                        ResultList getRL = null;
                        if(service != null) {
                            getRL = service.callFunction(DOMAIN_MANAGER_DATASCRIPT, getParams);
                        } else {
                            VitalApp appParam = null;
                            if(currentApp != null) {
                                //get it from domain jar
                                appParam = currentApp;
                            } else {
                                appParam = new VitalApp();
                                appParam.set(Property_hasAppID.class, r.getValue().get(Property_hasAppID.class));
                            }
                            getRL = serviceAdmin.callFunction(appParam, DOMAIN_MANAGER_DATASCRIPT, getParams);
                        }
                        checkRL(getRL);
                        
                        VITAL_Node content = (VITAL_Node) getRL.first();
                        jsonBytes = Base64.decodeBase64(content.get(Property_hasName.class).toString());
                        
                    }
                    
                    //check if a jar is already there, persist
                    FileUtils.writeByteArrayToFile(new File(VitalSigns.get().getVitalHomePath(), "domain-json-schema/" + rjfi.toFileName()), jsonBytes);
                    
                }
                
            }
            
        }
        
        if(sm == DomainsSyncMode.push || sm == DomainsSyncMode.both && (service != null || currentApp != null)) {
            
            for(String currentJson : currentJsons) {
                
                JsonSchemaFileInfo cjs = JsonSchemaFileInfo.fromString(currentJson);
                
                boolean doSave = true;
                
                for(String remoteJson : remoteJsons.keySet()) {
                    
                    JsonSchemaFileInfo jo = JsonSchemaFileInfo.fromString(remoteJson);
                    
                    if(cjs.getDomain().equals(jo.getDomain())) {
                        doSave = false;
                        break;
                    }
                    
                }
                
                if(doSave) {
                    
                    File jsonFile = new File(VitalSigns.get().getVitalHomePath(), "domain-json-schema/" + cjs.toFileName());
                    Map<String, Object> saveParams = new HashMap<String, Object>();
                    saveParams.put("action", "saveDomainJsonSchema");
                    saveParams.put("jsonSchemaName", cjs.toFileName());
                    saveParams.put("content", FileUtils.readFileToByteArray(jsonFile));
                    
                    ResultList saveRL = null;
                    if(service != null) {
                        saveRL = service.callFunction(DOMAIN_MANAGER_DATASCRIPT, saveParams);
                    } else {
                        saveRL = serviceAdmin.callFunction(currentApp, DOMAIN_MANAGER_DATASCRIPT, saveParams);
                    }
                    checkRL(saveRL);

                    
                }
                
            }
            
        }
        
        
        log.info("Sync complete");
        
        
    }

    private void checkRL(ResultList domainsRL) throws Exception {
        if(domainsRL.getStatus().getStatus() != VitalStatus.Status.ok) {
            throw new Exception("Datascript error: " + domainsRL.getStatus().getMessage());
        }
        
    }
    
    private void checkStatus(VitalStatus status) throws Exception {
        if(status.getStatus() != VitalStatus.Status.ok) {
            throw new Exception("Vital service error status: " + status.getMessage());
        }
    }

    private byte[] downloadFile(DomainModel m) throws Exception {
        
        String appID = (String) m.getRaw(Property_hasAppID.class);
        
        String assetURI = "domain://" + m.getRaw(Property_hasOrganizationID.class) + "/" + appID + "/" + m.getRaw(Property_hasName.class);
        
        Integer length = 1024 * 1024;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(length);
        
        VitalStatus downloadStatus = null;
                
        if(service != null) {
            downloadStatus = service.downloadFile(URIProperty.withString(assetURI), m.getRaw(Property_hasName.class).toString(), outputStream, true);
        } else if(serviceAdmin != null) {
            downloadStatus = serviceAdmin.downloadFile(VitalApp.withId(appID), URIProperty.withString(assetURI), m.getRaw(Property_hasName.class).toString(), outputStream, true); 
        } else {
            throw new Exception("No active service or serviceAdmin");
        }
        
        checkStatus(downloadStatus);
        return outputStream.toByteArray();
        
    }
}
