package ai.vital.vitalsigns.conf;

import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.HashSet
import java.util.List
import java.util.Set

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

import ai.vital.vitalsigns.VitalSigns;

public class VitalSignsConfig {

	public static enum DomainsStrategy {
		/**
		 * Only classpath domains may be (re|un)loaded
		 */
		classpath,
		dynamic
	}
	
	public static enum DomainsSyncMode {
	    
	    /**
	     * default, don't push
	     */
	    none,
	    /**
	     * pushes local domains into remote server
	     */
	    push,
	    /**
	     * pulls domains from remote server
	     */
	    pull,
	    /*
	     * 
	     */
	    both
	}
	
	public static enum DomainsSyncLocation {
	    
	    inmemory,
	    
	    domainsDirectory
	    
	}
	
	
	public static enum DomainsVersionConflict {
	    
	    /**
	     * default, use the server version (unloading the local version regardless of which one is newer)
	     */
	    server,
	    
	    /**
	     * throw a warning and unload the local version (thus making data in that domain not able to be serialized/deserialized) 
	     */
	    unload
	    
	}
	
	public static enum VersionEnforcement {
	    /**
	     * an exception is thrown if any mismatches (unless the domain model 
	     * specifically has an annotation to allow it)
	     */
	    strict,
	    
	    /**
	     * versions are not enforced, but exceptions would be thrown if trying 
	     * to deserialize an object with a property that doesn't exist in the current model
	     */
	    tolerant,
	    
	    /**
	     * which would drop any values if the current domain model didn't have that property and would not throw any exceptions. 
	     */
	    lenient
	    
	}
	
	public final static String STDOUT = "STDOUT";
	
	public final static String DEFAULT = "DEFAULT";
	
	public List<String> ignoreFilesPatterns;
	
	public Integer cacheLruMapSize = 10000;
	
	public String logLevel = "WARN";
	
	public String logLocation = STDOUT;
	
	public boolean enforceConstraints = false;
	
	public String uriBase = "http://vital.ai";
	
	public Config externalConfig = ConfigFactory.empty();
	
	private final static Logger log = LoggerFactory.getLogger(VitalSignsConfig.class);
	
	//https://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/ontology/OntModelSpec.html
	public String inferenceLevel = "OWL_MEM";
	
	/**
	 * This filter ( enabled by default ) makes sure all forbidden xml characters are stripped out
	 * Valid XML 1.0 unicode characters: \u0009, \u0020-\uD7FF, \uE000-\uFFFD, \ud800\udc00-\udbff\udfff 
	 */
	public boolean xmlStringFilter = true;
	
	/**
	 * Determines the way VitalSigns handles domains. 
	 * 
	 */
	public DomainsStrategy domainsStrategy = DomainsStrategy.classpath;
	
	
	/**
	 * A setting for domainStrategy = DomainsStrategy.dynamic - When true it attempts to load all domain jars
	 */
	public boolean autoLoad = true;
	
	/**
	 * A setting for domainStrategy = DomainsStrategy.dynamic - When true it attempts to load all domain jars 
	 * from VITAL_HOME/domain-groovy-deployed directory
	 */
	public boolean loadDeployedJars = false;
	
	
	public DomainsSyncMode domainsSyncMode = DomainsSyncMode.none;
	
	public DomainsSyncLocation domainsSyncLocation = DomainsSyncLocation.inmemory;
	
	public DomainsVersionConflict domainsVersionConflict = DomainsVersionConflict.server;
	
	
	/**
     * Determines if external properties (not defined in ontology) are supported
     */
    public boolean externalProperties = true;
    
	public VersionEnforcement versionEnforcement = VersionEnforcement.tolerant;
	
	
	/**
	 * Optional hook - no args fully qualified method call like:name with parenthesis package1.subpackage2.ClassName.methodName() 
	 */
	public String postInitializationHook = null;
	
	static Set<String> validLevels = new HashSet<String>();
	
	static {
		
		validLevels.add("TRACE_INT");
		validLevels.add("OFF");
		validLevels.add("FATAL");
		validLevels.add("ERROR");
		validLevels.add("WARN");
		validLevels.add("INFO");
		validLevels.add("DEBUG");
		validLevels.add("TRACE");
		validLevels.add("ALL");
		
	} 
	
	public static VitalSignsConfig fromTypesafeConfig(File configFile) throws Exception {
		Config config = ConfigFactory.parseFile(configFile);
		config = config.resolve();
		return  fromTypesafeConfigObject(config); 
	}
	
	public static VitalSignsConfig fromTypesafeConfigStream(InputStream inputStream) throws Exception {
		
		InputStreamReader isr = null;
		
		try {
			isr = new InputStreamReader(inputStream, "UTF-8");
			Config config = ConfigFactory.parseReader(isr);
			config = config.resolve();
			return  fromTypesafeConfigObject(config);
		} finally {
			IOUtils.closeQuietly(isr);
		}
	} 
	
