package ai.vital.vitalservice.impl;

import groovy.lang.GroovyShell;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.query.querybuilder.VitalBuilder;
import ai.vital.vitalservice.BaseDowngradeUpgradeOptions;
import ai.vital.vitalservice.DowngradeMapping;
import ai.vital.vitalservice.DowngradeOptions;
import ai.vital.vitalservice.DropMapping;
import ai.vital.vitalservice.ServiceOperations;
import ai.vital.vitalservice.ServiceOperations.Type;
import ai.vital.vitalservice.UpgradeMapping;
import ai.vital.vitalservice.UpgradeOptions;
import ai.vital.vitalservice.VitalService;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.VitalSignsDomainClassLoader;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.BlockIterator;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.VitalBlock;
import ai.vital.vitalsigns.block.CompactStringSerializer;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader;
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader.VersionedPackage;
import ai.vital.vitalsigns.model.DomainModel;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalSegment;
import ai.vital.vitalsigns.model.properties.Property_hasSegmentID;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.properties.PropertyMetadata;

public class UpgradeDowngradeProcedure {

    private final static Logger log = LoggerFactory.getLogger(UpgradeDowngradeProcedure.class);
    
    private VitalService service;
    
    private VitalServiceAdmin adminService;
    
    private VitalApp adminApp;
    
    public UpgradeDowngradeProcedure(VitalService service) {
        this.service = service;
    }
    
    public UpgradeDowngradeProcedure(VitalServiceAdmin adminService,
            VitalApp adminApp) {
        super();
        this.adminService = adminService;
        this.adminApp = adminApp;
    }

    //UPGRADE\s*\{.*\}
    static Pattern upgradePattern = Pattern.compile("UPGRADE\\s*\\{.*\\}");
    static Pattern downgradePattern = Pattern.compile("DOWNGRADE\\s*\\{.*\\}");
    
    // value\s+(\S+)\s*:\s*('(\\'|[^'])*')
    static Pattern valuePattern1 = Pattern.compile("value\\s+(\\S+)\\s*:\\s*('(\\\\'|[^'])*')");
    
    // value\s+(\S+)\s*:\s*("(\\"|[^"])*")
    static Pattern valuePattern2 = Pattern.compile("value\\s+(\\S+)\\s*:\\s*(\"(\\\\\"|[^\"])*\")");
    
    // value\s+(\S+)\s*:\s*(\[[^\]]*\])
    static Pattern valuePattern3 = Pattern.compile("value\\s+(\\S+)\\s*:\\s*(\\[[^\\]]*\\])");
    
    
    public static ServiceOperations buildOperations(ServiceOperations serviceOps) {
        if(serviceOps.isParsed()) throw new RuntimeException("Service operations object was already built by vitalbuilder");
        if(serviceOps.getDowngradeUpgradeBuilderContents() == null) throw new RuntimeException("No source builder contents");
        return new VitalBuilder().queryString(serviceOps.getDowngradeUpgradeBuilderContents()).toService();
        
    }
    
