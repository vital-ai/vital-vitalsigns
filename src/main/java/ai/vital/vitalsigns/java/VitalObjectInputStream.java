package ai.vital.vitalsigns.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.VitalSignsSingleton;

public class VitalObjectInputStream extends ObjectInputStream {

	public VitalObjectInputStream(InputStream arg0) throws IOException {
		
		super(arg0);
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass arg0) throws IOException,
			ClassNotFoundException {

		Class<?> clz = null;
				
		try {
			return super.resolveClass(arg0);
		} catch(ClassNotFoundException e) {
		}
		
		//try loading it from vitalsigns...
		for(ClassLoader cl : VitalSigns.get().getOntologiesClassLoaders()) {
			
			try {
				return cl.loadClass(arg0.getName());
			} catch(ClassNotFoundException e) {}
			
		}
		
		throw new ClassNotFoundException(arg0.getName());
		
	}

	
	
}