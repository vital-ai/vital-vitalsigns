package ai.vital.vitalsigns.groovy;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;

import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.VitalCompilationUnit;
import org.codehaus.groovy.control.CompilationUnit.GroovyClassOperation;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.tools.GroovyClass;

import groovy.lang.GroovyClassLoader;

/**
 * Special compiler class loader that adds the ability to output the generated class into Path (nio)
 *
 */
public class VitalGroovyClassLoader extends GroovyClassLoader {

	private Path outputPath = null;
	
	public VitalGroovyClassLoader() {
		super();
	}

	public VitalGroovyClassLoader(ClassLoader arg0, CompilerConfiguration arg1,
			boolean arg2) {
		super(arg0, arg1, arg2);
	}

	public VitalGroovyClassLoader(ClassLoader loader,
			CompilerConfiguration config) {
		super(loader, config);
	}

	public VitalGroovyClassLoader(ClassLoader loader) {
		super(loader);
	}

	public VitalGroovyClassLoader(GroovyClassLoader parent) {
		super(parent);
	}
	
	
	@Override
	protected CompilationUnit createCompilationUnit(
			CompilerConfiguration config, CodeSource source) {
		
		VitalCompilationUnit cu = new VitalCompilationUnit(config, source, this);
		
		if(outputPath != null) {
			cu.addPhaseOperation(new PathOutputOperation(cu, outputPath));
		}
		
		return cu;
	}

	public Path getOutputPath() {
		return outputPath;
	}

	public void setOutputPath(Path outputPath) {
		this.outputPath = outputPath;
	}
	
	private static class PathOutputOperation extends GroovyClassOperation {
		
		CompilationUnit compilationUnit;
		
		Path outputPath;

		public PathOutputOperation(CompilationUnit compilationUnit,
				Path outputPath) {
			super();
			this.compilationUnit = compilationUnit;
			this.outputPath = outputPath;
		}

		public void call(GroovyClass gclass) throws CompilationFailedException {
			 
			OutputStream stream = null;
			
			 try {
				 
				 String name = gclass.getName().replace('.', '/') + ".class";
				 
				 Path path = outputPath.resolve(name);
				 
				 //
				 // Ensure the path is ready for the file
				 //
				 Files.createDirectories(path.getParent());
				 
				 //
				 // Create the file and write out the data
				 //
				 byte[] bytes = gclass.getBytes();
				 
				 stream = Files.newOutputStream(path, StandardOpenOption.CREATE);
				 stream.write(bytes, 0, bytes.length);
				 
			 } catch (IOException e) {
				 
				 compilationUnit.getErrorCollector().addError(Message.create(e.getMessage(), compilationUnit));
				 
			 } finally {
				 
				 IOUtils.closeQuietly(stream);
				 
			 }
		 }
		
	}
	
}
