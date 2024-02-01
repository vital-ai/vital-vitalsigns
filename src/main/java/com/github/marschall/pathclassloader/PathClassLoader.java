package com.github.marschall.pathclassloader;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Loads classes from a certain {@link Path}.
 * 
 * <p>Class loading will happen in a parent first manner which is
 * the Java SE default may.
 */
public final class PathClassLoader extends ClassLoader {

  private final Path path;

  public Path getPath() {
    return path;
  }

static {
    registerAsParallelCapable();
  }

  /**
   * Creates a new {@link PathClassLoader} with no parent class loader.
   * 
   * @param path the path from with to load the classes
   */
  public PathClassLoader(Path path) {
    this(path, null);
  }

  public Class<?> findLoadedClassPublic(String name) {
      return findLoadedClass(name);
  }
  
  /**
   * Creates a new {@link PathClassLoader} with a parent class loader.
   * 
   * @param path the path from with to load the classes
   * @param parent the class loader from which to try loading classes
   *  first
   */
  public PathClassLoader(Path path, ClassLoader parent) {
    super(parent);
    this.path = path;
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    // TODO path injection
    Path classPath = this.path.resolve(name.replace('.', '/').concat(".class"));
    if (Files.exists(classPath)) {
      try {
        byte[] byteCode = Files.readAllBytes(classPath);
        return this.defineClass(name, byteCode, 0, byteCode.length);
      } catch (IOException e) {
        throw new ClassNotFoundException(name, e);
      }
    } else {
      throw new ClassNotFoundException(name);
    }
  }

  public Class<?> findClass_public(String name) throws ClassNotFoundException {
	  return this.findClass(name);
  }
  
  @Override
  protected URL findResource(String name) {
    // TODO path injection
    Path resolved = this.path.resolve(name);
    if (Files.exists(resolved)) {
      try {
        return toURL(resolved);
      } catch (IOException e) {
        throw new RuntimeException("could not open " + resolved, e);
      }
    } else {
      return null;
    }
  }

  public URL findResource_public(String name) {
	  return this.findResource(name);
  }
  
  @Override
  protected Enumeration<URL> findResources(final String name) throws IOException {
    // TODO correct?
    final List<URL> resources = new ArrayList<URL>(1);

    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (!name.isEmpty()) {
          this.addIfMatches(resources, file);
        }
        return super.visitFile(file, attrs);
      }
      
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (!name.isEmpty() || path.equals(dir)) {
          this.addIfMatches(resources, dir);
        }
        return super.preVisitDirectory(dir, attrs);
      }

      void addIfMatches(List<URL> resources, Path file) throws IOException {
        String rel = path.relativize(file).toString().replace('\\', '/');
        if (rel.equals(name)) {
          resources.add(toURL(file));
        }
      }
      
    });
    return Collections.enumeration(resources);
  }
  
  public Enumeration<URL> findResources_public(final String name) throws IOException {
	  return this.findResources(name);
  }

  private URL toURL(Path path) throws IOException {
    return new URL(null, path.toUri().toString(), PathURLStreamHandler.INSTANCE);
  }

/*
	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {

		//if it's 
		if(name.startsWith("ai.vital.vitalsigns.model.property.")) {
			String cls = "/" + name.replace('.', '/') + ".class";
			InputStream r = null;
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			try {
				r = VitalSigns.class.getResourceAsStream(cls);
				try {
					IOUtils.copy(r, os);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch(Exception e) {
				return super.loadClass(name, resolve);
			} finally {
				IOUtils.closeQuietly(r);
			}
			
			
			byte[] byteArray = os.toByteArray();
			return defineClass(name, byteArray, 0, byteArray.length);
			
		}
		
		return super.loadClass(name, resolve);
	}


	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return this.loadClass(name, false);
	}
	*/
  

}
