package ai.vital.vitalsigns.domains


import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.algorithms.TopologicalSorting
import ai.vital.vitalsigns.command.patterns.OwlFileInfo;
import ai.vital.vitalsigns.model.DomainModel;
import ai.vital.vitalsigns.model.DomainOntology;
import ai.vital.vitalsigns.model.Edge_hasChildDomainModel
import ai.vital.vitalsigns.model.VITAL_Container;

import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.ontology.OntologyProcessor;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.rdf.RDFUtils;
import ai.vital.vitalsigns.uri.URIGenerator;
import ai.vital.vitalsigns.utils.StringUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import ai.vital.vitalsigns.model.VITAL_Node;

/**
 * An utility class the allows for having different versions of same ontology loaded
 *
 */
public class DifferentDomainVersionLoader {

//    public static final String _OLDVERSION_NS_SUFFIX = "_oldversion";

//    public final static String oldversion_package_suffix = "oldversion";
    
    
    
    
    public final static class VersionedPackage {
        
        public String basePackage;
        
        public String versionPart;
        
        public final static Pattern oldversion_package_pattern = Pattern.compile( "(.+)(\\.v_\\d+_\\d+_\\d+)" );
        
        public static VersionedPackage analyze(String input) {
            
            VersionedPackage vp = new VersionedPackage();
            
            Matcher matcher = oldversion_package_pattern.matcher(input);
            
            if(matcher.matches()) {
                
                vp.basePackage = matcher.group(1);
                vp.versionPart = matcher.group(2);
                
            } else {
                
                vp.basePackage = input;
                
            }
            
            return vp;
            
        }
        
        public static String toVersionedPackage(String inputPackage, DomainOntology ont) {
            return inputPackage + ".v_" + ont.getMajor() + "_" + ont.getMinor() + "_" + ont.getPatch();
        }
        
    }
    
    public final static class VersionedDomain {
        
        public static final Pattern _OLDVERSION_NS_PATTERN = Pattern.compile( "(.+)(_v_\\d+_\\d+_\\d+)" );

        public String domainURI;
        
        public String versionPart;
        
        public static VersionedDomain analyze(String domainURI) {
            
            Matcher matcher = _OLDVERSION_NS_PATTERN.matcher(domainURI);
            
            VersionedDomain v = new VersionedDomain();
            
            if(matcher.matches()) {
                v.domainURI = matcher.group(1);
                v.versionPart = matcher.group(2);
            } else {
                v.domainURI = domainURI;
            }
            
            return v;
            
        }
        
        public static String toVersionedNS(String inputNS, DomainOntology ont) {
            return inputNS + "_v_" + ont.getMajor() + "_" + ont.getMinor() + "_" + ont.getPatch();
        }
        
    }
    
    public final static class VersionedURI {
        
        public static final Pattern _OLDVERSION_NS_PATTERN = Pattern.compile( "(.+)(_v_\\d+_\\d+_\\d+)(#.*)?" );
        
        public String inputURI;
        
        public String versionlessURI;
        
        public String versionPart;
        
        public static VersionedURI analyze(String domainURI) {
            
            Matcher matcher = _OLDVERSION_NS_PATTERN.matcher(domainURI);
            
            VersionedURI v = new VersionedURI();
            
            v.inputURI = domainURI;
            
            if(matcher.matches()) {
                v.versionPart = matcher.group(2);
                v.versionlessURI = matcher.group(1) + (matcher.group(3) != null ? matcher.group(3) : "");
            } else {
                v.versionPart = null;
                v.versionlessURI = domainURI;
            }
            
            return v;
            
        }
        
        public static String toVersionedNS(String inputNS, DomainOntology ont) {
            return inputNS + "_v_" + ont.getMajor() + "_" + ont.getMinor() + "_" + ont.getPatch();
        }
        
    }
    
    private final static Logger log = LoggerFactory.getLogger(DifferentDomainVersionLoader.class);
    
    public Map<String, String> uri2TempURI = new HashMap<String, String>();
    
    public Map<String, String> tempURI2uri = new HashMap<String, String>();
    
    public Map<String, DomainWrapper> tempURI2domainsList = new LinkedHashMap<String, DomainWrapper>();
    
    //versioned to regular
    public Map<String, String> tempPackage2Package = new HashMap<String, String>();
    
    public Map<String, String> package2TempPackage = new HashMap<String, String>();
    
    Map<String, DomainWrapper> uri2DomainsMap = new  HashMap<String, DomainWrapper>();
    

    private List<VITAL_Node> sortedDomains;
    
    public DifferentDomainVersionLoader() {
        
    }
    
    
    public static class DomainWrapper {
        
        public Model model;
        
        public DomainOntology domainOntology;

        byte[] ontologyContent = null;
        
