package ai.vital.vitalsigns

import ai.vital.vitalsigns.model.VITAL_Edge

import java.lang.reflect.UndeclaredThrowableException
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path
import java.util.Map.Entry
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.marschall.pathclassloader.PathClassLoader;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.typesafe.config.Config;

import ai.vital.lucene.model.LuceneSegment;
import ai.vital.lucene.model.LuceneSegmentType;
import ai.vital.service.lucene.impl.LuceneServiceQueriesImpl;
import ai.vital.service.lucene.model.LuceneSegmentConfig;
import ai.vital.vitalservice.VitalService;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalExternalSparqlQuery;
import ai.vital.vitalservice.query.VitalExternalSqlQuery;
import ai.vital.vitalservice.query.VitalQuery;
import ai.vital.vitalsigns.binary.VitalSignsBinaryFormat;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.BlockIterator;
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.VitalBlock;
import ai.vital.vitalsigns.block.VitalBlockListener;
import ai.vital.vitalsigns.block.VitalBlockParser;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.classes.ClassesRegistry;
import ai.vital.vitalsigns.classloader.VitalSignsRootClassLoader;
import ai.vital.vitalsigns.conf.VitalSignsConfig;
import ai.vital.vitalsigns.conf.VitalSignsConfig.DomainsStrategy;
import ai.vital.vitalsigns.domains.DomainsSyncImplementation;
import ai.vital.vitalsigns.global.GlobalHashTable;
import ai.vital.vitalsigns.global.GlobalHashTableEdgesResolver;
import ai.vital.vitalsigns.global.PostInitializationHookHandler;
// import ai.vital.vitalsigns.groovy.JavaOperators;
import ai.vital.vitalsigns.json.JSONSchemaGenerator;
import ai.vital.vitalsigns.meta.AnnotationsImplementation;
import ai.vital.vitalsigns.meta.ContainerEdgesResolver;
import ai.vital.vitalsigns.meta.DomainAnnotationsAssigner;
import ai.vital.vitalsigns.meta.DomainPropertyAnnotation;
import ai.vital.vitalsigns.meta.EdgesResolver;
import ai.vital.vitalsigns.meta.GraphContext;
import ai.vital.vitalsigns.meta.HierarchyAccessAssigner;
import ai.vital.vitalsigns.meta.PropertiesHelperAssigner;
import ai.vital.vitalsigns.model.ClassPropertiesHelper;
import ai.vital.vitalsigns.model.DomainModel;
import ai.vital.vitalsigns.model.DomainOntology;
import ai.vital.vitalsigns.model.Edge_hasChildDomainModel;
import ai.vital.vitalsigns.model.Edge_hasParentDomainModel;
import ai.vital.vitalsigns.model.GraphObject;

import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalOrganization;
import ai.vital.vitalsigns.model.VitalSegment;
import ai.vital.vitalsigns.model.properties.Property_hasBackwardCompVersion;
import ai.vital.vitalsigns.model.properties.Property_hasDefaultPackageValue;
import ai.vital.vitalsigns.model.properties.Property_hasDomainOWLHash;

import ai.vital.vitalsigns.model.properties.Property_hasName;



import ai.vital.vitalsigns.model.properties.Property_hasPreferredImportVersions;
import ai.vital.vitalsigns.model.properties.Property_hasVersionInfo;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.ontology.DomainGenerator;
import ai.vital.vitalsigns.ontology.ExtendedOntologyDescriptor;
import ai.vital.vitalsigns.ontology.OntologyDescriptor;
import ai.vital.vitalsigns.ontology.OntologyProcessor;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.properties.PropertiesRegistry;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.properties.PropertyTrait;
import ai.vital.vitalsigns.rdf.RDFFormat;
import ai.vital.vitalsigns.rdf.RDFSerialization;
import ai.vital.vitalsigns.rdf.RDFUtils;
import ai.vital.vitalsigns.uri.URIGenerator;
import ai.vital.vitalsigns.utils.MemoryUtils;
import ai.vital.vitalsigns.utils.NIOUtils;
import ai.vital.vitalsigns.utils.StringUtils;
import ai.vital.vitalsigns.utils.SystemExit

public class VitalSigns {

    public final static String VERSION = "0.8.0";
    
    public final static String GROUP_ID = "vital-ai";
    
    public final static String ARTIFACT_ID = "vitalsigns";
    
	public final static String CACHE_DOMAIN = "CACHE_DOMAIN";
	
	public static File getConfigFile(File vitalHome) {
		return new File(vitalHome, "vital-config/vitalsigns/vitalsigns.config");
	}
	
	private VitalSignsDomainClassLoader vsdcl = VitalSignsDomainClassLoader.get();
	
	private Resource NamedIndividual = ResourceFactory.createResource(OWL.NS + "NamedIndividual");
			
	public static Pattern owlIRIPattern = Pattern.compile("^.+\\/(?<app>[^/]+)\$");
	
	private final static Logger log = LoggerFactory.getLogger(VitalSigns.class);
	
	private PropertiesRegistry propertiesRegistry

	private ClassesRegistry classesRegistry
	
	private static VitalSigns singleton = null
	
	//ontology URI to 
	private Map<String, String> ontologyURI2Package = new HashMap<String, String>();
	
	private Map<String, String> package2OntologyURI = new HashMap<String, String>();
	
	
	private Map<String, DomainOntology> ontologyURI2DomainOntology = new LinkedHashMap<String, DomainOntology>();
	
	private byte[] coreModelBytes = null;
	
	// private Mod
	
	private VitalApp currentApp;
	
	private VitalSignsConfig config;

	private Map<String, String> app2OntologyURI = new HashMap<String, String>();
	
	private Map<String, String> app2Package = new HashMap<String, String>();

	private Map<String, List<String>> ontologyURI2ImportsTree = new HashMap<String, List<String>>();

	private Map<String, ClassLoader> ontologyURI2ClassLoader = new HashMap<String, ClassLoader>();
	
	private Map<String, LuceneSegment> ontologyURI2Segment = new HashMap<String, LuceneSegment>();

	private Map<String, Path> ontologyURI2JimfsDir = new HashMap<String, Path>();
	
	//always keep temp copies of loaded domains, just branches reloading
	private Map<String, File> ontologyURI2TempCopy = new HashMap<String, File>();
	
	private ClassLoader customClassLoader;

	private Map<String, Model> ontologyURI2Model = new HashMap<String, Model>();
	
	
	/**
	 * This static object can be set to override the vitalsigns config before first use
	 */
	public static VitalSignsConfig overriddenConfig;
	
	/**
	 * Base config to be merged with file config (fallback)
	 */
	public static Config mergeConfig;
	
	
	/**
	 * suppresses missing parent domain errors when regenerating a domain in the middle of a tree
	 */
	public static String domainBeingRegenerated = null;
	
	private ModelManager modelManager;
	
	public final static String VITAL_HOME = "VITAL_HOME";

	public static boolean skipVitalDomainLoading = false;
	
	private String vitalHomePath;

	// private VitalLicenseManager licenseMgr;

	private VitalOrganization currentOrganization = null
	
	// private LicenseDetails licenseDetails;

	private GlobalHashTable cache;

	private Map<GraphContext, EdgesResolver> edgesResolvers = new HashMap<GraphContext, EdgesResolver>();

	private VitalService vitalService
	
	private VitalServiceAdmin vitalServiceAdmin

	private OntModel ontModel = null;
	
	//contains the special individuals types that should be ignored - used by vitalsigns only 
	private Set<Resource> specialIndividuals = new HashSet<Resource>(Arrays.asList(VitalCoreOntology.RestrictionAnnotationValue));
	
	private FileSystem jimfs = null;

	
	private Map<String, String> externalNamespaces = new HashMap<String, String>();
	
	private VitalDomainsManager domainsManager = new VitalDomainsManager();
	
