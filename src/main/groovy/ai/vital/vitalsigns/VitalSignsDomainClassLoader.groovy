package ai.vital.vitalsigns;

import com.github.marschall.pathclassloader.PathClassLoader;

public class VitalSignsDomainClassLoader extends ClassLoader {

    private static VitalSignsDomainClassLoader singleton;
    
    public static VitalSignsDomainClassLoader get() {
        if(singleton == null) {
            synchronized (VitalSignsDomainClassLoader.class) {
                if(singleton == null) {
                    singleton = new VitalSignsDomainClassLoader(VitalSigns.class.getClassLoader());
                }
            }
        }
        return singleton;
    }
    
    private VitalSignsDomainClassLoader(ClassLoader parent) {
        super(parent);
    }
    
    @Override
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        
        for(ClassLoader cl : VitalSigns.get().getOntologiesClassLoaders()) {
            
            if( cl instanceof VitalSignsURLClassLoader ) {
                
                VitalSignsURLClassLoader vsucl = (VitalSignsURLClassLoader) cl;
                
                Class<?> c = vsucl.findLoadedClassPublic(name);
                
                if(c != null) return c;
                
                try {
                    c = vsucl.findClassPublic(name);
                } catch(ClassNotFoundException e) {
                    
                }
                
                if(c != null) return c;
                
            } else if(cl instanceof PathClassLoader) {
                
                PathClassLoader pcl = (PathClassLoader) cl;
                
                Class<?> c = pcl.findLoadedClassPublic(name);
                
                if(c != null) return c;
                
                try {
                    c = pcl.findClass_public(name);
                } catch(ClassNotFoundException e) {
                    
                }
                
                if(c != null) return c;
                
            }
            
        }
        
        return super.loadClass(name, resolve);
        
    }

    
    
}