    public static ServiceOperations parseUpgradeDowngradeBuilder(String builderContents) {
        
        String oneLiner = builderContents.replace("\r\n", " ").replace("\n", " ");
        
        boolean upgrade = upgradePattern.matcher(oneLiner).find();
        boolean downgrade = downgradePattern.matcher(oneLiner).find();
        
        if(!upgrade && !downgrade) throw new RuntimeException("Builder string does not seem to be UPGRADE/DOWNGRADE type");
        if(upgrade && downgrade) throw new RuntimeException("Builder string has both DOWNGRADE and UPGRADE sections - it is forbidden");
        
        
        ServiceOperations ops = new ServiceOperations();
        BaseDowngradeUpgradeOptions opts = null;
        
        if(upgrade) {
            opts = new UpgradeOptions();
            ops.setType(Type.UPGRADE);
            ops.setUpgradeOptions((UpgradeOptions) opts);
        } else {
            opts = new DowngradeOptions();
            ops.setType(Type.DOWNGRADE);
            ops.setDowngradeOptions((DowngradeOptions) opts);
        }
        
        Map<String, String> stringVals = new HashMap<String, String>();
        
        Matcher m1 = valuePattern1.matcher(oneLiner);
        
        Matcher m2 = valuePattern2.matcher(oneLiner);
        
        GroovyShell shell = new GroovyShell(VitalSignsDomainClassLoader.get());
        
//        ServiceOperations ops = new ServiceOperations();
//        ops.set
        
        while(m1.find()) {
            String n = m1.group(1);
            String valEscaped = m1.group(2);
            String val = (String) shell.evaluate(valEscaped);
            stringVals.put(n, val);
        }
        
        while(m2.find()) {
            String n = m2.group(1);
            String valEscaped = m2.group(2);
            String val = (String) shell.evaluate(valEscaped);
            stringVals.put(n, val);
        }

        for(Entry<String, String> e : stringVals.entrySet()) {
            
            String k = e.getKey();
            String v = e.getValue();
            
            if(k.equals("oldOntologyFileName")) {
                opts.setOldOntologyFileName(v);
            } else if(k.equals("oldOntologiesDirectory")) {
                opts.setOldOntologiesDirectory(v);
            } else if(k.equals("sourcePath")) {
                opts.setSourcePath(v);
            } else if(k.equals("sourceSegment")) {
                opts.setSourceSegment(v);
            } else if(k.equals("destinationPath")) {
                opts.setDestinationPath(v);
            } else if(k.equals("destinationSegment")) {
                opts.setDestinationSegment(v);
            } else {
                throw new RuntimeException("Unknown property: " + k);
            }
            
        }
        
        Matcher m3 = valuePattern3.matcher(oneLiner);
        
        while(m3.find()) {
            
            String n = m3.group(1);
            String valEscaped = m3.group(2);
            
            List<String> vals = (List<String>) shell.evaluate(valEscaped);
            
            if(n.equals("domainJars")) {
                opts.setDomainJars(vals);
            } else {
                throw new RuntimeException("Unknown list property: " + n);
            }
            
        }
        
        ops.setDowngradeUpgradeBuilderContents(builderContents);
        
//        //ignore imports
//        List<String> readLines = IOUtils.readLines(new StringReader(builderContents));
//        
//        for(String line : ) {
//            
//            
//            
//        }
        
        return ops;
        
    }


    public static GraphObject upgrade(ServiceOperations serviceOps, GraphObject olderVersion /*String oldObjectLine*/, DifferentDomainVersionLoader loader) throws Exception {
        

//        GraphObject olderVersion = loader.readConverted(oldObjectLine);
//        
//        if(olderVersion == null) throw new RuntimeException("No object deserialized from line: " + oldObjectLine);
        
        for(DropMapping dm : serviceOps.getUpgradeOptions().getDropMappings()) {
            
            if(olderVersion.getClass().getCanonicalName().equals(dm.getDropClass().getCanonicalName())) {
                //drop this object
                return null;
            }
            
        }
        
        UpgradeMapping mapping = null;
        
        
        for(UpgradeMapping m : serviceOps.getUpgradeOptions().getUpgradeMappings()) {
            
            if(olderVersion.getClass().getCanonicalName().equals(m.getOldClass().getCanonicalName())) {
                
                if(mapping != null) throw new Exception("More than 1 mapping found class: " + m.getOldClass());
                
                mapping = m;
                
            }
            
        }
        
        GraphObject ni = null;
        
        if(mapping != null) {
            
            ClassMetadata ncmd = VitalSigns.get().getClassesRegistry().getClassMetadata(mapping.getNewClass());
            if(ncmd == null) throw new Exception("Graph object class not found: " + mapping.getNewClass());
            
            ni = ncmd.getClazz().newInstance();
            
            ni.setURI(olderVersion.getURI());
            
            mapping.getClosure().call(olderVersion, ni);
            
            
            
        } else {
            
            //find corresponding class
            String pkgName = null;
            if(olderVersion.getClass().getPackage() != null) {
                pkgName = olderVersion.getClass().getPackage().getName();    
            } else {
                String cname = olderVersion.getClass().getCanonicalName(); 
                pkgName = cname.substring(0, cname.lastIndexOf('.')); 
            }
            
            VersionedPackage vp = VersionedPackage.analyze(pkgName);
            
            String pkg = vp.basePackage;
            
            String canonicalName = pkg + "." + olderVersion.getClass().getSimpleName();
            
            ClassMetadata ncmd = VitalSigns.get().getClassesRegistry().getClassMetadata(canonicalName);
            if(ncmd == null) throw new Exception("Current graph object class not found: " + canonicalName);
            
            ni = ncmd.getClazz().newInstance();
            
            ni.setURI(olderVersion.getURI());
            
            //have some utility to copy properties
            
            cloneProperties(olderVersion, ni, loader);
            
        }
        
        return ni;
        
        
        
    }
    
