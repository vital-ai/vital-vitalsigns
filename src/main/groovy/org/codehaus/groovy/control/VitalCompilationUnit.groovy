package org.codehaus.groovy.control

import groovy.lang.GroovyClassLoader;

import java.security.CodeSource
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.tools.GroovyClass

/**
 * 
 * Special compilation unit the ignores output phase - no file system is touched
 *
 */

public class VitalCompilationUnit extends CompilationUnit {

	public VitalCompilationUnit(CompilerConfiguration arg0, CodeSource arg1,
								GroovyClassLoader arg2) {
		super(arg0, arg1, arg2)

		// TODO how to handle this?
		// temporarily make this public
		phaseOperations[Phases.OUTPUT].clear();

		// Phases.OUTPUT




	}
}