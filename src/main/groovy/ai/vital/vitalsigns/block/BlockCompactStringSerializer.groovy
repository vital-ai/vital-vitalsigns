package ai.vital.vitalsigns.block;

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.conf.VitalSignsConfig.VersionEnforcement;
import ai.vital.vitalsigns.domains.DifferentDomainVersionLoader.VersionedDomain;
import ai.vital.vitalsigns.global.GlobalHashTable;
import ai.vital.vitalsigns.model.DomainModel;
import ai.vital.vitalsigns.model.DomainOntology;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_Container;
import ai.vital.vitalsigns.model.ValidationStatus;
import ai.vital.vitalsigns.model.property.BooleanProperty;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;

public class BlockCompactStringSerializer {

	private final static Logger log = LoggerFactory.getLogger(BlockCompactStringSerializer.class);
	
	public static final String DOMAIN_HEADER_PREFIX = "@";
	
	public static final String BLOCK_SEPARATOR = "|";
	
	public static final String BLOCK_SEPARATOR_WITH_NLINE = "|\n";
	
	private StringBuilder innerBuilder = new StringBuilder();
	
	static void serializeBlock(List<GraphObject> objects, Writer writer) throws IOException {
		
		writer.write(BLOCK_SEPARATOR_WITH_NLINE);
		
		for(GraphObject object : objects) {
			writer.write(object.toCompactString());
			writer.write((char)'\n');
		}	
	}
	
	public static Iterator<GraphObject> getHashTableBackedBlocksIterator(BufferedReader reader) throws IOException {
		return new HashTableBackedBlocksIterator(reader);
	}
	
	public static BlockIterator getBlocksIterator(BufferedReader reader) throws IOException {
		return new BlockIterator(reader, true, null);
	}
	
	public static BlockIterator getBlocksIterator(BufferedReader reader, boolean closeOnFinish) throws IOException {
		return new BlockIterator(reader, true, null, closeOnFinish);
	}
	
	public static BlockIterator getBlocksIteratorWithSelector(BufferedReader reader, boolean throwExceptions, Set<String> acceptClasses) throws IOException {
		return new BlockIterator(reader, throwExceptions, acceptClasses, true);
	}
	
	public static BlockIterator getBlocksIteratorWithSelector(BufferedReader reader, boolean throwExceptions, Set<String> acceptClasses, boolean closeOnFinish) throws IOException {
		return new BlockIterator(reader, throwExceptions, acceptClasses, closeOnFinish);
	}
	
	public static BlockIterator getBlocksIterator(File file) throws IOException {
		return getBlocksIterator(file, true, null);
	}
	
	public static BlockIterator getBlocksIterator(File file, boolean throwExceptions, Set<String> acceptClasses) throws IOException {
	
		InputStream inputS = null;
		
		if(file.getName().endsWith(".vital.gz")) {
			inputS = new GZIPInputStream(new FileInputStream(file));
		} else if(file.getName().endsWith(".vital")) {
			inputS = new FileInputStream(file);
		} else {
			throw new IOException("File name must end with '.vital[.gz]'");
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputS, "UTF-8"));
		