    public static GraphObject downgrade(ServiceOperations serviceOps, GraphObject g,
            DifferentDomainVersionLoader loader) throws Exception {

        GraphObject ni = null;
        
        for(DropMapping dm : serviceOps.getDowngradeOptions().getDropMappings()) {
            
            if(g.getClass().getCanonicalName().equals(dm.getDropClass().getCanonicalName())) {
                //drop this object
                return null;
            }
            
        }
        
        
        DowngradeMapping mapping = null;
        
        for(DowngradeMapping m : serviceOps.getDowngradeOptions().getDowngradeMappings()) {
            
            if(g.getClass().getCanonicalName().equals(m.getNewClass().getCanonicalName())) {
                
                if(mapping != null) throw new Exception("More than 1 mapping for class: " + m.getNewClass());
                
                mapping = m;
                
            }
            
        }
        
        
        
        if(mapping != null) {

            Class<? extends GraphObject> oldClass = mapping.getOldClass();
            
            ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClassMetadata(oldClass);
            if(cm == null) throw new Exception("Class metadata not found for class: " + oldClass);
            
            ni = cm.getClazz().newInstance();
            
            ni.setURI(g.getURI());
            
            mapping.getClosure().call(g, ni);
            
        } else {
            
            
//            loader.
            
            
            //find corresponding class
            String pkgName = g.getClass().getPackage().getName();
            
            String pkg = loader.package2TempPackage.get(pkgName);
            
            String canonicalName = pkg + "." + g.getClass().getSimpleName();
            
            ClassMetadata ocmd = VitalSigns.get().getClassesRegistry().getClassMetadata(canonicalName);
            
            if(ocmd == null) {
                
                ocmd = VitalSigns.get().getClassesRegistry().getClassMetadata(g.getClass());
                
            }
        
            
            if(ocmd == null) throw new Exception("Older graph object class not found: " + canonicalName + " / " + g.getClass().getCanonicalName());
            
            ni = ocmd.getClazz().newInstance();
            
            ni.setURI(g.getURI());
            
            //have some utility to copy properties
            
            cloneProperties(g, ni, loader);
            
            
        }
        
        return ni;
        
    }
    
