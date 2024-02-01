package ai.vital.vitalsigns;

import java.net.URL;
import java.net.URLClassLoader;

public class VitalSignsURLClassLoader extends URLClassLoader {

    public VitalSignsURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Class<?> findLoadedClassPublic(String name) {
        return findLoadedClass(name);
    }
    
    public Class<?> findClassPublic(String name) throws ClassNotFoundException {
        return findClass(name);
    }
    
}
