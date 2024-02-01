package ai.vital.vitalsigns.rdf

import java.io.IOException;
import java.util.zip.GZIPInputStream

import org.apache.commons.io.IOUtils;

import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.GraphObject



class VitalNTripleIterator implements Iterator<GraphObject>, Closeable{

	private GraphObject graphObject
	
	private BufferedReader reader
	
	private StringBuilder buffer = new StringBuilder()
			
	private String lastSubject = null
	
	private int c = 0
	
	public VitalNTripleIterator(File ntripleFile) {
		this(openNtripleFile(ntripleFile))

	}
	public VitalNTripleIterator(InputStream ntripleInputStream) throws IOException {
		
		this.reader = new BufferedReader(new InputStreamReader(ntripleInputStream, "UTF-8"))
		
		this.readNextGraphObject();
		
	}
	
	static InputStream openNtripleFile(File ntripleFile) throws IOException {
		
		if(! (ntripleFile.name.endsWith(".nt") || ntripleFile.name.endsWith(".nt.gz")) ) throw new IOException("Expected a file name with .nt[.gz] extension ")
		
		InputStream is = new BufferedInputStream(new FileInputStream(ntripleFile));
		
		if(ntripleFile.getName().endsWith(".gz")) is = new GZIPInputStream(is);
		
		return is;
		
	}
	
	private void readNextGraphObject() {
		
		for(String l = reader.readLine(); l != null; l = reader.readLine()) {
				
			c++
				
			l = l.trim()
								
			if(l.isEmpty()) continue
				
			if(l.startsWith('#')) continue
				
			int firstSpace = l.indexOf(" ")
			int firstTab = l.indexOf("\t")
				
			if( firstSpace < 1 && firstTab < 1 ) throw new Exception("Invalid n-triples line - no first whitespace found - ${c}: ${l}")
				
			if(firstSpace < 0) firstSpace = Integer.MAX_VALUE.intValue()
			if(firstTab < 0) firstTab = Integer.MAX_VALUE.intValue()
			
			
			int firstWS = Math.min(firstTab, firstSpace)
				
			String subject = l.substring(0, firstWS)
			
			boolean quit = false	
			
			if(lastSubject != null && !lastSubject.equals(subject)) {
					
				GraphObject g = bufferToGraphObject(buffer)
					
				this.graphObject = g
				
				//clear the buffer now
				buffer.setLength(0)
				
				quit = true
					
			}
				
			buffer.append(l).append("\n")
				
			lastSubject = subject;
			
			if(quit) {
				return
			}
				
		}
			
		//end of file, check buffer
		
		if(buffer.length() > 0) {
				
			GraphObject g = bufferToGraphObject(buffer)
				
			buffer.setLength(0)
					
			this.graphObject = g
			
			
		} else {
			this.graphObject = null
		}
			
		
	}
	
	@Override
	public void close() throws IOException {
		
		if(this.reader != null) {
			IOUtils.closeQuietly(this.reader);
			this.reader = null;
		}
		
	}

	@Override
	public boolean hasNext() {
		if(this.reader == null) throw new RuntimeException("NTriple iteartor already closed.");
		return graphObject != null;
	}

	@Override
	public GraphObject next() {
		if(this.reader == null) throw new RuntimeException("NTriple iteartor already closed.");
		if(this.graphObject == null) throw new NoSuchElementException();
		GraphObject current = this.graphObject;
		try {
			readNextGraphObject();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return current;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException(VitalNTripleIterator.class.simpleName + " does not support removals");
	}

	private Model m = ModelFactory.createDefaultModel()
	
	private GraphObject bufferToGraphObject(StringBuilder buffer) {
		
		m.read(new ByteArrayInputStream(buffer.toString().getBytes()), null, "N-TRIPLE")
		
		List<GraphObject> g = VitalSigns.get().readAllGraphObjects(m)
		
		m.removeAll()
		
		if(g.size() == 0) {
			throw new RuntimeException("No graph object deserialized from N-Triple object: " + buffer.toString())
		}
		
		if(g.size() > 1) {
			throw new RuntimeException("More than 1 graph object deserialized from N-triple object: " + buffer.toString())
		}
		
		return g[0]
		
	}
}