        public DomainWrapper(Model model, DomainOntology domainOntology, byte[] ontologyContent) {
            super();
            this.model = model;
            this.domainOntology = domainOntology;
            this.ontologyContent = ontologyContent;
        }
        
    }
    
    
    public void load(String ontologyFileName) throws Exception {
        
        File vh;
        try {
            vh = VitalSigns.get().getVitalHomePath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        File domainOntologyDir = null;
        File domainOntologyArchiveDir = null;
        domainOntologyDir = new File(vh, "domain-ontology");
        if(!domainOntologyDir.isDirectory()) throw new RuntimeException("$VITAL_HOME/domain-ontology does not exist or not a directory: " + domainOntologyDir.getAbsolutePath());
        domainOntologyArchiveDir = new File(domainOntologyDir, "archive");
        if(!domainOntologyArchiveDir.isDirectory()) throw new RuntimeException("$VITAL_HOME/domain-ontology/archive does not exist or not a directory: " + domainOntologyArchiveDir.getAbsolutePath());

        File archivedVersion = new File(domainOntologyArchiveDir, ontologyFileName);
        if(!archivedVersion.isFile()) throw new Exception("Archived owl file does not exist or not a file: " + archivedVersion.getAbsolutePath());
        
        byte[] ontologyBytes = FileUtils.readFileToByteArray(archivedVersion);
        
        
        List<DomainWrapper> allOntologies = readAllOntologies();
        
        load(ontologyBytes, allOntologies);
     
        
    }
    
    public void load(byte[] ontologyBytes, List<DomainWrapper> allOntologies) throws Exception {
        Model model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(ontologyBytes), "UTF-8");
        DomainOntology ontologyMetaData = OntologyProcessor.getOntologyMetaData(model);
        
        String _package = ontologyMetaData.getDefaultPackage();
        if(StringUtils.isEmpty(_package)) throw new Exception("Ontology does not provide default package - cannot load it: " + ontologyMetaData.toVersionIRI());
        
        String thisOriginalURI = ontologyMetaData.getUri();
        
        DomainOntology existingOntology = VitalSigns.get().getDomainOntology(thisOriginalURI);
        if(existingOntology == null) throw new Exception("Current domain ontology not found: " + thisOriginalURI);
        
        if(existingOntology.compareTo(ontologyMetaData) == 0) throw new Exception("Current ontology and other ontology versions are the same: " + existingOntology.toVersionIRI());
        if(existingOntology.compareTo(ontologyMetaData) < 0) throw new Exception("Current ontology is older than the other ontology: " + existingOntology.toVersionString() + " older? " + ontologyMetaData.toVersionString());
        
        uri2DomainsMap.put(ontologyMetaData.getUri(), new DomainWrapper(model, ontologyMetaData, ontologyBytes));
        
        Set<String> parents = ontologyMetaData.getParentOntologies();
        
        for(Iterator<String> iterator = parents.iterator(); iterator.hasNext(); ) {
            String u = iterator.next();
            if(VitalCoreOntology.ONTOLOGY_IRI.equals(u) || "http://vital.ai/ontology/vital".equals(u)) {
                iterator.remove();
            }
        }
        
        while(parents.size() > 0) {
            
            Set<String> newParents = new HashSet<String>();
            
            for(String parentURI : parents) {
                
                String parentDomainName = RDFUtils.getLocalURIPart(parentURI);
                
                OwlFileInfo preferredVersion = null;
                
                Set<String> preferredImportVersions = ontologyMetaData.getPreferredImportVersions();
                if(preferredImportVersions != null) {
                    for(String vi : preferredImportVersions) {
                        OwlFileInfo ofi = OwlFileInfo.fromString(vi);
                        if(ofi.getDomain().equals(parentDomainName)) {
                            preferredVersion = ofi;
                            break;
                        }
                    }
                }
                
                
                DomainWrapper parent = null;
                
                List<DomainWrapper> dws = new ArrayList<DomainWrapper>();
                
                //check current then domain
                for(DomainWrapper dw : allOntologies) {
                    
                    if(dw.domainOntology.getUri().equals(parentURI)) {
                        dws.add(dw);
                    }
                    
                }

                if(dws.size() == 0) throw new Exception("Parent ontology not found: " + parentURI);
                
                if(preferredVersion != null) {
                    
                    for(DomainWrapper dw : dws) {
                        
                        if( dw.domainOntology.toVersionString().equals(preferredVersion.toVersionNumberString() ) ) {
                            parent = dw;
                            log.info("Preferred parent version found: " + preferredVersion.toFileName());
                            break;
                        }
                        
                    }
                    
                }
                
                if(parent == null) {
                    parent = dws.get(0);
                    if(preferredVersion != null) {
                        log.warn("Preferred parent version not found: " + preferredVersion.toFileName() + ", using other: " + parent.domainOntology.toVersionString());
                    }
                }
                
                //
                
                if(parent == null) throw new Exception("Parent ontology not found: " + parentURI);

                uri2DomainsMap.put(parentURI, parent);
                
                for(String u : parent.domainOntology.getParentOntologies()) {

                    if(VitalCoreOntology.ONTOLOGY_IRI.equals(u) || "http://vital.ai/ontology/vital".equals(u)) {
                        
                    } else if(uri2DomainsMap.containsKey(parentURI)) {
                        
                    } else {
                        newParents.add(u);
                    }
                    
                }
                
                
                
            }
            
            parents = newParents;
            
        }
        
        //create the dependencies graph
        VITAL_Container container = new VITAL_Container();
        
        for(DomainWrapper wrapper : uri2DomainsMap.values() ) {
            
            if(StringUtils.isEmpty(wrapper.domainOntology.getDefaultPackage())) throw new Exception("Ontology does not provide default package - cannot load it: " + wrapper.domainOntology.toVersionIRI());
            
            DomainModel dm = new DomainModel();
            dm.setURI(wrapper.domainOntology.getUri());
            container.putGraphObject(dm);
        }
        for(DomainWrapper wrapper : uri2DomainsMap.values() ) {
            DomainModel dm = (DomainModel) container.get(wrapper.domainOntology.getUri());
            for(String parent : wrapper.domainOntology.getParentOntologies()) {
                DomainModel parentNode = (DomainModel) container.get(parent);
                if(parentNode == null) continue;
                Edge_hasChildDomainModel e = new Edge_hasChildDomainModel();
                e.setURI(URIGenerator.generateURI((VitalApp)null, Edge_hasChildDomainModel.class));
                e.addSource(parentNode).addDestination(dm);
                container.putGraphObject(e);
                
            }
        }
        
        this.sortedDomains = TopologicalSorting.sort(container, Edge_hasChildDomainModel.class, true);
        
        for(DomainWrapper wrapper : uri2DomainsMap.values() ) {
            
            //prepare the
            String originalURI = wrapper.domainOntology.getUri();
            String oldURI = VersionedDomain.toVersionedNS(originalURI, wrapper.domainOntology);// _OLDVERSION_NS_SUFFIX;
            
            tempURI2uri.put(oldURI, originalURI);
            uri2TempURI.put(originalURI, oldURI);
         
            String originalPackage = wrapper.domainOntology.getDefaultPackage();
            String oldPackage = VersionedPackage.toVersionedPackage(originalPackage, wrapper.domainOntology);
            
            tempPackage2Package.put(oldPackage, originalPackage);
            package2TempPackage.put(originalPackage, oldPackage);
            
        }
        
        for(VITAL_Node dmn : sortedDomains) {
            
            DomainWrapper wrapper = uri2DomainsMap.get(dmn.getURI());
            
            log.info("Loading domain: " + wrapper.domainOntology.getUri());
            
            String originalURI = wrapper.domainOntology.getUri();
            String oldURI = uri2TempURI.get(originalURI);
            
            
            DomainWrapper domainOntology = tempURI2domainsList.get(oldURI);
            if(domainOntology != null) {
                throw new Exception("Cannot load an older version, already an old version detected: " + domainOntology.domainOntology.toVersionString());
            }
            
            
//            String packageSuffx = "." + oldversion_package_suffix ;//"version_" + ontologyMetaData.toVersionString().replace('.', '_');
            
            String __package = VersionedPackage.toVersionedPackage(wrapper.domainOntology.getDefaultPackage(), wrapper.domainOntology);
            
            String ontString = new String(wrapper.ontologyContent, "UTF-8");
            
            //replace all uris
            for(Entry<String, String> e : uri2TempURI.entrySet() ) {
                
                String _originalURI = e.getKey();
                String _oldURI = e.getValue();
                
                ontString = ontString.replace(_originalURI + "#", _oldURI + "#").replace(_originalURI + "\"", _oldURI + "\"");
                
            }
            
            byte[] _ontologyBytes = ontString.getBytes("UTF-8");
            
            
            VitalStatus status = VitalSigns.get().registerOWLOntology(new ByteArrayInputStream(_ontologyBytes), __package);
            
            if(status.getStatus() != VitalStatus.Status.ok) throw new Exception("Couldn't register other ontology version: " + status.getMessage());
            
            
            log.info("Domain registered: " + wrapper.domainOntology.getUri());
            
            tempURI2domainsList.put(oldURI, wrapper);
            
        }
        
        log.info("All ontologies registered");
        
        //refactor ontology URI
//        Resource ontRes = model.getResource(originalURI);
//        ontRes = ResourceUtils.renameResource(ontRes, oldIRI);
        
//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        model.write(os);
        
        
    }
    
