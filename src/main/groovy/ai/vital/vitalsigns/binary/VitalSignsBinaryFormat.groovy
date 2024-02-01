package ai.vital.vitalsigns.binary

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.util.ArrayList
import java.util.Arrays
import java.util.List
import java.util.Map

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.block.BlockCompactStringSerializer
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.BlockIterator
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.VitalBlock
import ai.vital.vitalsigns.block.CompactStringSerializer

public class VitalSignsBinaryFormat {

	public final static String encoding = "UTF-8"
	
	public final static int MAGIC_LENGTH = 8
	
	private final static Logger log = LoggerFactory.getLogger(VitalSignsBinaryFormat.class)
	
	private static byte _NEWLINE = (byte) '\n'
	
	public static byte[] VITAL_MAGIC = null
	
	public static byte[] VERSION_1 = null
    
	public static byte[] VERSION_2 = null
	
	protected static void init() {
		if(VITAL_MAGIC == null) {
			synchronized(VitalSignsBinaryFormat.class) {
				if(VITAL_MAGIC == null) {
					try {
						VITAL_MAGIC = "VTL".getBytes(encoding)
						VERSION_1 = "00001".getBytes(encoding)
						VERSION_2 = "00002".getBytes(encoding)
					} catch (UnsupportedEncodingException e) {
						log.error(e.getLocalizedMessage(), e)
					}
				}
			}
		}
	}

	public static byte[] encodeBlock(List<GraphObject> objects) throws IOException {
        return encodeBlock(objects, null)
    }
    
	public static byte[] encodeBlock(List<GraphObject> objects, Map<String, String> overriddenDomainVersions) throws IOException {
		
		init()

		ByteArrayOutputStream os = new ByteArrayOutputStream()
		
		os.write(VITAL_MAGIC)
		//5 bytes version
		os.write(VERSION_2)

		boolean first = true;
		
        OutputStreamWriter osw = new OutputStreamWriter(os)
        
        BlockCompactStringSerializer writer = new BlockCompactStringSerializer(osw, overriddenDomainVersions)
        
        writer.startBlock()
        
		for(GraphObject g : objects) {
			
            writer.writeGraphObject(g)
            
		}
        		
        writer.endBlock()
        
        writer.close();
        
		return os.toByteArray();
		
	}
    
    public static byte[] encodeBlock(VitalBlock block) throws IOException {
        return encodeBlock(block.toList())
    }
    
    
    public static byte[] encodeBlockStrings(List<String> objectsCompactStrings) throws IOException {
        
        init()

        ByteArrayOutputStream os = new ByteArrayOutputStream()
        
        os.write(VITAL_MAGIC)
        //5 bytes version
        os.write(VERSION_2)

        os.write(BlockCompactStringSerializer.BLOCK_SEPARATOR_WITH_NLINE.getBytes(encoding))
        
        boolean first = true
        
        for(String compactString : objectsCompactStrings) {
            
            byte[] bytes = compactString.getBytes(encoding)
            
            if(first) {
                first = false
            } else {
                os.write(_NEWLINE)
            }
            
            os.write(bytes)
            
        }
        
        return os.toByteArray()
        
    }
	
	public static boolean isVitalBlock(byte[] bytes) {
		return isVitalBlock(bytes, 0, bytes.length)
	}
	
	public static boolean isVitalBlock(byte[] bytes, int offset, int length) {
		
		init()
		
		if(bytes == null) throw new NullPointerException("Bytes array cannot be null!")
		
		if(bytes.length - offset < MAGIC_LENGTH) return false
		
		byte[] magic = Arrays.copyOfRange(bytes, offset, offset + VITAL_MAGIC.length)
		
		if( ! Arrays.equals( VITAL_MAGIC, magic)) return false

		byte[] version = Arrays.copyOfRange(bytes, offset + VITAL_MAGIC.length, offset + MAGIC_LENGTH)

		if( Arrays.equals( VERSION_1 ,  version) || Arrays.equals(VERSION_2, version)) {
			return true
		}
		
		return false
		
	}
    
	public static int getVersion(byte[] bytes, int offset, int length) throws IOException{
        
	    init()
        
        if(bytes == null) throw new NullPointerException("Bytes array cannot be null!")
        
        if(bytes.length - offset < MAGIC_LENGTH) throw new IOException("data array too short")
        
        byte[] magic = Arrays.copyOfRange(bytes, offset, offset + VITAL_MAGIC.length)
        
        if( ! Arrays.equals( VITAL_MAGIC, magic)) throw new IOException("not a binary vital block (invalid magic bytes)")

        byte[] version = Arrays.copyOfRange(bytes, offset + VITAL_MAGIC.length, offset + MAGIC_LENGTH)

        if( Arrays.equals( VERSION_1 ,  version) )  {
            return 1
	    } else if( Arrays.equals(VERSION_2, version) ) {
            return 2
        } else {
            throw new IOException("unsupported version: " + new String(version).toString())
        }
        
        
	}
    
    public static int getVersion(byte[] bytes) {
        return getVersion(bytes, 0, bytes.length)    
    }
    
	
	public static List<GraphObject> decodeBlock(byte[] bytes) throws IOException {
		return decodeBlock(bytes, 0, bytes.length)
	}
	
	public static List<GraphObject> decodeBlock(byte[] bytes, int offset, int length) throws IOException {
        
		init()
		
        int version = getVersion(bytes, offset, length)

        if(version == 1) {
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes, offset + MAGIC_LENGTH, length), encoding))
            
            List<GraphObject> out = new ArrayList<GraphObject>()
            
            for(String l = reader.readLine(); l != null; l = reader.readLine()) {
                
                l = l.trim()
                
                if(l.isEmpty()) continue
                
                GraphObject graphObject = CompactStringSerializer.fromString(l)

                out.add(graphObject)
                
            }
            
            return out
            
        } else {
        
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes, offset + MAGIC_LENGTH, length), encoding))

            List<GraphObject> out = new ArrayList<GraphObject>()
            
            for( BlockIterator iterator = BlockCompactStringSerializer.getBlocksIterator(reader); iterator.hasNext(); ) {
                
                VitalBlock block = iterator.next()
                
                out.addAll(block.toList())
                
            }
            
            return out
        
        }
	}
    
    public static List<String> decodeBlockStrings(byte[] bytes, int offset, int length) throws IOException {

        init()
        
        if(! isVitalBlock(bytes, offset, length) ) throw new IOException("The magic bytes are not of type Vital")
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes, offset + MAGIC_LENGTH, length), encoding))
        
        List<String> out = new ArrayList<String>()
        
        for(String l = reader.readLine(); l != null; l = reader.readLine()) {
            
            l = l.trim()
            
            if(l.isEmpty()) continue
            
            if(l.startsWith("@") || l.startsWith("#") || l.startsWith("|")) continue
            
            out.add(l)
            
        }
        
        return out

    }

	public static VitalBlock decodeVitalBlock(byte[] bytes) throws IOException {
		return decodeVitalBlock(bytes, 0, bytes.length)
	}
	
	public static VitalBlock decodeVitalBlock(byte[] bytes, int offset, int length) throws IOException {
		
		List<GraphObject> decodeBlock = decodeBlock(bytes, offset, length)
		
		VitalBlock block = new VitalBlock()

		if(decodeBlock.size() > 0) {
			block.setMainObject(decodeBlock.remove(0))
		}

		block.setDependentObjects(decodeBlock)
		
		return block
	}
	
}
