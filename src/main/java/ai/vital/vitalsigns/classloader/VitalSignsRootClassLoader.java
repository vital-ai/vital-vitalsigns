package ai.vital.vitalsigns.classloader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.marschall.pathclassloader.PathClassLoader;
import com.github.marschall.pathclassloader.PathURLStreamHandler;

import ai.vital.vitalsigns.VitalSignsURLClassLoader;

/**
 * A class loader that may loads classes from both classpath or jimfs instance.
 * May be set as a root classloader
 * 
 *
 */
public class VitalSignsRootClassLoader extends URLClassLoader {

    private static URL[] constructClassPath(String extraPath) {
        
        if(singleton != null) throw new RuntimeException(VitalSignsRootClassLoader.class.getCanonicalName() + " singleton already set");
        
        List<URL> l = new ArrayList<URL>();
        
        if(extraPath == null) {
            extraPath = System.getProperty("vital.classpath");
        }
        
        if(extraPath == null || extraPath.isEmpty() ) throw new RuntimeException("No 'vital.classpath', required if the custom classpath not set directly");
        
        String[] jars = extraPath.split(File.pathSeparator);
        
        for(String jar : jars) {
            try {
                l.add(new File(jar).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        
        return l.toArray(new URL[l.size()]);
        
    }
	
	static {
		registerAsParallelCapable();
	}
	
	
	
	private static VitalSignsRootClassLoader singleton;
	
	public static VitalSignsRootClassLoader get() throws Exception {
		if(singleton == null) throw new Exception("VitalSignsRootClassLoader not initialized!");
		return singleton;
	}

	/**
	 * Creates a new {@link VitalSignsRootClassLoader} with a parent class loader.
	 * 
	 * @param path
	 *            the path from with to load the classes
	 * @param parent
	 *            the class loader from which to try loading classes first
	 */
	public VitalSignsRootClassLoader(ClassLoader parent) {
		super(constructClassPath(null), parent);
		singleton = this;
	}
	
	public VitalSignsRootClassLoader(ClassLoader parent, String classpath) {
	    super(constructClassPath(classpath), parent);
	    singleton = this;
	}
	
	private ArrayList<Path> emptyList = new ArrayList<Path>();

	private Collection<ClassLoader> vitalSignsClassLoaders = new ArrayList<ClassLoader>(); //new ArrayList<Path>();
	
	
	//cache classes to avoid "attempted  duplicate class definition" exceptions
	Map<String, Class<?>> _dynamicCache = Collections.synchronizedMap(new HashMap<String, Class<?>>());
	
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		
		Class<?> c = null;
		try {
			c = super.findClass(name);
		} catch(Exception e) {
			
		}
		
		if(c != null) {
			return c;
		}
		
		c = _dynamicCache.get(name);
		
		if(c != null) {
			return c;
		}
		
		//try to load the class on our own
		
		for(ClassLoader cl : vitalSignsClassLoaders) {
			
			if(cl instanceof PathClassLoader) {
				try {
					c = ((PathClassLoader) cl).findClass_public(name);
				} catch(Exception e) {}
			} else if(cl instanceof VitalSignsURLClassLoader) {
			    try {
			        c = ((VitalSignsURLClassLoader)cl).findClassPublic(name);
			    } catch(Exception e) {}
			} else {
				throw new RuntimeException("TODO! make all classloader finders public!");
			}
			
	         if(c != null) {
	             _dynamicCache.put(name, c);
	             return c;
	         }
			
		}
		
		/*
		Collection<Path> rootPaths = v;
		
		for(Path p : rootPaths) {
			
			// TODO path injection
			Path classPath = p.resolve("classes").resolve(name.replace('.', '/').concat(".class"));
			
			if (Files.exists(classPath)) {
				try {
					
					byte[] byteCode = Files.readAllBytes(classPath);
					return this.defineClass(name, byteCode, 0, byteCode.length);
					
//					return getParent().defineClass(name, byteCode, 0, byteCode.length);
				} catch (Exception e) {
					throw new ClassNotFoundException(name, e);
				}
			}
			
			
		}
		*/
		
		throw new ClassNotFoundException(name);
	}

	public Class<?> findClass_public(String name) throws ClassNotFoundException {
		return this.findClass(name);
	}

	@Override
	public URL findResource(String name) {
		// TODO path injection
		
		URL u = super.findResource(name);
		if(u != null) return u;
		
		
		for(ClassLoader cl : vitalSignsClassLoaders) {
			if(cl instanceof PathClassLoader) {
				u = ((PathClassLoader) cl).findResource_public(name);
				if(u != null) return u;
			} else if(cl instanceof VitalSignsURLClassLoader) {
			    u = ((VitalSignsURLClassLoader)cl).findResource(name);
			    if(u != null) return u;
			} else {
				throw new RuntimeException("TODO! make all classloader finders public!");
			}
		}
		
		/*
		Collection<Path> rootPaths = vitalSignsPaths;
		
		for(Path p : rootPaths) {
		
			Path resolved = p.resolve("classes").resolve(name);
			if (Files.exists(resolved)) {
				try {
					return toURL(resolved);
				} catch (IOException e) {
					throw new RuntimeException("could not open " + resolved, e);
				}
			}
		}
		*/
		
		return null;
	}

	public URL findResource_public(String name) {
		return this.findResource(name);
	}

	@Override
	public Enumeration<URL> findResources(final String name)
			throws IOException {
		
		Enumeration<URL> superResources = super.findResources(name);
		
		// TODO correct?
		final List<URL> resources = new ArrayList<URL>();
		
		while( superResources.hasMoreElements() ) {
			resources.add(superResources.nextElement());
		}
		
		
		for(ClassLoader cl : vitalSignsClassLoaders) {
			
			if(cl instanceof PathClassLoader) {
				
				Enumeration<URL> r2 = ((PathClassLoader) cl).findResources_public(name);
				while(r2.hasMoreElements()) {
					resources.add(r2.nextElement());
				}
			} else if(cl instanceof VitalSignsURLClassLoader) {
			    
			    Enumeration<URL> r2 = ((VitalSignsURLClassLoader)cl).findResources(name);
                while(r2.hasMoreElements()) {
                    resources.add(r2.nextElement());
                }			    
			    
			} else {
				throw new RuntimeException("TODO! make all classloader finders public!");
			}
			
			
			
			
		}
		
		/*
		Collection<Path> rootPaths = vitalSignsPaths;
		
		for(Path p : rootPaths) {
		
			final Path path = p.resolve("classes");
		
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					if (!name.isEmpty()) {
						this.addIfMatches(resources, file);
					}
					return super.visitFile(file, attrs);
				}
	
				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					if (!name.isEmpty() || path.equals(dir)) {
						this.addIfMatches(resources, dir);
					}
					return super.preVisitDirectory(dir, attrs);
				}
	
				void addIfMatches(List<URL> resources, Path file)
						throws IOException {
					if (path.relativize(file).toString().equals(name)) {
						resources.add(toURL(file));
					}
				}
	
			});
		}
		*/
		
		return Collections.enumeration(resources);
	}

	public Enumeration<URL> findResources_public(final String name)
			throws IOException {
		return this.findResources(name);
	}

	private URL toURL(Path path) throws IOException {
		return new URL(null, path.toUri().toString(),
				PathURLStreamHandler.INSTANCE);
	}

	public Collection<ClassLoader> getVitalSignsClassLoaders() {
		return vitalSignsClassLoaders;
	}

	public void setVitalSignsClassLoaders(
			Collection<ClassLoader> vitalSignsClassLoaders) {
		//purge the cache - vitalsigns classloaders list has changed
		_dynamicCache.clear();
		this.vitalSignsClassLoaders = vitalSignsClassLoaders;
	}

	
}
