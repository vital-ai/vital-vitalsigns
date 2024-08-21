package ai.vital.vitalsigns

import java.nio.file.Files
import java.nio.file.Path
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalsigns.algorithms.TopologicalSorting;
import ai.vital.vitalsigns.command.OntVersionCommands;
import ai.vital.vitalsigns.command.OntVersionCommands.LoadedOntology;
import ai.vital.vitalsigns.command.patterns.JarFileInfo;
import ai.vital.vitalsigns.command.patterns.JsonSchemaFileInfo;
import ai.vital.vitalsigns.command.patterns.OwlFileInfo;
import ai.vital.vitalsigns.conf.VitalSignsConfig.DomainsStrategy;
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader;
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader.VersionedDomain;
import ai.vital.vitalsigns.json.JSONSchemaGenerator;
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.model.DomainModel;
import ai.vital.vitalsigns.model.DomainOntology;
import ai.vital.vitalsigns.model.Edge_hasChildDomainModel;
import ai.vital.vitalsigns.model.Edge_hasParentDomainModel;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_Container;

import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.properties.Property_hasAppID;
import ai.vital.vitalsigns.model.properties.Property_hasBackwardCompVersion;
import ai.vital.vitalsigns.model.properties.Property_hasDefaultPackageValue;
import ai.vital.vitalsigns.model.properties.Property_hasDomainOWLHash;

import ai.vital.vitalsigns.model.properties.Property_hasPreferredImportVersions;
import ai.vital.vitalsigns.model.properties.Property_hasVersionInfo;
import ai.vital.vitalsigns.ontology.DomainGenerator;
import ai.vital.vitalsigns.ontology.DomainJarAnalyzer;
import ai.vital.vitalsigns.ontology.DomainJarAnalyzer.DomainJarModel;
import ai.vital.vitalsigns.ontology.OntologyDescriptor;
import ai.vital.vitalsigns.ontology.OntologyProcessor;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.uri.URIGenerator;
import ai.vital.vitalsigns.utils.NIOUtils;
import ai.vital.vitalsigns.utils.StringUtils;

import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_Edge;

class VitalDomainsManager {

    //default segment created for every app
    public final static String domains_segment = "system-domains";
    
    private final static Logger log = LoggerFactory
            .getLogger(VitalDomainsManager.class);

    DomainModel coreDomainModel = null;

    DomainModel vitalDomainModel = null;

    VITAL_Container domainModelContainer = new VITAL_Container();

    boolean orgAppMode = false;
    
    File deployedGroovyJarsDir;
    
    File deployedOntologiesDir;
    
    File deployedJsonSchemasDir;
    