    public VitalStatus execute(ServiceOperations serviceOps) throws Exception {
        
        if(serviceOps.isParsed()) throw new Exception("ServiceOperations object was already parsed with vitalbuilder");
        
        BaseDowngradeUpgradeOptions opts = null;
        
        if(serviceOps.getUpgradeOptions() != null && serviceOps.getDowngradeOptions() != null) {
            throw new Exception("Cannot handle both upgrade and download");
        }
        if(serviceOps.getUpgradeOptions() == null && serviceOps.getDowngradeOptions() == null) {
            throw new Exception("No upgrade/downgrade options set");
        }
        
        if(serviceOps.getUpgradeOptions() != null) {
            opts = serviceOps.getUpgradeOptions();
            if(opts == null) throw new Exception("No upgrade options");
        } else {
            opts = serviceOps.getDowngradeOptions();
            if(opts == null) throw new Exception("No downgrade options");
        }
        
        DifferentDomainVersionLoader loader = null;
        
        BufferedReader reader = null;
        
        BlockCompactStringSerializer writer = null;
        
        BlockIterator blocksIterator = null;
      
        BufferedOutputStream bos = null;
        
        try {
            
            
            
            if(opts.getSourcePath() == null && opts.getSourceSegment() == null) throw new Exception("No source, path or segment required");
            if(opts.getDestinationPath() == null && opts.getDestinationSegment() == null) throw new Exception("No destination, path or segment required");
            
            if(opts.getSourcePath() != null && opts.getSourceSegment() != null) throw new Exception("Cannot use both source file path and source segment");
            if(opts.getDestinationPath() != null && opts.getDestinationSegment() != null) throw new Exception("Cannot use both destination file path and destination segment");
            
            if(opts.getSourcePath() != null && opts.getDestinationSegment() != null) throw new Exception("Cannot mix source path and destination segment");
            if(opts.getSourceSegment() != null && opts.getDestinationPath() != null) throw new Exception("Cannot mix source segment and destination path");

            loader = new DifferentDomainVersionLoader();
            
            loader.load(opts.getOldOntologyFileName());
            
            serviceOps = buildOperations(serviceOps);
            
            VitalSegment source = null;
            
            VitalSegment destination = null;
            
            
            File inputFile = null;
            
            File outputFile = null;
            
            if(opts.getSourcePath() != null && opts.getDestinationPath() != null) {

                if(opts.getSourcePath().equals(opts.getDestinationPath())) throw new Exception("Source and destination paths must not be the same: " + opts.getSourcePath());
                
                inputFile = new File(URI.create(opts.getSourcePath()));
                
                outputFile = new File(URI.create(opts.getDestinationPath()));
                
            } else if(opts.getSourceSegment() != null && opts.getDestinationSegment() != null) {
    
                List<VitalSegment> segments = service != null ? service.listSegments() : adminService.listSegments(adminApp);
                
                for(VitalSegment s : segments) {
                    if(s.getRaw(Property_hasSegmentID.class).equals(opts.getSourceSegment())) {
                        source = s;
                    }
                    if(s.getRaw(Property_hasSegmentID.class).equals(opts.getDestinationSegment())) {
                        destination = s;
                    }
                }
                
                if(source == null) throw new Exception("Source segment not found: " + opts.getSourceSegment());
                
                if(destination == null) throw new Exception("Destination segment not found: " + opts.getDestinationSegment());
                
                inputFile = File.createTempFile("vital", ".vital");
                inputFile.deleteOnExit();
//                outputFile = File.createTempFile("vital", "vital");
                
                bos = new BufferedOutputStream(new FileOutputStream(inputFile));
                
                
                //temporary set the older version as preferred
                List<DomainModel> olderVersions = new ArrayList<DomainModel>();
                
                if(opts instanceof UpgradeOptions) {
                    for(DomainModel dm : VitalSigns.get().getDomainModels()) {
                        if(loader.tempURI2uri.containsKey(dm.getURI())) {
                            dm.setProperty("preferred", true);
                            olderVersions.add(dm);
                        }
                    }
                }
                
                if(service != null) {
                    service.bulkExport(source, bos);
                } else {
                    adminService.bulkExport(adminApp, source, bos);
                }
                
                for(DomainModel dm : olderVersions) {
                    dm.setProperty("preferred", null);
                }
                
                bos.close();
                
                if(source.getRaw(Property_hasSegmentID.class).equals(destination.getRaw(Property_hasSegmentID.class))) {
                    
                    //purge the source segment
                    if(service != null) {
                        service.delete(URIProperty.getMatchAllURI(destination));
                    } else {
                        adminService.delete(adminApp, URIProperty.getMatchAllURI(destination));
                    }
                    
                } else {
                    
                    if(opts.isDeleteSourceSegment()) {
                        
                        log.info("Removing source segment: " + source.getRaw(Property_hasSegmentID.class));
                        if(service != null) {
                            log.warn("VitalService does not support segment removals");
                        } else {
                            adminService.removeSegment(adminApp, source, true);
                        }
                        
                    } else {
                        
                        log.info("Source segment left intact: " + source.getRaw(Property_hasSegmentID.class));
                        
                    }
                    
                }
                
            } else {
                
                throw new RuntimeException("Unandled source / destination combination");
                
            }
    
            
            List<GraphObject> buffer = new ArrayList<GraphObject>();
            
            //upgrade procedure
            if(opts instanceof UpgradeOptions) {
                    
                //persist the graph objects temporarily
                if(destination != null) {
                } else {
//                    Map<String, String> domain2Version = loader.getDomainURI2VersionMap();
                    writer = new BlockCompactStringSerializer(outputFile);
                }
                
                //use normal reader
                for( blocksIterator = BlockCompactStringSerializer.getBlocksIterator(inputFile); blocksIterator.hasNext(); ) {

                    VitalBlock block = blocksIterator.next();

                    boolean blockStarted = false;
                    
                    
                    for(GraphObject g : block.toList()) {
                        
                        GraphObject ni = upgrade(serviceOps, g, loader);
                        
                        if(ni != null) {
                            
                            if(!blockStarted) {
                                
                                if(writer != null) {
                                    writer.startBlock();
                                }
                                
                                blockStarted = true;
                                
                            }
                            
                            //look for mapper
                            if(writer != null) {
                                writer.writeGraphObject(ni);
                            }
                            
                            if(destination != null) {
                                buffer.add(ni);
                                flushBuffer(buffer, destination, false);
                            }
                            
                        }
                        
                    }
                 
                    if(blockStarted && writer != null) {
                        writer.endBlock();
                    }
                    
                }
                
                if(writer != null) {
                    writer.close();
                }
                
                if(destination != null) {
                    flushBuffer(buffer, destination, true);
                }
                
                /* XXX older version - updating compact string in place - should no longer be necessary */
                /*
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF-8"));
                
                if(destination != null) {
                    
                } else {
                    writer = new BlockCompactStringSerializer(outputFile);
                }
                
                boolean inblock = false;
                
                boolean blockStarted = false;
                
                for( String line = reader.readLine(); line != null; line = reader.readLine() ) {
                    
                    line = line.trim();
                    if(line.isEmpty()) continue;
                    
                    if(line.startsWith(BlockCompactStringSerializer.BLOCK_SEPARATOR)) {
                        
                        if(inblock && blockStarted && writer != null) {
                            writer.endBlock();
                            blockStarted = false;
                        }
                        
                        inblock = true;
                        continue;
                        
                    }
                    
                    if(line.startsWith(BlockCompactStringSerializer.DOMAIN_HEADER_PREFIX)) continue;
                    
                    //log.info("Upgrading object from line: " + line);
                    GraphObject ni = upgrade(serviceOps, line, loader);
                    
                    if(ni != null) {
                        
                        if(!blockStarted) {
                            
                            if(writer != null) {
                                writer.startBlock();
                            }
                            
                            blockStarted = true;
                            
                        }
                        
                        if(writer != null) {
                            writer.writeGraphObject(ni);
                        }
                        
                        if(destination != null) {
                            buffer.add(ni);
                            flushBuffer(buffer, destination, false);
                        }
                        
                    }
                    
                }
                
                if(writer != null) {
                    
                    if(inblock && blockStarted) writer.endBlock();
                    writer.close();
                    
                }
                
                if(destination != null) {
                    flushBuffer(buffer, destination, true);
                }
                
                
                reader.close();
                
                */
                
            } else if(opts instanceof DowngradeOptions) {

                //persist the graph objects temporarily
                if(destination != null) {
                } else {
                    Map<String, String> domain2Version = loader.getDomainURI2VersionMap();
                    writer = new BlockCompactStringSerializer(outputFile, domain2Version);
                }
                
                //use normal reader
                for( blocksIterator = BlockCompactStringSerializer.getBlocksIterator(inputFile); blocksIterator.hasNext(); ) {

                    VitalBlock block = blocksIterator.next();

                    boolean blockStarted = false;
                    
                    
                    for(GraphObject g : block.toList()) {
                        
                        GraphObject ni = downgrade(serviceOps, g, loader);
                        
                        if(ni != null) {
                            
                            if(!blockStarted) {
                                
                                if(writer != null) {
                                    writer.startBlock();
                                }
                                
                                blockStarted = true;
                                
                            }
                            
                            //look for mapper
                            if(writer != null) {
                                writer.writeGraphObject(ni);
                            }
                            
                            if(destination != null) {
                                buffer.add(ni);
                                flushBuffer(buffer, destination, false);
                            }
                            
                        }
                        
                    }
                 
                    if(blockStarted && writer != null) {
                        writer.endBlock();
                    }
                    
                }
                
                if(writer != null) {
                    writer.close();
                }
                
                if(destination != null) {
                    flushBuffer(buffer, destination, true);
                }
                
                
            }
            
            if(destination != null) {
//                FileUtils.deleteQuietly(inputFile);
            }
            
        } finally {
            
            try {
                loader.cleanup();
            } catch(Exception e) {}
            
            IOUtils.closeQuietly(reader);
            
            try {writer.close(); } catch(Exception e){}
            
            
            IOUtils.closeQuietly(blocksIterator);
            
            IOUtils.closeQuietly(bos);
            
        }
        
        return VitalStatus.withOK();
        
    }



