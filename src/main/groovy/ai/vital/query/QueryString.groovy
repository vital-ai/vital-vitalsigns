
package ai.vital.query

import java.util.List
import java.util.regex.Matcher
import java.util.regex.Pattern

import ai.vital.query.querybuilder.VitalBuilder;
import ai.vital.vitalservice.ServiceOperations;
import ai.vital.vitalservice.query.VitalGraphQuery;
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalsigns.VitalSignsDomainClassLoader;
import ai.vital.vitalsigns.ast.GroovyBuilderCustomizer
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.VitalBlock

import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.groovy.control.CompilerConfiguration

class QueryString {

	static Pattern importPattern = Pattern.compile("^import\\s.+")
	
	String query

    //see ai.vital.predictmodel.builder.ModelString
    static List<String> defaultImports = [
        
        'import ai.vital.vitalsigns.model.*',
        'import ai.vital.domain.*',
        'import ai.vital.vitalservice.query.*',
        'import ai.vital.vitalservice.segment.*'
        
    ]
    
    public List<VitalBlock> toBlock() {
        
        Query q = eval()
        
        return q.toBlock()
        
    }
    
	public ServiceOperations toService() {
	    
	    Query q = eval()
	            
        return q.toService()
	                    
	}
    
	protected Query eval() {
		
		//
		List imports = []
		
		List body = []
		
		boolean inBody = false
		
		int c = 0
		
		for(String line : query.split("\n")) {
	
			c++
			
			if(line.trim().isEmpty()) {
				body.add(line)
				continue
			}
			
			Matcher m = importPattern.matcher(line.trim())
			
			if(m.matches()) {
			
				if( inBody ) {
					throw new RuntimeException("Line: ${c} - import statements are forbidden inside query body: ${line}")
				} 
		
				imports.add( line )
				
					
			} else {
			
				inBody = true
				
				body.add(line)
				
			}
			
		}
		
        for(String defaultImport : defaultImports) {
            if( ! imports.contains(defaultImport) ) imports.add(defaultImport)
        }
		
        
        def config = new CompilerConfiguration()
        
        //XXX customizer disabled for now
//        def customizer = new GroovyBuilderCustomizer()
//        config.addCompilationCustomizers(customizer)
        
		GroovyShell shell = new GroovyShell(VitalSignsDomainClassLoader.get(), config)
		
        
		String script = """\
		${imports.join('\n')}

		def builder = new ${VitalBuilder.class.canonicalName}()
		return builder.query {
			${body.join('\n')}
		}

"""
        Query query = shell.evaluate(script)
        
//        query.domainsList = customizer.getDomainsList()
//        query.otherClasses = customizer.getOtherClassesList()
        
        //analyze imports and referenced classes
        return query
        
        
	}
    
    /**
     * Unified to query method - select, graph, path query cases
     * @return
     */
    public VitalQuery toQuery() {
        
		Query q = eval()
		
		return q.toQuery() 

	}
	
}