    void initDomains() {

        DomainModel vsCore = new DomainModel();
        vsCore.setURI(new VitalCoreOntology().getOntologyIRI());
        domainModelContainer.putGraphObject(vsCore);
        coreDomainModel = vsCore;

        if (!VitalSigns.skipVitalDomainLoading) {

            log.info("Registering vital domain ontology...");

            try {

                Class<?> vitalDomainClass = Class
                        .forName("ai.vital.domain.ontology.VitalOntology");

                OntologyDescriptor desc = (OntologyDescriptor) vitalDomainClass
                        .newInstance();

                InputStream inputStream = desc.getOwlInputStream();

                if (inputStream == null) {
                    // try local resource
                    inputStream = vitalDomainClass
                            .getResourceAsStream("/resources/vital-ontology/"
                                    + vitalDomainClass.getField("FILE_NAME")
                                            .get(null));
                }

                VitalSigns.get().registerOntology(desc, null);

                // for(ExtendedIterator<OntClass> ei = ontModel.listClasses();
                // ei.hasNext(); ) {
                // OntClass c = ei.next();
                // if(c.getLocalName() != null) {
                // //don't restrict vital domain classes
                // //classesRegistry.getRestrictedClassNames().add(c.getLocalName());
                // }
                // }

                DomainModel vdm = new DomainModel();
                vdm.setURI(desc.getOntologyIRI());
                vitalDomainModel = vdm;

                Edge_hasChildDomainModel edm = new Edge_hasChildDomainModel();
                edm.setURI(URIGenerator.generateURI((VitalApp) null,
                        Edge_hasChildDomainModel.class));
                edm.addSource(vsCore).addDestination(vdm);

                Edge_hasParentDomainModel epm = new Edge_hasParentDomainModel();
                epm.setURI(URIGenerator.generateURI((VitalApp) null,
                        Edge_hasParentDomainModel.class));
                epm.addSource(vdm).addDestination(vsCore);
                domainModelContainer.putGraphObject(edm);
                domainModelContainer.putGraphObject(epm);
                domainModelContainer.putGraphObject(vdm);

            } catch (Exception e) {
                System.err.println("Failed to registed vital ontology: "
                        + e.getLocalizedMessage());
            }

        } else {

            log.warn("Vital domain ontology loading skipped");

        }

        if (VitalSigns.get().getConfig().domainsStrategy == DomainsStrategy.classpath) {

            log.info("Loading dynamic ontology descriptors from classpath - service loader ...");

            ServiceLoader<OntologyDescriptor> loader = ServiceLoader
                    .load(OntologyDescriptor.class);

            Map<String, OntologyDescriptor> descriptorsMap = new HashMap<String, OntologyDescriptor>();

            // create a graph of dependencies and use topological sorting

            for (Iterator<OntologyDescriptor> descriptors = loader.iterator(); descriptors
                    .hasNext();) {

                OntologyDescriptor descriptor = descriptors.next();
                descriptorsMap.put(descriptor.getOntologyIRI(), descriptor);

                // get parent o
                Model m = ModelFactory.createDefaultModel();

                InputStream owlInputStream = null;

                String md5Hash = null;
                
                try {
                    owlInputStream = descriptor.getOwlInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    IOUtils.copy(owlInputStream, bos);
                    byte[] byteArray = bos.toByteArray();
                    md5Hash = DigestUtils.md5Hex(byteArray);
                    m.read(new ByteArrayInputStream(byteArray), null);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtils.closeQuietly(owlInputStream);
                }

                Set<String> imports2Check = OntologyProcessor.getDirectImports(
                        m, descriptor.getOntologyIRI());

                
                DomainModel dm = new DomainModel();
                dm.setURI(descriptor.getOntologyIRI());
                dm.set(Property_hasDomainOWLHash.class, md5Hash);
                domainModelContainer.putGraphObject(dm);
                
                try {
                    DomainOntology _do = OntologyProcessor.getOntologyMetaData(m);
                    if(_do.getBackwardCompatibleVersion() != null) {
                        dm.set(Property_hasBackwardCompVersion.class, _do.getBackwardCompatibleVersion().toVersionString());
                    }
                    dm.set(Property_hasDefaultPackageValue.class, _do.getDefaultPackage());
                    dm.set(Property_hasVersionInfo.class, _do.toVersionString());
                    if(_do.getPreferredImportVersions() != null) {
                        dm.set(Property_hasPreferredImportVersions.class, _do.getPreferredImportVersions());
                    }
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }

                if (imports2Check.size() == 0)
                    throw new RuntimeException("No imports found in ontology: "
                            + descriptor.getOntologyIRI());

                for (String im : imports2Check) {

                    // DomainModel parent = (DomainModel)
                    // loadedOntologiesContainer.get(im);
                    //
                    // if(parent == null) {
                    // parent = new DomainModel();
                    // parent.setURI(im);
                    // loadedOntologiesContainer.putGraphObject(parent);
                    // }

                    Edge_hasChildDomainModel e = new Edge_hasChildDomainModel();
                    e.setURI(URIGenerator.generateURI((VitalApp) null,
                            Edge_hasChildDomainModel.class));
                    e.setSourceURI(im);
                    e.addDestination(dm);

                    Edge_hasParentDomainModel e2 = new Edge_hasParentDomainModel();
                    e2.setURI(URIGenerator.generateURI((VitalApp) null,
                            Edge_hasParentDomainModel.class));
                    e2.addSource(dm);
                    e2.setDestinationURI(im);

                    domainModelContainer.putGraphObject(e);
                    domainModelContainer.putGraphObject(e2);

                }

            }
            
            boolean inRegen = VitalSigns.domainBeingRegenerated != null;
            Set<String> domainsToSkip = new HashSet<String>();

            DomainModel rootDomain = null;
            
            // check if all dependencies will be met
            for (Iterator<Edge_hasChildDomainModel> edgeIter = domainModelContainer
                    .iterator(Edge_hasChildDomainModel.class); edgeIter
                    .hasNext();) {
                Edge_hasChildDomainModel edge = edgeIter.next();
                if (domainModelContainer.get(edge.getSourceURI()) == null) {
                    if(inRegen && edge.getSourceURI().equals( VitalSigns.domainBeingRegenerated)) {
                        domainsToSkip.add(edge.getSourceURI());
                        //fake domain node
                        rootDomain = new DomainModel();
                        rootDomain.setURI(edge.getSourceURI());
                        domainModelContainer.putGraphObject(rootDomain);
                        continue;
                    }
                    throw new RuntimeException("Dependent ontology not found: "
                            + edge.getSourceURI());
                }
                if (domainModelContainer.get(edge.getDestinationURI()) == null) {
                    if(inRegen && edge.getDestinationURI().equals( VitalSigns.domainBeingRegenerated)) {
                        domainsToSkip.add(edge.getDestinationURI());
                        //fake domain nodes
                        rootDomain = new DomainModel();
                        rootDomain.setURI(edge.getDestinationURI());
                        domainModelContainer.putGraphObject(rootDomain);
                        continue;
                    }
                    
                    throw new RuntimeException("Dependent ontology not found: "
                            + edge.getDestinationURI());
                }
            }

            List<VITAL_Node> sortedDomains = TopologicalSorting.sort(
                    domainModelContainer, Edge_hasChildDomainModel.class, true);

            
            if(rootDomain != null) {
                
                Set<String> roots = new HashSet<String>(Arrays.asList(rootDomain.getURI()));
                
                //cut out the whole tree starting at regenerated domain
                while(roots.size() > 0) {
                
                    domainsToSkip.addAll(roots);
                    
                    Set<String> newRoots = new HashSet<String>();
                    
                    for(String uri : roots) {
                        
                        for(Iterator<Edge_hasChildDomainModel> iter = domainModelContainer.iterator(Edge_hasChildDomainModel.class, true); iter.hasNext(); ) {
                            Edge_hasChildDomainModel next = iter.next();
                            if(next.getSourceURI().equals(uri)) {
                                newRoots.add(next.getDestinationURI());
                            }
                        }
                        
                    }
                    
                    roots = newRoots;
                    
                }
                
            }
            
            
            for (VITAL_Node domain : sortedDomains) {

                if(domainsToSkip.contains(domain.getURI())) {
                    String parentDomain = null;
                    for(Iterator<Edge_hasChildDomainModel> iter = domainModelContainer.iterator(Edge_hasChildDomainModel.class, true); iter.hasNext(); ) {
                        Edge_hasChildDomainModel next = iter.next();
                        if(next.getDestinationURI().equals(domain.getURI())) {
                            parentDomain = next.getSourceURI();
                        }
                    }
                    
                    if(VitalSigns.domainBeingRegenerated.equals(parentDomain)) {
                        log.warn("Skipping loading of domain {}, depends on domain being generated {}", domain.getURI(), parentDomain);
                    } else {
                        log.warn("Skipping loading of domain {}, depends on domain {} that depends on domain being generated {}", domain.getURI(), parentDomain, VitalSigns.domainBeingRegenerated);
                    }
                    
                    continue;
                }
                
                OntologyDescriptor descriptor = descriptorsMap.get(domain
                        .getURI());

                // vital core or vital domain
                if (descriptor == null)
                    continue;

                log.info("Registering ontology - ns:{} package:{}",
                        descriptor.getOntologyIRI(), descriptor.getPackage());

                try {
                    VitalSigns.get().registerOntology(descriptor, null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        } else {

            String vitalHomePath = System.getenv(VitalSigns.VITAL_HOME);

            
            if(VitalSigns.get().getConfig().loadDeployedJars) {
                
                orgAppMode = true;
                
                log.info("LoadDeployedJars = true - loading org/app jars");
                
                File vitalHome = null;
                try {
                    vitalHome = VitalSigns.get().getVitalHomePath();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                
                deployedGroovyJarsDir = new File(vitalHome, "domain-groovy-jar-deployed");
                if(!deployedGroovyJarsDir.isDirectory()) throw new RuntimeException("domain-groovy-jar-deployed path not found or not a directory: " + deployedGroovyJarsDir.getAbsolutePath());
                
                deployedOntologiesDir = new File(vitalHome, "domain-ontology-deployed");
                if(!deployedOntologiesDir.isDirectory()) throw new RuntimeException("domain-ontology-deployed path not found or not a directory: " + deployedOntologiesDir.getAbsolutePath());
                
                deployedJsonSchemasDir = new File(vitalHome, "domain-json-schema-deployed");
                if(!deployedJsonSchemasDir.isDirectory()) throw new RuntimeException("domain-json-schema-deployed path not found or not a directory: " + deployedJsonSchemasDir.getAbsolutePath());
                
                //list files
                Map<String, File> uri2File = new HashMap<String, File>();
                
                for(File orgDir : deployedGroovyJarsDir.listFiles()) {
                    
                    if(!orgDir.isDirectory()) throw new RuntimeException("Only organization directories allowed in " + deployedGroovyJarsDir.getAbsolutePath() + " - " + orgDir.getName());
                    
                    for(File appDir : orgDir.listFiles()) {
                        
                        if(!appDir.isDirectory()) throw new RuntimeException("Only app directories allowed in organization dir " + orgDir.getAbsolutePath() + " - " + appDir.getName());
                        
                        
                        Map<String, Set<String>> uri2Imports = new HashMap<String, Set<String>>();
                        
                        for( File f : appDir.listFiles() ) {
                            
                            if(!f.isFile()) throw new RuntimeException("Only groovy jar files allowed in org/app dir " + f.getAbsolutePath());
                            
                            JarFileInfo.fromString(f.getName());
                            
                            try {
                                
                                DomainJarModel analyzeDomainJar = DomainJarAnalyzer
                                        .analyzeDomainJar(f);
    
                                if (uri2File
                                        .containsKey(analyzeDomainJar.ontologyURI))
                                    throw new Exception("Domain ontology with URI "
                                            + analyzeDomainJar.ontologyURI
                                            + " occurred twice, files: "
                                            + f.getAbsolutePath()
                                            + " and "
                                            + uri2File.get(
                                                    analyzeDomainJar.ontologyURI)
                                                    .getAbsolutePath());
    
                                uri2File.put(analyzeDomainJar.ontologyURI, f);
    
                                DomainModel dm = new DomainModel();
                                dm.setURI(analyzeDomainJar.ontologyURI);
                                dm.set(Property_hasAppID.class, appDir.getName());
                                dm.set(Property_hasOrganizationID.class, orgDir.getName());
                                dm.set(Property_hasName.class, f.getName());
                                dm.set(Property_hasDomainOWLHash.class, analyzeDomainJar.md5Hash);
    
                                try {
                                    
                                    DomainOntology _do = OntologyProcessor.getOntologyMetaData(analyzeDomainJar.model);
                                    
                                    if(_do.getBackwardCompatibleVersion() != null) {
                                        dm.set(Property_hasBackwardCompVersion.class, _do.getBackwardCompatibleVersion().toVersionString());
                                    }
                                    dm.set(Property_hasDefaultPackageValue.class, _do.getDefaultPackage());
                                    dm.set(Property_hasVersionInfo.class, _do.toVersionString());
                                    if(_do.getPreferredImportVersions() != null) {
                                        dm.set(Property_hasPreferredImportVersions.class, _do.getPreferredImportVersions());
                                    }
                                } catch(Exception e) {
                                    throw new RuntimeException(e);
                                }
                                
                                
                                Set<String> imports2Check = OntologyProcessor
                                        .getDirectImports(analyzeDomainJar.model,
                                                analyzeDomainJar.ontologyURI);
                                
                                uri2Imports.put(analyzeDomainJar.ontologyURI, imports2Check);
                                
                                if (imports2Check.size() == 0)
                                    throw new RuntimeException(
                                            "No imports found in ontology: "
                                                    + analyzeDomainJar.ontologyURI);
                                domainModelContainer.putGraphObject(dm);
                                
                                for (String im : imports2Check) {
                                    Edge_hasChildDomainModel e = new Edge_hasChildDomainModel();
                                    e.setURI(URIGenerator.generateURI((VitalApp) null,
                                            Edge_hasChildDomainModel.class));
                                    e.setSourceURI(im);
                                    e.addDestination(dm);
    
                                    Edge_hasParentDomainModel e2 = new Edge_hasParentDomainModel();
                                    e2.setURI(URIGenerator.generateURI((VitalApp) null,
                                            Edge_hasParentDomainModel.class));
                                    e2.addSource(dm);
                                    e2.setDestinationURI(im);
    
                                    domainModelContainer.putGraphObject(e);
                                    domainModelContainer.putGraphObject(e2);
                                }
                                
                            } catch (Exception e) {
                                throw new RuntimeException(
                                        "Error when processing domain jar: "
                                                + f.getAbsolutePath() + " "
                                                + e.getLocalizedMessage());
                            }
    
                        }
        
                        // check if all dependencies will be met
                        /*
                        for (Iterator<Edge_hasChildDomainModel> edgeIter = domainModelContainer
                                .iterator(Edge_hasChildDomainModel.class); edgeIter
                                .hasNext();) {
                            Edge_hasChildDomainModel edge = edgeIter.next();
                            if (domainModelContainer.get(edge.getSourceURI()) == null)
                                throw new RuntimeException(
                                        "Dependent ontology not found: "
                                                + edge.getSourceURI());
                            if (domainModelContainer.get(edge.getDestinationURI()) == null)
                                throw new RuntimeException(
                                        "Dependent ontology not found: "
                                                + edge.getDestinationURI());
                        }
                        */
                        
                        /* single app domains branch limitation removed 
                        for(Entry<String, Set<String>> e : uri2Imports.entrySet()) {
                            
                            for(String im : e.getValue() ) {
                                
                                if( coreDomainModel.getURI().equals(im) || ( vitalDomainModel != null && im.equals(vitalDomainModel.getURI()) ) || uri2File.containsKey(im) ) {
                                    
                                } else {
                                    
                                    throw new RuntimeException("Domain " + e.getKey() + " import " + im + " is not a vital domain nor is found in app directory");
                                    
                                }
                                
                            }
                            
                            
                        }
                        
                        List<VITAL_Node> sortedDomains = TopologicalSorting.sort(
                                domainModelContainer,
                                Edge_hasChildDomainModel.class, true);
    
                        for (VITAL_Node domain : sortedDomains) {
    
                            // vital core or vital domain
                            if (VitalSigns.get().getOntologyURI2Package()
                                    .containsKey(domain.getURI()))
                                continue;
    
                            File ontFile = uri2File.get(domain.getURI());
    
                            log.info("Registering ontology - ns:{} file:{}",
                                    domain.getURI(), ontFile.getAbsolutePath());
    
                            try {
                                VitalSigns.get().registerOntology(ontFile.toURI().toURL());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            
                        }
                         */
                        
                    }
                    
                }
                
                for (Iterator<Edge_hasChildDomainModel> edgeIter = domainModelContainer
                        .iterator(Edge_hasChildDomainModel.class); edgeIter
                        .hasNext();) {
                    Edge_hasChildDomainModel edge = edgeIter.next();
                    if (domainModelContainer.get(edge.getSourceURI()) == null)
                        throw new RuntimeException(
                                "Dependent ontology not found: "
                                        + edge.getSourceURI());
                    if (domainModelContainer.get(edge.getDestinationURI()) == null)
                        throw new RuntimeException(
                                "Dependent ontology not found: "
                                        + edge.getDestinationURI());
                }
                
                List<VITAL_Node> sortedDomains = TopologicalSorting.sort(
                        domainModelContainer,
                        Edge_hasChildDomainModel.class, true);

                for (VITAL_Node domain : sortedDomains) {

                    // vital core or vital domain
                    if (VitalSigns.get().getOntologyURI2Package()
                            .containsKey(domain.getURI()))
                        continue;

                    File ontFile = uri2File.get(domain.getURI());

                    log.info("Registering ontology - ns:{} file:{}",
                            domain.getURI(), ontFile.getAbsolutePath());

                    try {
                        VitalSigns.get().registerOntology(ontFile.toURI().toURL());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    
                }
                
            } else {
                
                if (VitalSigns.get().getConfig().autoLoad) {
    
                    log.info("autoLoad=true - Loading dynamic ontologies from $VITAL_HOME/domain-groovy-jar ...");
    
                    if (!StringUtils.isEmpty(vitalHomePath)) {
    
                        File domainJarDir = new File(vitalHomePath,
                                "domain-groovy-jar");
                        if (!domainJarDir.isDirectory())
                            throw new RuntimeException(
                                    "$VITAL_HOME/domain-groovy-jar not found or not a directory: "
                                            + domainJarDir);
    
                        Map<String, File> uri2File = new HashMap<String, File>();
    
                        for (File f : FileUtils.listFiles(domainJarDir,
                                new String[] { "jar" }, false)) {
    
                            try {
                                DomainJarModel analyzeDomainJar = DomainJarAnalyzer
                                        .analyzeDomainJar(f);
    
                                if (uri2File
                                        .containsKey(analyzeDomainJar.ontologyURI))
                                    throw new Exception("Domain ontology with URI "
                                            + analyzeDomainJar.ontologyURI
                                            + " occurred twice, files: "
                                            + f.getAbsolutePath()
                                            + " and "
                                            + uri2File.get(
                                                    analyzeDomainJar.ontologyURI)
                                                    .getAbsolutePath());
    
                                uri2File.put(analyzeDomainJar.ontologyURI, f);
    
                                DomainModel dm = new DomainModel();
                                dm.setURI(analyzeDomainJar.ontologyURI);
                                dm.set(Property_hasName.class, f.getName());
                                dm.set(Property_hasDomainOWLHash.class, analyzeDomainJar.md5Hash);
    
                                try {
                                    
                                    DomainOntology _do = OntologyProcessor.getOntologyMetaData(analyzeDomainJar.model);
                                    
                                    if(_do.getBackwardCompatibleVersion() != null) {
                                        dm.set(Property_hasBackwardCompVersion.class, _do.getBackwardCompatibleVersion().toVersionString());
                                    }
                                    dm.set(Property_hasDefaultPackageValue.class, _do.getDefaultPackage());
                                    dm.set(Property_hasVersionInfo.class, _do.toVersionString());
                                    if(_do.getPreferredImportVersions() != null) {
                                        dm.set(Property_hasPreferredImportVersions.class, _do.getPreferredImportVersions());
                                    }
                                } catch(Exception e) {
                                    throw new RuntimeException(e);
                                }
                                
                                
                                Set<String> imports2Check = OntologyProcessor
                                        .getDirectImports(analyzeDomainJar.model,
                                                analyzeDomainJar.ontologyURI);
                                if (imports2Check.size() == 0)
                                    throw new RuntimeException(
                                            "No imports found in ontology: "
                                                    + analyzeDomainJar.ontologyURI);
                                domainModelContainer.putGraphObject(dm);
                                for (String im : imports2Check) {
                                    Edge_hasChildDomainModel e = new Edge_hasChildDomainModel();
                                    e.setURI(URIGenerator.generateURI((VitalApp) null,
                                            Edge_hasChildDomainModel.class));
                                    e.setSourceURI(im);
                                    e.addDestination(dm);
    
                                    Edge_hasParentDomainModel e2 = new Edge_hasParentDomainModel();
                                    e2.setURI(URIGenerator.generateURI((VitalApp) null,
                                            Edge_hasParentDomainModel.class));
                                    e2.addSource(dm);
                                    e2.setDestinationURI(im);
    
                                    domainModelContainer.putGraphObject(e);
                                    domainModelContainer.putGraphObject(e2);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(
                                        "Error when processing domain jar: "
                                                + f.getAbsolutePath() + " "
                                                + e.getLocalizedMessage());
                            }
    
                        }
                        
                        
                        boolean inRegen = VitalSigns.domainBeingRegenerated != null;
                        Set<String> domainsToSkip = new HashSet<String>();

                        DomainModel rootDomain = null;
    
                        // check if all dependencies will be met
                        for (Iterator<Edge_hasChildDomainModel> edgeIter = domainModelContainer
                                .iterator(Edge_hasChildDomainModel.class); edgeIter
                                .hasNext();) {
                            Edge_hasChildDomainModel edge = edgeIter.next();
                            if (domainModelContainer.get(edge.getSourceURI()) == null) {
                                if(inRegen && edge.getSourceURI().equals( VitalSigns.domainBeingRegenerated)) {
                                    domainsToSkip.add(edge.getSourceURI());
                                    //fake domain node
                                    rootDomain = new DomainModel();
                                    rootDomain.setURI(edge.getSourceURI());
                                    domainModelContainer.putGraphObject(rootDomain);
                                    continue;
                                }
                                throw new RuntimeException(
                                        "Dependent ontology not found: "
                                                + edge.getSourceURI());
                            }
                            if (domainModelContainer.get(edge.getDestinationURI()) == null) {
                                if(inRegen && edge.getDestinationURI().equals( VitalSigns.domainBeingRegenerated)) {
                                    domainsToSkip.add(edge.getDestinationURI());
                                    //fake domain nodes
                                    rootDomain = new DomainModel();
                                    rootDomain.setURI(edge.getDestinationURI());
                                    domainModelContainer.putGraphObject(rootDomain);
                                    continue;
                                }
                                throw new RuntimeException(
                                        "Dependent ontology not found: "
                                                + edge.getDestinationURI());
                            }
                        }
    
                        List<VITAL_Node> sortedDomains = TopologicalSorting.sort(
                                domainModelContainer,
                                Edge_hasChildDomainModel.class, true);
                        
                        
                        
                        if(rootDomain != null) {
                            
                            Set<String> roots = new HashSet<String>(Arrays.asList(rootDomain.getURI()));
                            
                            //cut out the whole tree starting at regenerated domain
                            while(roots.size() > 0) {
                            
                                domainsToSkip.addAll(roots);
                                
                                Set<String> newRoots = new HashSet<String>();
                                
                                for(String uri : roots) {
                                    
                                    for(Iterator<Edge_hasChildDomainModel> iter = domainModelContainer.iterator(Edge_hasChildDomainModel.class, true); iter.hasNext(); ) {
                                        Edge_hasChildDomainModel next = iter.next();
                                        if(next.getSourceURI().equals(uri)) {
                                            newRoots.add(next.getDestinationURI());
                                        }
                                    }
                                    
                                }
                                
                                roots = newRoots;
                                
                            }
                            
                        }
                        
                        
                        /*
                        FileSystem jimfs = null;
                        Path jimfsTempDir = null;
                        if(sortedDomains.size() > 0) {
                            jimfs = VitalSigns.get().getJimfs();
                            jimfsTempDir = jimfs.getPath("/temp-" + System.currentTimeMillis());
                            try {
                                Files.createDirectories(jimfsTempDir);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        */
                        
                        File tempDir = null;
                        try {
                            tempDir = Files.createTempDirectory("domain-jars").toFile();
                        } catch (IOException e1) {
                            throw new RuntimeException(e1);
                        }
                        tempDir.deleteOnExit();
                        
                        long start = System.currentTimeMillis();
                        
                        for (VITAL_Node domain : sortedDomains) {
    
                            // vital core or vital domain
                            if (VitalSigns.get().getOntologyURI2Package()
                                    .containsKey(domain.getURI())) {
                                continue;
                            }
                            
                            if(domainsToSkip.contains(domain.getURI())) {
                                String parentDomain = null;
                                for(Iterator<Edge_hasChildDomainModel> iter = domainModelContainer.iterator(Edge_hasChildDomainModel.class, true); iter.hasNext(); ) {
                                    Edge_hasChildDomainModel next = iter.next();
                                    if(next.getDestinationURI().equals(domain.getURI())) {
                                        parentDomain = next.getSourceURI();
                                    }
                                }
                                
                                if(VitalSigns.domainBeingRegenerated.equals(parentDomain)) {
                                    log.warn("Skipping loading of domain {}, depends on domain being generated {}", domain.getURI(), parentDomain);
                                } else {
                                    log.warn("Skipping loading of domain {}, depends on domain {} that depends on domain being generated {}", domain.getURI(), parentDomain, VitalSigns.domainBeingRegenerated);
                                }
                                
                                continue;
                            }
                                
                            
                            File ontFile = uri2File.get(domain.getURI());
    
                            File targetFile = new File(tempDir, ontFile.getName());
                            
                            try {
                                FileUtils.copyFile(ontFile, targetFile);
                            } catch (IOException e1) {
                                throw new RuntimeException();
                            }
                            
                            targetFile.deleteOnExit();
                            ontFile = targetFile;
                            
                            /*
                            Path target = jimfsTempDir.resolve(ontFile.getName());
                            
                            try {
                                Files.createDirectories(target);
                                
                                //unzip the jar there
                                unzip(ontFile.toPath(), target);
                                
//                                Files.copy(ontFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e1) {
                                throw new RuntimeException(e1);
                            }
                            
                            
                            
                            log.info("Registering ontology - ns:{} jimfs file: {}",
                                    domain.getURI(), target.toString());
    
                            URI uri = target.toUri();
                            */
                            
                            try {
//                                URL url = new URL(null, uri.toString(), new JimfsURLStreamHandler());
//                                  VitalSigns.get().registerOntology(url);
                                  VitalSigns.get().registerOntology(ontFile.toURI().toURL());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
    
                        log.info("Dynamic domains loading time: {}ms", System.currentTimeMillis() - start);
                        
                    } else {
    
                        log.warn("VITAL_HOME not set, no domain ontologies loaded");
    
                    }
    
                } else {
    
                    log.info("autoLoad=false, only vital core and domain ontologies loaded on startup");
    
                }
                
            }
            
        }

        try {
            domainModelContainer.close();
        } catch (IOException e) {
        }

    }
    
    public List<DomainModel> getDomainModels(String uriFilter) {
        
        List<DomainModel> out = new ArrayList<DomainModel>();
        
        for( Iterator<DomainModel> iterator = domainModelContainer.iterator(DomainModel.class, true); iterator.hasNext(); ) {
        
            DomainModel next = iterator.next();
            
            VersionedDomain vd = VersionedDomain.analyze(next.getURI());
            
            if(vd.domainURI.equals(uriFilter)) {
                out.add(next);
            }
            
        }
        
        return out;
        
    }
    
    public List<DomainModel> getDomainModels() {

        List<VITAL_Node> l2 = TopologicalSorting.sort(domainModelContainer, Edge_hasChildDomainModel.class, true);
        
        List<DomainModel> l = new ArrayList<DomainModel>();
        
        for(VITAL_Node o : l2) {
            
            DomainModel dm = (DomainModel) o;
            
            String u = dm.getURI();
            if (coreDomainModel.getURI().equals(u)
                    || (vitalDomainModel != null && vitalDomainModel.getURI()
                            .equals(u))) {
                continue;
            }
            
            l.add(dm);
            
        }
        
        return l;

    }

    public List<DomainModel> getParentModels(String ontologyURI, boolean direct) {
        return getDomainModelsImpl(ontologyURI, direct, true);
    }

    public List<DomainModel> getChildModels(String ontologyURI, boolean direct) {
        return getDomainModelsImpl(ontologyURI, direct, false);
    }

    private List<DomainModel> getDomainModelsImpl(String ontologyURI,
            boolean direct, boolean parentNotChild) {

        DomainModel dm = (DomainModel) domainModelContainer.get(ontologyURI);
        if (dm == null)
            throw new RuntimeException("No domain model with ontology URI "
                    + ontologyURI + " found");

        List<DomainModel> l = new ArrayList<DomainModel>();

        List<DomainModel> roots = Arrays.asList(dm);

        while (roots != null) {

            List<DomainModel> newRoots = new ArrayList<DomainModel>();

            for (DomainModel root : roots) {

                for (VITAL_Node n : root.getCollection(
                        parentNotChild ? "parentDomainModels"
                                : "childDomainModels", GraphContext.Container,
                        domainModelContainer)) {

                    if (!l.contains(n)) {
                        l.add((DomainModel) n);
                        newRoots.add((DomainModel) n);
                    }

                }

            }

            if (direct || newRoots.size() == 0) {
                roots = null;
            } else {
                roots = newRoots;
            }

        }

        return l;

    }
    
    public void saveAppDomainGroovyJar(String organizationID, String appID, String jarName, byte[] content) throws Exception {

        checkOrgAppMode();
        
        File targetJar = new File(deployedGroovyJarsDir, organizationID + "/" + appID + "/" + jarName);
        
        JarFileInfo jfi = JarFileInfo.fromString(targetJar.getName());
        
        if(jfi.getDomain().equals("vital") || jfi.getDomain().equals("vital-core")) throw new Exception("Cannot save vital/vital-core domains in deployed domains location");
        
        DomainJarModel analyzeDomainJar = DomainJarAnalyzer.analyzeDomainJar(new ByteArrayInputStream(content));
        
        if(coreDomainModel.getURI().equals( analyzeDomainJar.ontologyURI) ) throw new Exception("Cannot save vital-core domain in deployed domains location");
        if(vitalDomainModel != null && vitalDomainModel.getURI().equals(analyzeDomainJar.ontologyURI)) throw new Exception("Cannot save vital domain in deployed domains location");
        
        if( domainModelContainer.get(analyzeDomainJar.ontologyURI) != null ) throw new RuntimeException("Ontology with URI: " + analyzeDomainJar.ontologyURI + " already loaded - unload it first before saving another version");
        
        
        //list files to check if the jar is not loaded somewhere else
        for(File f : FileUtils.listFiles(deployedGroovyJarsDir, new String[]{"jar"}, true)) {
            if(f.getAbsolutePath().equals(targetJar.getAbsolutePath())) continue;
            
            JarFileInfo j = JarFileInfo.fromString(f.getName());
            
            if(jfi.getDomain().equals(j.getDomain())) throw new Exception("An existing domain " + j.getDomain() + " jar found in another location: " + f.getAbsolutePath());
            
        }
        
        targetJar.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(targetJar, content);
        
    }
    
    public void saveAppDomainJsonSchema(String organizationID, String appID, String jsonSchemaName, byte[] content) throws Exception {
        
        checkOrgAppMode();
        
        File targetJson = new File(deployedJsonSchemasDir, organizationID + "/" + appID + "/" + jsonSchemaName);
        
        JsonSchemaFileInfo jfi = JsonSchemaFileInfo.fromString(targetJson.getName());
        
        if(jfi.getDomain().equals("vital") || jfi.getDomain().equals("vital-core")) throw new Exception("Cannot save vital/vital-core schemas in deployed domains location");
        
        
        //list files to check if the jar is not loaded somewhere else
        for(File f : FileUtils.listFiles(deployedJsonSchemasDir, new String[]{"js"}, true)) {
            if(f.getAbsolutePath().equals(targetJson.getAbsolutePath())) continue;
            
            JsonSchemaFileInfo j = JsonSchemaFileInfo.fromString(f.getName());
            
            if(jfi.getDomain().equals(j.getDomain())) throw new Exception("An existing domain " + j.getDomain() + " json schema found in another location: " + f.getAbsolutePath());
            
        }
        
        targetJson.getParentFile().mkdirs();
        FileUtils.writeByteArrayToFile(targetJson, content);
        
    }
    
    public void saveAppDomainOntology(String organizationID, String appID, String owlName, byte[] content) throws Exception {
        
        File tempDir = null;
        
        try {
            
            checkOrgAppMode();
            
            File targetOWL = new File(deployedOntologiesDir, organizationID + "/" + appID + "/" + owlName);

            OwlFileInfo ofi = OwlFileInfo.fromString(owlName);

            if(ofi.getDomain().equals("vital") || ofi.getDomain().equals("vital-core")) throw new Exception("Cannot save vital/vital-core domains in deployed domains location");

            tempDir = Files.createTempDirectory("domain").toFile();
            tempDir.deleteOnExit();
            File ontFile = new File(tempDir, owlName);
            
            FileUtils.writeByteArrayToFile(ontFile, content);
            
            LoadedOntology readOntology = OntVersionCommands.readOntology(ontFile);
            
            if( domainModelContainer.get(readOntology.getDomainOntology().getUri() ) != null ) throw new RuntimeException("Ontology with URI: " + readOntology.getDomainOntology().getUri() + " already loaded - unload it first before saving an owl file");
            
            //list files to check if the jar is not loaded somewhere else
            for(File f : FileUtils.listFiles(deployedOntologiesDir, new String[]{"owl"}, true)) {
                if(f.getAbsolutePath().equals(targetOWL.getAbsolutePath())) continue;
                
                OwlFileInfo j = OwlFileInfo.fromString(f.getName());
                
                if(ofi.getDomain().equals(j.getDomain())) throw new Exception("An existing domain " + j.getDomain() + " owl found in another location: " + f.getAbsolutePath());
                
            }
            
            FileUtils.writeByteArrayToFile(targetOWL, content);
            
         
        } finally {
            
            FileUtils.deleteQuietly(tempDir);
            
        }
        
        
    }
    
    
    public void deleteAppDomainGroovyJar(String organizationID, String appID, String jarName) throws Exception {
        
        checkOrgAppMode();
        
        File targetJar = new File(deployedGroovyJarsDir, organizationID + "/" + appID + "/" + jarName);
        
        if(!targetJar.exists()) throw new Exception("Domain jar not found: " + targetJar.getAbsolutePath());
        
        JarFileInfo.fromString(targetJar.getName());
        
        DomainJarModel analyzeDomainJar = DomainJarAnalyzer.analyzeDomainJar(targetJar);
        
        if( domainModelContainer.get(analyzeDomainJar.ontologyURI) != null ) throw new RuntimeException("Ontology with URI: " + analyzeDomainJar.ontologyURI + " loaded - unload it first");
        
        FileUtils.deleteQuietly(targetJar);
        
    }
    
    public void deleteAppDomainOntology(String organizationID, String appID, String owlName) throws Exception {
        
        checkOrgAppMode();
        
        File targetOwl = new File(deployedOntologiesDir, organizationID + "/" + appID + "/" + owlName);
        
        if(!targetOwl.exists()) throw new Exception("Domain ontology not found: " + targetOwl.getAbsolutePath());
        
        OwlFileInfo.fromString(targetOwl.getName());
        
        LoadedOntology readOntology = OntVersionCommands.readOntology(targetOwl);
        
        if( domainModelContainer.get(readOntology.getDomainOntology().getUri()) != null ) throw new RuntimeException("Ontology with URI: " + readOntology.getDomainOntology().getUri() + " loaded - unload it first");
        
        FileUtils.deleteQuietly(targetOwl);
        
    }
    
    public void deleteAppDomainJsonSchema(String organizationID, String appID, String jsonSchemaName) throws Exception {
        
        checkOrgAppMode();
        
        File targetJson = new File(deployedJsonSchemasDir, organizationID + "/" + appID + "/" + jsonSchemaName);
        
        if(!targetJson.exists()) throw new Exception("Domain json schema not found: " + targetJson.getAbsolutePath());
        
        JsonSchemaFileInfo.fromString(targetJson.getName());
        
        FileUtils.deleteQuietly(targetJson);
        
    }
    
    public void loadAppDomainOntology(String organizationID, String appID, String jarName) throws Exception {
        
        //first analyze the content and check dependencies
        checkOrgAppMode();

        File targetJar = new File(deployedGroovyJarsDir, organizationID + "/" + appID + "/" + jarName);
        
        if(!targetJar.exists()) throw new RuntimeException("Jar not found: " + targetJar.getAbsolutePath());
        
        JarFileInfo.fromString(targetJar.getName());
        
        DomainJarModel analyzeDomainJar = DomainJarAnalyzer.analyzeDomainJar(targetJar);
        
        if( domainModelContainer.get(analyzeDomainJar.ontologyURI) != null ) throw new RuntimeException("Ontology with URI: " + analyzeDomainJar.ontologyURI + " already loaded"); 
        
        
        DomainModel dm = new DomainModel();
        dm.setURI(analyzeDomainJar.ontologyURI);
        dm.set(Property_hasAppID.class, appID);
        dm.set(Property_hasOrganizationID.class, organizationID);
        dm.set(Property_hasName.class, jarName);
        dm.set(Property_hasDomainOWLHash.class, analyzeDomainJar.md5Hash);
        
        try {
            
            DomainOntology _do = OntologyProcessor.getOntologyMetaData(analyzeDomainJar.model);
            
            if(_do.getBackwardCompatibleVersion() != null) {
                dm.set(Property_hasBackwardCompVersion.class, _do.getBackwardCompatibleVersion().toVersionString());
            }
            dm.set(Property_hasDefaultPackageValue.class, _do.getDefaultPackage());
            dm.set(Property_hasVersionInfo.class, _do.toVersionString());
            if(_do.getPreferredImportVersions() != null) {
                dm.set(Property_hasPreferredImportVersions.class, _do.getPreferredImportVersions());
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        
        List<GraphObject> objectsToInsert = new ArrayList<GraphObject>();
        objectsToInsert.add(dm);
        
        //current app jars
        Set<String> imports2Check = OntologyProcessor.getDirectImports(analyzeDomainJar.model,analyzeDomainJar.ontologyURI);
        
        Set<String> currentAppOnts = new HashSet<String>();
        
        for(Iterator<DomainModel> iter = domainModelContainer.iterator(DomainModel.class, true); iter.hasNext(); ) {
        
            DomainModel d = iter.next();
            Object _orgID = d.getProperty("organizationID");
            Object _appID = d.getProperty("appID");
            
            if(_orgID != null && _orgID.toString().equals(organizationID) && _appID != null && _appID.toString().equals(appID)) {
                currentAppOnts.add(d.getURI());
            }            
            
        }
        
        
        
        for(String im : imports2Check) {
                
            if( coreDomainModel.getURI().equals(im) || ( vitalDomainModel != null && im.equals(vitalDomainModel.getURI()) ) || currentAppOnts.contains(im) ) {
                
            } else {
                
                throw new RuntimeException("Domain " + analyzeDomainJar.ontologyURI + " import " + im + " is not a vital domain nor is found in app directory");
                
            }
            
            DomainModel parent = (DomainModel) domainModelContainer.get(im);
            
            Edge_hasChildDomainModel edm = new Edge_hasChildDomainModel();
            edm.setURI(URIGenerator.generateURI((VitalApp) null,
                    Edge_hasChildDomainModel.class));
            edm.addSource(parent).addDestination(dm);

            Edge_hasParentDomainModel epm = new Edge_hasParentDomainModel();
            epm.setURI(URIGenerator.generateURI((VitalApp) null,
                    Edge_hasParentDomainModel.class));
            epm.addSource(dm).addDestination(parent);
 
            objectsToInsert.add(edm);
            objectsToInsert.add(epm);
            
        }
        
        //all is ok
        log.info("Registering ontology - ns:{} file:{}",
                analyzeDomainJar.ontologyURI, targetJar.getAbsolutePath());

        try {
            VitalSigns.get().registerOntology(targetJar.toURI().toURL());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        domainModelContainer.putGraphObjects(objectsToInsert);
        
        
        
    }
    
    public void unloadAppDomainOntology(String organizationID, String appID, String jarName) throws Exception {
        
        checkOrgAppMode();
        
        File targetJar = new File(deployedGroovyJarsDir, organizationID + "/" + appID + "/" + jarName);
        
        if(!targetJar.exists()) throw new RuntimeException("Jar not found: " + targetJar.getAbsolutePath());
        
        DomainJarModel analyzeDomainJar = DomainJarAnalyzer.analyzeDomainJar(targetJar);
        
        DomainModel d = (DomainModel) domainModelContainer.get(analyzeDomainJar.ontologyURI);
        
        if( d == null ) throw new RuntimeException("Ontology with URI: " + analyzeDomainJar.ontologyURI + " not loaded");
        
        Object _orgID = d.getProperty("organizationID");
        Object _appID = d.getProperty("appID");
        
        if(_orgID != null && _orgID.toString().equals(organizationID) && _appID != null && _appID.toString().equals(appID)) {
            //ok
        } else {
            throw new Exception("Current domain jar org/app pair does not match:" + _orgID + "/" + _appID + " input: " + organizationID + "/" + appID);
        }
        
        VitalStatus status = VitalSigns.get().deregisterOntology(analyzeDomainJar.ontologyURI);
        if(status.getStatus() != VitalStatus.Status.ok) throw new Exception("Couldn't unload domain: " + status.getMessage());
        
    }

    private void checkOrgAppMode() {

        if(!orgAppMode) throw new RuntimeException("this method call is only valid with config domainsStrategy=dynamic and loadDeployedJars=true ");

        
        
    }

    public List<GraphObject> getDomainModelsWithEdges() {

//        checkOrgAppMode();

        List<GraphObject> l = new ArrayList<GraphObject>();
        
        Set<String> validDomains = new HashSet<String>();
        
        for (Iterator<GraphObject> iterator = domainModelContainer.iterator(); iterator.hasNext();) {
            GraphObject g = iterator.next();
            if(g instanceof DomainModel) {
                DomainModel dm = (DomainModel) g;
                String u = dm.getURI();
                if (coreDomainModel.getURI().equals(u)
                        || (vitalDomainModel != null && vitalDomainModel.getURI()
                        .equals(u))) {
                    continue;
                }
                l.add(dm);
                validDomains.add(dm.getURI());
            }
        }
        
        for (Iterator<GraphObject> iterator = domainModelContainer.iterator(); iterator.hasNext();) {
            GraphObject g = iterator.next();
            if(g instanceof Edge_hasChildDomainModel || g instanceof Edge_hasParentDomainModel) {
                VITAL_Edge e = (VITAL_Edge) g;
                if(validDomains.contains( e.getSourceURI()) && validDomains.contains(e.getDestinationURI() ) ) {
                    l.add(e);
                }
                    
            }
        }

        return l;
        
    }

    public void compileDomainAppOntologyIntoJar(String organizationID,
            String appID, String owlName) throws Exception {
        
        File compileDir = null;
        
        try {
            
            checkOrgAppMode();
            
            File ontFile = new File(deployedOntologiesDir, organizationID + "/" + appID + "/" + owlName);

            OwlFileInfo ofi = OwlFileInfo.fromString(owlName);
            
            if(!ontFile.exists()) throw new Exception("OWL file not found: " + owlName);
            
            LoadedOntology readOntology = OntVersionCommands.readOntology(ontFile);
            
            if( domainModelContainer.get(readOntology.getDomainOntology().getUri() ) != null ) throw new RuntimeException("Ontology with URI: " + readOntology.getDomainOntology().getUri() + " already loaded - unload it first before saving an owl file");
            
            File targetJar = null;
                
            if( StringUtils.isEmpty(readOntology.getDefaultPackage()) ) throw new Exception("Cannot compile - no vital-core:hasDefaultPackage property set");
                
            JarFileInfo jfi = JarFileInfo.fromOwlInfo(ofi);
                
            targetJar = new File(deployedGroovyJarsDir, organizationID + "/" + appID + "/" + jfi.toFileName());
            
            targetJar.getParentFile().mkdirs();
            
            //check
            //list files to check if the jar is not loaded somewhere else
            for(File f : FileUtils.listFiles(deployedGroovyJarsDir, new String[]{"jar"}, true)) {
                    
                if(f.getAbsolutePath().equals(targetJar.getAbsolutePath())) continue;
                    
                JarFileInfo j = JarFileInfo.fromString(f.getName());
                    
                if(jfi.getDomain().equals(j.getDomain())) throw new Exception("An existing domain " + j.getDomain() + " jar found in another location or with different version: " + f.getAbsolutePath());
                    
            }
            
            FileUtils.deleteQuietly(targetJar);
                
            Path basePath = Files.createTempDirectory("domain");
                
            compileDir = basePath.toFile();
                
            Path srcPath = basePath.resolve("src");
            Files.createDirectories(srcPath);
            Path destPath = basePath.resolve("classes");
            Files.createDirectories(destPath);
                
            DomainGenerator g = VitalSigns.get().generateDomainClasses(new ByteArrayInputStream(FileUtils.readFileToByteArray(ontFile)), srcPath, DomainGenerator.GROOVY, readOntology.getDefaultPackage());
            g.compileSource(destPath);
            g.generateJar(destPath, targetJar.toPath());
                
            //delete source dir
            NIOUtils.deleteDirectoryRecursively(srcPath);
                
        } finally {
            
            FileUtils.deleteQuietly(compileDir);
            
        }
        
        
    }

    public void compileDomainAppOntologyIntoJsonSchema(String organizationID,
            String appID, String owlName) throws Exception {

        FileInputStream fis = null;
        
        BufferedOutputStream os = null;
        
        try {
            
            checkOrgAppMode();
            
            File ontFile = new File(deployedOntologiesDir, organizationID + "/" + appID + "/" + owlName);

            OwlFileInfo ofi = OwlFileInfo.fromString(owlName);
            
            if(!ontFile.exists()) throw new Exception("OWL file not found: " + owlName);
            
            LoadedOntology readOntology = OntVersionCommands.readOntology(ontFile);

            //this is no longer a restriction
//            if( domainModelContainer.get(readOntology.getDomainOntology().getUri() ) != null ) throw new RuntimeException("Ontology with URI: " + readOntology.getDomainOntology().getUri() + " already loaded - unload it first before saving a json file");
            
            File targetJsonSchema = null;
                
            if( StringUtils.isEmpty(readOntology.getDefaultPackage()) ) throw new Exception("Cannot compile - no vital-core:hasDefaultPackage property set");
                
            JsonSchemaFileInfo jfi = JsonSchemaFileInfo.fromOwlInfo(ofi);
                
            targetJsonSchema = new File(deployedJsonSchemasDir, organizationID + "/" + appID + "/" + jfi.toFileName());
            
            targetJsonSchema.getParentFile().mkdirs();
            
            //check
            //list files to check if the jar is not loaded somewhere else
            for(File f : FileUtils.listFiles(deployedJsonSchemasDir, new String[]{"jar"}, true)) {
                    
                if(f.getAbsolutePath().equals(targetJsonSchema.getAbsolutePath())) continue;
                    
                JsonSchemaFileInfo j = JsonSchemaFileInfo.fromString(f.getName());
                    
                if(jfi.getDomain().equals(j.getDomain())) throw new Exception("An existing domain " + j.getDomain() + " json schema found in another location or with different version: " + f.getAbsolutePath());
                    
            }
            
            FileUtils.deleteQuietly(targetJsonSchema);

            
            fis = new FileInputStream(ontFile);
            
            os = new BufferedOutputStream(new FileOutputStream(targetJsonSchema));
                
            JSONSchemaGenerator gen = VitalSigns.get().createJSONSchemaGenerator(fis);
                
            gen.generateSchema();
                
            gen.writeSchemaToOutputStream(false, targetJsonSchema.getName(), os);
                
            IOUtils.closeQuietly(os);
            
                
        } finally {
            
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(os);
            
        }
        
    }

    public void loadOtherDomainVersion(String olderOWLFileName) throws Exception {
        
        DifferentDomainVersionLoader loader = new DifferentDomainVersionLoader();
        loader.load(olderOWLFileName);
        
        
    }

    public int unloadDomains() {

        List<DomainModel> models = getDomainModels();
        
        for(int i = models.size() - 1; i >= 0; i --) {
            VitalSigns.get().deregisterOntology(models.get(i).getURI());
        }
        
        return models.size();
    }

    public int resetDomains() {

        unloadDomains();

        coreDomainModel = null;
        vitalDomainModel = null;
        domainModelContainer = new VITAL_Container();
        orgAppMode = false;
        deployedGroovyJarsDir = null;
        deployedOntologiesDir = null;
        deployedJsonSchemasDir = null;
        
        initDomains();
        
        return getDomainModels().size();
    }

}
