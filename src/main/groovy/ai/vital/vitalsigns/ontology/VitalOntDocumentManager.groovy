package ai.vital.vitalsigns.ontology

import java.nio.file.FileSystems;
import java.nio.file.FileSystem;
import java.nio.file.Path
import java.security.CodeSource
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalsigns.rdf.RDFFormat;

import com.hp.hpl.jena.ontology.OntDocumentManager
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

class VitalOntDocumentManager extends OntDocumentManager {

	private Map<String, File> cachedLocations = null
	
	private Map<String, String> cachedJarLocations = null
	
	private final static Logger log = LoggerFactory.getLogger(VitalOntDocumentManager.class)
	
	private File vitalHome = null;
	
	private File domainOntologyDir = null;
	private File vitalOntologyDir = null;
	private File externalOntologyDir = null;
	
	private Class codeClass
	
	public VitalOntDocumentManager() {

		super()
	
		codeClass = VitalOntDocumentManager.class
		
		setCacheModels(true)
			
		setProcessImports(true)
		
		String vitalHomePath = System.getenv("VITAL_HOME")
		
		if(vitalHomePath) {
			
			vitalHome = new File(vitalHomePath)
			
			domainOntologyDir = new File(vitalHome, "domain-ontology")

			vitalOntologyDir = new File(vitalHome, "vital-ontology")
			
			externalOntologyDir = new File(vitalHome, "external-ontology")			
				
		}
		
		
		
		
		
	}

	void initCachedLocations() {
		if(cachedLocations == null) {
			synchronized(VitalOntDocumentManager.class) {
				if(cachedLocations == null) {
					long start = System.currentTimeMillis()
					cachedLocations = Collections.synchronizedMap(new HashMap<String, File>())
					cachedJarLocations = Collections.synchronizedMap(new HashMap<String, String>())
					searchForOntologies();
					long stop = System.currentTimeMillis()
					log.info("Cached ontology locations: ${cachedLocations.size()}, time: ${stop-start}ms")
					
				}
			}
		}
	}
	
	void searchForOntologies() {

		if(vitalHome != null) {
			
			processDir(domainOntologyDir);
			
			processDir(vitalOntologyDir);
			
			processDir(externalOntologyDir);
			
			
		} else {
		
			log.info("VITAL_HOME not set")
			
			CodeSource src = codeClass.getProtectionDomain().getCodeSource();
			
			if (src != null) {
				
				ZipInputStream zip = null
				
				try {
					
					URL jar = src.getLocation();
					zip = new ZipInputStream(jar.openStream());
					
					ZipEntry en = null
					
					while (  ( en = zip.getNextEntry() ) != null ) {
						
						String fn = en.getName()
					
						String[] pathEls = fn.split("/")
						
						if(pathEls.length == 3 && pathEls[0] == 'resources' &&
							( pathEls[1] == 'domain-ontology' || pathEls[1] == 'external-ontology' || pathEls[1] == 'vital-ontology' )
						) {
						
							String n = pathEls[2]
						
							if(n.startsWith("catalog-") && n.endsWith(".xml")) continue
							if(n == 'README.md') continue
						
							
							Model m = ModelFactory.createDefaultModel()
							
							try {
								
								m = ModelFactory.createDefaultModel()
								RDFFormat format = guessFormat(n);
								
								//jena closes the stream! copy it into memory first
								ByteArrayOutputStream os = new ByteArrayOutputStream()
								IOUtils.copy(zip, os)
								m.read(new ByteArrayInputStream(os.toByteArray()), null, format.toJenaTypeString())
								
							} catch(Exception e) {
							
								//ignore errors
								continue;
							
							}
							
							
							for(Resource ont : m.listSubjectsWithProperty(RDF.type, OWL.Ontology)) {
								
								String ontURI = ont.getURI();
								
								if(ontURI != null) {
									cachedJarLocations.put(ontURI, fn)
								}
								
							}
							
							m.close()
							
						}
						
						
					}
					
				} catch(Exception e) {
				
					log.error("Couldn't list resource in fat jar: " + e.localizedMessage, e)
					
				} finally {
				
					IOUtils.closeQuietly(zip)
					
				}
				
					
				
			} else {
						
				log.warn("No /resources/ directory in vital fat jar or cannot access code source")
				
			
			}
		
		}
		
				
	}
	
	void processDir(File dir) {
		
		if(!dir.isDirectory()) return
		
		for(File f : dir.listFiles()) {
			
			if(!f.isFile()) continue
			

			if(f.name.startsWith("catalog-") && f.name.endsWith(".xml")) continue
			if(f.name == 'README.md') continue
			
			
			Model m = ModelFactory.createDefaultModel()
			
			try {
				
				m = FileManager.get().readModel(m, f.getAbsolutePath())
				
			} catch(Exception e) {
			
				//ignore errors
				continue;	
			
			}
			
			
			for(Resource ont : m.listSubjectsWithProperty(RDF.type, OWL.Ontology)) {
				
				String ontURI = ont.getURI();
				
				if(ontURI != null) {
					cachedLocations.put(ontURI, f)
				}
				
			}
			
			m.close()
			
		}
		
	}
	
	@Override
	public Model getModel(String uri) {

		initCachedLocations();
		
		File loc = cachedLocations.get(uri)
		
		if(loc != null) {
			
			log.info("Found precached model location for URI: ${uri} - ${loc.absolutePath}")
			
			Model m = ModelFactory.createDefaultModel();

			m = FileManager.get().readModel(m, loc.getAbsolutePath())
			
			return m;
			
		}
		
		String jarLoc = cachedJarLocations.get(uri)
		
		if(jarLoc != null) {
			
			//scan the jar looking for the file
			CodeSource src = codeClass.getProtectionDomain().getCodeSource();
			
			if (src != null) {
				
				ZipInputStream zip = null
				
				try {
					
					URL jar = src.getLocation();
					zip = new ZipInputStream(jar.openStream());
					
					ZipEntry en = null
					
					while (  ( en = zip.getNextEntry() ) != null ) {
						
						String fn = en.getName()
					
						if(fn != jarLoc) continue
							
						Model m = ModelFactory.createDefaultModel()
							
						try {
								
							m = ModelFactory.createDefaultModel()
							RDFFormat format = guessFormat(fn);
								
							//jena closes the stream! copy it into memory first
							ByteArrayOutputStream os = new ByteArrayOutputStream()
							IOUtils.copy(zip, os)
							m.read(new ByteArrayInputStream(os.toByteArray()), null, format.toJenaTypeString())
							
							return m	
							
						} catch(Exception e) {
							
							//ignore errors
							continue;
							
						}
						
					}
							
				} catch(Exception e) {
				
					log.error("Couldn't list resource in fat jar: " + e.localizedMessage, e)
					
				} finally {
				
					IOUtils.closeQuietly(zip)
					
				}
				
			}			
		}
		
		log.info("No cached model for URI: ${uri}, delegating to default handler")
		
		return super.getModel(uri);
		
	}


	public static RDFFormat guessFormat(String name) {
		
		name = name.toLowerCase()
		
		RDFFormat format = RDFFormat.RDF_XML
		
		if(name.endsWith(".n3")) {
			
			format = RDFFormat.N3
			
		} else if(name.endsWith(".ttl")) {
		
			format = RDFFormat.TURTLE
		
		} else if(name.endsWith(".nt")) {
		
			format = RDFFormat.N_TRIPLE
		
		}
		
		return format
		
	}		
}