		return new BlockIterator(reader, throwExceptions, acceptClasses);
	}
	
	public static class VitalBlock {
		
		private GraphObject mainObject;
		
		private List<GraphObject> dependentObjects = new ArrayList<GraphObject>();

		public VitalBlock() {
			
		}
		
		public VitalBlock(List<GraphObject> inputList) {
			if(inputList == null) throw new NullPointerException("input list must not be null");
			if(inputList.size() < 1) throw new RuntimeException("input list must have at least 1 graph object (main)");
			mainObject = inputList.get(0);
			if(inputList.size() > 1) {
				dependentObjects.addAll(inputList.subList(1, inputList.size()));
			}
		}
		
		public GraphObject getMainObject() {
			return mainObject;
		}

		public void setMainObject(GraphObject mainObject) {
			this.mainObject = mainObject;
		}

		public List<GraphObject> getDependentObjects() {
			return dependentObjects;
		}

		public void setDependentObjects(List<GraphObject> dependentObjects) {
			this.dependentObjects = dependentObjects;
		}
		
		public VITAL_Container toContainer() {
			return this.toContainer(true);
		}
		
		public List<GraphObject> toList() {
			List<GraphObject> objects = new ArrayList<GraphObject>();
			if(mainObject != null) objects.add(mainObject);
			if(dependentObjects != null) objects.addAll(dependentObjects);
			return objects; 
		}
		
		public VITAL_Container toContainer(boolean queryable) {
			VITAL_Container c = new VITAL_Container(queryable);
			if(mainObject != null) c.putGraphObject(mainObject);
			if(dependentObjects != null && dependentObjects.size() > 0 ) c.putGraphObjects(dependentObjects);
			return c;
		}
	}
	
	private Writer _writer = null;
	
	public BlockCompactStringSerializer(Writer writer) throws IOException {
	    this(writer, null);
	}
	
	public BlockCompactStringSerializer(Writer writer, Map<String, String> overriddenDomainVersions) throws IOException {
	    if(writer == null) throw new NullPointerException("Null writer");
	    this._writer = writer;
	    printOntologiesHeader(overriddenDomainVersions);
	}
	
	private void printOntologiesHeader(Map<String, String> overriddenDomainVersions) throws IOException {
	    
	    // collect all preferred versions
	    for(DomainModel dm : VitalSigns.get().getDomainModels()) {
	        
	        IProperty preferredV = (IProperty) dm.getProperty("preferred");
	        if(preferredV == null) continue;
	        
	        String versionInfo = ((IProperty)dm.getProperty("versionInfo")).toString(); 
	        
	        VersionedDomain vd = VersionedDomain.analyze(dm.getURI());
	        
	        String domainURI = vd.domainURI;
	        
	        if( ((BooleanProperty)preferredV.unwrapped()).booleanValue() ) {
	            
	            if(overriddenDomainVersions == null || !overriddenDomainVersions.containsKey(domainURI)) {
	                
	                if(overriddenDomainVersions == null) overriddenDomainVersions = new HashMap<String, String>();
	                
	                overriddenDomainVersions.put(domainURI, versionInfo); 
	            }
	        }
	    }
	    
		for(DomainOntology ont : VitalSigns.get().getDomainList()) {
		    
			if( ont.getUri().equals(VitalCoreOntology.ONTOLOGY_IRI) || ont.getUri().equals("http://vital.ai/ontology/vital") ) {
			    continue;
			}
			
			VersionedDomain vd = VersionedDomain.analyze(ont.getUri());
			
			if(vd.versionPart != null) continue;
			
			String domainIRI = null;
			if(overriddenDomainVersions != null && overriddenDomainVersions.containsKey( ont.getUri() ) ) {
			    domainIRI = ont.getUri() + '$' + overriddenDomainVersions.get(ont.getUri());
			} else {
			    domainIRI = ont.toVersionIRI();
			}
			
			this._writer.write(DOMAIN_HEADER_PREFIX);
			this._writer.write(domainIRI);
			this._writer.write('\n');
			
		}
	}
	
	public BlockCompactStringSerializer(File file) throws IOException {
	    this(file, null);
	}
	public BlockCompactStringSerializer(File file, Map<String, String> overriddenDomainVersions) throws IOException {
		if(file == null) throw new NullPointerException("Null file!");
		
		OutputStream outputStream = null;//new FileOutputStream(file);
		if(file.getName().endsWith(".vital.gz")) {
			outputStream = new GZIPOutputStream(new FileOutputStream(file));
		} else if(file.getName().endsWith(".vital")) {
			outputStream = new FileOutputStream(file);
		} else {
			throw new IOException("File name must end with '.vital[.gz]'");
		}
		//128K buffer
		this._writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"), 128 * 1024);
		printOntologiesHeader(overriddenDomainVersions);
	}
	
	public void startBlock() throws IOException {
		_writer.write(BLOCK_SEPARATOR_WITH_NLINE);
	}
	
	public void writeGraphObject(GraphObject go) throws IOException {
		
		if(VitalSigns.get().getConfig().enforceConstraints) {
			ValidationStatus s = go.validate();
			if(s.getStatus() != ValidationStatus.Status.ok) {
				throw new RuntimeException("Graph object validation error(s): " + s.getErrors());
			}
		}
		
		int l = innerBuilder.length();
		if(l > 0) {
			innerBuilder.delete(0, innerBuilder.length());
		}
		CompactStringSerializer.toCompactStringBuilder(go, innerBuilder);
		_writer.write(innerBuilder.toString());
		_writer.write((char)'\n');
	}
	
	public void flush() throws IOException {
		_writer.flush();
	}
	
	public void endBlock() {
		//do nothing
	}
	
	public void close() {
		try {
			_writer.close();
		} catch(Exception e) {}
	}
	
	public static class HashTableBackedBlocksIterator implements Iterator<GraphObject> {

		private BlockIterator blockIterator;
		
		public HashTableBackedBlocksIterator(BufferedReader reader) throws IOException {
			blockIterator = new BlockIterator(reader);
		}
		
		@Override
		public boolean hasNext() {
			return blockIterator.hasNext();
		}

		@Override
		public GraphObject next() {
			VitalBlock block = blockIterator.next();
			GlobalHashTable ght = GlobalHashTable.get();
			ght.purge();
			ght.putAll(Arrays.asList(block.mainObject));
			ght.putAll(block.dependentObjects);
			return block.mainObject;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
	}
	
	public static class BlockIterator implements Iterator<VitalBlock>, Closeable {

		static Pattern ontologyVersionPattern = Pattern.compile("([^\$]+)\\\$(\\d+\\.\\d+\\.\\d+)");
		
		BufferedReader reader = null;
		
		private VitalBlock buffered = null;
		
		String lastLine = null;
		
		private boolean throwExceptions = true;
		
		private Set<String> acceptClasses = null;
		
		public BlockIterator(BufferedReader reader) throws IOException {
			this(reader, true, null);
		}
		
		int linesSkipped = 0;
		
		boolean firstBlockSpotted = false;

		private boolean closeOnFinish = false;
		
		//stores the uri - > versioned uri mapping from the versions context
		private Map<String, String> uri2TempURI = new HashMap<String, String>();
		
		public BlockIterator(BufferedReader reader, boolean throwExceptions, Set<String> acceptClasses) throws IOException {
			this(reader, throwExceptions, acceptClasses, true);
		}
		
		public BlockIterator(BufferedReader reader, boolean throwExceptions, Set<String> acceptClasses, boolean closeOnFinish) throws IOException {
			super();
			this.reader = reader;
			this.throwExceptions = throwExceptions;
			this.acceptClasses = acceptClasses;
			this.reader = reader;
			this.closeOnFinish = closeOnFinish;
			
			lastLine = reader.readLine();
			
			buffered = readNextBlock();
			
		}

		public boolean isCloseOnFinish() {
			return closeOnFinish;
		}

		public void setCloseOnFinish(boolean closeOnFinish) {
			this.closeOnFinish = closeOnFinish;
		}

		@Override
		public boolean hasNext() {
			if(buffered == null) {
				BlockCacheRegistry.get().removeIterator(this);
			}
			return buffered != null;
		}

		@Override
		public VitalBlock next() {
			return next(true);
		}
		
		public VitalBlock next(boolean cache) {
			if(buffered == null) throw new NoSuchElementException("No more blocks!");
			VitalBlock toReturn = buffered;
			try {
				buffered = readNextBlock();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			if(cache) {
				BlockCacheRegistry.get().addNewBlock(this, toReturn);
			}
			return toReturn;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
		
		private VitalBlock readNextBlock() throws IOException {
			
			//no more records
			if(lastLine == null) {
				return null;
			}
			
			VitalBlock newBlock = null;
			
			boolean keepReading = true;
			
			while( keepReading ) {
				
				lastLine = lastLine.trim();

				if(lastLine.startsWith("#")) {
					
					// ignore comments in block file

				} else if(lastLine.startsWith(DOMAIN_HEADER_PREFIX)) {
				
					if(firstBlockSpotted) {
						
						log.warn("Ontology annotation detected inside block file - ignored: " + lastLine);
						
					} else {

						// ontology check annotation
					
						String ontIRI = lastLine.substring(1).trim();
					
						analyzeOntologyIRI(ontIRI, false);						
					}
										
				} else if(!lastLine.isEmpty()) {
					
					if(lastLine.startsWith(BLOCK_SEPARATOR)) {
						
						if(lastLine.length() > 1) {
							
							if(!lastLine.substring(1,2).equals(DOMAIN_HEADER_PREFIX)) {
								if(throwExceptions) throw new RuntimeException("VitalBlock start may only be followed by @ontologyIRI annotation");
							}
							
							String ontIRI = lastLine.substring(2).trim();
							
							analyzeOntologyIRI(ontIRI, true);
							
						}
						
						firstBlockSpotted = true;
						
						if(newBlock == null) {
							newBlock = new VitalBlock();
						} else {
							break;
							// return newBlock;
						}
					} else {
					
						if(newBlock == null) {
							
							if(throwExceptions) throw new RuntimeException("No vital block start");

							linesSkipped++;
							
							log.warn("Skipped {} lines...", linesSkipped);
							
						} else {
						
						    // CompactStringSerializer.fromString(lastLine, acceptClasses);
							
							GraphObject g = readConverted(lastLine);
							
							if(g != null) {
								
								if(newBlock.mainObject == null) {
									newBlock.mainObject = g;
								} else {
									newBlock.dependentObjects.add(g);
								}
								
							} else {
							
								// always throw that exception from now on!
								// if(acceptClasses == null && throwExceptions)
								
								throw new RuntimeException("No object deserialized from line: " + lastLine);
							}
						}
					}
				}				
				
				lastLine = reader.readLine();
				
				if(lastLine == null) {
					
					if(closeOnFinish) {
						reader.close();
					}
					
					reader = null;
					
					keepReading = false;
				}
			}

			if(newBlock != null && newBlock.mainObject == null) throw new RuntimeException("Empty block!");
			
			return newBlock;
		}
		
	    // converts the compactString into temp domain and loads 
	    public GraphObject readConverted(String compactString) {
	        
	        // use very detailed replace strategy ?
	        
	        if(uri2TempURI.size() > 0) {
	            
	            for(Entry<String, String> e : uri2TempURI.entrySet()) {
	                
	                String inputURI = e.getKey();
	                
	                String outputURI = e.getValue();
	                
	                compactString = compactString.replace(inputURI + "#", outputURI + "#")
	                        .replace(inputURI + "\"", outputURI + "\"");
	                
	            }
	            
	        }
	        
	            
	        
	        //replace all references, all properties
//	        String[] columns = compactString.split("\t");
//	        
//	        StringBuilder b = new StringBuilder();
//	        
//	        for(int i = 0 ; i < columns.length; i++) {
//	            
//	            if(i > 0) b.append('\t');
//	            
//	            String col = columns[i];
//	            
//	            int indexOfColon = col.indexOf("=");
//	            
//	            String name = col.substring(0, indexOfColon);
//	            
//	            int typeIndex = name.indexOf('|');
//	            
//	            String typeAppender = null;
//	            
//	            if(typeIndex > 0) {
//	                typeAppender = name.substring(typeIndex);
//	                name= name.substring(0, typeIndex);
//	            }
//	            
//	            if(uri2TempURI.containsKey(name)) {
//	                
//	            }
//	            
//	            if(typeAppender != null) {
//	                b.append(typeAppender);
//	            }
//	            
//	            //string dquotes
//	            String value = col.substring(indexOfColon + 2, col.length() - 1);
//	            
//	            String unescaped = StringEscapeUtils.unescapeJava(value);
//	            
//	            //replace
//	            if(uri2TempURI.containsKey(unescaped)) {
//	              
//	                String newURI = uri2TempURI.get(unescaped);
//	                
//	                value = StringEscapeUtils.escapeJava(newURI);
//	                
//	            }
//	            
//	            b.append("=\"").append(value).append('"');
//	            
//	        }
	        
	        return CompactStringSerializer.fromString(compactString, acceptClasses);
	        
	    }
		
		private void analyzeOntologyIRI(String ontIRI, boolean block) {
			
			Matcher m = ontologyVersionPattern.matcher(ontIRI);
			
			if(!m.matches()) {
				if(throwExceptions) throw new RuntimeException((block ? "block" : "global") + " annotation ontology+version does not match pattern: " + ontologyVersionPattern + ", line: " + lastLine);
			}
			
			String ontologyURI = m.group(1);
			
			String version = m.group(2);
			
			DomainOntology fileOnt = new DomainOntology(ontologyURI, version);

			// Map<String, String> oldVersions = null;
			
			DomainOntology olderVersionOnt = null;
			
			DomainOntology foundOnt = null;
			
			for(DomainOntology ontology : VitalSigns.get().getDomainList()) {
			    
			    VersionedDomain vns = VersionedDomain.analyze(ontology.getUri());
	                
                if(vns.versionPart != null) {
                    
                    String ontURI = vns.domainURI;
                    
                    if( ontURI.equals(ontologyURI) ) {
                        
                        if(ontology.compareTo(fileOnt) == 0) {
                            
                            olderVersionOnt = ontology;
                            
                            uri2TempURI.put(ontURI, ontology.getUri());
                            
                            // oldVersions = new HashMap<String, String>();
                            // oldVersions.put(ontURI, d.getUri());
                            // collect imports tree
                            List<String> imports = VitalSigns.get().getOntologyURI2ImportsTree().get(ontology.getUri());
                            
                            if(imports != null) {
                                for(String i : imports) {
                                    VersionedDomain ivd = VersionedDomain.analyze(i);
                                    if(ivd.versionPart != null) {
                                    	// oldVersions.put(ivd.domainURI, i);
                                        uri2TempURI.put(ivd.domainURI, i);
                                    }
                                }
                            }
                            
                        }
                        
                    }
                    
                } else {
                    
                    if(ontology.getUri().equals(fileOnt.getUri())) {
                    	
                        foundOnt = ontology;

                        // && ontology.getMajor().intValue() == fileOnt.getMajor().intValue() && ontology.getMinor().intValue() >= fileOnt.getMinor().intValue() ) {
                    }
                }
			}
			
			if(olderVersionOnt != null) {
			    
			} else {
			    
			    if(foundOnt == null) {
			        throw new RuntimeException("No active ontology with URI: " + ontIRI);
			        // if(throwExceptions) throw new RuntimeException("No ontology for " + ( block ? "block" : "global" ) + " annotation version IRI found: " + lastLine + ", loaded: " + VitalSigns.get().getDomainList().toString());
			    }
			    
			    int c = fileOnt.compareTo(foundOnt);
			    
			    if(c != 0) {
			        
			        if( VitalSigns.get().getConfig().versionEnforcement == VersionEnforcement.strict ) {
			            
			            boolean backwardCompatible = false;
			            
			            String backwardMsg = "";
			            
			            //give it a try
			            if(c < 1 && foundOnt.getBackwardCompatibleVersion() != null) {
			                
			                c = fileOnt.compareTo( foundOnt.getBackwardCompatibleVersion());
			                
			                if(c >= 0) {
			                    
			                    backwardCompatible = true;
			                    
			                } else {
			                    
			                    backwardMsg = " nor its backward compatible version: " + foundOnt.getBackwardCompatibleVersion().toVersionString();
			                    
			                }
			                
			            }
			            
			            if(!backwardCompatible) 
			                throw new RuntimeException("Strict version mode - persisted object domain " + ontIRI + " version " + fileOnt.toVersionString() + " does not match currently loaded: " + foundOnt.toVersionString() + backwardMsg);
			        }    
			    }
			}
		}
		
		@Override
		public void close() {
			
			try {
				if(reader != null) {
					reader.close();
					reader = null;
				}
			} catch(Exception e) {
			
			}
			
			BlockCacheRegistry.get().removeIterator(this);	
		}
		
		@Override
		protected void finalize() throws Throwable {
			BlockCacheRegistry.get().removeIterator(this)
			super.finalize()
		}	
	}
}