    private List<DomainWrapper> readAllOntologies() throws Exception {

        File vh;
        try {
            vh = VitalSigns.get().getVitalHomePath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        File domainOntologyDir = null;
        File domainOntologyArchiveDir = null;
        domainOntologyDir = new File(vh, "domain-ontology");
        if(!domainOntologyDir.isDirectory()) throw new RuntimeException("$VITAL_HOME/domain-ontology does not exist or not a directory: " + domainOntologyDir.getAbsolutePath());
        domainOntologyArchiveDir = new File(domainOntologyDir, "archive");
        if(!domainOntologyArchiveDir.isDirectory()) throw new RuntimeException("$VITAL_HOME/domain-ontology/archive does not exist or not a directory: " + domainOntologyArchiveDir.getAbsolutePath());
        
        List<DomainWrapper> l = new ArrayList<DomainWrapper>();
        
        List<File> files = new ArrayList<File>(FileUtils.listFiles(domainOntologyDir, new String[]{"owl"}, false));
        
        files.addAll(FileUtils.listFiles(domainOntologyArchiveDir, new String[]{"owl"}, false));
        
        for(File f : files) {

            Model model = ModelFactory.createDefaultModel();
            byte[] content = FileUtils.readFileToByteArray(f);
            model.read(new ByteArrayInputStream(content), null);
            DomainOntology ontologyMetaData = OntologyProcessor.getOntologyMetaData(model);
            
            l.add(new DomainWrapper(model, ontologyMetaData, content));
            
            
        }
        
        return l;
    }

    /*
    //converts the compactString into temp domain and loads 
    public GraphObject readConverted(String compactString) {
        
        //use very detailed replace strategy ?
        
        for(Entry<String, String> e : uri2TempURI.entrySet()) {
         
            String inputURI = e.getKey();
            
            String outputURI = e.getValue();
            
            compactString = compactString.replace(inputURI + "#", outputURI + "#")
                    .replace(inputURI + "\"", outputURI + "\"");
            
        }
            
        
        //replace all references, all properties
//        String[] columns = compactString.split("\t");
//        
//        StringBuilder b = new StringBuilder();
//        
//        for(int i = 0 ; i < columns.length; i++) {
//            
//            if(i > 0) b.append('\t');
//            
//            String col = columns[i];
//            
//            int indexOfColon = col.indexOf("=");
//            
//            String name = col.substring(0, indexOfColon);
//            
//            int typeIndex = name.indexOf('|');
//            
//            String typeAppender = null;
//            
//            if(typeIndex > 0) {
//                typeAppender = name.substring(typeIndex);
//                name= name.substring(0, typeIndex);
//            }
//            
//            if(uri2TempURI.containsKey(name)) {
//                
//            }
//            
//            if(typeAppender != null) {
//                b.append(typeAppender);
//            }
//            
//            //string dquotes
//            String value = col.substring(indexOfColon + 2, col.length() - 1);
//            
//            String unescaped = StringEscapeUtils.unescapeJava(value);
//            
//            //replace
//            if(uri2TempURI.containsKey(unescaped)) {
//              
//                String newURI = uri2TempURI.get(unescaped);
//                
//                value = StringEscapeUtils.escapeJava(newURI);
//                
//            }
//            
//            b.append("=\"").append(value).append('"');
//            
//        }
        
        return CompactStringSerializer.fromString(compactString);
        
    }
    */
    
    /*
    //upgraded/downgraded object in temp domains
    public String writeConverted(GraphObject g) {
        
        String compactString = g.toCompactString();
        
        
        for(Entry<String, String> e : uri2TempURI.entrySet()) {
         
            String inputURI = e.getValue();
            
            String outputURI = e.getKey();
            
            compactString = compactString.replace(inputURI + "#", outputURI + "#")
                    .replace(inputURI + "\"", outputURI + "\"");
            
        }
        
        return compactString;
        
    }
    */
    
    public void cleanup() throws Exception {
        
        for(int i = sortedDomains.size() - 1; i >= 0; i--) {
            
            String originalURI = sortedDomains.get(i).getURI();
            
            String oldURI = uri2TempURI.get(originalURI);
            
            VitalStatus status = VitalSigns.get().deregisterOntology(oldURI);
            
            if(status.getStatus() != VitalStatus.Status.ok) throw new Exception("Couldn't deregister older ontology version: " + status.getMessage());
            
        }
        
    }
    
    public Map<String, String> getDomainURI2VersionMap() {
        
        Map<String, String> m = new HashMap<String, String>();
        
        for(Entry<String, DomainWrapper> entry : uri2DomainsMap.entrySet()) {
            
            m.put(entry.getKey(), entry.getValue().domainOntology.toVersionString());
            
        }
        
        return m;
        
    }
    
}