	public static VitalSignsConfig fromTypesafeConfigObject(Config config) throws Exception {
		
	    if(VitalSigns.mergeConfig != null) {
	        
	        log.warn("VitalSigns.mergeConfig instance set, merging with input config, {}", VitalSigns.mergeConfig);

	        config = VitalSigns.mergeConfig.withFallback(config);
	        
	    }
	    
		VitalSignsConfig cfg = new VitalSignsConfig();
		
		cfg.ignoreFilesPatterns = config.getStringList("ignoreFilesPatterns");
		
		try {
			
			cfg.cacheLruMapSize = config.getInt("cacheLruMapSize");
			
		} catch(ConfigException.Missing e) {
			
			//ignore
			log.warn("No cacheLruMapSize, using default: " + cfg.cacheLruMapSize);
			
		}
		
		try {
			
			cfg.logLevel = config.getString("logLevel");
			
		} catch(ConfigException.Missing e) {
		
			log.warn("No logLevel, using default: " + cfg.logLevel);
			
		}

		if( ! validLevels.contains(cfg.logLevel) ) {
			throw new Exception("Invalid logLevel: " + cfg.logLevel + ", valid levels: " + validLevels.toString());
		}
				
		
		try {
			
			cfg.logLocation = config.getString("logLocation");
			
		} catch(ConfigException.Missing e) {
		
			log.warn("No logLocation, using default: " + cfg.logLocation);
		
		}
		
		
		try {
			
			cfg.enforceConstraints = config.getBoolean("enforceConstraints");
			
		} catch(ConfigException.Missing e) {
		
			log.warn("No enforceConstraints, using default: " + cfg.enforceConstraints);	
		
		}
		
		try {
			
			cfg.uriBase = config.getString("uriBase");
			
		} catch(ConfigException.Missing e) {
			
			log.info("No uriBase, using default: " + cfg.uriBase);	
			
		}
		
		try {
			
			cfg.inferenceLevel = config.getString("inferenceLevel");
			
		} catch(ConfigException.Missing e) {
			log.info("No inferenceLevel, using default: " + cfg.inferenceLevel);
		}
		
		
		try {
			
			cfg.xmlStringFilter = config.getBoolean("xmlStringFilter");
			
		} catch(ConfigException.Missing e) {
		
			log.info("No xmlStringFilter, using default: " + cfg.xmlStringFilter);
		
		}

		
		try {
			
			String domainsStrategy = config.getString("domainsStrategy");
			
			DomainsStrategy strat = DomainsStrategy.valueOf(domainsStrategy);
			
			cfg.domainsStrategy = strat;
			
		} catch(ConfigException.Missing e) {
			
			log.info("No domainsStrategy, using default: " + cfg.domainsStrategy.name());
			
		}
		
		try {
		    
		    String domainsSyncMode = config.getString("domainsSyncMode");
		    
		    DomainsSyncMode domainsSyncModeV = DomainsSyncMode.valueOf(domainsSyncMode);
		    
		    cfg.domainsSyncMode = domainsSyncModeV;
		    
		} catch(ConfigException.Missing e) {
		    
		    log.info("No domainsSyncMode, using default: " + cfg.domainsSyncMode.name());
		    
		}
		
		
		try {
		    
		    String domainsSyncLocation = config.getString("domainsSyncLocation");
		    
		    DomainsSyncLocation domainsSyncLocationV = DomainsSyncLocation.valueOf(domainsSyncLocation);
		    
		    cfg.domainsSyncLocation = domainsSyncLocationV;
		    
		} catch(ConfigException.Missing e) {
		    
		    log.info("No domainsSyncLocation, using default: " + cfg.domainsSyncLocation.name());
		    
		}
		
		try {
		    
		    String domainsVersionConflict = config.getString("domainsVersionConflict");
		    
		    DomainsVersionConflict domainsVersionConflictV = DomainsVersionConflict.valueOf(domainsVersionConflict);
		    
		    cfg.domainsVersionConflict = domainsVersionConflictV;
		    
		} catch(ConfigException.Missing e) {
		
		    log.info("No domainsVersionConflict, using default: " + cfg.domainsVersionConflict.name());
		    
		}
		
		
		try {
		    
		    Boolean autoLoad = config.getBoolean("autoLoad");
		    
		    cfg.autoLoad = autoLoad;
		    
		} catch(ConfigException.Missing e) {
		    
		    log.info("No autoLoad, using default: " + cfg.autoLoad);
		    
		}
		
		
		try {
		     
		    Boolean loadDeployedJars = config.getBoolean("loadDeployedJars");
		    
		    cfg.loadDeployedJars = loadDeployedJars;
		    
		} catch(ConfigException.Missing e) {
		    
		    log.info("No loadDeployedJars, using default: " + cfg.loadDeployedJars);
		    
		}
		
		
		try {
		    
		    String versionEnforcementS = config.getString("versionEnforcement");
		    
		    VersionEnforcement versionEnforcement = VersionEnforcement.valueOf(versionEnforcementS);
		    
		    cfg.versionEnforcement = versionEnforcement;
		    
		    
		} catch(ConfigException.Missing e) {
		    
		    log.info("No versionEnforcement, using default: " + cfg.versionEnforcement);
		    
		}
		
		
		try {
		    
		    cfg.externalProperties = config.getBoolean("externalProperties");
		    
		} catch(ConfigException.Missing e) {
		    log.info("No externalProperties, using default: " + cfg.externalProperties);
		}
		
		
		try {
		    
		    cfg.externalConfig = config.getConfig("config");
		    
		} catch(ConfigException.Missing e) {
		    log.info("No config node (external), using empty config object");
		    cfg.externalConfig = ConfigFactory.empty();
		}
		
		try {
		    cfg.postInitializationHook = config.getString("postInitializationHook");
		} catch(ConfigException.Missing e) {
		    log.info("No postInitializationHook set");
		}
		
		return cfg;
		
		
	}

    public Object getExternalConfigValue(String key) {

        try {
            
            ConfigValue value = externalConfig.getValue(key);

            if(value == null) return null;
                    
            return value.unwrapped();
            
        } catch(ConfigException.Missing e) {
            return null;
        }
    }
	
}