    private void flushBuffer(List<GraphObject> buffer, VitalSegment destination, boolean forced) throws Exception {

        if( ( forced && buffer.size() > 0 ) || buffer.size() >= 1000) {
            log.info("Flushing buffer, size: " + buffer.size());
            
            //use bulk import to ignore uri checking
//            StringWriter sw = new StringWriter();
            StringBuilder sb = new StringBuilder();
            sb.append(BlockCompactStringSerializer.BLOCK_SEPARATOR_WITH_NLINE);
            for(GraphObject go : buffer) {
                CompactStringSerializer.toCompactStringBuilder(go, sb, true);
                sb.append('\n');
            }
//            BlockCompactStringSerializer tempWriter = new BlockCompactStringSerializer(sw);
//            tempWriter.startBlock();
//            for(GraphObject g : buffer) {
//              tempWriter.writeGraphObject(g);  
//            }
//            tempWriter.endBlock();
//            tempWriter.close();
            
            ByteArrayInputStream bis = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
            
            VitalStatus rl = service != null ? service.bulkImport(destination, bis) : adminService.bulkImport(adminApp, destination, bis);
            if(rl.getStatus() != VitalStatus.Status.ok) throw new Exception("Error when persisting: " + rl.getMessage());
            buffer.clear();
        }
        
    }

