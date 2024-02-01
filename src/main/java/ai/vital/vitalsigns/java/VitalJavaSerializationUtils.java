package ai.vital.vitalsigns.java;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.StringWriter;
import java.io.PrintWriter;


/**
 * A class that fixes lots of issues related to pure single classloader serialization.
 * Single classloader does not handle dynamic ontologies
 * Replaces org.apache.commons.lang3.SerializationUtils.deserialize(byte[])
 *
 */

public class VitalJavaSerializationUtils {

    private final static Logger log = LoggerFactory.getLogger(VitalJavaSerializationUtils.class);

	/**
	 * Deserializes an object from bytes array.
	 * @param bytes
	 * @return
	 */
	public static Object deserialize(byte[] bytes) {
		
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		
		return deserialize(is);	
	}
	
	/**
	 * Clones a serializable java object in a safe and efficient way - serialize/deserialize 
	 * @param obj
	 * @return
	 */
	public static Serializable clone(Serializable obj) {
		
		byte[] serialized = SerializationUtils.serialize(obj);
		
		return (Serializable) deserialize(serialized);
	}

	/**
	 * Deserializes a java object from input stream
	 * The stream will be closed once the object is written. This avoids the need for a finally clause, and maybe also exception handling, in the application code.
	 * @param inputStream
	 * @return
	 */
	public static Object deserialize(InputStream inputStream) {
		
		VitalObjectInputStream vois = null;
		
		try {
			
			vois = new VitalObjectInputStream(inputStream);
			
			return vois.readObject();
			
		} catch (IOException e) {
			
			log.error(e.getLocalizedMessage());

			StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    e.printStackTrace(pw);
		   
			log.error(sw.toString());
			
			throw new RuntimeException(e);
			
		} catch (ClassNotFoundException e) {
			
			log.error(e.getLocalizedMessage());

			StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    e.printStackTrace(pw);
		   
			log.error(sw.toString());
			
			throw new RuntimeException(e);
			
		} finally {
			
			IOUtils.closeQuietly(vois);
		}
	}
}