	private VitalSigns() {
		
//		GStringEqualsFix.fixGStringEquals();
//		GNumbersEqualsFix.fixGNumbersEquals();
		// JavaOperators.init();
//		TruthConstantsInjector.init();
		
		classesRegistry = new ClassesRegistry()
		propertiesRegistry = new PropertiesRegistry()
	
//		ontologyURI2Package.put(VitalCoreOntology.ONTOLOGY_IRI, VitalCoreOntology.PACKAGE);
//		ontologyURI2ImportsTree.put(VitalCoreOntology.ONTOLOGY_IRI, new ArrayList<String>());
		
		vitalHomePath = System.getenv(VITAL_HOME);
		
		// VitalLicenseContent licenseContent = null;
		
		if( StringUtils.isEmpty(vitalHomePath) ) {
			
			System.err.println(VITAL_HOME + " environment variable not set...");
			
			
			String corePath = "/resources/vital-ontology/" + VitalCoreOntology.getFileName();
			
			try {
				coreModelBytes = IOUtils.toByteArray(VitalSigns.class.getResourceAsStream(corePath));

			} catch (IOException e1) {

			    String m = "Couldn't load core vital model into memory from classpath, path: " + corePath;

				log.error(m);

				System.err.println(m);

				SystemExit.exit(-1, 3000L);

				return
			}
			
			//check if license is embedded in package
			try {
				// byte[] licenseBytes = IOUtils.toByteArray(VitalSigns.class.getResourceAsStream(VitalLicenseManager.LICENSE_RESOURCE));
				
				try {

					//validate license now
					// licenseMgr = new VitalLicenseManager(null);
					// licenseContent = licenseMgr.verifyLicenseBytes(licenseBytes);
					
				} catch(Exception licenseException) {

					/*
				    String m = "VitalSigns inner license validation failed: " + licenseException.getLocalizedMessage();
				    log.error(m);
					System.err.println(m);
					licenseException.printStackTrace();
					if(licenseException instanceof UndeclaredThrowableException) {
						Throwable inner = ((UndeclaredThrowableException)licenseException).getUndeclaredThrowable();
						String m1 = "Inner message: " + ( inner != null? inner.getLocalizedMessage() : null);
						System.err.println(m1);
						if(inner != null) {
						    log.error(m1, inner);
							inner.printStackTrace();
						} else {
						    log.error(m1);
						}
					}
					System.err.flush();
					SystemExit.exit(-1, 3000L);
					return;
					*/


				}
				
			} catch(Exception e) {

				/*

			    String m = "Couldn't find vital-license.lic in classpath: " + VitalLicenseManager.LICENSE_RESOURCE;
			    log.error(m);
				System.err.println(m);
				SystemExit.exit(-1, 3000L);
				return;

				*/
			}
			
			if(overriddenConfig != null) {
				log.info("Using overridden config");
				this.config = overriddenConfig;
			} else {
			
				try {
				
					log.info("Trying to load config from classpath - /resources/vital-config/vitalsigns/vitalsigns.config ...");
					
					this.config = VitalSignsConfig.fromTypesafeConfigStream(VitalSigns.class.getResourceAsStream("/resources/vital-config/vitalsigns/vitalsigns.config"));
						
				} catch(Exception e) {
				
					log.warn("couldn't load vitalsigns config from classpath: /resources/vital-config/vitalsigns/vitalsigns.config - using default");

					this.config = new VitalSignsConfig();
					
				}
			
			}
			
			
		} else {
		
			File vitalHome = new File(vitalHomePath);
			
			if(!vitalHome.exists()) {
			    String m = VITAL_HOME + " path does not exist: " + vitalHome.getAbsolutePath();
			    log.error(m);
				System.err.println(m);
				SystemExit.exit(-1, 3000L);
				return;
			}
			
			if(!vitalHome.isDirectory()) {
			    String m = VITAL_HOME + " path is not a directory: " + vitalHome.getAbsolutePath();
			    log.error(m);
				System.err.println(m);
				SystemExit.exit(-1, 3000L);
				return;
			}
			
			if(overriddenConfig != null) { 
				
				log.info("Using overridden config");
				this.config = overriddenConfig;
				
			} else {

				File configFile = getConfigFile(vitalHome);
				
				if(!configFile.exists()) {
				    String m = "VitalSigns config file does not exist, path: " + configFile.getAbsolutePath();
				    log.error(m);
					System.err.println(m);
					SystemExit.exit(-1, 3000L);
					return;
				}
						
				log.info("Loading config from file: " + configFile.getAbsolutePath());
			
				try {
					this.config = VitalSignsConfig.fromTypesafeConfig(configFile);
				} catch(Exception e) {
				    String m = "VitalSigns config parse error: " + e.getLocalizedMessage() + ", path: " + configFile.getAbsolutePath();
				    log.error(m);
					System.err.println(m);
					SystemExit.exit(-1, 3000L);
					return;
				}
				
			}
			
			
			
			try {
				//validate license now
				// licenseMgr = new VitalLicenseManager(vitalHome);
				// licenseContent = licenseMgr.verifyLicense();
			} catch(Exception licenseException) {

				/*

				System.err.println("VitalSigns license validation failed: " + licenseException.getLocalizedMessage());
				if(licenseException instanceof UndeclaredThrowableException) {
					Throwable inner = ((UndeclaredThrowableException)licenseException).getUndeclaredThrowable();
					String m = "Inner message: " + (inner != null ? inner.getLocalizedMessage() : null);
					System.err.println(m);
					if(inner != null) {
					    log.error(m, inner);
						inner.printStackTrace();
					} else {
					    log.error(m);
					}
				}
				System.err.flush();
				SystemExit.exit(-1, 3000L);
				return;

				*/
			}
			
			
			File coreModelFile = new File(vitalHome, "vital-ontology/" + VitalCoreOntology.getFileName());
			// File coreOwvitalHome
			
			if(!coreModelFile.exists()) {
			    String m = "Vital core ontology file not found: " + coreModelFile.getAbsolutePath();
			    log.error(m);
				System.err.println(m);
				SystemExit.exit(-1, 3000L);
				return;
			}
			
			try {
				coreModelBytes = FileUtils.readFileToByteArray(coreModelFile);
			} catch (IOException e) {
			    String m = "Couldn't load vital core ontology file into memory: " + coreModelFile.getAbsolutePath();
			    log.error(m);
				System.err.println(m);
				SystemExit.exit(-1, 3000L);
			}
			
		}

		
		Level level = Level.toLevel(this.config.logLevel);

		log.info("LogLevel: " + level);
		
		try {
			org.apache.log4j.Logger.getRootLogger().setLevel(level);
		} catch(Throwable e) {
			log.error("Cannot change log level, it may be run in osgi container with a log4j v2 library " + e.getLocalizedMessage());
		}
		
		
		log.info(VITAL_HOME + ": {}", vitalHomePath);
		log.info("LogLocation: " + this.config.logLocation);
		
		if( this.config.logLocation.equals(VitalSignsConfig.STDOUT)) {
		
			//org.apache.log4j.Logger.getRootLogger().addAppender(new ConsoleAppender(new SimpleLayout()))
			
		} else {
		
			File location = new File(this.config.logLocation);
			
			if(!location.isDirectory()) {
				log.error("VitalSigns logLocation does not exist or is not a directory: " + this.config.logLocation);
//				SystemExit.exit(-1, 3000L);
//				return;
			}
			
			if(!location.canRead() || !location.canWrite() || !location.canExecute()) {
				log.error("VitalSigns logLocation path insufficient privileges - Read: " + location.canRead() + " Write: " + location.canWrite() + " Execute: " + location.canExecute() +
						" - " + this.config.logLocation);
//				SystemExit.exit(-1, 3000L);
//				return;
			}
			
			
			try {
				org.apache.log4j.Logger.getRootLogger().addAppender(new FileAppender(new PatternLayout("%d{ISO8601} %-5p: %m%n"), this.config.logLocation, true));
			} catch (IOException e) {
				log.error(e.getLocalizedMessage(), e);
			}
		
		}
		
		java.util.logging.Logger.getLogger(FactoryBuilderSupport.class.getCanonicalName()).setLevel(java.util.logging.Level.SEVERE);
		
		this.modelManager = new ModelManager(this.config);
		
		try {
			this.ontModel = this.modelManager.createNewOntModel();
		} catch (Exception e2) {
			throw new RuntimeException(e2);
		}
		
		
		DomainAnnotationsAssigner.domainAnnotationsInterceptor();
		
		HierarchyAccessAssigner.heirarchyAccessInterceptor();
		
		PropertiesHelperAssigner.assignPropertiesHelper();
		
		VitalSignsRootClassLoader loader = null;

		try {
			loader = VitalSignsRootClassLoader.get();
		} catch(Exception e) {}
		
		if( config.domainsStrategy == DomainsStrategy.classpath ) {
			
			if(loader != null) {
				log.warn("Root classloader set but domains strategy set to classpath!");
			}
			
		} else {
			
			//			if(loader == null) {
			//				log.warn("Root classloader not initialized but domains strategy set to dynamic!");
			//			}
			
		}
		
	}
	
	private void initDomains() {
	    
	    domainsManager.initDomains();
	    
	}
	
	public static VitalSigns get() {
		
		if(singleton == null){
			synchronized (VitalSigns.class) {
				if(singleton == null) {
					initSingleton()
				}
			}
		}
		
		return singleton;
		
	}

	private static long time() { return System.currentTimeMillis(); }
	