    public static void cloneProperties(GraphObject source, GraphObject dest, DifferentDomainVersionLoader loader) {

        String pkgName = null;
        if(dest.getClass().getPackage() != null) {
            pkgName = dest.getClass().getPackage().getName();    
        } else {
            String cname = dest.getClass().getCanonicalName(); 
            pkgName = cname.substring(0, cname.lastIndexOf('.')); 
        }

        
        boolean destVersioned = VersionedPackage.analyze(pkgName).versionPart != null;
        
        for(Entry<String, IProperty> entry : source.getPropertiesMap().entrySet()) {
            
            String pname = entry.getKey();
            
            Object v = entry.getValue().rawValue();
            
            int indexOfColon = pname.indexOf(':');
            
            if(indexOfColon >= 0) {
                
                PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(pname);
                if( pm != null ) {
                    
                    if(pm.getURI().equals(VitalCoreOntology.vitaltype.getURI())) {
                        continue;
                    }
                    
                    if(pm.getURI().equals(VitalCoreOntology.types.getURI())) {
                        
                        Collection c = (Collection) v;
                        
                        List<String> newTypes = new ArrayList<String>();
                        
                        for(Object _t : c) {
                            
                            String tURI = (String) _t;
                            
                            if(destVersioned) {
                                
                                //turn subset into versioned uris
                                for( Entry<String, String> x : loader.uri2TempURI.entrySet() ) {
                                    
                                    tURI = tURI.replace(x.getKey() + '#', x.getValue() + '#');
                                    
                                }
                                
                            } else {
                                
                                //remove versions

                                for( Entry<String, String> x : loader.uri2TempURI.entrySet() ) {
                                    
                                    tURI = tURI.replace(x.getValue() + '#', x.getKey() + '#');
                                    
                                }
                                
                            }
                            
                            newTypes.add(tURI);
                            
                        }
                        
                        //we should have a list of string properties 
                        dest.setProperty(pm.getShortName(), newTypes);
                        //only update types
                        continue;
                        
                    }
                    dest.setProperty(pm.getShortName(), v);
                    
                } else {
                    
                    dest.setProperty(pname, v);
                    
                }
            
            
            } else {
                
                dest.setProperty(pname, v);
                
            }
             
        }
        
    }
    
}