	private static void initSingleton() {

		long start = time()
		
		singleton = new VitalSigns()
		
		log.debug("VitalSigns constructor time: {}ms", time() -  start);

		try {

			start = time()

			VitalCoreOntology vitalCoreOntology = new VitalCoreOntology()

			singleton.registerOntology(vitalCoreOntology, null)

			log.debug("Vital Core ontology registration time: {}ms", time() - start);

		} catch(Exception e) {
		    throw new RuntimeException(e)
		}

		// singleton.organization = VitalOrganization.withId(singleton.licenseDetails.getOrganizationID());
        // singleton.organization.set(Property_hasName.class, singleton.licenseDetails.getOrganizationName());

		VitalOrganization organization = VitalOrganization.withId("vital-ai")

		organization.name = "VitalAI"

		println "Organization: " + organization

		// singleton.organization = VitalOrganization.withId("vital-ai")

		// singleton.organization.set(Property_hasName.class, "VitalAI")

		singleton.currentOrganization = organization

		GlobalHashTable.LRU_MAP_SIZE = singleton.@config.cacheLruMapSize

        singleton.cache = GlobalHashTable.get();
        singleton.@cache.setEdgeIndexEnabled(true);
        
          //default resolvers
        singleton.@edgesResolvers.put(GraphContext.Local, new GlobalHashTableEdgesResolver(true, singleton.ontologyURI2Segment));
        singleton.@edgesResolvers.put(GraphContext.Container, new ContainerEdgesResolver());
		
		try {
			singleton.initDomains();	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if(!StringUtils.isEmpty(singleton.@config.postInitializationHook)) {
		    PostInitializationHookHandler.callPostinitializationHook(singleton.config.postInitializationHook, 60d);
		}
		
	}
	
	public static VitalSigns initEmptyVitalSigns() {
		
		if(singleton != null) throw new RuntimeException("VitalSigns already initilized!");
		
		singleton = new VitalSigns();
		
		return singleton;
		
	}

	public PropertiesRegistry getPropertiesRegistry() {
		return propertiesRegistry;
	}

	public Map<String, String> getOntologyURI2Package() {
		return ontologyURI2Package;
	}

	
	public ClassesRegistry getClassesRegistry() {
		return classesRegistry;
	}


	
	// @SuppressWarnings({ "rawtypes", "unchecked" })
	public Class<? extends PropertyTrait> getPropertyClass(String ontologyURI, String pClassName) throws Exception {

		ClassLoader loader1 = ontologyURI2ClassLoader.get(ontologyURI);

		Class cls = null;
		
		if(loader1 != null) {
			try {
				cls = loader1.loadClass(pClassName);
			} catch(Exception e) {
			}
		}
				
		if(cls == null) {
			
			try {
				if(customClassLoader != null) {
					cls = customClassLoader.loadClass(pClassName);
				} else {
					cls = Class.forName(pClassName);
				}
			} catch(Exception e) {
				
			}
		}
		
		try {
			cls = VitalSignsRootClassLoader.get().loadClass(pClassName);
		} catch(Exception e) {}
		
		if(cls == null) return null;
				
		if( ! PropertyTrait.class.isAssignableFrom(cls) ) {
			throw new RuntimeException("Class does not implement PropertyTrait: " + cls.getCanonicalName());
		}
				
		return cls;
		
	}

	// @SuppressWarnings({ "rawtypes", "unchecked" })
	public Class<? extends GraphObject> getGraphObjectClass(String ontologyURI, String clsName) throws Exception {
		
//		return classesRegistry.getGraphObjectClass(clsName);

		ClassLoader loader1 = ontologyURI2ClassLoader.get(ontologyURI);
		
		Class cls = null;
		
		if(loader1 != null) {
			try {
				cls = loader1.loadClass(clsName);
			} catch(Exception e) {
			}
		}
				
		if(cls == null) {
			
			try {
				if(customClassLoader != null) {
					cls = customClassLoader.loadClass(clsName);
				} else {
					cls = Class.forName(clsName);
				}
			} catch(Exception e) {
			}
			
		}
		
		try {
			cls = VitalSignsRootClassLoader.get().loadClass(clsName);
		} catch(Exception e) {}
			
		if(cls == null) return null;
		
		if(!GraphObject.class.isAssignableFrom(cls) ) {
			throw new RuntimeException("Class does not extend GraphObject: " + cls.getCanonicalName());
		}
		
		return cls;
		
	}
	
	
	public GraphObject getIndividual(URIProperty uri) throws IOException {
		return getIndividual(uri.get());
	}
	
	public GraphObject getIndividual(String uri) throws IOException {
		
		if(StringUtils.isEmpty(uri)) throw new NullPointerException("Null or empty uri"); 
		for(LuceneSegment s : new ArrayList<LuceneSegment>(ontologyURI2Segment.values())) {
			GraphObject g = s.getGraphObject(uri);
			if(g != null) return g;
		}
		
		return null;
		
	}
	
	public ModelManager getModelManager() {
		return this.modelManager;
	}
	
	public VitalBlockParser getVitalBlockParser(final Closure<?> c) {
		
		VitalBlockListener listener = new VitalBlockListener(){
			public void onVitalBlock(String URI, List<GraphObject> block) {
				c.call(URI, block);
			}
		};
		
		return new VitalBlockParser(listener);
	}
	
	public VitalBlockParser getVitalBlockParser(VitalBlockListener listener) {
		return new VitalBlockParser(listener);
	}

	
	private void strategyCheck() throws Exception {
		if(config.domainsStrategy == DomainsStrategy.classpath) throw new Exception("Cannot load a domain from URL - no dynamic strategy is set.");
	}
	
	public synchronized int registerOntology(URL domainJarURL) throws Exception {
		
		strategyCheck();
		
		log.info("Loading dynamic ontology from domainJarURL: {}...", domainJarURL.toString());
		
		List<URL> urls = Arrays.asList(domainJarURL);
		
//		ClassLoader loader = URLClassLoader.newInstance(
//			urls.toArray(new URL[urls.size()]),
////			java.lang.ClassLoader.getSystemClassLoader()
//			VitalSigns.class.getClassLoader()
//		);
		
		String domainOntologyURI = null;
		
		//TODO read manifest file to get version info
		InputStream openStream = null;
		JarInputStream jis = null;
		
		Manifest manifest = null;
		
		File tempFile = File.createTempFile("vitaldomain", "jar");
		tempFile.deleteOnExit();
		
		try {
		    
		if(domainJarURL.getProtocol().equals(Jimfs.URI_SCHEME)) {
		    
    //          Preconditions.checkState(path != null);
    		    FileSystem fileSystem = FileSystems.getFileSystem(URI.create("jimfs://" + domainJarURL.getAuthority()));
                Path path = fileSystem.getPath(domainJarURL.getPath() + "/META-INF/MANIFEST.MF");
                openStream = Files.newInputStream(path);
    		    
                manifest = new Manifest(openStream);
                
    		} else {
    		    
    		        openStream = domainJarURL.openStream();
    		        
    		        byte[] cached = IOUtils.toByteArray(openStream);
    		        
    		        jis = new JarInputStream(new ByteArrayInputStream(cached));
    		        manifest = jis.getManifest();
    //		        a.putValue("vital-ontology-version", version);
    		        
    		        FileUtils.writeByteArrayToFile(tempFile, cached);
    		        
    		}
		
		    Attributes attributes = manifest.getMainAttributes();
		    domainOntologyURI = attributes.getValue(VitalManifest.VITAL_ONTOLOGY_URI);
		
		} finally {
		    IOUtils.closeQuietly(jis);
		    IOUtils.closeQuietly(openStream);
		}
		
		
		
		if(StringUtils.isEmpty(domainOntologyURI)) {
		    throw new RuntimeException("No vital-ontology-uri in manifest, URL: " + domainJarURL.toString());
		}

		
		ClassLoader loader = null;
//		VitalSignsURLClassLoader loader = new VitalSignsURLClassLoader(urls.toArray(new URL[urls.size()]), vsdcl);
		
        ClassLoader parentCL = null;
        
        try {
            parentCL = VitalSignsRootClassLoader.get();
        } catch(Exception e) {}
		
		if(domainJarURL.getProtocol().equals(Jimfs.URI_SCHEME)) {
		    loader = new PathClassLoader(getJimfs().getPath(domainJarURL.getPath()), parentCL != null ? parentCL : vsdcl);
		} else {
		    loader = new VitalSignsURLClassLoader(urls.toArray(new URL[urls.size()]), parentCL != null ? parentCL : vsdcl);
		}

		String jarName = domainJarURL.getPath();
		int lastSlash = jarName.lastIndexOf('/');
		if(lastSlash >= 0 && lastSlash < jarName.length() - 1) {
		    jarName = jarName.substring(lastSlash + 1);
		}
		
		int c = registerOntologyFromClassLoader(loader, jarName, domainOntologyURI);
		
		log.info("Ontologies loaded from jar: {} - {}", domainJarURL.toString(), c);
		
		if(c > 0) {
		    ontologyURI2TempCopy.put(domainOntologyURI, tempFile);
		}
		
		return c;
		
	}
	
	private synchronized int registerOntologyFromClassLoader(ClassLoader loader, String jarName, String domainFilter) throws Exception {
		
		strategyCheck();
		
		ServiceLoader<OntologyDescriptor> sLoader = ServiceLoader.load(OntologyDescriptor.class, loader);

		int c = 0;
		
		for( Iterator<OntologyDescriptor> descriptors = sLoader.iterator(); descriptors.hasNext(); ) {
		    
			OntologyDescriptor descriptor = descriptors.next();
			
			String ontologyIRI = descriptor.getOntologyIRI();
			
			if(ontologyURI2Package.containsKey(ontologyIRI)) {
			    continue;
			}
			
			if(domainFilter != null && ! ontologyIRI.equals(domainFilter)) {
			    continue;
			}
			
			
			log.info("Registering ontology - ns:{} package:{}", descriptor.getOntologyIRI(), descriptor.getPackage());
			
			/*
		    InputStream owlInputStream = null;
	        
	        Model m = ModelFactory.createDefaultModel();
	        
	        try {
	            owlInputStream = descriptor.getOwlInputStream();
	            m.read(owlInputStream, null);
	        } finally {
	            IOUtils.closeQuietly(owlInputStream);
	        }
	        */
//	        Set<String> directImports = OntologyProcessor.getDirectImports(m, descriptor.getOntologyIRI());
	        
	        /*
	        ClassLoader parentClassLoader = null;
	        for(String im : directImports) {
	            parentClassLoader = ontologyURI2ClassLoader.get(im);
	        }
	        
	        URLClassLoader classLoaderToClose = null;
	        
	        if(parentClassLoader != null) {
	            
	            if(loader instanceof URLClassLoader) {
	                URLClassLoader current = (URLClassLoader) loader;
	                classLoaderToClose = current;
	                loader = new URLClassLoader(current.getURLs(), parentClassLoader); 
	            } else if(loader instanceof PathClassLoader) {
	                PathClassLoader current = (PathClassLoader) loader;
	                loader = new PathClassLoader(current.getPath(), parentClassLoader);
	            }
	        }
	        */
			
			//XXX
			ontologyURI2ClassLoader.put(descriptor.getOntologyIRI(), loader);
			
			try {
				VitalSignsRootClassLoader.get().setVitalSignsClassLoaders(ontologyURI2ClassLoader.values());
			} catch (Exception e) {}
			
			try {
				singleton.registerOntology(descriptor, jarName);
			} catch(Exception e) {
				ontologyURI2ClassLoader.remove(descriptor.getOntologyIRI());
				throw e;
			}
			
			c++;
			
			if(domainFilter != null) return c;
			
//			if(classLoaderToClose != null) {
//			    classLoaderToClose.close();
//			}
				
		}

		return c;
		
	}
	
	
	FileSystem getJimfs() {
	    if(jimfs == null) {
            jimfs = Jimfs.newFileSystem(Configuration.unix());
	    }
	    return jimfs;
	}
	
	/**
	 * loads the domain straight from the (ontology) owl input stream.
	 * It generates the domain jar internally
	 * @param ontologyInputStream - stream, not closed
	 */
	public synchronized VitalStatus registerOWLOntology(InputStream ontologyInputStream, String _package) {
		
		try {
			strategyCheck();
		} catch (Exception e1) {
			return VitalStatus.withError(e1.getLocalizedMessage());
		}
		
		String ontologyURI = null;
		
		try {

			//required for dynamic properties
//			ClassLoader loader = VitalSignsRootClassLoader.get();
			
		    String base = "/";
		    
		    FileSystem jimfs = getJimfs();
		    
			String uniqueID = null;
			
			while ( uniqueID == null ) {
				uniqueID = RandomStringUtils.randomAlphanumeric(6);
				if(ontologyURI2JimfsDir.values().contains(uniqueID)) {
					uniqueID = null;
				}
				
			}
			
			
			
			//each namespace will get own file system directory
			Path basePath = jimfs.getPath(base + uniqueID);
			
			Path srcPath = basePath.resolve("src");
			Files.createDirectories(srcPath);
			Path destPath = basePath.resolve("classes");
			Files.createDirectories(destPath);
			
			DomainGenerator g = generateDomainClasses(ontologyInputStream, srcPath, DomainGenerator.GROOVY, _package);

			g.compileSource(destPath);
			
			//
			File tempFile = File.createTempFile("vitaldomain", "jar");
			tempFile.deleteOnExit();
			
			g.generateJar(destPath, tempFile.toPath());
			
			
			//delete source dir
			NIOUtils.deleteDirectoryRecursively(srcPath);
			
			/*
			try {
				loader = VitalSignsRootClassLoader.get(); 
			} catch(Exception e) {}
			
			if(loader == null) {
				loader = new PathClassLoader(destPath, VitalSigns.class.getClassLoader());
			}
			*/
			
			ClassLoader parentCL = null;
			
			try {
				parentCL = VitalSignsRootClassLoader.get();
			} catch(Exception e) {}
			
			PathClassLoader loader = new PathClassLoader(destPath, parentCL != null ? parentCL : vsdcl);
			
			//XXX not custom class loader!
//			loader = new VitalClassLoader((PathClassLoader) loader);
//			singleton.setCustomClassLoader(loader);
			
			ontologyURI2JimfsDir.put(g.getOntologyURI(), basePath);
			
			
			//register vitalsigns instance in overridden classloader
//			try {
//				VitalSignsRootClassLoader.get().setVitalSignsPaths(new ArrayList<Path>(ontologyURI2JimfsDir.values()));
//			} catch (Exception e) {
//				log.info("VitalSigns system classloader not set.");
//			}

			String jarName = null;
//			if(ontologyInputStream instanceof FileInputStream) {
//			    ((FileInputStream) ontologyInputStream).g
//			}
			
			int c = registerOntologyFromClassLoader(loader, jarName, null);
			
			if(c < 1) throw new Exception("No ontologies registered for given input stream");
			
			
//			ontologyURI2JimfsDir.put(g.getOntologyURI(), basePath);
			
			ontologyURI = g.getOntologyURI();
			
			ontologyURI2TempCopy.put(g.getOntologyURI(), tempFile);
			
		} catch(Exception e ){
		
			log.error(e.getLocalizedMessage(), e);
			
			if(ontologyURI != null) {
				ontologyURI2JimfsDir.remove(ontologyURI);
			}
			
			return VitalStatus.withError(e.getLocalizedMessage());
		
		}
		
		return VitalStatus.withOKMessage("Successfully registered domain ontology: " + ontologyURI);
		
	}

	public synchronized boolean registerOntology(OntologyDescriptor descriptor, String jarName) throws Exception {

		long start = System.currentTimeMillis();
		
		String ontologyIRI = descriptor.getOntologyIRI();

		log.debug("Registering ontology from descriptor {}, package: {}", ontologyIRI, descriptor.getPackage());
		
		if(ontologyURI2Package.containsKey(ontologyIRI)) {
		    log.warn("Ontology already loaded: {}", ontologyIRI);
			return false;
		}
		
		if(descriptor instanceof ExtendedOntologyDescriptor) {
		    String vdkVersion = ((ExtendedOntologyDescriptor)descriptor).getVitalSignsVersion();
		    if(!VERSION.equals(vdkVersion)) {
		        log.warn("Domain {} was generated with different VitalSigns: {}, current: {}", ontologyIRI, vdkVersion, VERSION);
		    }
		}
		    
		Matcher matcher = owlIRIPattern.matcher(ontologyIRI);
		if(!matcher.matches()) throw new RuntimeException("Ontology URI does not match the pattern " + owlIRIPattern.pattern() + ": " + ontologyIRI);
		
		String app = matcher.group("app");
		
		if(app2OntologyURI.containsKey(app)) {
			throw new RuntimeException("App '" + app + "' already registered with ontology: " + app2OntologyURI.get(app));
		}
		

		for(Entry<String, String> entry : ontologyURI2Package.entrySet()) {
		    
		    if( entry.getValue().equals(descriptor.getPackage())) {
		        
		        String msg = "App '" + app + "' ontologyURI " +  descriptor.getOntologyIRI() + " package " + descriptor.getPackage() + " already registered with another domain ontologyURI: " + entry.getKey();
		        
		        if( config.domainsStrategy == DomainsStrategy.classpath ) {
		            log.warn(msg);
		        } else {
		            throw new RuntimeException(msg);
		        }
		        
		    }
		}
		
		
		InputStream owlInputStream = null;
		
		Model m = ModelFactory.createDefaultModel();
		
		String md5Hash = null;
		
		long mStart = time();
		
		try {
			owlInputStream = descriptor.getOwlInputStream();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			log.debug("Copying source owl stream");
			IOUtils.copy(owlInputStream, os);
			log.debug("Obtaining OWL byte array");
			byte[] byteArray = os.toByteArray();
			log.debug("Calculating MD5 hex");
			md5Hash = DigestUtils.md5Hex(byteArray);
			log.debug("Reading OWL into simple default model");
			m.read(new ByteArrayInputStream(byteArray), null);
			log.debug("Simple model ready {}ms", time() - mStart);
		} finally {
			IOUtils.closeQuietly(owlInputStream);
		}

		mStart = time();
		modelManager.normalizeIndividuals(m);
		log.debug("Normalizing individuals {}ms", time() - mStart);
		
		
		mStart = time();
		if(descriptor instanceof VitalCoreOntology) {
			
			for(ResIterator ri = m.listSubjectsWithProperty(RDF.type, OWL.Class); ri.hasNext(); ) {

				Resource r = ri.nextResource();
				if(OWL.Thing.getURI().equals(r.getURI())) {
					continue;
				}
				
				if(r.getLocalName() != null) {
					classesRegistry.getRestrictedClassNames().add(r.getLocalName());
				}
				
			}
			
		} else {
			
			//check if local class names are unique
			for(ResIterator ri = m.listSubjectsWithProperty(RDF.type, OWL.Class); ri.hasNext(); ) {
				Resource r = ri.nextResource();
				if(OWL.Thing.getURI().equals(r.getURI())) {
					continue;
				}
				if( r.getLocalName() != null && classesRegistry.getRestrictedClassNames().contains(r.getLocalName()) ) {
					throw new RuntimeException("Forbidden class name detected in ontology: " + ontologyIRI + " - " + r.getLocalName() + " - " + r.getURI());
				}
				
			}
		}
		

		log.debug("Model validation time: {}", time() - mStart);
		
		//this part validates the imports
		mStart = time();
		OntModel _ontModel = this.modelManager.createIntermediateOntModel();
		log.debug("Empty ont model time: {}", time() - mStart);
		
		mStart = time();
		OntologyProcessor ontologyProcessor = new OntologyProcessor(classesRegistry, propertiesRegistry);
		DomainOntology _do = ontologyProcessor.processOntology(m, _ontModel, ontologyIRI, descriptor.getPackage());
		log.debug("Domain ontology processor time: {}", time() - mStart);
		
		ontologyURI2Model.put(ontologyIRI, m);
		ontologyURI2Package.put(ontologyIRI, descriptor.getPackage());
		package2OntologyURI.put(descriptor.getPackage(), ontologyIRI);
		ontologyURI2DomainOntology.put(ontologyIRI, _do);
		
		app2OntologyURI.put(app, ontologyIRI);
		app2Package.put(app, descriptor.getPackage());
		
		
		//only if not alreadyt added!
		if( domainsManager.domainModelContainer.get(ontologyIRI) == null ) {
		    
		    DomainModel vdm = new DomainModel();
		    vdm.setURI(ontologyIRI);
		    vdm.set(Property_hasDomainOWLHash.class, md5Hash);
		    vdm.set(Property_hasName.class, jarName);
		    if(_do.getBackwardCompatibleVersion() != null) {
		        vdm.set(Property_hasBackwardCompVersion.class, _do.getBackwardCompatibleVersion().toVersionString());
		    }
		    vdm.set(Property_hasDefaultPackageValue.class, _do.getDefaultPackage());
		    vdm.set(Property_hasVersionInfo.class, _do.toVersionString());
		    if(_do.getPreferredImportVersions() != null) {
		        vdm.set(Property_hasPreferredImportVersions.class, _do.getPreferredImportVersions());
		    }
		    
		    domainsManager.domainModelContainer.putGraphObject(vdm);
		    
		    for(String parentDomain : _do.getParentOntologies()) {
		        
		        DomainModel dm = (DomainModel) domainsManager.domainModelContainer.get(parentDomain);
		        if(dm == null) throw new RuntimeException("Parent domain not found: " + parentDomain);
		        
		        Edge_hasChildDomainModel edm = new Edge_hasChildDomainModel();
		        edm.setURI(URIGenerator.generateURI((VitalApp)null, Edge_hasChildDomainModel.class));
		        edm.addSource(dm).addDestination(vdm);
		        
		        Edge_hasParentDomainModel epm = new Edge_hasParentDomainModel();
		        epm.setURI(URIGenerator.generateURI((VitalApp)null, Edge_hasParentDomainModel.class));
		        epm.addSource(vdm).addDestination(dm);
		        domainsManager.domainModelContainer.putGraphObject(edm);
		        domainsManager.domainModelContainer.putGraphObject(epm);
		        
		    }
		    
		}
		
		//add to main model
//		_ontModel.add(m);
		
		ontModel.add(m);
		
		loadBaseIndividuals(ontologyIRI);
		
		log.info("Ontology " + ontologyIRI + " registration time: {}ms", time() - start);
		
		if(log.isDebugEnabled()) {
		    MemoryUtils.printMemoryUsage();
		}
		
		return true;
		
		/*
		try {
			EdgesAccess.annotateDomain(_package);
			log.debug("Domain package enhanced successfully.");
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e.getLocalizedMessage(), e);
		}
		
		log.debug("Enhancing all graph objects with hyper edge access...");
		
		try {
			HyperEdgeAccess.annotateDomain(_package);
			log.debug("Domain package enhanced with hyper edges successfully.");
		} catch(Exception e) {
			e.printStackTrace();
			log.error(e.getLocalizedMessage(), e);
		}
		
		
		DomainAnnotationsAccessor.annotateDomain(_package, namespace)
		
		HierarchyAccess.annotateDomain(namespace)
		
		if(loadIndividuals) {
		}
		
		return ontModel
		*/
		
		
		
	}

	
	public synchronized VitalStatus deregisterOntology(String ontologyURI) {
		
		long start = time();
		
		if(VitalCoreOntology.ONTOLOGY_IRI.equals(ontologyURI)) {
			return VitalStatus.withError("Cannot deregister vital core ontology: "+ ontologyURI);
		}
		
		String _package = ontologyURI2Package.get(ontologyURI);
		
		if(_package == null) return VitalStatus.withError("Ontology not found: " + ontologyURI);
		
		//check if ontology can be deregistered
		for(Entry<String, List<String>> e : ontologyURI2ImportsTree.entrySet()) {
			if(e.getValue().contains(ontologyURI)) {
				return VitalStatus.withError("Cannot deregister ontology: " + ontologyURI + ". Another ontology depends on it: " + e.getKey());
			}
		}
		
		
		//important to process the properties first
		propertiesRegistry.deregisterDomain(ontologyURI, _package);
		
		classesRegistry.deregisterDomain(ontologyURI, _package);
		
		
		ontologyURI2Package.remove(ontologyURI);
		package2OntologyURI.remove(_package);
		ontologyURI2DomainOntology.remove(ontologyURI);
		ontologyURI2ImportsTree.remove(ontologyURI);
		
		ClassLoader cl = ontologyURI2ClassLoader.remove(ontologyURI);
		if(cl != null) {
		    if(cl instanceof URLClassLoader) {
		        try {
		            log.info("Closing URLClassLoader {}", cl.getClass().getCanonicalName());
                    ((URLClassLoader)cl).close();
                } catch (IOException e1) {
                    log.error(e1.getLocalizedMessage(), e1);
                }
//		    } else if(cl instanceof PathClassLoader) {
//		        ((PathClassLoader)cl).cl
		    }
		}
		
		
		DomainModel model = (DomainModel) domainsManager.domainModelContainer.get(ontologyURI);

		if(model != null) {
		    List<VITAL_Edge> edges = new ArrayList<VITAL_Edge>();
		    edges.addAll( model.getIncomingEdges(GraphContext.Container, domainsManager.domainModelContainer) );
		    edges.addAll( model.getOutgoingEdges(GraphContext.Container, domainsManager.domainModelContainer) );
		    
		    for(VITAL_Edge e : edges) {
		        domainsManager.domainModelContainer.remove(e.getURI());
		    }
		    domainsManager.domainModelContainer.remove(model.getURI());
		    
		}
		
		
		Path jimfsDir = ontologyURI2JimfsDir.remove(ontologyURI);
		if(jimfsDir != null) {
			//delete in memory directory
			try {
				NIOUtils.deleteDirectoryRecursively(jimfsDir);
			} catch(Exception e) {
				//ignore errros
			}
			
		}
		
		//register vitalsigns instance in overridden classloader
		try {
//			VitalSignsRootClassLoader.get().setVitalSignsPaths(new ArrayList<Path>(ontologyURI2JimfsDir.values()));
			VitalSignsRootClassLoader.get().setVitalSignsClassLoaders(ontologyURI2ClassLoader.values());
		} catch (Exception e) {
			log.info("VitalSigns system classloader not set.");
		}
		
		
		Model m = ontologyURI2Model.remove(ontologyURI);
		if(m != null) {
			try { m.close(); } catch(Exception e){}
		}
		
		LuceneSegment luceneSegment = ontologyURI2Segment.get(ontologyURI);
		
		if(luceneSegment != null) {
			try {luceneSegment.close();} catch(Exception e){}
		}
			
		for(Iterator<Entry<String, String>> iterator = app2OntologyURI.entrySet().iterator(); iterator.hasNext(); ) {
			Entry<String, String> e = iterator.next();
			if(e.getValue().equals(ontologyURI)) {
				iterator.remove();
			}
		}
		
		for(Iterator<Entry<String, String>> iterator = app2Package.entrySet().iterator(); iterator.hasNext(); ) {
			Entry<String, String> e = iterator.next();
			if(e.getValue().equals(_package)) {
				iterator.remove();
			}
		}
		
		
		//renew ont model
		ontModel.close();
		
		try {
			ontModel = this.modelManager.createNewOntModel();
		} catch (Exception e1) {
			return VitalStatus.withError("couldn't recreate ont model: " + e1.getLocalizedMessage());
		}
		
		for(Entry<String, Model> e : ontologyURI2Model.entrySet()) {
			ontModel.add(e.getValue());
		}
		
		cache.purgeDomain(ontologyURI);
		
		File f = ontologyURI2TempCopy.remove(ontologyURI);
		if(f != null) {
		    FileUtils.deleteQuietly(f);
		}
		
		log.info("Ontology {} deregistration time: {}ms", ontologyURI, time() - start);
		
		return VitalStatus.withOKMessage("Ontology deregistered successfully: " + ontologyURI);
		
	}

	public List<DomainOntology> getDomainList() {
		return new ArrayList<DomainOntology>(ontologyURI2DomainOntology.values());
	}


	public VitalApp getCurrentApp() {
		return currentApp;
	}

	
	public void setCurrentApp(VitalApp currentApp) {
		this.currentApp = currentApp;
	}

	public VitalSignsConfig getConfig() {
		return config;
	}
	
	/**
	 * Returns an raw external configuration values or <code>null</code> if config value is not set.
	 * Values types: strings, numbers list, maps - raw values of typesafe config objects  
	 * @param key
	 * @return
	 */
	public Object getConfig(String key) {
	    return config.getExternalConfigValue(key);
	}



	public Map<String, String> getApp2ns() {
		return app2OntologyURI;
	}



	public Map<String, List<String>> getOntologyURI2ImportsTree() {
		return ontologyURI2ImportsTree;
	}



	public Map<String, String> getNs2Package() {
		return ontologyURI2Package;
	}



	public Map<String, ClassLoader> getNs2ClassLoader() {
		return ontologyURI2ClassLoader;
	}



	public ClassLoader getCustomClassLoader() {
		return customClassLoader;
	}

	public void setCustomClassLoader(ClassLoader customClassLoader) {
		this.customClassLoader = customClassLoader;
	}



	public synchronized void addToCache(Collection<GraphObject> graphObjects) {
		this.cache.putAll(graphObjects);
	}
	
	public synchronized void addToCache(GraphObject graphObject) {
		this.cache.putAll(Arrays.asList(graphObject));
	}
	
	public Iterator<GraphObject> getCacheIterator() {
	    return this.cache.iterator();
	}
	
    public <T extends GraphObject> Iterator<T> iterator(Class<T> cls) {
        return this.cache.iterator(cls, false);
    }
    
    public <T extends GraphObject> Iterator<T> iterator(Class<T> cls, boolean strict) {
        return this.cache.iterator(cls, strict);
    }
	
	public GraphObject removeFromCache(String uri) {
		return this.cache.remove(uri);
	}
	
	public GraphObject getFromCache(URIProperty uri) {
		if(uri == null) throw new NullPointerException("Null uri parameter");
		return this.cache.get(uri.get());
	}
	
	public GraphObject getFromCache(String uri) {
		if(uri == null) throw new NullPointerException("Null uri parameter");
		return this.cache.get(uri);
	}

	public int getCacheSize() {
		return this.cache.size();
	}
		
	public void purgeCache() {
		this.cache.purge();
	}	
	
	public OntModel getOntologyModel() {
		return ontModel;
	}



	public DomainOntology getClassDomainOntology(
			Class<? extends GraphObject> class1) {


	    String pkg = null;
	    
	    if(class1.getPackage() == null) {
	        pkg = class1.getCanonicalName().substring(0, class1.getCanonicalName().lastIndexOf('.'));
        } else {
            pkg = class1.getPackage().getName();
        }
	    
		for( Entry<String, String> entry : ontologyURI2Package.entrySet() ) {
			
			if(entry.getValue().equals(pkg)) {
				
				return ontologyURI2DomainOntology.get(entry.getKey());
				
			}
			
		}
		
		return null;
		
	}



	public DomainOntology getDomainOntology(String ontologyIRI) {
		return ontologyURI2DomainOntology.get(ontologyIRI);
	}


	public Model getOntologyBaseModel(String ontologyURI) {
		return ontologyURI2Model.get(ontologyURI);
	}
	

	public InputStream getCoreModelInputStream() {
		return new ByteArrayInputStream(coreModelBytes);
	}


	public EdgesResolver getEdgesResolver(GraphContext graphContext) {
		return edgesResolvers.get(graphContext);
	}


	/**
	 * Returns current active vitalService
	 * @return
	 */
	public VitalService getVitalService() {
		return vitalService;
	}

	/**
	 * Sets current active vitalService
	 * @param vitalService
	 */
	public void setVitalService(VitalService vitalService) {
		this.vitalService = vitalService;
	}

	
	/**
	 * Returns current active vitalServiceAdmin
	 */
	public VitalServiceAdmin getVitalServiceAdmin() {
        return vitalServiceAdmin;
    }
	
	/**
	 * Sets current active vitalServiceAdmin
	 * @param vitalServiceAdmin
	 */
    public void setVitalServiceAdmin(VitalServiceAdmin vitalServiceAdmin) {
        this.vitalServiceAdmin = vitalServiceAdmin;
    }

    public boolean isInferenceEnabled() {
		return this.config.inferenceLevel.endsWith("_INF");
	}
	
	public boolean isVitalBlock(byte[] bytes, int offset, int length) {
		return VitalSignsBinaryFormat.isVitalBlock(bytes, offset, length);
	}
	
	/**
	 * Returns a list of all individuals from domain models, exact class
	 * @return
	 */
	@SuppressWarnings("unchecked")
    public <T extends GraphObject> List<T> listDomainIndividuals(Class<T> clazz, String optionalNamespaceFilter) {
		
		List<T> r = new ArrayList<T>();
		
		ClassMetadata cm = classesRegistry.getClassMetadata(clazz);
		
		if(cm == null) throw new RuntimeException("Class not found in VitalSigns: " + clazz.getCanonicalName());

		
		List<ClassMetadata> subclasses = classesRegistry.getSubclasses(cm, true);
		
		List<OntClass> ontClasses = new ArrayList<OntClass>();
		
		for(ClassMetadata c : subclasses) {

			OntClass ontClass = ontModel.getOntClass(c.getURI());
			
			if(ontClass == null) throw new RuntimeException("Class not found in ont model: " + c.getURI());
			
			ontClasses.add(ontClass);
			
		}
		
		for(OntClass cls : ontClasses) {
			
			for(ResIterator iterator = ontModel.listSubjectsWithProperty(RDF.type, cls); iterator.hasNext(); ) {
				Resource mRes = iterator.next();
				
				if(optionalNamespaceFilter != null && ! mRes.getURI().startsWith(optionalNamespaceFilter)) continue;
				
				Model m = ModelFactory.createDefaultModel();
				
				for( StmtIterator listStatements = ontModel.listStatements(mRes, null, (RDFNode) null); listStatements.hasNext(); ) {
					Statement stmt = listStatements.next();
//					if(RDF.type.equals( stmt.getPredicate())) {
//						if(!rdfClass.equals(stmt.getResource().getURI())) continue;
						
//					}
					m.add(stmt);
				}
				
				GraphObject g = VitalSigns.get().readGraphObject(mRes.getURI(), m);
				
				if(g != null) {
					r.add((T) g);
				}
				
			}
			
		}
		
		
		/*
		String rdfClass = VitalSigns.get().getRDFClass(clazz);
		
		
		List<OntClass> classes = new ArrayList<OntClass>();
		classes.add(ontClass);
		EdgesAccess.collectAllSubclasses(ontClass, classes);
		

			
		*/
		return r;
		
	}
	
	
	
	//INDIVIDUALS
	public synchronized int addIndividuals(String ontologyURI, File individualsRDFFile) throws IOException {
		
		Model m = ModelFactory.createDefaultModel();
		m = FileManager.get().readModel(m, individualsRDFFile.getAbsolutePath());
		
		return addIndividuals(ontologyURI, m);
		
	}
	
	public synchronized int addIndividuals(String ontologyURI, Model m) throws IOException {
		
		if(ontologyURI == null) throw new NullPointerException("Domain URI cannot be null");
		
		LuceneSegment segment = ontologyURI2Segment.get(ontologyURI);
		
		if(segment == null) throw new RuntimeException("No lucene segment for URI: " + ontologyURI);
	
		return loadIndividualsFromModel(segment, m);
		
	}
	
	public synchronized int addIndividualsList(String ontologyURI, Collection<GraphObject> objects) throws IOException {
		
		if(ontologyURI == null) throw new NullPointerException("OntologyURI cannot be null");
		
		LuceneSegment segment = ontologyURI2Segment.get(ontologyURI);
		
		if(segment == null) throw new RuntimeException("No lucene segment for URI: " + ontologyURI);
		
		segment.insertOrUpdateBatch(objects);
		
		return objects.size();
		
	}
	
	public synchronized int addIndividualsFromBlockFile(String ontologyURI, File blockFile) throws IOException {
		
		if(ontologyURI == null) throw new NullPointerException("Ontology URI cannot be null");
		
		LuceneSegment segment = ontologyURI2Segment.get(ontologyURI);
		
		if(segment == null) throw new RuntimeException("No lucene segment for URI: " + ontologyURI);
		
		BlockIterator bi = null;
		
		int batchSize = 1000;
		
		List<GraphObject> batch = new ArrayList<GraphObject>();
		
		int total = 0;
		
		try {
			
			for( bi = BlockCompactStringSerializer.getBlocksIterator(blockFile); bi.hasNext() ; ) {
				
				VitalBlock block = bi.next();
			
				batch.add(block.getMainObject());
				batch.addAll(block.getDependentObjects());
				
				total = total + 1 + block.getDependentObjects().size(); 
				
				if(batch.size() >= batchSize) {
					
					segment.insertOrUpdateBatch(batch);
					
					batch.clear();
				}
					
				
			}
			
			if(batch.size() > 0) {
				
				segment.insertOrUpdateBatch(batch);
				
				batch.clear();
			}
			
		} finally {
			if(bi != null) {
				bi.close();
			}
		}
		
		return total;
	}
	
	private int loadIndividualsFromModel(LuceneSegment segment, Model m) throws IOException {
		
		
		List<GraphObject> gos = new ArrayList<GraphObject>();

		for( ExtendedIterator<Resource> iter = m.listSubjectsWithProperty(RDF.type, NamedIndividual); iter.hasNext(); ) {
			
			Resource r = iter.next();
			
			Model local = ModelFactory.createDefaultModel();
			
			Resource rType = null;

			for(StmtIterator si = r.listProperties(); si.hasNext(); ) {
				
				Statement s = si.nextStatement();
				
				if( s.getPredicate().equals(RDF.type)) {
					
					Resource t = s.getResource();
					
					if( NamedIndividual.equals( t ) ) {
						continue;
					}
					
					rType = t;
					
				}
				
				local.add(s);
				
			}
			
			if(rType == null) continue;
			if(specialIndividuals.contains(rType)) {
				continue;
			}
			
			
			if(local.size() > 0) {
				
				//
			    try {
			        GraphObject g = RDFSerialization.deserialize(r.getURI(), local, true);
			        if(g != null) {
			            gos.add(g);
			        }
			    } catch(Exception e) {
			        log.error("Individual skipped: {} - problem: {}", r.getURI(), e.getLocalizedMessage());
			    }
				
			}
		
		}
		
		if(gos.size() > 0) {
			
			segment.insertOrUpdateBatch(gos);
			
		}	
		
		return gos.size();
		
		
	}
	
	public synchronized int reloadIndividuals(String ontologyURI) throws IOException {
		
		if(ontologyURI == null) throw new NullPointerException("Ontology URI cannot be null");
		
		LuceneSegment segment = ontologyURI2Segment.get(ontologyURI);
		
		if(segment == null) throw new RuntimeException("No lucene segment for ontology URI: " + ontologyURI);
	
		segment.close();
		
		ontologyURI2Segment.remove(ontologyURI);
		
		return loadBaseIndividuals(ontologyURI);
			
	}
	
	private int loadBaseIndividuals(String ontologyURI) throws IOException {
		
		Model m = ontologyURI2Model.get(ontologyURI);
		
		if(m == null) throw new RuntimeException("Model for domain URI: " + ontologyURI + " not found.");
		
		VitalOrganization organization = VitalOrganization.withId("vital");
		VitalApp app = VitalApp.withId("vitalsigns");
		VitalSegment segmentObj = VitalSegment.withId(ontologyURI);
		
		LuceneSegmentConfig cfg = new LuceneSegmentConfig(LuceneSegmentType.memory, true, true, null);
        LuceneSegment segment = new LuceneSegment(organization, app, segmentObj, cfg);
		segment.open();
		
		ontologyURI2Segment.put(ontologyURI, segment);
		
		return loadIndividualsFromModel(segment, m);
		
	}

	
	public List<GraphObject> decodeBlock(byte[] bytes, int offset, int length) throws IOException {
		return VitalSignsBinaryFormat.decodeBlock(bytes, offset, length);
	}
	
	public byte[] encodeBlock(List<GraphObject> objects) throws IOException {
		return VitalSignsBinaryFormat.encodeBlock(objects);
	}

	/**
	 * Executes query in domain individuals segments and cache
	 * @param query
	 * @param domains
	 * @return
	 */
	public ResultList query(VitalQuery query) {
		return this.query(query, null);
	}
	
	/**
	 * Executes query in domain individuals segments or cache ( use {@link #CACHE_DOMAIN} )
	 * @param query
	 * @param domains
	 * @return
	 */
	public ResultList query(VitalQuery query, List<String> domains) {
		
	    if(query instanceof VitalExternalSparqlQuery || query instanceof VitalExternalSqlQuery) {
	        throw new RuntimeException("External queries are not supported by vitalsigns locally");
	    }
	    
		List<LuceneSegment> segments = new ArrayList<LuceneSegment>();
		
		if(domains == null) {
			segments.add(this.cache.getLuceneSegment());
			segments.addAll(ontologyURI2Segment.values());
		} else if(domains.size() == 0) {
			throw new RuntimeException("Domains list must not be empty, null means cache+all domain segments");
		} else {
			
			for(String domain : domains) {
				
				if(CACHE_DOMAIN.equals(domain)) {
					segments.add(this.cache.getLuceneSegment());
				} else {
					
					LuceneSegment segment = ontologyURI2Segment.get(domain);
					
					if(segment == null) throw new RuntimeException("Segment for domain " + domain + " not found");
					
					segments.add(segment);
					
				}
				
			}
			
		}

		VitalApp app = VitalApp.withId("vitalsigns-internal");
		
		return LuceneServiceQueriesImpl.handleQuery(currentOrganization, app, query, segments);
		/*
		if(query instanceof VitalSelectQuery) {
			return LuceneServiceQueriesImpl.selectQuery(segments, (VitalSelectQuery) query);
		} else if(query instanceof VitalGraphQuery) {
			Executor executor = new LuceneGraphQueryExecutor(segments);
			GraphQueryImplementation impl = new GraphQueryImplementation(executor, (VitalGraphQuery) query);
			return impl.execute();
		} else if(query instanceof VitalPathQuery) {
			try {
				App app = new App();
				app.setID("vitalsigns-internal");
				app.setOrganizationID(organization.getID());
				return new PathQueryImplementation((VitalPathQuery)query, new LucenePathQueryExecutor(organization, app, segments)).execute();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new RuntimeException("Local query of type " + query.getClass().getCanonicalName() + " not supported");
		}
		*/
		
	}

	
	public GraphObject fromRDF(String ntripleString) {
		return this.fromRDF(ntripleString, RDFFormat.N_TRIPLE);
	}
	
	public GraphObject fromRDF(String rdfString, RDFFormat format) {
		
		//read that into jena model
		Model m = ModelFactory.createDefaultModel();
		m.read(new ByteArrayInputStream(rdfString.getBytes(StandardCharsets.UTF_8)), null, format.toJenaTypeString());
		
		List<GraphObject> gos = RDFSerialization.deserializeFromModel(m, true);
		if(gos.size() == 0) throw new RuntimeException("No graph object deserialized from rdf string");
		if(gos.size() > 1) throw new  RuntimeException("More than 1 graph object deserialized from rdf string, use fromRDFGraph method instead");
		return gos.get(0);
	}
	
	public List<GraphObject> fromRDFGraph(String ntripleString) {
		return this.fromRDFGraph(ntripleString, RDFFormat.N_TRIPLE);
	}
	
	public List<GraphObject> fromRDFGraph(String rdfString, RDFFormat format) {
	
		Model model = ModelFactory.createDefaultModel();
		model.read(new ByteArrayInputStream(rdfString.getBytes(StandardCharsets.UTF_8)), null, format.toJenaTypeString());
		
		return RDFSerialization.deserializeFromModel(model, true);
			
	}
	
	
	//json schema is generated into
	public JSONSchemaGenerator createJSONSchemaGenerator(InputStream owlFileInputStream) throws Exception {
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		IOUtils.copy(owlFileInputStream, os);

		byte[] ontBytes = os.toByteArray();
		
		OntologyData ontData = processOWL(ontBytes);
		
		JSONSchemaGenerator g = new JSONSchemaGenerator(ontData.model, ontData.ontURI);
		g.setVersion(ontData.version);
		g.setDomainOWLHash(ontData.domainOWLHash);
		return g;
		
	}
	
	private OntologyData processOWL(byte[] ontBytes) throws Exception {
		
		Model m = ModelFactory.createDefaultModel();
		m.read(new ByteArrayInputStream(ontBytes), null);

		List<Resource> onts = m.listSubjectsWithProperty(RDF.type, OWL.Ontology).toList();
		
		
		
		if(onts.size() == 0 ) throw new Exception("No ontologies found!");
		if(onts.size() > 1) throw new Exception("More than 1 ontology found!");
		
		String ontURI = onts.get(0).getURI();
		
		String version = RDFUtils.getStringPropertySingleValue(onts.get(0), OWL.versionInfo);
		
		OntModel model = this.modelManager.createIntermediateOntModel();
		
		List<Statement> list = onts.get(0).listProperties(OWL.imports).toList();
		Set<String> alreadyLoaded = new HashSet<String>();
		for(Statement s : list){
			Resource r = s.getResource();
			if(r == null || !r.isURIResource()) throw new Exception("Ontology imports must be URI resources");
			Model im = ontologyURI2Model.get(r.getURI());
			if(im == null) throw new Exception("Ontology import not found in VitalSigns: " + r.getURI());
			model.add(im);
			for( String sub : ontologyURI2ImportsTree.get( r.getURI() ) ) {
				if( alreadyLoaded.add(sub) ) {
					Model x = ontologyURI2Model.get(sub);
					model.add(x);
				}
			}
			
		}
		
		model.add(m);
		//process imports now
		
		String domainOWLHash = DigestUtils.md5Hex(ontBytes);
		return new OntologyData(model, ontURI, version, domainOWLHash);
		
	}
	
	/**
	 * This outputs the 
	 * @param owlFileInputStream the stream is not closed!
	 * @param targetSourceDirectory
	 * @param type
	 * @param _package
	 * @return
	 * @throws Exception 
	 */
	public DomainGenerator generateDomainClasses(InputStream owlFileInputStream, Path targetSourceDirectory, String type, String _package) throws Exception {
	    return generateDomainClasses(owlFileInputStream, targetSourceDirectory, type, _package, false);
	}
	
	public DomainGenerator generateDomainClasses(InputStream owlFileInputStream, Path targetSourceDirectory, String type, String _package, boolean skipDsld) throws Exception {

	    log.info("Copying owl input stream into memory");
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		IOUtils.copy(owlFileInputStream, os);

		byte[] ontBytes = os.toByteArray();
		
		log.info("processing OWL file");
		OntologyData ontData = processOWL(ontBytes);
		
		DomainGenerator generator = new DomainGenerator(ontData.model, ontBytes, ontData.ontURI, _package, targetSourceDirectory);
		generator.skipDsld = skipDsld;
		log.info("generating classes (source)");
		generator.generateDomainSourceClasses();
		log.info("generating properties (source)");
		generator.generateDomainSourceProperties();
		log.info("all source files generated");
		return generator;
		
	}
	
	private static class OntologyData {
		public OntModel model;
		public String ontURI;
		public String version;
		public String domainOWLHash;
		
		public OntologyData(OntModel model, String ontURI, String version, String domainOWLHash) {
			super();
			this.model = model;
			this.ontURI = ontURI;
			this.version = version;
			this.domainOWLHash = domainOWLHash;
		}
	}

	/**
	 * Returns VITAL_HOME dir if only it exists. Exception is thrown otherwise
	 * @return
	 * @throws Exception
	 */
	public File getVitalHomePath() throws Exception {
		if( StringUtils.isEmpty(vitalHomePath) ) throw new Exception(VitalSigns.VITAL_HOME + " not set!");
		return new File(vitalHomePath);
	}
	
	
	public List<DomainPropertyAnnotation> listAnnotatedProperties(Property annotation, IProperty filter) {
		return AnnotationsImplementation.listAnnotatedProperties(annotation, filter);
	}
	
	
	public Map<String, List<DomainPropertyAnnotation>> getPropertyAnnotations(Class<? extends PropertyTrait> vitalProperty, boolean includeSubproperties) {
	    return AnnotationsImplementation.getPropertyAnnotations(vitalProperty, includeSubproperties);
	}
	
	//helper methods
	/**
	 * Returns property instance for given short name or <code>null</code> if not found or more than 1 property found.
	 * An instance is a base class + trait combination
	 * @param name
	 * @return
	 */
	public IProperty getProperty(String name) {
		List<PropertyMetadata> props = propertiesRegistry.listPropertiesWithShortName(name);
		if(props.size() == 0 || props.size() > 1) return null;
		PropertyMetadata pm = props.get(0);
		return pm.getPattern();
	}
	
	/**
	 * Returns property instance with given URI or <code>null</code> if not found
	 * An instance is a base class + trait combination
	 * @param uri
	 * @return
	 */
	public IProperty getProperty(URI uri) {
		PropertyMetadata pm = propertiesRegistry.getProperty(uri.toString());
		if(pm != null) return pm.getPattern();
		return null;
	}
	
	/**
	 * Returns property with given URI or <code>null</code> if not found
	 * An instance is a base class + trait combination
	 * @param uri
	 * @return
	 */
	public IProperty getProperty(URIProperty uri) {
		PropertyMetadata pm = propertiesRegistry.getProperty(uri.get());
		if(pm != null) return pm.getPattern();
		return null;
	}
	
	
	/**
	 * Returns property instance for given trait class or <code>null</code> if not found.
	 * An instance is a base class + trait combination
	 * @param traitClass
	 * @return
	 */
	public IProperty getPropertyByTrait(Class<? extends PropertyTrait> traitClass) {
		PropertyMetadata pm = propertiesRegistry.getPropertyByTrait(traitClass);
		if(pm != null) return pm.getPattern();
		return null;		
	}
	
	
	
	/**
	 * Returns all properties instances with given short name
	 * @param name
	 * @return
	 */
	public List<IProperty> getAllProperties(String name) {
		List<PropertyMetadata> l = propertiesRegistry.listPropertiesWithShortName(name);
		List<IProperty> p = new ArrayList<IProperty>(l.size());
		for(PropertyMetadata m : l) {
			p.add(m.getPattern());
		}
		return p;
	}
	
	/**
	 * Returns graph object class for given short name or <code>null</code> if not found or more than 1 class found.
	 * @param name
	 * @return
	 */
	public Class<? extends GraphObject> getClass(String name) {
		List<ClassMetadata> classes = classesRegistry.listClassesWithShortName(name);
		if(classes.size() == 0 || classes.size() > 1) return null;
		return classes.get(0).getClazz();
	}
	
	/**
	 * Returns class with given URI or <code>null</code> if not found
	 * @param uri
	 * @return
	 */
	public Class<? extends GraphObject> getClass(URI uri) {
		ClassMetadata cm = classesRegistry.getClass(uri.toString());
		if(cm != null) return cm.getClazz();
		return null;
	}
	
	/**
	 * Returns class with given URI or <code>null</code> if not found
	 * @param uri
	 * @return
	 */
	public Class<? extends GraphObject> getClass(URIProperty uri) {
		ClassMetadata cm = classesRegistry.getClass(uri.get());
		if(cm != null) return cm.getClazz();
		return null;
	}
	
	/**
	 * Returns all classes with given short name
	 * @param name
	 * @return
	 */
	public List<Class<? extends GraphObject>> getAllClasses(String name) {
		List<ClassMetadata> l = classesRegistry.listClassesWithShortName(name);
		List<Class<? extends GraphObject>> o = new ArrayList<Class<? extends GraphObject>>();
		for(ClassMetadata cm : l) {
			o.add(cm.getClazz());
		}
		return o;
	}

	public Collection<Path> getDomainsRootPaths() {
		return ontologyURI2JimfsDir.values();
	}

	public Collection<ClassLoader> getOntologiesClassLoaders() {
		return ontologyURI2ClassLoader.values();
	}
	
	public Class<? extends ClassPropertiesHelper> getPropertiesHelperClass(Class<? extends GraphObject> clazz) {
	    
        String helperClass = clazz.getCanonicalName() + DomainGenerator.PROPERTIES_HELPER;
        
        String clsURI = classesRegistry.getClassURI(clazz);
        
        String ontologyURI = RDFUtils.getOntologyPart(clsURI);
        
//      String ontologyURI = package2OntologyURI.get(clazz.getPackage().getName());
        
        ClassLoader cl = ontologyURI2ClassLoader.get(ontologyURI);
        
        Class<?> loadClass = null;
        
        if(cl != null) {
            
            try {
                loadClass = cl.loadClass(helperClass);
            } catch(Exception e){}          
        }

        if(loadClass == null && customClassLoader != null) {
            
            try {
                loadClass = customClassLoader.loadClass(helperClass);
            } catch(Exception e) {}
            
        }
        
        if(loadClass == null) {
            try {
                loadClass = VitalSignsRootClassLoader.get().loadClass(helperClass);
            } catch(Exception e) {}
        }

        if(loadClass == null) {
            try {
                loadClass = Class.forName(helperClass); 
            } catch(Exception e) {}
        }

        if(loadClass == null) throw new RuntimeException("Helper class not found: " + helperClass);
        
        return (Class<? extends ClassPropertiesHelper>) loadClass;
	    
	}
	
	//TODO cache it ?
	public ClassPropertiesHelper getPropertiesHelper(Class<? extends GraphObject> clazz) {
		
		String helperClass = clazz.getCanonicalName() + DomainGenerator.PROPERTIES_HELPER;
		
		String clsURI = classesRegistry.getClassURI(clazz);
		
		String ontologyURI = RDFUtils.getOntologyPart(clsURI);
		
//		String ontologyURI = package2OntologyURI.get(clazz.getPackage().getName());
		
		ClassLoader cl = ontologyURI2ClassLoader.get(ontologyURI);
		
		Class<?> loadClass = null;
		
		if(cl != null) {
			
			try {
				loadClass = cl.loadClass(helperClass);
			} catch(Exception e){}			
		}

		if(loadClass == null && customClassLoader != null) {
			
			try {
				loadClass = customClassLoader.loadClass(helperClass);
			} catch(Exception e) {}
			
		}
		
		if(loadClass == null) {
			try {
				loadClass = VitalSignsRootClassLoader.get().loadClass(helperClass);
			} catch(Exception e) {}
		}

		if(loadClass == null) {
			try {
				loadClass = Class.forName(helperClass); 
			} catch(Exception e) {}
		}

		if(loadClass == null) throw new RuntimeException("Helper class not found: " + helperClass);
		
		try {
			return (ClassPropertiesHelper) loadClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Couldn't initialize helper instance: " + loadClass, e);
		}
		
		
		
		
//	ClassLoader loader1 = ontologyURI2ClassLoader.get(ontologyURI);
//		
//		Class cls = null;
//		
//		if(loader1 != null) {
//			try {
//				cls = loader1.loadClass(clsName);
//			} catch(Exception e) {
//			}
//		}
//				
//		if(cls == null) {
//			
//			try {
//				if(customClassLoader != null) {
//					cls = customClassLoader.loadClass(clsName);
//				} else {
//					cls = Class.forName(clsName);
//				}
//			} catch(Exception e) {
//			}
//			
//		}
//		
//		try {
//			cls = VitalSignsRootClassLoader.get().loadClass(clsName);
//		} catch(Exception e) {}
//			
//		if(cls == null) return null;
//		
//		if(!GraphObject.class.isAssignableFrom(cls) ) {
//			throw new RuntimeException("Class does not extend GraphObject: " + cls.getCanonicalName());
//		}
//		
//		return cls;
		

	}
	
	public GraphObject readGraphObject(String uri, Model model) {
		return RDFSerialization.deserialize(uri, model, true);
	}
	
	public List<GraphObject> readAllGraphObjects(Model model) {
		
		List<GraphObject> res = new ArrayList<GraphObject>();
		
		for(ResIterator iter = model.listSubjects(); iter.hasNext(); ) {
			
			Resource r = iter.nextResource();
			
			GraphObject go = readGraphObject(r.getURI(), model);
			
			if(go != null) res.add(go);
			
		}

		return res;
		
	}

	/**
	 * Adds external namespace to vitalsigns. Must not contain hashes (#).
	 * @param prefix
	 * @param namespace
	 * @return <code>true</code> if it's a new namespace, <code>false</false> if prefix was already mapped
	 */
	public boolean addExternalNamespace(String prefix, String namespace) {
		if(prefix == null) throw new RuntimeException("prefix must not be null");
		if(StringUtils.isEmpty(namespace)) throw new RuntimeException("namespace must not be null nor empty");
		if(namespace.indexOf('#') >= 0) throw new RuntimeException("namespace must not contain hashes (#)");
		boolean r = true;
		if(externalNamespaces.containsKey(prefix)) {
			r = false;
		}
		externalNamespaces.put(prefix, namespace);
		return r;
	}
	
	/**
	 * Removes external namespace from vitalsigns
	 * @param prefix
	 * @return <code>true</code> if namespace existed, <code>false</code> otherwise
	 */
	public boolean removeExternalNamespace(String prefix) {
		if(prefix == null) throw new RuntimeException("prefix must not be null");
		return externalNamespaces.remove(prefix) != null;
	}
	
	/**
	 * Returns <prefix,namespace> map 
	 * @return <prefix,namespace> map
	 */
	public Map<String, String> listExternalNamespaces() {
		return new HashMap<String, String>(externalNamespaces);
//		return new externalNamespaces;
	}

	public Map<GraphContext, EdgesResolver> getEdgesResolvers() {
		return edgesResolvers;
	}

	/**
	 * Organization data loaded from license file
	 */
	public VitalOrganization getOrganization() {
		return currentOrganization;
	}



	
	public static VitalStatus dependencyCheck() {

		try {

			String vitalHomePath = System.getenv(VITAL_HOME);
			
			if(StringUtils.isEmpty(vitalHomePath)) {
				
				// URL lurl = VitalSigns.class.getResource(VitalLicenseManager.LICENSE_RESOURCE);
				// if(lurl == null) throw new RuntimeException(VITAL_HOME + " not set, license classpath resource not found: " + VitalLicenseManager.LICENSE_RESOURCE);
			
				URL courl = VitalSigns.class.getResource("/resources/vital-ontology/" + VitalCoreOntology.getFileName());

				// if(courl == null) throw new RuntimeException(VITAL_HOME + " not set, vital core ontology resource not found: " + VitalLicenseManager.LICENSE_RESOURCE);

				// don't check config file
				
			} else {

				File vitalHome = new File(vitalHomePath);
				if(!vitalHome.exists()) throw new RuntimeException( VITAL_HOME + " path does not exist: " + vitalHome.getAbsolutePath() );
				if(!vitalHome.isDirectory()) throw new RuntimeException(VITAL_HOME + " path is not a directory: " + vitalHome.getAbsolutePath());
				
				// File licenseFile = VitalLicenseManager.getLicensePath(vitalHome);
				// if(!licenseFile.exists()) throw new RuntimeException("License file does not exist: " + licenseFile.getAbsolutePath());
				// if(!licenseFile.isFile()) throw new RuntimeException("License path does not denote a file: " + licenseFile.getAbsolutePath());

				
				File coreModelFile = new File(vitalHome, "vital-ontology/" + VitalCoreOntology.getFileName());
				if(!coreModelFile.isFile()) throw new RuntimeException("Vital core ontology file does not exist or is not a file: " + coreModelFile.getAbsolutePath());
				
				
				File configFile = getConfigFile(vitalHome);
				
				if(!configFile.isFile()) throw new RuntimeException("VitalSigns config file does not exist or is not a file: " + configFile.getAbsolutePath());
				
			}

			
		} catch(Exception e) {
			return VitalStatus.withError(e.getLocalizedMessage());
		}
		
		return VitalStatus.withOKMessage("VitalSigns dependency check passed");
		
	}

	public Iterator<GraphObject> getDomainIndividualsIterator(String ontologyURI) {
	    LuceneSegment segment = ontologyURI2Segment.get(ontologyURI);
	    if(segment == null) throw new RuntimeException("Domain with ontologyURI not loaded");
	    return segment.iterator();
	}

	public <T extends GraphObject> Iterator<T> getDomainIndividualsIterator(String ontologyURI, Class<T> clazz) {
	    LuceneSegment segment = ontologyURI2Segment.get(ontologyURI);
	    if(segment == null) throw new RuntimeException("Domain with ontologyURI not loaded");
	    return segment.iterator(clazz);
	}
	
	public <T extends GraphObject> Iterator<T> getDomainIndividualsIterator(String ontologyURI, Class<T> clazz, boolean strict) {
	    LuceneSegment segment = ontologyURI2Segment.get(ontologyURI);
	    if(segment == null) throw new RuntimeException("Domain with ontologyURI not loaded");
	    return segment.iterator(clazz, strict);
	}
	
	/**
	 * @return list of DomainModel graph objects for domains currently loaded, excluding core and vital domains
	 */
	public List<DomainModel> getDomainModels() {
	    return domainsManager.getDomainModels();
	}
	
	
	/**
	 * Unloads all dynamic domains.
	 * @return number or unloaded domains
	 * @throws Exception 
	 */
	public int unloadDomains() throws Exception {
	    strategyCheck();
	    return domainsManager.unloadDomains();
	}
	
	/**
	 * Reloads dynamic domains
	 * @return number of loaded domains
	 * @throws Exception 
	 */
	public int resetDomains() throws Exception {
	    strategyCheck();
	    return domainsManager.resetDomains();
	}
	
	/**
	 * @param uriFilter
	 * @return list of DomainModel graph objects for given uri filter, the list will return current domain as well as older versions
	 * loaded under versioned package/URI
	 */
	public List<DomainModel> getDomainModels(String uriFilter) {
	   return domainsManager.getDomainModels(uriFilter); 
	}
	
	/**
	 * @return list containing DomainModel, Edge_hasChildOntology, Edge_hasParentOntology, excluding core and vital domains 
	 */
	public List<GraphObject> getDomainModelsWithEdges() {
	    return domainsManager.getDomainModelsWithEdges();
	}
	
	public List<DomainModel> getParentModels(String ontologyURI, boolean direct) {
	    return domainsManager.getParentModels(ontologyURI, direct);
	}
    public List<DomainModel> getChildModels(String ontologyURI, boolean direct) {
        return domainsManager.getChildModels(ontologyURI, direct);
    }

    
    /*
     * Dynamic domains methods
     */
    
    /**
     * 
     * @param organizationID
     * @param appID
     * @param jarName
     * @param content
     * @throws Exception
     */
    public void saveAppDomainGroovyJar(String organizationID, String appID, String jarName, byte[] content) throws Exception {
        domainsManager.saveAppDomainGroovyJar(organizationID, appID, jarName, content);
    }
    
    /**
     * 
     * @param organizationID
     * @param appID
     * @param ontologyName
     * @param content
     * @throws Exception
     */
    public void saveAppDomainOntology(String organizationID, String appID, String ontologyName, byte[] content) throws Exception {
        domainsManager.saveAppDomainOntology(organizationID, appID, ontologyName, content);
    }
    
    /**
     * 
     * @param organizationID
     * @param appID
     * @param ontologyName
     * @param content
     * @throws Exception
     */
    public void saveAppDomainJsonSchema(String organizationID, String appID, String ontologyName, byte[] content) throws Exception {
        domainsManager.saveAppDomainJsonSchema(organizationID, appID, ontologyName, content);
    }
    
    /**
     * 
     * @param organizationID
     * @param appID
     * @param owlName
     * @param content
     * @throws Exception
     */
    public void deleteAppDomainOntology(String organizationID, String appID, String owlName) throws Exception {
        domainsManager.deleteAppDomainOntology(organizationID, appID, owlName);
    }
    
    /**
     * 
     * @param organizationID
     * @param appID
     * @param jarName
     * @param content
     * @throws Exception
     */
    public void deleteAppDomainGroovyJar(String organizationID, String appID, String jarName) throws Exception {
        domainsManager.deleteAppDomainGroovyJar(organizationID, appID, jarName);
    }
    
    /**
     * 
     * @param organizationID
     * @param appID
     * @param jsonSchemaName
     * @param content
     * @throws Exception
     */
    public void deleteAppDomainJsonSchema(String organizationID, String appID, String jsonSchemaName) throws Exception {
        domainsManager.deleteAppDomainJsonSchema(organizationID, appID, jsonSchemaName);
    }
    
    
    /**
     * Loads existing deployed domain jar
     * @param organizationID
     * @param appID
     * @param jarName
     * @throws Exception
     */
    public void loadAppDomainOntology(String organizationID, String appID, String jarName) throws Exception {
        domainsManager.loadAppDomainOntology(organizationID, appID, jarName);
    }
    
    /**
     * Loads an older version of an active domain from archived OWL file looked up in VITAL_HOME/domain-ontology/archive
     * @param olderOWLFileName
     * @throws Exception
     */
    public void loadOtherDomainVersion(String olderOWLFileName) throws Exception {
        domainsManager.loadOtherDomainVersion(olderOWLFileName);
    }
    
    /**
     * Unloads the deployed domain jar
     * @param organizationID
     * @param appID
     * @param jarName
     * @throws Exception
     */
    public void unloadAppDomainOntology(String organizationID, String appID, String jarName) throws Exception {
        domainsManager.unloadAppDomainOntology(organizationID, appID, jarName);
    }
    
    /**
     * Compiles deployed domain ontology into jar
     * @param organizationID
     * @param appID
     * @param owlName
     * @throws Exception
     */
    public void compileDomainAppOntologyIntoJar(String organizationID, String appID, String owlName) throws Exception {
        domainsManager.compileDomainAppOntologyIntoJar(organizationID, appID, owlName); 
    }
    
    /**
     * Compiles deployed domain ontology into json schema
     * @param organizationID
     * @param appID
     * @param owlName
     * @throws Exception
     */
    public void compileDomainAppOntologyIntoJsonSchema(String organizationID, String appID, String owlName) throws Exception {
        domainsManager.compileDomainAppOntologyIntoJsonSchema(organizationID, appID, owlName); 
    }
    
    /**
     * Synchronizes the remote <-> local domain jars. The domainStrategy needs to be set to 'dynamic'.
     * There must a be sync-able vitalservice instance initialized
     * @throws Exception
     */
    public void sync() throws Exception {
        new DomainsSyncImplementation().doSync();
    }

    /**
     * Returns domain jar content or <code>null</code> if not available
     * @throws Exception if domain not found
     * @return
     */
    public byte[] getDomainJarBytes(String ontologyURI) throws Exception {
        if( ! ontologyURI2Package.containsKey(ontologyURI) ) throw new Exception("Domain not found");
        File f = ontologyURI2TempCopy.get(ontologyURI);
        if( f != null ) return FileUtils.readFileToByteArray(f);
        return null;
    }

}
