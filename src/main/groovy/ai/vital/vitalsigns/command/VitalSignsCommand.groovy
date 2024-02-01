package ai.vital.vitalsigns.command

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.FileSystem
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.regex.Matcher
import java.util.regex.Pattern

//import javax.security.auth.login.Configuration;

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.Option
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.semanticweb.owlapi.io.FileDocumentSource
import org.semanticweb.owlapi.io.OWLOntologyLoaderMetaData;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import owl2vcs.tools.GitMergeTool
import owl2vcs.tools.OntologyNormalizer
import owl2vcs.tools.OntologyNormalizer.CommentPattern;

import com.google.common.jimfs.Jimfs;
import com.hp.hpl.jena.ontology.OntDocumentManager
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource
import com.hp.hpl.jena.rdf.model.Statement
import com.hp.hpl.jena.rdf.model.StmtIterator
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.json.JSONSchemaGenerator;
import ai.vital.vitalsigns.model.DomainOntology;
import ai.vital.vitalsigns.ontology.DomainGenerator;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.rdf.RDFUtils;
import ai.vital.vitalsigns.scripts.TaxonomyEdgesVerifier;
import ai.vital.vitalsigns.command.patterns.*;
import ai.vital.vitalsigns.conf.VitalSignsConfig
import ai.vital.vitalsigns.conf.VitalSignsConfig.DomainsStrategy;

import com.google.common.jimfs.Configuration

import org.semanticweb.owlapi.apibinding.OWLManager


import groovy.cli.picocli.CliBuilder

class VitalSignsCommand {

	static String VS = 'vitalsigns'
	
	static String CMD_GENERATE = 'generate'
	
	static String CMD_STATUS = 'status'
	
	static String CMD_VERSION = 'version'
	
	static String CMD_LISTINDIVIDUALS = 'listindividuals'
	
	static String CMD_MERGEINDIVIDUALS = 'mergeindividuals'
	
	static String CMD_REMOVEINDIVIDUALS = 'removeindividuals'
	
	static String CMD_MERGE = 'merge'
	
	static String CMD_NORMALIZEONTOLOGY = 'normalizeontology'
	
	static String CMD_VALIDATEONTOLOGY = 'validateontology'
	
	static String CMD_DIFF = 'diff'
	
	static String CMD_HELP = 'help'
	
	static String CMD_VERIFY = 'verify'
	
	static String CMD_GITENABLE = 'gitenable'
	
	static String CMD_GITDISABLE = 'gitdisable'
	
	static String CMD_GITPOSTMERGE = 'gitpostmerge'
	
	static String CMD_MERGESTATUS = 'mergestatus'
	
	static String CMD_GITMERGETOOL = 'gitmergetool'
	
	static String CMD_GITJARMERGE = 'gitjarmerge'
	
	static String CMD_UPVERSION = 'upversion'
	
	static String CMD_DOWNVERSION = 'downversion'
	
	static String CMD_CHECKIN = 'checkin'
	
	static String CMD_PURGE = 'purge'
    
	static String CMD_DEPLOY = 'deploy'
    
	static String CMD_UNDEPLOY = 'undeploy'
	
	
	static Map cmd2CLI = [:];
	
	static Set allCommands = null;
	
	public static Pattern vitalCoreOwlFilePattern = Pattern.compile("(vital\\-core)\\-(\\d+)\\.(\\d+)\\.(\\d+)\\.owl");
	
	public static Pattern vitalOwlFilePattern = Pattern.compile("(vital)\\-(\\d+)\\.(\\d+)\\.(\\d+)\\.owl");
	
	public static Pattern vitalSuperAdminOwlFilePattern = Pattern.compile("(vital\\-superadmin)\\-(\\d+)\\.(\\d+)\\.(\\d+)\\.owl");
	
	
	
	public static Pattern vitalJarFilePattern = Pattern.compile("(VitalDomain\\-groovy)\\-(\\d+)\\.(\\d+)\\.(\\d+)\\.jar");
	
	public static Pattern vitalSuperAdminJarFilePattern = Pattern.compile("(VitalSuperAdmin\\-groovy)\\-(\\d+)\\.(\\d+)\\.(\\d+)\\.jar");
	
	
	
	public static Pattern vitalJsonFilePattern = Pattern.compile("(vital)\\-(\\d+)\\.(\\d+)\\.(\\d+)\\.js");
	
	public static Pattern vitalCoreJsonFilePattern = Pattern.compile("(vital\\-core)\\-(\\d+)\\.(\\d+)\\.(\\d+)\\.js");
	
	public static Pattern vitalSuperAdminJsonFilePattern = Pattern.compile("(vital\\-superadmin)\\-(\\d+)\\.(\\d+)\\.(\\d+)\\.js");
	
	static {
		
		def cmdGenerateCLI = new CliBuilder(usage: "${VS} ${CMD_GENERATE} [options]", stopAtNonOption: false)
		
		cmdGenerateCLI.with {
			// h longOpt: 'help', "Show '${CMD_GENERATE}' command help"
			o longOpt: "ontology", "OWL Domain Ontology File", args: 1, required: true
			p longOpt: "package", "(only groovy) package (ie. com.your-domain.your-app-name), required if ontology does not have default package property", args: 1, required: false
			j longOpt: "jar", "(only groovy) optional output jar file path, by default it will write domain jar at \$VITAL_HOME/domain-groovy-jar/app-groovy-version.jar", args: 1, required: false
			js longOpt: "json-schema", "(only json) optional output json schema file path, by default it will write domain schema json at \$VITAL_HOME/domain-json-schema/app-version.js", args: 1, required: false
			or longOpt: "override", "force to generate the new version even if no changes detected or json schema exists", args: 0, required: false
			t longOpt: "target", "output target: 'groovy' or 'json', default 'groovy'", args: 1, required: false
            tl longOpt: 'temp-location', "(only groovy) optional temp location for disk based generation (very large domains), memory if not specified", args: 1, required: false
            sd longOpt: 'skip-dsld', "(only groovy) skip dsld generation part", args: 0, required: false
		}
		
		cmd2CLI[CMD_GENERATE] = cmdGenerateCLI
		
		
		def cmdVersionCLI = new CliBuilder(usage: "${VS} ${CMD_VERSION} [options]", stopAtNonOption: false)
		
		cmdVersionCLI.with {
//			h longOpt: 'help', "Show '${CMD_GENERATE}' command help"
			o longOpt: "ontology", "OWL Domain Ontology File", args: 1, required: true
		}
		
		cmd2CLI[CMD_VERSION] = cmdVersionCLI

		
		def cmdStatusCLI = new CliBuilder(usage: "${VS} ${CMD_STATUS} (no options)", stopAtNonOption: false)
		cmdStatusCLI.with {
			//no arguments
			q longOpt: 'quick', 'quick - skip parsing ontologies', args: 0, required: false
			v longOpt: 'verbose', 'print warnings and other messages', args: 0, required: false
		}
		cmd2CLI[CMD_STATUS] = cmdStatusCLI
		
		
		def cmdListindividualsCLI = new CliBuilder(usage: "${VS} ${CMD_LISTINDIVIDUALS} [options]", stopAtNonOption: false)
		cmdListindividualsCLI.with {
			o longOpt: "ontology", "OWL Domain Ontology File", args: 1, required: true
		}
		cmd2CLI[CMD_LISTINDIVIDUALS] = cmdListindividualsCLI
		
		
		def cmdMergeIndividualsCLI = new CliBuilder(usage: "${VS} ${CMD_MERGEINDIVIDUALS} [options]", stopAtNonOption: false)
		cmdMergeIndividualsCLI.with {
			i longOpt: "individuals", "individuals ontology file path", args: 1, required: true
			o longOpt: "ontology", "ontology file name in \$VITAL_HOME/domain-ontology/", args: 1, required: true
		}
		cmd2CLI[CMD_MERGEINDIVIDUALS] = cmdMergeIndividualsCLI
		
		
		def cmdRemoveIndividualsCLI = new CliBuilder(usage: "${VS} ${CMD_REMOVEINDIVIDUALS} [options]", stopAtNonOption: false)
		cmdRemoveIndividualsCLI.with {
			i longOpt: "individuals", "individuals ontology file path", args: 1, required: true
			o longOpt: "ontology", "ontology file name in \$VITAL_HOME/domain-ontology/", args: 1, required: true
		}
		cmd2CLI[CMD_REMOVEINDIVIDUALS] = cmdRemoveIndividualsCLI
		
		
		def cmdDiffCLI = new CliBuilder(usage:"${VS} ${CMD_DIFF} [options]", stopAtNonOption: false)
		cmdDiffCLI.with {
			o longOpt: 'ont', "exactly 1 or 2 such params required: ontology file path, when just file name used it will look in \$VITAL_HOME/domain-ontology/**", args: 1, required: true
			h longOpt: 'history', 'show the Nth prior version found in the archive - used with single ont param only', args: 1, required: false
		}
		cmd2CLI[CMD_DIFF] = cmdDiffCLI
		
		
		def cmdMergeCLI = new CliBuilder(usage: "${VS} ${CMD_MERGE} [options]", stopAtNonOption: false)
		cmdMergeCLI.with {
			o longOpt: 'ont', "input ontology", args: 1, required: true
			m longOpt: 'merging', "merging ontology", args: 1, required: true
		}
		cmd2CLI[CMD_MERGE] = cmdMergeCLI
		
		def cmdNormalizeOntologyCLI = new CliBuilder(usage: "${VS} ${CMD_NORMALIZEONTOLOGY} [options]", stopAtNonOption: false)
		cmdNormalizeOntologyCLI.with {
			o longOpt: 'ont', "ontology (will be replaced)", args: 1, required: true
			cb longOpt: 'commentsbefore', "comments inserted before, after if flag not specified", args: 0, required: false
		}
		cmd2CLI[CMD_NORMALIZEONTOLOGY] = cmdNormalizeOntologyCLI
		
		
		def cmdValidateOntologyCLI = new CliBuilder(usage: "${VS} ${CMD_VALIDATEONTOLOGY} [options]", stopAtNonOption: false)
		cmdValidateOntologyCLI.with {
			o longOpt: 'ont', "input ontology", args: 1, required: true
		}
		cmd2CLI[CMD_VALIDATEONTOLOGY] = cmdValidateOntologyCLI
		
		def cmdVerifyCLI = new CliBuilder(usage: "${VS} ${CMD_VERIFY} (no options)", stopAtNonOption: false)
		cmdVerifyCLI.with {
			
		}
		cmd2CLI[CMD_VERIFY] = cmdVerifyCLI
		
		
		def cmdGitEnableCLI = new CliBuilder(usage: "${VS} ${CMD_GITENABLE} (no options)", stopAtNonOption: false)
		cmdGitEnableCLI.with {
			
		}
		cmd2CLI[CMD_GITENABLE] = cmdGitEnableCLI
		
		def cmdGitDisableCLI = new CliBuilder(usage: "${VS} ${CMD_GITDISABLE} (no options)", stopAtNonOption: false)
		cmdGitDisableCLI.with {
			
		}
		cmd2CLI[CMD_GITDISABLE] = cmdGitDisableCLI
		
		
		def cmdUpversionCLI = new CliBuilder(usage: "${VS} ${CMD_UPVERSION} [options]", stopAtNonOption: false)
		cmdUpversionCLI.with {
			o longOpt: 'ont', "input ontology, either file name or path to domain owl in (\$VITAL_HOME/domain-ontology/)", args: 1, required: true
		}
		cmd2CLI[CMD_UPVERSION] = cmdUpversionCLI
		
		def cmdDownversionCLI = new CliBuilder(usage: "${VS} ${CMD_DOWNVERSION} [options]", stopAtNonOption: false)
		cmdDownversionCLI.with {
			o longOpt: 'ont', "input ontology, either file name or path to domain owl in (\$VITAL_HOME/domain-ontology/)", args: 1, required: true
			v longOpt: 'version', "optional version to be reverted to, n.n.n, latest used if not specified", args: 1, required: false
		}
		cmd2CLI[CMD_DOWNVERSION] = cmdDownversionCLI
		
		
		def cmdCheckinCLI = new CliBuilder(usage: "${VS} ${CMD_CHECKIN} [options]", stopAtNonOption: false)
		cmdCheckinCLI.with {
			o longOpt: 'ont', "external ontology file, must not be located in \$VITAL_HOME", args: 1, required: true
		}
		cmd2CLI[CMD_CHECKIN] = cmdCheckinCLI
		
		
		def cmdPurgeCLI = new CliBuilder(usage: "${VS} ${CMD_PURGE} [options]", stopAtNonOption: false)
		cmdPurgeCLI.with {
			a longOpt: 'app', "owl and jar files prefix", args: 1, required: true
		}
		cmd2CLI[CMD_PURGE] = cmdPurgeCLI
		
		def cmdGitPostMerge = new CliBuilder(usage: "${VS} ${CMD_GITPOSTMERGE} (no options)", stopAtNonOption: false)
		cmdGitPostMerge.with {
			
		}
		cmd2CLI[CMD_GITPOSTMERGE] = cmdGitPostMerge
		
		def cmdMergeStatus = new CliBuilder(usage: "${VS} ${CMD_MERGESTATUS} (no options)", stopAtNonOption: false)
		cmdMergeStatus.with {
			
		}
		cmd2CLI[CMD_MERGESTATUS] = cmdMergeStatus
		
		def cmdGitMergeTool = new CliBuilder(usage: "${VS} ${CMD_GITMERGETOOL} (options)", stopAtNonOption: false)
		cmdGitMergeTool.with {
			b longOpt: 'base', "base owl file path", args: 1, required: true
			l longOpt: 'local', "local owl file path", args: 1, required: true
			r longOpt: 'remote', "remote owl file path", args: 1, required: true
			o longOpt: 'output', "output merged owl file path", args: 1, required: true
		}
		cmd2CLI[CMD_GITMERGETOOL] = cmdGitMergeTool
		
		def cmdGitJarMerge = new CliBuilder(usage: "${VS} ${CMD_GITJARMERGE} (options)", stopAtNonOption: false)
		cmdGitJarMerge.with {
			b longOpt: 'base', "base jar file path", args: 1, required: true
			l longOpt: 'local', "local jar file path", args: 1, required: true
			r longOpt: 'remote', "remote jar file path", args: 1, required: true
			o longOpt: 'output', "output merged jar file path", args: 1, required: true
		}
		cmd2CLI[CMD_GITJARMERGE] = cmdGitJarMerge
		
        
        
        def cmdDeploy = new CliBuilder(usage: "${VS} ${CMD_DEPLOY} (options)", stopAtNonOption: false)
        cmdDeploy.with {
            org longOpt: 'organization', "Organization ID", args: 1, required: true
            app longOpt: 'application', "Application ID", args: 1, required: true
            o longOpt: "ontology", "OWL Domain Ontology File", args: 1, required: false
            j longOpt: "jar", "domain jar file path", args: 1, required: false
            js longOpt: "json-schema", "json schema file path", args: 1, required: false
        }
        cmd2CLI[CMD_DEPLOY] = cmdDeploy
        
        def cmdUndeploy = new CliBuilder(usage: "${VS} ${CMD_UNDEPLOY} (options)", stopAtNonOption: false)
        cmdUndeploy.with {
            o longOpt: "ontology", "OWL Domain Ontology File", args: 1, required: false
            j longOpt: "jar", "domain jar file path", args: 1, required: false
            js longOpt: "json-schema", "json schema file path", args: 1, required: false
        }
        cmd2CLI[CMD_UNDEPLOY] = cmdUndeploy
        
		allCommands = new HashSet(cmd2CLI.keySet())
		allCommands.add(CMD_HELP)
		
	}
		
	public static void main(String[] args) {

		//first level simply iterates over all options without parsing to determine the structure
		
		String VITAL_HOME = System.getenv('VITAL_HOME');
		if(!VITAL_HOME) error("VITAL_HOME environment variable not set.");
		
		File vitalHome = new File(VITAL_HOME);
		if(!vitalHome.exists()) error("$VITAL_HOME path does not exist: ${VITAL_HOME} ");
		if(!vitalHome.isDirectory()) error("VITAL_HOME does not denote a directory: ${VITAL_HOME}");
		
		if(args.length == 0 || args[0] == CMD_HELP) {
			println "Usage: ${VS} <command> [options]"
			println "   where command is one of: ${allCommands}"
			
			println "\n"
			
			for(def e : cmd2CLI.entrySet() ) {
				String cmd = e.key
				def cli = e.value
				cli.usage();
				println "\n"
			}
			
			return;
		}
		
		String cmd = args[0];
		
		def cli = cmd2CLI[cmd];
		
		if(cli == null) {
			println "Unknown command: ${cmd}"
			println "available commands: ${allCommands}"
			return
		}
		
		List params = [];
		for(int i = 1 ; i < args.length; i++) {
			params.add(args[i]);
		}
		
		
		//https://www.kernel.org/pub/software/scm/git/docs/git.html#_git_diffs
		//path old-file old-hex old-mode new-file new-hex new-mode
		if(cmd == CMD_DIFF && params.size() == 7) {
			//println "DIFF command, args: ${args}"
			
			try {
				DiffCommand.diff(vitalHome, params.get(1), params.get(4), null)
			} catch(Exception e) {
				error e.localizedMessage
			}
			
			return
			
		}
		
		println "VITAL_HOME: ${VITAL_HOME} - resolved to ${vitalHome.absolutePath}"
		
		println "command: ${cmd}"
		
		def options = cli.parse(params.toArray(new String[params.size()]))
		
		if(!options) return
		
		/*	
		def cli = new CliBuilder(usage: 'vitalsigns [command] [options]')
		cli.with {
			
			h longOpt: "help", "Show usage information"
			v longOpt: "version", "Show version"
			g longOpt: "generate", "generate"
			p longOpt: "package", "package (ie. com.your-domain.your-app-name)", args: 1
			j longOpt: "jar", "output jar file path", args: 1
			o longOpt: "ontology", "OWL Domain Ontology File", args: 1
			
		}
		*/
		
		/*
		if(options.h) {
			cli.usage();
			return;	
		}
		*/
		
		if(cmd == CMD_STATUS) {
			
			boolean quick = Boolean.TRUE.equals(options.q)
			
			boolean verbose = Boolean.TRUE.equals(options.v)
			
            println "VDK ${VitalSigns.VERSION}"
            
			if(verbose) {
				
				println "verbose mode ON - printing warnings and info messages"
				println "skip owl parsing (quick mode): ${quick}"
				
			}
			
			


			
			println ""
			
			List<List<String>> errorsWarnings = handleStatus(vitalHome, gitAvailable(vitalHome, verbose), verbose, !quick)
			
			List<String> errors = errorsWarnings[0]
			List<String> warnings = errorsWarnings[1]
			
            for(String s : VitalModulesVersionsCheck.checkModulesVersions()) {
                println "WARN: " + s
            }
            
			for(String w : warnings) {
				if(verbose) {
					println "WARN: ${w}"
				}
			}
			
			if(errors.size() < 1) {
				println "VitalSigns status OK - no errors"
			} else {
				println "VitalSigns status ERROR - ${errors.size()} error${errors.size() != 1 ? 's' : ''} found"
				
				for(int i = 0 ; i < errors.size(); i++) {
					println "${i+1}: ${errors[i]}"
				}
				
			}
            
            if(verbose) {
                VitalSignsCommandHelper.printOntologies(vitalHome)
            }
			
			return			
			
		}
		
		if(cmd == CMD_GITENABLE) {
			GitDiffCommands.gitEnable(vitalHome)
			return
		}
		
		if(cmd == CMD_GITDISABLE) {
			GitDiffCommands.gitDisable(vitalHome)
			return
		}
		
		if(cmd == CMD_GITPOSTMERGE || cmd == CMD_MERGESTATUS) {
//			List<String> errors = GitPostMergeHook.postMerge(vitalHome)
			List<String> errors = []
			List<String> warnings = []
			
			List<Pattern> patterns = readPatterns(vitalHome)
			
			String label = cmd == CMD_GITPOSTMERGE ? 'post-merge' : 'merge'
			
			validateDomainOntologiesAndJars(vitalHome, patterns, errors, warnings, false, false)
			if(errors.size() > 0) {
				println "${errors.size()} ${label} error${errors.size() != 1 ? 's' : ''} detected"
				for(int i = 0 ; i < errors.size(); i++) {
					println "${i+1}. ${errors.get(i)}"
				}
				System.exit(1)
			} else {
				println "OK"
			}
			return
		}
		
		if(cmd == CMD_GITMERGETOOL) {
			
			try {
				
				String base = options.b
				String local = options.l
				String remote = options.r
				String output = options.o
				
				String errors = new GitMergeTool().merge(vitalHome, base, local, remote, output)
				
				if(!errors) {
					println "merged without conflicts"
				} else {
					println "merger errors: ${errors}"
					System.exit(1)
				}
				
			} catch(Exception e) {
				System.err.println e.localizedMessage
				System.exit(1)
			}
			
			return
		}
		
		if(cmd == CMD_GITJARMERGE) {
			
			try {
				
				String base = options.b
				String local = options.l
				String remote = options.r
				String output = options.o
				
				String errors = new GitJarMerge().merge(vitalHome, base, local, remote, output)
				
				if(!errors) {
					println "jars merged without conflicts"
				} else {
					println "jars merger errors: ${errors}"
					System.exit(1)
				}
				
			} catch(Exception e) {
				System.err.println e.localizedMessage
				System.exit(1)
			}
			
			return
			
		}
		
		if(cmd == CMD_UPVERSION) {
			
			String ont = options.o
			
			try {
				OntVersionCommands.upVersion(vitalHome, ont)
			} catch(Exception e) {
				e.printStackTrace()
				error e.localizedMessage
			}
		
			return	
		}
		
		if(cmd == CMD_DOWNVERSION) {
			
			String ont = options.o
			String versionString = null
			
			Object version = options.v
			
			if(version) {
				versionString = version
			}
			
			try {
				OntVersionCommands.downVersion(vitalHome, ont, versionString)
			} catch(Exception e) {
				error e.localizedMessage
			}
			return
			
		}
		
		if(cmd == CMD_CHECKIN) {
			
			String ont = options.o
			
			try {
				OntVersionCommands.checkin(vitalHome, ont)
			} catch(Exception e) {
				error e.localizedMessage
			}
			return	
		}

		if(cmd == CMD_PURGE) {
			
			String app = options.a
			
			try {
				OntVersionCommands.purge(vitalHome, app)
			} catch(Exception e) {
				error e.localizedMessage
			}
			return	
			
		}		
		
        if(cmd == CMD_DEPLOY) {
            try {
                DeployUndeployCommand.deploy(vitalHome, options.org, options.app, options.o ? options.o : null, options.j ? options.j : null, options.js ? options.js : null)
            } catch(Exception e) {
                error e.localizedMessage
            }
            return
        }
        
        if(cmd == CMD_UNDEPLOY) {
            try {
                DeployUndeployCommand.undeploy(vitalHome, options.o ? options.o : null, options.j ? options.j : null, options.js ? options.js : null)
            } catch(Exception e) {
                error e.localizedMessage
            }
            return
        }
		
		if(cmd == CMD_LISTINDIVIDUALS) {
			
			File ontFile = new File(options.o)
			println "Input ontology file: ${ontFile.absolutePath}"
			ListIndividualsCommand.listIndividuals(ontFile, true)
			
			return
		}
		
		if(cmd == CMD_MERGEINDIVIDUALS || cmd == CMD_REMOVEINDIVIDUALS) {
			
			File sourceOWLFile = new File(options.i)
			println "Individuals OWL file: ${sourceOWLFile.absolutePath}"
			
			String ontologyFileName = options.o
			println "Domain ontology OWL file: ${ontologyFileName}"
			
			try {
				MergeIndividualsCommand.mergeIndividuals(vitalHome, sourceOWLFile, ontologyFileName, cmd == CMD_MERGEINDIVIDUALS)
			} catch(Exception e) {
				error e.localizedMessage
			}
			return 
		}
		
		if(cmd == CMD_VALIDATEONTOLOGY) {
			
			System.exit( ValidateOntologyCommand.validateOntology(vitalHome, options.o) )
			return 
			
		}
		
		if(cmd == CMD_NORMALIZEONTOLOGY) {
			
			File input = new File(options.o)
			
			File output = new File(options.o)
			
			Boolean commentsBefore = options.cb
			if(commentsBefore == null) commentsBefore = false
			
			String c = ''
			for(int i = 0; i < 100; i++) {
				c += 'C'
			}
			
			if(commentsBefore) {
				c += 'I'
			} else {
				c = 'I' + c
			}
			
			CommentPattern pattern = CommentPattern.parseFromParam(c)
			
			OntologyNormalizer on = new OntologyNormalizer(input, pattern)
			
			if(on.isOntologyInOrder()) {
				println "Ontology already normalized"
				return
			}
			
			on.normalizeOntology(output)
			
			println "DONE"
			
			return
		}
		
		if(cmd == CMD_DIFF) {
			
			List<String> onts = options.os
			
			if(onts.size() != 2 && onts.size() != 1) error "Expected exactly 1 or 2 intput --ont (-o) parameters"
			
			String historyParam = options.h ? options.h : null
			
			Integer history = 1 
			
			if(historyParam != null) {
				
				if(onts.size() > 1) {
					println "History param ignored when two ontologies specified."
				} else {
					try {
						history = Integer.parseInt(historyParam)
						if(history < 0) throw new Exception('must be a positive integer')
					} catch(Exception e) {
						error "Invalid history param: ${historyParam} - ${e.localizedMessage}"
					}
					
					println "History: ${history}"
				}
				
			} else {
			
				if(onts.size() > 1) {
					println "History param ignored when two ontologies specified."
				} else {
					println "Default history param: ${history}"
				}
			}
			
			String f1 = onts[0]
			String f2 = onts.size() > 1 ? onts[1] : null
			
			try {
				DiffCommand.diff(vitalHome, f1, f2, history)
			} catch(Exception e) {
				error e.localizedMessage
			}
			
			return
			
		}
		
		if(cmd == CMD_MERGE) {
			
			String ont = options.o
			String mergingOntPath = options.m
			
			println "Ontology file: ${ont}"
			println "Merging ontology path: ${mergingOntPath}"
			
			new MergeOntologiesGitMergeCommand(vitalHome).mergeOntologies(ont, mergingOntPath)
			
		}
		
		if(cmd == CMD_VERIFY) {
			
			TaxonomyEdgesVerifier.main(new String[0])
			return
			
		}
		
		if(cmd == CMD_GENERATE || cmd == CMD_VERSION) {
			
			println "Checking vitalsigns status first..."
			
			boolean gitAvailable = gitAvailable(vitalHome, false)
			
			List<List<String>> errorsWarnings = handleStatus(vitalHome, gitAvailable, false, false)
			
			List<String> errors = errorsWarnings[0]
			List<String> warnings = errorsWarnings[1]
			
			for(String w : warnings) {
				println "WARN: ${w}"
			}
			
			if(errors.size() > 0) {
				println "vitalsigns status wasn't OK - please fix the following errors first:"
				for(int i = 0 ; i < errors.size(); i ++) {
					println "${i+1}: ${errors.get(i)}"
				}
				error("")
			}
			
			
			String owlFile = options.o;
			
			if(!owlFile) error("No owl file param");
			
			File owlF = new File(owlFile);
			
			Matcher matcherX = OwlFileInfo.domain_version.matcher(owlF.name) 
			if(!matcherX.matches()) {
				error("Owl file name: ${owlF.name} must match pattern: ${OwlFileInfo.domain_version.pattern()}");
			}
			
			OwlFileInfo matcher = OwlFileInfo.fromString(owlF.name)
			
			println "Owl file input path: ${owlFile}"
			
			File domainOntologyDir = new File(vitalHome, 'domain-ontology')
			
			File domainArchiveDir = new File(domainOntologyDir, 'archive')
			
			
			File domainJarDir = new File(vitalHome, 'domain-groovy-jar')
			File domainJarArchiveDir = new File(domainJarDir, 'archive')
			
			domainArchiveDir.mkdir()
			
			domainJarArchiveDir.mkdir()
			

			owlF = new File(owlFile)
							
			
			if(!owlF.exists()) error("Owl file not found at location: ${owlF.absolutePath}")
			
			
			String app = matcher.domain
			Integer file_majorVersion = matcher.major
			Integer file_minorVersion = matcher.minor
			Integer file_patchVersion = matcher.patch
			
			/*
			Ontology activeOntology = null;
			for( ExtendedIterator<Ontology> iter = m.listOntologies(); iter.hasNext(); ) {
				Ontology ont = iter.next();
				if(ont.getURI() == ontNS) {
					activeOntology = ont;
				}
			}
			
			if(activeOntology == null) error("No active ontology found in owl file: ${owlFile}");
			
			String versionInfo = activeOntology.getVersionInfo();
			*/
			
			println "File major version: " + file_majorVersion
			println "File minor version: " + file_minorVersion
			println "File patch version: " + file_patchVersion
			
			
			OntDocumentManager manager = new OntDocumentManager()

			boolean processImports = manager.getProcessImports()

			manager.setProcessImports(false)
			
			OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM)

			spec.setDocumentManager(manager)

			OntModel m  = ModelFactory.createOntologyModel(spec, null );
			
			// OntModel m = ModelFactory.createOntologyModel();

			m.read(new FileInputStream(owlF), null);
			
			Ontology ontology = null;
			
			for( ExtendedIterator<Ontology> iterator = m.listOntologies(); iterator.hasNext(); ) {
				if(ontology != null) error("More than 1 ontology detected in owl file: ${owlF.absolutePath}")
				ontology = iterator.next();
			}
			
			if(ontology == null) {
				error("No ontology found in file: ${owlF.absolutePath}")
			}
			
			String ontologyURI = ontology.getURI()
			println "Ontology URI: ${ontologyURI}"
            
            VitalSigns.domainBeingRegenerated = ontologyURI
			
			// if(ontologyURI.contains("_")) error("Ontology IRI cannot contain underscores: ${ontologyURI}")
			
			if(!ontologyURI) error("No ontology prefix found.");

			if(ontologyURI.endsWith("#")) ontologyURI = ontologyURI.substring(0, ontologyURI.length() - 1 );
			
			String versionInfo = ontology.getVersionInfo()

			if(!versionInfo) error("Ontology ${owlF.absolutePath} URI: ${ontologyURI} does not have versionInfo annotation!");

			List<Statement> packageStmts = ontology.listProperties(VitalCoreOntology.hasDefaultPackage).toList()
			
			if(packageStmts.size() > 1) error("Ontology ${owlF.absolutePath} URI: ${ontologyURI} cannot have more than vital-core:hasDefaultPackage annotation property.")
			
			String _defaultPackage = null;
			
			if(packageStmts.size() > 0) {
				Statement stmt = packageStmts[0]
				RDFNode v = stmt.getObject()
				if(!v.isLiteral()) error("Ontology ${owlF.absolutePath} URI: ${ontologyURI} vital-core:hasDefaultPackage value must be a literal") 
				_defaultPackage = v.asLiteral().getString()
				if(!_defaultPackage) error("Ontology ${owlF.absolutePath} URI: ${ontologyURI} vital-core:hasDefaultPackage literal must be a non empty string")
				println "Default package: ${_defaultPackage}"
			}
			
			
			Matcher versionMatcher = DomainOntology.versionPattern.matcher(versionInfo)
			
			if(!versionMatcher.matches()) error("Ontology versionInfo annotation does not match pattern: ${DomainOntology.versionPattern} - ${versionInfo}")
			
			Integer ont_majorVersion = Integer.parseInt(versionMatcher.group(1))
			Integer ont_minorVersion = Integer.parseInt(versionMatcher.group(2))
			Integer ont_patchVersion = Integer.parseInt(versionMatcher.group(3))
			
			println "Ontology major version: " + ont_majorVersion
			println "Ontology minor version: " + ont_minorVersion
			println "Ontology patch version: " + ont_patchVersion
			
			if(file_majorVersion != ont_majorVersion || file_minorVersion != ont_minorVersion || file_patchVersion != ont_patchVersion) {
				error("File and ontology version numbers don't match: ${file_majorVersion}.${file_minorVersion}.${file_patchVersion} vs ${ont_majorVersion}.${ont_minorVersion}.${ont_patchVersion}")
			}
			
			if(cmd == CMD_VERSION) {
				return;
			}
			
			
			String _package = options.p ? options.p : null;
			
			Object _target = options.t
			
			if(_target == null || _target == false) {
				_target = 'groovy'
			}
			
			if(_target == 'groovy') {
				
				if(!_package) {
					if(!_defaultPackage) error("No package parameter given but the ontology does not provide default package")
					println "Using default package: ${_defaultPackage}"
					_package = _defaultPackage
				} else {
					println "Package: ${_package}"
				}
				
				// if( ((String)_package).startsWith("ai.vital.") ) {
				// 	error "Cannot use a package that starts with 'ai.vital.' - ${_package}"
				// }
				
				
			}
			
			
			
			println "Target: ${_target}"
			
			if(_target == 'groovy' || _target == 'json') {
				
			} else {
				error "Unknown target: ${_target} , expected: 'groovy', 'json'"
			}
			
			Boolean override = options.or

			if(override == null) override = false
			
			
			// check if ontology contains only own owl resource
			OntologyNormalizer norm1 = new OntologyNormalizer(owlF, CommentPattern.parseFromParam("ICCCCCCCCCC"))
			
			String classes = norm1.checkOtherOntologiesClasses()
			
			if(classes) {
				error(owlF.absolutePath + ": " + classes);
			}
			
			if(_target == 'groovy') {
				
				println "Generating groovy jar..."
				
				def j = options.j;
                
                String tl = options.tl ? options.tl : null
                
                Boolean skipDsld = options.sd ? options.sd : false
                
                File tempLocation = null
                
                if(tl != null) {
                    tempLocation = new File(tl)
                    println "Using tempLocation ${tempLocation.absolutePath}"
                    if(!tempLocation.isDirectory()) error("Temp location does not exist or is not a directory: ${tempLocation.absolutePath}")
                } 
                
                println "skip dsld ? ${skipDsld}"
				
				File currentVersion = new File(domainJarDir, "${app}-groovy-${ont_majorVersion}.${ont_minorVersion}.${ont_patchVersion}.jar");
				
				if(!j && currentVersion.exists()) {
					
					println "Current domain jar exists - ${currentVersion.absolutePath} - checking if ontology has changed"
					
					boolean different = DiffCommand.checkIfVersionHasChanged(owlF, currentVersion)
					
					if(! different ) {
						
						if(!override) {
							println "No changes to current domain jar version detected - exiting now."
							return
						} else {
							println "No changes to current domain jar version detected but override flag is set - proceeding..."
						}
						
					}
					
				}
				
				//not increasing...
				int newPatchVersion = ont_patchVersion + 0//1
				
				println "Generating domain jar - *not* increasing patch version to ${newPatchVersion}..."
				
				File targetJar = null;
	
				
				if(j) {
					targetJar = new File(j);
					println "Custom target jar location: ${targetJar.absolutePath}"
				} else {
					targetJar = new File(domainJarDir, "${app}-groovy-${file_majorVersion}.${file_minorVersion}.${newPatchVersion}.jar");
					println "Creating jar at default location: ${targetJar.absolutePath}"
				}
				
				Matcher jarMatcherX = JarFileInfo.domain_version.matcher(targetJar.name)
				
				if( ! jarMatcherX.matches() ) {
					error("Target jar file name does not match pattern ${JarFileInfo.domain_version.pattern()} - ${targetJar.absolutePath}")
				}
				
				JarFileInfo jarMatcher = JarFileInfo.fromString(targetJar.name)
				
				Integer target_majorVersion = jarMatcher.major
				Integer target_minorVersion = jarMatcher.minor
				Integer target_patchVersion = jarMatcher.patch
				
	
				if(target_majorVersion != ont_majorVersion || target_minorVersion != ont_minorVersion || target_patchVersion != newPatchVersion) {
					error("Target jar version must be equal to ${ont_majorVersion}.${ont_minorVersion}.${newPatchVersion}, current: ${target_majorVersion}.${target_minorVersion}.${target_patchVersion}")
				}
				

                
                if( VitalSigns.get().getConfig().domainsStrategy == DomainsStrategy.dynamic ) {
                    println "WARNING: '${CMD_GENERATE}' command should be used with static domains strategy"
                }				
				
				
				//use current owl file!
				
	//			String jarFile = 
				
	//		println("Usage: <owlFile> <namespace> <package> <jarFile> <type>");
				
//				Generator.generateJar(owlF, ontologyURI, _package, targetJar, Generator.GROOVY)
				
				//generate into memory FS, then output to jar path
				FileSystem fs = null;
				//XXX connect generator
				FileInputStream fis = null;
				try {
                    
                    Path targetSrcPath = null
                    Path targetClassesPath = null
                    
                    if(tempLocation != null) {
                        
                        fs = FileSystems.getDefault()
                        Path basePath = tempLocation.getCanonicalFile().toPath()
                        targetSrcPath = fs.getPath(basePath.toString(), "target-src")
                        targetClassesPath = fs.getPath(basePath.toString(), "target-classes")
                        
                    } else {
                    
					    fs = Jimfs.newFileSystem(Configuration.unix());
                        targetSrcPath = fs.getPath("/target-src")
                        targetClassesPath = fs.getPath("/target-classes")//new
                        
                    }
                    
                    
					fis = new FileInputStream(owlF)

					Files.createDirectories(targetSrcPath)

					Files.createDirectories(targetClassesPath)

					DomainGenerator gen = VitalSigns.get().generateDomainClasses(fis, targetSrcPath, DomainGenerator.GROOVY, _package, skipDsld);
                    println "compiling source classes"
					gen.compileSource(targetClassesPath)
                    println "generating jar"
					gen.generateJar(targetClassesPath, targetJar.toPath())
                    
				} finally {
					IOUtils.closeQuietly(fis)
                    if(tempLocation == null) {
                        IOUtils.closeQuietly(fs)//new
                    }
				}
				
				
				
				
				if(gitAvailable && targetJar.getParentFile().getCanonicalFile().equals(domainJarDir.getCanonicalFile())) {
					
					//normal domain jar case, add to git
					
					runProcess(vitalHome, ['git', 'add', targetJar.absolutePath])
					
				}
				
                println "DONE"
                
			} else if(_target == 'json') {
			
				println "Generating json schema..."


				File domainJsonDir = new File(vitalHome, 'domain-json-schema')
				File domainJsonArchiveDir = new File(domainJsonDir, 'archive')
				
				domainJsonDir.mkdir()
				domainJsonArchiveDir.mkdir()
				
				File currentVersion = new File(domainJsonDir, "${app}-${ont_majorVersion}.${ont_minorVersion}.${ont_patchVersion}.js");
				
				String js = options.js ? options.js : null;
				
				if(!js && currentVersion.exists()) {
					
					println "Current json schema exists - ${currentVersion.absolutePath} - checking if ontology has changed"
					
					if(!override) {
						println "Current json schema exists - use --override option"
						return
					}
						
				}
				
				//not increasing...
				int newPatchVersion = ont_patchVersion + 0//1
				
				File targetSchema = null;
	
				
				if(js) {
					targetSchema = new File(js);
					println "Custom json schema location: ${targetSchema.absolutePath}"
				} else {
					targetSchema = new File(domainJsonDir, "${app}-${file_majorVersion}.${file_minorVersion}.${file_patchVersion}.js");
					println "Creating json schema at default location: ${targetSchema.absolutePath}"
				}
				
				Matcher schemaMatcherX = JsonSchemaFileInfo.domain_version.matcher(targetSchema.name)
				
				if( ! schemaMatcherX.matches() ) {
					error("Target json schema file name does not match pattern ${JsonSchemaFileInfo.domain_version.pattern()} - ${targetSchema.absolutePath}")
				}
				
				JsonSchemaFileInfo schemaMatcher = JsonSchemaFileInfo.fromString(targetSchema.name)
				
				Integer target_majorVersion = schemaMatcher.major
				Integer target_minorVersion = schemaMatcher.minor
				Integer target_patchVersion = schemaMatcher.patch
				
	
				if(target_majorVersion != ont_majorVersion || target_minorVersion != ont_minorVersion || target_patchVersion != newPatchVersion) {
					error("Target jar version must be equal to ${ont_majorVersion}.${ont_minorVersion}.${newPatchVersion}, current: ${target_majorVersion}.${target_minorVersion}.${target_patchVersion}")
				}
				
				BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(targetSchema))
				FileInputStream fis = null 
				try {
					
					fis = new FileInputStream(owlF)
					
					JSONSchemaGenerator gen = VitalSigns.get().createJSONSchemaGenerator(fis)
					
					gen.generateSchema()
					
					gen.writeSchemaToOutputStream(false, targetSchema.name, os)
					
				} finally {
					IOUtils.closeQuietly(fis)
					IOUtils.closeQuietly(os)
				}
				
				if(gitAvailable && targetSchema.getParentFile().getCanonicalFile().equals(domainJsonDir.getCanonicalFile())) {
					
					//normal domain jar case, add to git
					
					runProcess(vitalHome, ['git', 'add', targetSchema.absolutePath])
					
				}
				
				println "DONE"
				
			}
			
		}
		
	}

	static void error(String m) {
		System.err.println(m);
		System.exit(-1);
	}
	
	static List<Pattern> readPatterns(File vitalHome) {
		
		List<Pattern> patterns = []
		
		//parse the config file without 
		File cfgFile = new File(vitalHome, 'vital-config/vitalsigns/vitalsigns.config');
		
		if(cfgFile.exists()) {
			
			VitalSignsConfig cfg = VitalSignsConfig.fromTypesafeConfig(cfgFile)
			
			for(String rule : cfg.ignoreFilesPatterns) {
				rule = rule.trim()
				if(!rule) continue
				if(rule.startsWith('#')) continue
				patterns.add(Pattern.compile(rule, Pattern.CASE_INSENSITIVE))
			}
			
		} 
		
		return patterns
		
	}
	
	static List<List<String>> handleStatus(File vitalHome, boolean gitAvailable, boolean verbose, boolean validateOWL) {
		
		List<String> errors = []
		
		List<String> warnings = []
		
		//check if 
		
		List<Pattern> patterns = readPatterns(vitalHome)
		

			
		//println "Patterns compiled: ${patterns.size()}"
		
		
		//first check domain ontology and directory
		
		File vitalJarDir = new File(vitalHome, 'vital-domain')
		File vitalOntologyDir = new File(vitalHome, 'vital-ontology')
		File vitalJsonDir = new File(vitalHome, 'vital-json-schema')
		
		File vitalJarArchiveDir = null;
		File vitalOntologyArchiveDir = null;
		File vitalJsonArchiveDir = null;
		
		File[] vitalJarDirFiles = new File[0]
		File[] vitalOntologyDirFiles = new File[0]
		File[] vitalJsonDirFiles = new File[0]
		
		if(!vitalJarDir.exists()) {
			errors.add("SEVERE: vital-domain directory not found in \$VITAL_HOME: ${vitalJarDir.absolutePath}")
		} else if(!vitalJarDir.isDirectory()) {
			errors.add("SEVERE: vital-domain path is not a directory: ${vitalJarDir.absolutePath}")
		} else {
			vitalJarDirFiles = vitalJarDir.listFiles()
		}
		
		if(!vitalOntologyDir.exists()) {
			errors.add("SEVERE: vital-ontology directory not found in \$VITAL_HOME: ${vitalOntologyDir.absolutePath}")
		} else if(!vitalOntologyDir.isDirectory()) {
			errors.add("SEVERE: vital-ontology path is not a directory: ${vitalOntologyDir.absolutePath}")
		} else {
			vitalOntologyDirFiles = vitalOntologyDir.listFiles()
		} 
		
		
		if(!vitalJsonDir.exists()) {
			errors.add("SEVERE: vital-json-schema directory not found in \$VITAL_HOME: ${vitalJsonDir.absolutePath}")
		} else if(!vitalJsonDir.isDirectory()) {
			errors.add("SEVERE: vital-json-schema path is not a directory: ${vitalJsonDir.absolutePath}")
		} else {
			vitalJsonDirFiles = vitalJsonDir.listFiles()
		}
		
		
		DomainOntology vitalCore = null;
		DomainOntology vitalDomain = null;
		DomainOntology vitalSuperAdmin = null;
		
		for(File file : vitalOntologyDirFiles) {
			
			if(file.isFile()) {
				boolean ignored = false;
				for(Pattern pattern : patterns) {
					if(pattern.matcher(file.name)) {
						if(verbose) {
							println "Ignoring file: ${file.absolutePath} - matches rule: ${pattern.pattern()}"
						}
						ignored = true
						break
					}
				}
				if(ignored) continue
			}
			
			if(file.isDirectory()) {
			
				if(file.name == 'archive') {
					vitalOntologyArchiveDir = file
				} else {
					errors.add("Only archive directory allowed in vital ontology directory, not ${file.name}")
				}
			
				continue
					
			}
			
			Matcher coreMatcher = vitalCoreOwlFilePattern.matcher(file.name);
			
			Matcher domainMatcher = vitalOwlFilePattern.matcher(file.name);
			
			Matcher superAdminMatcher = vitalSuperAdminOwlFilePattern.matcher(file.name);
			
			if(coreMatcher.matches()) {
			
				try {
					
					String app = coreMatcher.group(1)
					Integer majorV = Integer.parseInt(coreMatcher.group(2))
					Integer minorV = Integer.parseInt(coreMatcher.group(3))
					Integer patchV = Integer.parseInt(coreMatcher.group(4))
					
					DomainOntology ont = readOntologyFile(file)
					
					if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
						throw new Exception("Vital core ontology and file version mismatch: ${ont.toVersionString()} vs. ${majorV}.${minorV}.${patchV} - ${file.absolutePath}")
					}
					
					if(vitalCore != null) throw new Exception("More than 1 active vital core OWL version found.")
					
					vitalCore = ont
					
					if(vitalCore.uri != VitalCoreOntology.ONTOLOGY_IRI) {
						throw new Exception("Vital domain URI is invalid: ${vitalDomain.uri}, expected: ${VitalCoreOntology.ONTOLOGY_IRI}")
					}
					
				} catch(Exception e) {
				
					errors.add(e.localizedMessage)
				}
			
			} else if(domainMatcher.matches()){
			
				try {
					
					String app = domainMatcher.group(1)
					Integer majorV = Integer.parseInt(domainMatcher.group(2))
					Integer minorV = Integer.parseInt(domainMatcher.group(3))
					Integer patchV = Integer.parseInt(domainMatcher.group(4))
					
					DomainOntology ont = readOntologyFile(file)
					
					if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
						throw new Exception("Vital ontology and file version mismatch: ${ont.toVersionString()} vs. ${majorV}.${minorV}.${patchV} - ${file.absolutePath}")
					}
					
					if(vitalDomain != null) throw new Exception("More than 1 active vital domain OWL version found.")
					
					vitalDomain = ont
					
					if(vitalDomain.uri != 'http://vital.ai/ontology/vital') {
						throw new Exception("Vital domain URI is invalid: ${vitalDomain.uri}, expected: http://vital.ai/ontology/vital")
					}
					
				} catch(Exception e) {
				
					errors.add(e.localizedMessage)
				}
			
			} else if(superAdminMatcher.matches()) {

				try {
					
					String app = superAdminMatcher.group(1)
					Integer majorV = Integer.parseInt(superAdminMatcher.group(2))
					Integer minorV = Integer.parseInt(superAdminMatcher.group(3))
					Integer patchV = Integer.parseInt(superAdminMatcher.group(4))
					
					DomainOntology ont = readOntologyFile(file)
					
					if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
						throw new Exception("SuperAdmin ontology and file version mismatch: ${ont.toVersionString()} vs. ${majorV}.${minorV}.${patchV} - ${file.absolutePath}")
					}
					
					if(vitalSuperAdmin != null) throw new Exception("More than 1 active vital superadmin OWL version found.")
					
					vitalSuperAdmin = ont
					
					if(vitalSuperAdmin.uri != 'http://vital.ai/ontology/vital-superadmin') {
						throw new Exception("Vital superadmin URI is invalid: ${vitalSuperAdmin.uri}, expected: http://vital.ai/ontology/vita-superadmin")
					}
					
				} catch(Exception e) {
				
					errors.add(e.localizedMessage)
				}
						
			} else {
				errors.add("Unexpected file in vital-domain directory: ${file.absolutePath}");
			}
			
		}
		
		if(vitalDomain == null) {
			errors.add("SEVERE: vital domain OWL not found in ${vitalOntologyDir.absolutePath}")
		}
		
		List<DomainOntology> archivedVitalDomainOntologies = []
		
		if( vitalDomain != null && vitalOntologyArchiveDir != null ) {

			for(File afile : vitalOntologyArchiveDir.listFiles()) {
			
				if(afile.isDirectory()) {
					errors.add("No directory allowed in vital domain ontology archive: ${afile.absolutePath}")
					continue
				}
				
				boolean ignored = false;
				for(Pattern pattern : patterns) {
					if(pattern.matcher(afile.name)) {
						if(verbose) {
							println "Ignoring file: ${afile.absolutePath} - matches rule: ${pattern.pattern()}"
						}
						ignored = true
						break
					}
				}
				if(ignored) continue
				
				try {
					
					

					Matcher matcher = vitalOwlFilePattern.matcher(afile.name)
					
					if(!matcher.matches()) throw new Exception("Vital OWL archived file name does not match pattern: ${vitalOwlFilePattern} - ${afile.absolutePath}")
					
					String app = matcher.group(1)
					Integer majorV = Integer.parseInt(matcher.group(2))
					Integer minorV = Integer.parseInt(matcher.group(3))
					Integer patchV = Integer.parseInt(matcher.group(4))
					
					DomainOntology ont = readOntologyFile(afile)
					
					if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
						throw new Exception("Archived ontology and file version mismatch: ${ont.toVersionString()} vs. ${majorV}.${minorV}.${patchV} - ${afile.absolutePath}")
					}
					
					if(vitalDomain.uri != ont.uri) {
						throw new Exception("Archived version vital ontology URI invalid: ${ont.uri},  expected: ${vitalDomain.uri}")
					}
					
					archivedVitalDomainOntologies.add(ont)
					
					int c = vitalDomain.compareTo(ont)
					
					if(c == 0) {
						String m = "Same version of current and archived vital owl file detected: ${afile.absolutePath}"
						warnings.add(m)
//						throw new Exception(m)
					}
					if(c < 0) {
						String m = "Current vital owl version is lower than archived: ${vitalDomain.toVersionString()} vs. ${ont.toVersionString()}, file: ${afile.absolutePath}"
						warnings.add(m)
//						throw new Exception(m)
					}
					
				} catch(Exception e) {
					errors.add(e.localizedMessage)
				}
				
					
			}
			
		} else {
		
			if(verbose) {
				println "WARN: vital domain ontology archive directory does not exist"
			}
		
		} 
		
		
		
		boolean vitalDomainJarFound = false
		
		boolean superAdminJarFound = false
		
		for(File file : vitalJarDirFiles) {
			
			if(file.isDirectory()) {
				
				if(file.name == 'archive') {
					vitalJarArchiveDir = file
				} else {
					errors.add("Only archive directory allowed in vital domain directory, not ${file.name}")
				}
	
				continue
			}
								
			boolean ignored = false;
			for(Pattern pattern : patterns) {
				if(pattern.matcher(file.name)) {
					if(verbose) {
						println "Ignoring file: ${file.absolutePath} - matches rule: ${pattern.pattern()}"
					}
					ignored = true
					break
				}
			}
			
			if(ignored) continue

			Matcher domainMatcher = vitalJarFilePattern.matcher(file.name)
			
			Matcher superAdminMatcher = vitalSuperAdminJarFilePattern.matcher(file.name)
						
			if(domainMatcher.matches()) {
			
				try {
					
					if(vitalDomainJarFound) {
						throw new Exception("More than 1 active vital domain jar")
					}	
					
					String app = domainMatcher.group(1)
					Integer majorV = Integer.parseInt(domainMatcher.group(2))
					Integer minorV = Integer.parseInt(domainMatcher.group(3))
					Integer patchV = Integer.parseInt(domainMatcher.group(4))
					
					DomainOntology ont = vitalDomain
					
					if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
						throw new Exception("Current vital ontology and jar file version mismatch: ${ont.toVersionString()} vs. ${majorV}.${minorV}.${patchV} - ${file.absolutePath}")
					}
					vitalDomainJarFound = true
					
						
				} catch(Exception e) {
					
					errors.add(e.localizedMessage)
				}
				
			} else if(superAdminMatcher.matches()) {
			
				try {
				
					if(superAdminJarFound) {
						throw new Exception("More than 1 active superadmin jar")
					}
					
					if(vitalSuperAdmin == null) {
						throw new Exception("No ontology for superadmin jar found")
					}
					
					String app = superAdminMatcher.group(1)
					Integer majorV = Integer.parseInt(superAdminMatcher.group(2))
					Integer minorV = Integer.parseInt(superAdminMatcher.group(3))
					Integer patchV = Integer.parseInt(superAdminMatcher.group(4))
					
					DomainOntology ont = vitalSuperAdmin
					
					if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
						throw new Exception("Current vital superadmin ontology and jar file version mismatch: ${ont.toVersionString()} vs. ${majorV}.${minorV}.${patchV} - ${file.absolutePath}")
					}
					
					superAdminJarFound = true
					
						
				} catch(Exception e) {
					
					errors.add(e.localizedMessage)
				}
			
			} else {
				errors.add("Unexpected file in vital-domain directory: ${file.absolutePath}")
			}
				
		}
		if(!vitalDomainJarFound) {
			// errors.add("SEVERE: No active vital domain jar found!")
		}
		
		if(vitalSuperAdmin != null && !superAdminJarFound) {
			warnings.add("SuperAdmin ontology exists, but no corresponding vital superadmin jar")
		}
		
		if(vitalJarArchiveDir != null) {
			
			
			for(File afile : vitalJarArchiveDir.listFiles()) {
				
				if(afile.isDirectory()) {
					errors.add("No directory allowed in domain jar archive: ${afile.absolutePath}")
					continue
				}
				
				if(afile.isFile()) {
					boolean ignored = false;
					for(Pattern pattern : patterns) {
						if(pattern.matcher(afile.name)) {
							if(verbose) {
								println "Ignoring file: ${afile.absolutePath} - matches rule: ${pattern.pattern()}"
							}
							ignored = true
							break
						}
					}
					if(ignored) continue
				}
				
				try {
					
					Matcher matcher = vitalJarFilePattern.matcher(afile.name)
					
					if(!matcher.matches()) throw new Exception("archived vital domain jar file name does not match pattern: ${vitalJarFilePattern} - ${afile.absolutePath}")
					
					String app = matcher.group(1)
					Integer majorV = Integer.parseInt(matcher.group(2))
					Integer minorV = Integer.parseInt(matcher.group(3))
					Integer patchV = Integer.parseInt(matcher.group(4))
					
					DomainOntology archivedV = new DomainOntology("x", "${majorV}.${minorV}.${patchV}")

					DomainOntology currentV = vitalDomain
					
					int c = currentV.compareTo(archivedV)
					
					if( c == 0) {
						String m = "Current vital domain jar and archived jar with same version found: ${afile.absolutePath}"
						warnings.add(m)
//						throw new Exception(m)
					}
					
					if(c < 0) {
						String m = "Current vital domain jar version is lower than one of archived version: ${currentV.toVersionString()} vs. ${archivedV.toVersionString()}, file: ${afile.absolutePath}"
						warnings.add(m)
//						throw new Exception(m);
					}
					
					//look for corresponding archived owl version
					List<DomainOntology> archivedOWLs = archivedVitalDomainOntologies
					
					DomainOntology archivedOWLMatch = null;
					
					for(DomainOntology archivedOWL : archivedOWLs) {
						
						if(archivedOWL.toVersionString().equals(archivedV.toVersionString())) {
							archivedOWLMatch = archivedOWL
							break
						}
						
					}
					
					if(archivedOWLMatch == null) {
						throw new Exception("No matching archived vital domain OWL file found for archived jar: ${afile.absolutePath}")
					}
					
				} catch(Exception e) {
					errors.add(e.localizedMessage)
				}
				
				
			}
			
		} else {
		
			if(verbose) {
				println "WARN \$VITAL_HOME/vital-domain/archive directory does not exist"
			}
			
		}
		
		
		
		
		//JSON
		
		boolean vitalCoreJsonFound = false
		boolean vitalDomainJsonFound = false
		boolean vitalSuperAdminJsonFound = false
		
		for(File file : vitalJsonDirFiles) {
			
			if(file.isDirectory()) {
				
				if(file.name == 'archive') {
					vitalJsonArchiveDir = file
				} else {
					errors.add("Only archive directory allowed in vital json schemas directory, not ${file.name}")
				}
					
			} else {
			
				boolean ignored = false;
				for(Pattern pattern : patterns) {
					if(pattern.matcher(file.name)) {
						if(verbose) {
							println "Ignoring file: ${file.absolutePath} - matches rule: ${pattern.pattern()}"
						}
						ignored = true
						break
					}
				}
				if(ignored) continue
			
				try {
					
					Matcher matcher = vitalJsonFilePattern.matcher(file.name);
					
					Matcher coreMatcher = vitalCoreJsonFilePattern.matcher(file.name);
					
					Matcher superAdminMatcher = vitalSuperAdminJsonFilePattern.matcher(file.name);

					if(matcher.matches()) {
						
						if(vitalDomainJsonFound) { throw new Exception("More than 1 active vital domain schema json file: ${file.absolutePath}") }
						
						String app = matcher.group(1)
						Integer majorV = Integer.parseInt(matcher.group(2))
						Integer minorV = Integer.parseInt(matcher.group(3))
						Integer patchV = Integer.parseInt(matcher.group(4))
						
						DomainOntology ont = vitalDomain
						
						if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
							throw new Exception("Current vital ontology and json schema file version mismatch: ${ont.toVersionString()} vs. ${majorV}.${minorV}.${patchV} - ${file.absolutePath}")
						}
						vitalDomainJsonFound = true
						
					} else if(coreMatcher.matches()) {
					
						if(vitalCoreJsonFound) { throw new Exception("More than 1 active vital core schema json file: ${file.absolutePath}") }
					
						String app = coreMatcher.group(1)
						Integer majorV = Integer.parseInt(coreMatcher.group(2))
						Integer minorV = Integer.parseInt(coreMatcher.group(3))
						Integer patchV = Integer.parseInt(coreMatcher.group(4))
						
						DomainOntology ont = vitalCore
						
						if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
							throw new Exception("Current vital core ontology and json schema file version mismatch: ${ont.toVersionString()} vs. ${majorV}.${minorV}.${patchV} - ${file.absolutePath}")
						}
						vitalCoreJsonFound = true
						
					} else if(superAdminMatcher.matches()) {
					
						if(vitalSuperAdminJsonFound) { throw new Exception("More than 1 active vital superadmin schema json file: ${file.absolutePath}") }
						
						String app = superAdminMatcher.group(1)
						Integer majorV = Integer.parseInt(superAdminMatcher.group(2))
						Integer minorV = Integer.parseInt(superAdminMatcher.group(3))
						Integer patchV = Integer.parseInt(superAdminMatcher.group(4))
						
						vitalSuperAdminJsonFound = true
					
					} else {
						throw new Exception("Vital json schema file name does not match pattern: ${vitalJsonFilePattern.pattern()} or ${vitalCoreJsonFilePattern.pattern()}- ${file.absolutePath}")
					}
						
				} catch(Exception e) {
					
					errors.add(e.localizedMessage)
				}
				
			}
			
		}
		if(!vitalDomainJsonFound) {
			// errors.add("SEVERE: No active vital domain json schema found!")
		}
		if(!vitalCoreJsonFound) {
			// errors.add("SEVERE: No active vital core json schema found!")
		}
		
		if(vitalJsonArchiveDir != null) {
			
			
			for(File afile : vitalJsonArchiveDir.listFiles()) {
				
				if(afile.isDirectory()) {
					errors.add("No directory allowed in vital json schema archive: ${afile.absolutePath}")
					continue
				}
				
				if(afile.isFile()) {
					boolean ignored = false;
					for(Pattern pattern : patterns) {
						if(pattern.matcher(afile.name)) {
							if(verbose) {
								println "Ignoring file: ${afile.absolutePath} - matches rule: ${pattern.pattern()}"
							}
							ignored = true
							break
						}
					}
					if(ignored) continue
				}
				
				try {
					
					Matcher matcher = vitalJsonFilePattern.matcher(afile.name)
					
					Matcher coreMatcher = vitalCoreJsonFilePattern.matcher(afile.name)
					
					boolean core = false
					
					if(matcher.matches()) {
						
					} else if(coreMatcher.matches()) {
					
						core = true
					
					} else {
						throw new Exception("archived vital json schema file name does not match pattern: ${vitalJsonFilePattern.pattern()} or ${vitalCoreJsonFilePattern.pattern()}- ${afile.absolutePath}")
					}
					
					String label = core ? 'core' : 'domain'
					
					String app = matcher.group(1)
					Integer majorV = Integer.parseInt(matcher.group(2))
					Integer minorV = Integer.parseInt(matcher.group(3))
					Integer patchV = Integer.parseInt(matcher.group(4))
					
					DomainOntology archivedV = new DomainOntology("x", "${majorV}.${minorV}.${patchV}")

					DomainOntology currentV = core ? vitalCore : vitalDomain
					
					int c = currentV.compareTo(archivedV)
					
					if( c == 0) {
						String m = "Current vital ${label} json schema and archived json schema with same version found: ${afile.absolutePath}"
						warnings.add(m)
//						throw new Exception(m)
					}
					
					if(c < 0) {
						String m = "Current vital ${label} json schema version is lower than one of archived version: ${currentV.toVersionString()} vs. ${archivedV.toVersionString()}, file: ${afile.absolutePath}"
						warnings.add(m)
//						throw new Exception(m);
					}
					
					//look for corresponding archived owl version
					List<DomainOntology> archivedOWLs = archivedVitalDomainOntologies
					
					DomainOntology archivedOWLMatch = null;
					
					for(DomainOntology archivedOWL : archivedOWLs) {
						
						if(archivedOWL.toVersionString().equals(archivedV.toVersionString())) {
							archivedOWLMatch = archivedOWL
							break
						}
						
					}
					
					if(archivedOWLMatch == null) {
						throw new Exception("No matching archived vital ${label} OWL file found for archived json schema: ${afile.absolutePath}")
					}
					
				} catch(Exception e) {
					errors.add(e.localizedMessage)
				}
				
				
			}
			
		} else {
		
			if(verbose) {
				println "WARN \$VITAL_HOME/vital-json-schema/archive directory does not exist"
			}
			
		}
		
		
		
		validateDomainOntologiesAndJars(vitalHome, patterns, errors, warnings, verbose, validateOWL)
		
		if(gitAvailable) {
			
			try {
				
				String gitUserEmail = GitUtils.getGitUserEmail(vitalHome)
				
			} catch(Exception e) {
				errors.add("Error when checking 'git config user.email' : ${e.getLocalizedMessage()}")
			}
			
			
			
		}
		
		return [errors, warnings];
		
	}
	
	
	public static void validateDomainOntologiesAndJars(File vitalHome, List<Pattern> patterns, List<String> errors, List<String> warnings, boolean printWarnings, boolean validateOWL) {
		
		File domainOntologyDir = new File(vitalHome, 'domain-ontology')
		
		//use them for comparison
		Map<String, OwlFileInfo> app2DomainOntologyOWL = [:]

		Map<String, String> app2uri = [:]
		
		Map<String, List<OwlFileInfo>> app2ArchivedVersions = [:]
		
		File owlArchiveDir = null
		
		File[] domainOntologyDirFiles = new File[0]
		
		File[] domainJarDirFiles = new File[0]
		
		File[] domainJsonDirFiles = new File[0]
		
		if( ! domainOntologyDir.exists()) {
			errors.add("SEVERE: domain-ontology directory not found in \$VITAL_HOME: ${domainOntologyDir.absolutePath}")
		} else if(!domainOntologyDir.isDirectory()) {
			errors.add("SEVERE: domain-ontology path is not a directory: ${domainOntologyDir.absolutePath}")
		} else {
			domainOntologyDirFiles = domainOntologyDir.listFiles()
		}
		
		
		File domainJarDir = new File(vitalHome, 'domain-groovy-jar')
		File domainJarArchiveDir = null;
		
		File domainJsonDir = new File(vitalHome, 'domain-json-schema')
		File domainJsonArchiveDir = null
		
		if( ! domainJarDir.exists()) {
			errors.add("SEVERE: domain-groovy-jar directory not found in \$VITAL_HOME: ${domainJarDir.absolutePath}")
		} else if(!domainJarDir.isDirectory()) {
			errors.add("SEVERE: domain-groovy-jar path is not a directory: ${domainJarDir.absolutePath}")
		} else {
			domainJarDirFiles = domainJarDir.listFiles()
		}
		
		
		if( ! domainJsonDir.exists()) {
			errors.add("SEVERE: domain-json-schema directory not found in \$VITAL_HOME: ${domainJsonDir.absolutePath}")
		} else if(!domainJsonDir.isDirectory()) {
			errors.add("SEVERE: domain=json path is not a directory: ${domainJsonDir.absolutePath}")
		} else {
			domainJsonDirFiles = domainJsonDir.listFiles()
		}
		
		for(File file : domainOntologyDirFiles) {
			
			if(file.isFile()) {
				boolean ignored = false;
				for(Pattern pattern : patterns) {
					if(pattern.matcher(file.name)) {
						if(printWarnings) {
							println "Ignoring file: ${file.absolutePath} - matches rule: ${pattern.pattern()}"
						}
						ignored = true
						break
					}
				}
				if(ignored) continue
			}
			
			if(file.isDirectory()) {
				
				if(file.name == 'archive') {
					owlArchiveDir = file
				} else {
					errors.add("Only archive directory allowed in domain ontology directory, not ${file.name}")
				}
				
			} else {
			
				try {
					
					Matcher matcherX = OwlFileInfo.domain_version.matcher(file.name);

					if(!matcherX.matches()) throw new Exception("OWL file name does not match pattern: ${OwlFileInfo.domain_version.pattern()} - ${file.absolutePath}")
					
					OwlFileInfo matcher = OwlFileInfo.fromString(file.name)
					
					String app = matcher.domain
					Integer majorV = matcher.major
					Integer minorV = matcher.minor
					Integer patchV = matcher.patch
					
					if(validateOWL) {
						
						DomainOntology ont = readOntologyFile(file)
								
						if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
							throw new Exception("Ontology and file version mismatch: ${ont.toVersionString()} vs. ${majorV}.${minorV}.${patchV} - ${file.absolutePath}")
						}
						
						app2uri.put(app, ont.uri)
						
						OntologyNormalizer norm1 = new OntologyNormalizer(file, CommentPattern.parseFromParam("ICCCCCCCCCC"))
						
						String classes = norm1.checkOtherOntologiesClasses()
						
						if(classes) throw new Exception(file.absolutePath + ": " + classes);
						
						if(ont.defaultPackage != null && ont.defaultPackage.startsWith('ai.vital.')) {
							throw new Exception("Ontology ${ont.uri} default package mustn't start with ai.vital.")
						}
						
					}
					
					
					app2ArchivedVersions.put(app, []);
					
					if(app2DomainOntologyOWL.containsKey(app)) {
						OwlFileInfo other = app2DomainOntologyOWL.get(app)
						throw new Exception("More than one owl file for application: ${app} - ${matcher.toVersionNumberString()} ${other.toVersionNumberString()}")
					}
					
					app2DomainOntologyOWL.put(app, matcher)
					
					
					
				} catch(Exception e) {
				
					errors.add(file.absolutePath + ' - ' +  e.localizedMessage)
				}
			
			}
			
		}
		
		if(owlArchiveDir != null) {
			
			for(File afile : owlArchiveDir.listFiles()) {
				
				if(afile.isDirectory()) {
					errors.add("No directory allowed in domain ontology archive: ${afile.absolutePath}")
					continue
				}
				
				if(afile.isFile()) {
					boolean ignored = false;
					for(Pattern pattern : patterns) {
						if(pattern.matcher(afile.name)) {
							if(printWarnings) {
								println "Ignoring file: ${afile.absolutePath} - matches rule: ${pattern.pattern()}"
							}
							ignored = true
							break
						}
					}
					if(ignored) continue
				}
				
				try {
					
					//check if it's downgraded version
					OwlFileInfo i = null
					try {
						i = OwlFileInfo.fromString(afile.name)
					} catch(Exception e) {}
					
					if(i == null) throw new Exception("OWL archived file name does not match general archived owl pattern: ${OwlFileInfo.archivePattern} - ${afile.absolutePath}")
					
					String app = i.domain
					Integer majorV = i.major
					Integer minorV = i.minor
					Integer patchV = i.patch
					
					if(validateOWL) {
						
						DomainOntology ont = readOntologyFile(afile)
						
						if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
							throw new Exception("Archived ontology and file version mismatch: ${ont.toVersionString()} vs. ${majorV}.${minorV}.${patchV} - ${afile.absolutePath}")
						}
					
						
						String currentVersionURI = app2uri.get(app)
							
						if(currentVersionURI != ont.uri) {
							throw new Exception("Archived version ontology URI does not match current version URI, app: ${app}, ${currentVersionURI} vs. ${ont.uri}, file: ${afile.absolutePath}")
						}
						
						
						OntologyNormalizer norm1 = new OntologyNormalizer(afile, CommentPattern.parseFromParam("ICCCCCCCCCC"))
						
						String classes = norm1.checkOtherOntologiesClasses()
						
						if(classes) throw new Exception(afile.absolutePath + ": " + classes);
						
					}
					
					OwlFileInfo currentVersion = app2DomainOntologyOWL.get(app)
					
					if(currentVersion == null) {
						throw new Exception("No owl file for archived owl file: ${afile.absolutePath}")
					}
					
					
					List<OwlFileInfo> archivedOntologies = app2ArchivedVersions.get(app)
					archivedOntologies.add(i)
					
					//don't compare down versions!
					if(i.down == null) {
						
						int c = currentVersion.compareTo(i)
								
						if(c == 0) {
							String m = "Same version of current and archived owl file detected: ${afile.absolutePath}"
							warnings.add(m)
//							throw new Exception(m)
						}
						
						if(c < 0) {
							String m = "Current owl version is lower than archived: ${currentVersion.toVersionNumberString()} vs. ${i.toVersionNumberString()}, file: ${afile.absolutePath}"
							warnings.add(m)
//							throw new Exception(m)
						}
					}
					
				} catch(Exception e) {
					errors.add(afile.absolutePath + ' - ' + e.localizedMessage)
				}
				
			}
			
		} else {
		
			if(printWarnings) {
				println "WARN: \$VITAL_HOME/domain-ontology/archive directory does not exist"
			}
			
		}
		
		Map<String, File> activeDomainJars = new HashMap<String, File>()
		
		for(File file : domainJarDirFiles) {
			
			if(file.isDirectory()) {
				
				if(file.name == 'archive') {
					domainJarArchiveDir = file
				} else {
					errors.add("Only archive directory allowed in domain jar directory, not ${file.name}")
				}
				
			} else {
			
				boolean ignored = false;
				for(Pattern pattern : patterns) {
					if(pattern.matcher(file.name)) {
						if(printWarnings) {
							println "Ignoring file: ${file.absolutePath} - matches rule: ${pattern.pattern()}"
						}
						ignored = true
						break
					}
				}
				if(ignored) continue
			
				try {
					
					Matcher matcherX = JarFileInfo.domain_version.matcher(file.name);

					if(!matcherX.matches()) throw new Exception("Jar file name does not match pattern: ${JarFileInfo.domain_version.pattern()} - ${file.absolutePath}")
					
					JarFileInfo i = JarFileInfo.fromString(file.name)
					
					String app = i.domain
					Integer majorV = i.major
					Integer minorV = i.minor
					Integer patchV = i.patch
					
					File f = activeDomainJars.get(app);
					if(f != null) {
						throw new Exception("More than 1 active domain jar for app: ${app}, file1: ${file.absolutePath}, file2: ${f.absolutePath}")
					}
					
					activeDomainJars.put(app, file)
					
					OwlFileInfo ont = app2DomainOntologyOWL.get(app)
					
					if(ont == null) {
						throw new Exception("OWL file for active domain jar not found: ${file.absolutePath}")
					}
					
					if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
						throw new Exception("Current ontology and jar file version mismatch: ${ont.toVersionNumberString()} vs. ${majorV}.${minorV}.${patchV} - ${file.absolutePath}")
					}
					
				} catch(Exception e) {
					errors.add(e.localizedMessage)
				}
			
			}
			
		}
		
		if(domainJarArchiveDir != null) {
			
			for(File afile : domainJarArchiveDir.listFiles()) {
				
				if(afile.isDirectory()) {
					errors.add("No directory allowed in domain jar archive: ${afile.absolutePath}")
					continue
				}
				
				if(afile.isFile()) {
					boolean ignored = false;
					for(Pattern pattern : patterns) {
						if(pattern.matcher(afile.name)) {
							if(printWarnings) {
								println "Ignoring file: ${afile.absolutePath} - matches rule: ${pattern.pattern()}"
							}
							ignored = true
							break
						}
					}
					if(ignored) continue
				}
				
				try {
					
					
					JarFileInfo i = null
					
					try {
						i = JarFileInfo.fromString(afile.name)
					} catch(Exception e) {}
					
					if(i == null) throw new Exception("archived jar file name does not match pattern: ${JarFileInfo.archivePattern} - ${afile.absolutePath}")
					
					String app = i.domain
					
					File activeJarFile = activeDomainJars.get(app)
					if(!activeJarFile) {
						warnings.add("no active jar file found for archived: ${afile.absolutePath}");
						continue
					}

					JarFileInfo archivedV = i

					JarFileInfo currentInfo = JarFileInfo.fromString(activeJarFile.name)
					
					JarFileInfo currentV = currentInfo
					
					if(currentV.down == null) {
						
						int c = currentV.compareTo(archivedV)
								
						if( c == 0) {
							String m = "Current domain jar and archived jar with same version found: ${afile.absolutePath} vs ${activeJarFile.absolutePath}"
							warnings.add(m)
//							throw new Exception(m)
						}
						
						if(c < 0) {
							String m = "Current domain jar version is lower than one of archived version: ${currentV.toVersionNumberString()} vs. ${archivedV.toVersionNumberString()}, file: ${afile.absolutePath}"
							warnings.add(m)
//							throw new Exception(m);
						}
						
					}
					
					
					//look for corresponding archived owl version
					List<OwlFileInfo> archivedOWLs = app2ArchivedVersions.get(app)
					if( archivedOWLs == null ) archivedOWLs = []
					
					OwlFileInfo archivedOWLMatch = null;
					
					for(OwlFileInfo archivedOWL : archivedOWLs) {
						
						if(archivedOWL.toVersionNumberString().equals(archivedV.toVersionNumberString())) {
							archivedOWLMatch = archivedOWL
							break
						}
						
					}
					
					if(archivedOWLMatch == null) {
						throw new Exception("No matching archived OWL file found for archived jar: ${afile.absolutePath}")
					}
					
				} catch(Exception e) {
					errors.add(e.localizedMessage)
				}
				
				
			}
			
		} else {
		
			if(printWarnings) {
				println "WARN \$VITAL_HOME/domain-groovy-jar/archive directory does not exist"
			}
			
		}
		
		
		
		//JSON
		Map<String, File> activeDomainJsons = new HashMap<String, File>()
		
		for(File file : domainJsonDirFiles) {
			
			if(file.isDirectory()) {
				
				if(file.name == 'archive') {
					domainJsonArchiveDir = file
				} else {
					errors.add("Only archive directory allowed in domain json directory, not ${file.name}")
				}
				
			} else {
			
				boolean ignored = false;
				for(Pattern pattern : patterns) {
					if(pattern.matcher(file.name)) {
						if(printWarnings) {
							println "Ignoring file: ${file.absolutePath} - matches rule: ${pattern.pattern()}"
						}
						ignored = true
						break
					}
				}
				if(ignored) continue
			
				try {
					
					Matcher matcherX = JsonSchemaFileInfo.domain_version.matcher(file.name);

					if(!matcherX.matches()) throw new Exception("Json schema file name does not match pattern: ${JsonSchemaFileInfo.domain_version.pattern()} - ${file.absolutePath}")
					
					JsonSchemaFileInfo i = JsonSchemaFileInfo.fromString(file.name)
					
					String app = i.domain
					Integer majorV = i.major
					Integer minorV = i.minor
					Integer patchV = i.patch
					
					File f = activeDomainJsons.get(app);
					if(f != null) {
						throw new Exception("More than 1 active domain json schema for app: ${app}, file1: ${file.absolutePath}, file2: ${f.absolutePath}")
					}
					
					activeDomainJsons.put(app, file)
					
					OwlFileInfo ont = app2DomainOntologyOWL.get(app)
					
					if(ont == null) {
						throw new Exception("OWL file for active domain json schema not found: ${file.absolutePath}")
					}
					
					if(ont.major != majorV || ont.minor != minorV || ont.patch != patchV) {
						throw new Exception("Current ontology and json schema file version mismatch: ${ont.toVersionNumberString()} vs. ${majorV}.${minorV}.${patchV} - ${file.absolutePath}")
					}
					
				} catch(Exception e) {
					errors.add(e.localizedMessage)
				}
			
			}
			
		}
		
		if(domainJsonArchiveDir != null) {
			
			for(File afile : domainJsonArchiveDir.listFiles()) {
				
				if(afile.isDirectory()) {
					errors.add("No directory allowed in domain json schema archive: ${afile.absolutePath}")
					continue
				}
				
				if(afile.isFile()) {
					boolean ignored = false;
					for(Pattern pattern : patterns) {
						if(pattern.matcher(afile.name)) {
							if(printWarnings) {
								println "Ignoring file: ${afile.absolutePath} - matches rule: ${pattern.pattern()}"
							}
							ignored = true
							break
						}
					}
					if(ignored) continue
				}
				
				try {
					
					
					JsonSchemaFileInfo i = null
					
					try {
						i = JsonSchemaFileInfo.fromString(afile.name)
					} catch(Exception e) {}
					
					if(i == null) throw new Exception("archived json schema file name does not match pattern: ${JsonSchemaFileInfo.archivePattern} - ${afile.absolutePath}")
					
					String app = i.domain
					
					File activeJsonFile = activeDomainJsons.get(app)
					if(!activeJsonFile) {
						warnings.add("no active json schema file found for archived: ${afile.absolutePath}");
						continue
					}

					JsonSchemaFileInfo archivedV = i

					JsonSchemaFileInfo currentInfo = JsonSchemaFileInfo.fromString(activeJsonFile.name)
					
					JsonSchemaFileInfo currentV = currentInfo
					
					if(currentV.down == null) {
						
						int c = currentV.compareTo(archivedV)
								
						if( c == 0) {
							String m = "Current domain json schema and archived json schema with same version found: ${afile.absolutePath} vs ${activeJsonFile.absolutePath}"
							warnings.add(m)
//							throw new Exception(m)
						}
						
						if(c < 0) {
							String m = "Current domain json schema version is lower than one of archived version: ${currentV.toVersionNumberString()} vs. ${archivedV.toVersionNumberString()}, file: ${afile.absolutePath}"
							warnings.add(m)
//							throw new Exception(m);
						}
						
					}
					
					
					//look for corresponding archived owl version
					List<OwlFileInfo> archivedOWLs = app2ArchivedVersions.get(app)
					if( archivedOWLs == null ) archivedOWLs = []
					
					OwlFileInfo archivedOWLMatch = null;
					
					for(OwlFileInfo archivedOWL : archivedOWLs) {
						
						if(archivedOWL.toVersionNumberString().equals(archivedV.toVersionNumberString())) {
							archivedOWLMatch = archivedOWL
							break
						}
						
					}
					
					if(archivedOWLMatch == null) {
						throw new Exception("No matching archived OWL file found for archived json schema: ${afile.absolutePath}")
					}
					
				} catch(Exception e) {
					errors.add(e.localizedMessage)
				}
				
			}
			
		} else {
		
			if(printWarnings) {
				println "WARN \$VITAL_HOME/domain-json-schema/archive directory does not exist"
			}
			
		}
		
	}
	
	public static DomainOntology readOntologyFile(File ontFile) throws Exception {
		return readOntologyFile(ontFile, false)
	}
	
	public static DomainOntology readOntologyFile(File ontFile, boolean validateFileVersion) throws Exception {
		
		Model m = ModelFactory.createDefaultModel()
		
		m.read(new FileInputStream(ontFile), null, "RDF/XML")
		
		Resource ontRes = null;
		
		for( ResIterator resIterator = m.listSubjectsWithProperty(com.hp.hpl.jena.vocabulary.RDF.type, com.hp.hpl.jena.vocabulary.OWL.Ontology); resIterator.hasNext(); ) {
			
			if(ontRes != null) throw new Exception("More than 1 ontology found in file: ${ontFile.absolutePath}")

			ontRes = resIterator.nextResource();			
			
		}
		
		if(ontRes == null) throw new Exception("No ontology resource found in file: ${ontFile.absolutePath}")
		
		String ontologyURI = ontRes.getURI()
		
		//if(ontologyURI.contains("_")) error("Ontology IRI cannot contain underscores: ${ontologyURI}, file ${ontFile.absolutePath}")
		
		String versionInfo = RDFUtils.getStringPropertySingleValue(ontRes, com.hp.hpl.jena.vocabulary.OWL.versionInfo);
		
        if(versionInfo == null || versionInfo.isEmpty()) throw new Exception("Ontology owl:versionInfo property is empty or not a string, ontURI: ${ontologyURI}, file: ${ontFile.absolutePath}")
		
        
        String _defaultPackage = RDFUtils.getStringPropertySingleValue(ontRes, VitalCoreOntology.hasDefaultPackage)
        
        if(_defaultPackage != null) {
			if(_defaultPackage.isEmpty()) throw new Exception("Ontology ${ontFile.absolutePath} URI: ${ontologyURI} vital-core:hasDefaultPackage literal must be a non empty string")
		}
        
		if(!versionInfo) throw new Exception("Ontology owl:versionInfo property is not set, ontURI: ${ontologyURI}, file: ${ontFile.absolutePath}")
		
        
		String backVersion = RDFUtils.getStringPropertySingleValue(ontRes, VitalCoreOntology.hasBackwardCompatibilityVersion)

        if(backVersion != null) {        
            
            Matcher backMatcher = DomainOntology.versionPattern.matcher(backVersion)
            if(!backMatcher.matches()) throw new Exception("Ontology vital-core:${VitalCoreOntology.hasBackwardCompatibilityVersion.getLocalName()} annotation does not match pattern: ${DomainOntology.versionPattern} - ${backVersion}")
            
        }
        
		
		
		if(validateFileVersion) {
			
			OwlFileInfo matcher = OwlFileInfo.fromString(ontFile.name)
			
			String fileVersionInfo = "${matcher.major}.${matcher.minor}.${matcher.patch}"
			
			if(versionInfo != fileVersionInfo) throw new Exception("Ontology file version does not match ontology version property - file: ${fileVersionInfo} ontology: ${versionInfo}")
			
		}
		
		DomainOntology ont = new DomainOntology(ontologyURI, versionInfo, backVersion)
		
		ont.defaultPackage = _defaultPackage
		
        ont.preferredImportVersions = RDFUtils.getStringPropertyValues(ontRes, VitalCoreOntology.hasPreferredImportVersion)
        
        m.close()
        
		return ont;
		
	}

	def static boolean gitAvailable(File vitalHome) {
		return gitAvailable(vitalHome, true)	
	}
	
	def static File gitDir(File vitalHome) {
		
		//first check vitalhome
		File gitDir = new File(vitalHome, '.git')
		
		if(!gitDir.exists() || !gitDir.isDirectory()) {
			gitDir = new File(vitalHome.getParentFile(), '.git')
		}
		
		if(gitDir.exists() && gitDir.isDirectory()) {
			
			return gitDir
		}
		
		return null
		
		
	}
	
	def static boolean gitAvailable(File vitalHome, boolean verbose) {
		
		
		File gitDir = gitDir(vitalHome)
		
		if(gitDir == null) {
			if(verbose) {
				println "VITAL_HOME or its parent is not a git repository"
			}
			return false
		}

		if(verbose) {
			println "VITAL_HOME is located in a git repository: ${gitDir.absolutePath}"
			print "Testing git command ... ";
		}		
		
		try {
			def listProcess = new ProcessBuilder(['git', '--version']).redirectErrorStream(true).start()
			if(verbose) {
				print " OK\n"
			}
			return true
		} catch(Exception e) {
			if(verbose) {
				print " FAILED\n"
				println e.getMessage()
			}
			return false
		}
		
	}
	
	public static void runProcess(File processHome, List cmd) {
		
//		if(dryRun) {
//			println("Dry run - command skipped");
//			return;
//		}
		
		def process = new ProcessBuilder(cmd).directory(processHome).redirectErrorStream(true).start()
		
		process.inputStream.eachLine {println it}
		
	}
	
	def static void rewriteOntology(File newOwlF) {
		
		//rewrite the file with owlapi - nicer representation and protege versionInfo annotation bug fix
		FileDocumentSource mergingSource = new FileDocumentSource(newOwlF)

		OWLOntology mergingOntology = owl2vcs.utils.OntologyUtils.loadOntology(mergingSource)
		

		OWLOntologyManager ontManager = org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager()

		RDFXMLOntologyFormat format = new RDFXMLOntologyFormat()
		format.setAddMissingTypes(false)
		FileOutputStream outStream = null;

				
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ontManager.saveOntology(mergingOntology, format, os)
			
			//filter annotation properties!
			DocumentBuilder newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
			Document dom = newDocumentBuilder.parse(new ByteArrayInputStream(os.toByteArray()));
			
			Element doc = dom.getDocumentElement();

			NodeList nodes = doc.getChildNodes();
			
			Element ontElement = null;
			
			Element versionInfo = null 
			
			for(int i = 0; i < nodes.getLength(); i++) {
				
				Node node = nodes.item(i)
				
				if(!(node instanceof Element)) continue
				
				Element el = (Element)node;
				
				String nn = el.getNodeName()
				
				
				if("owl:Ontology".equals(nn)) {
					ontElement = el
				} else if("rdf:Description".equals(nn)) {

					NodeList chs = el.getChildNodes()
					
					for(int j = 0; j < chs.getLength(); j++) {
						
						Node n = chs.item(j)
						
						if(n instanceof Element && "owl:versionInfo".equals(n.getNodeName())) {
							versionInfo = (Element)n
							
							doc.removeChild(el)
							
						}
						
					}				
					
				
				}
				
			}
			
			outStream = new FileOutputStream(newOwlF)
			
			if(versionInfo) {
				
				if(ontElement == null) throw new RuntimeException("No ontology element!");
				ontElement.appendChild(dom.createTextNode("    "))
				ontElement.appendChild(versionInfo)
				ontElement.appendChild(dom.createTextNode("\n    "))
				
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty(OutputKeys.METHOD, "xml");
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
				
				Result output = new StreamResult(outStream);
				Source input = new DOMSource(doc);
				
				transformer.transform(input, output);
				
				
			} else {
			
				//just copy the source
				IOUtils.copy(new ByteArrayInputStream(os.toByteArray()), outStream)
			
			}
						
			
		} finally {
			IOUtils.closeQuietly(outStream)
		}
		
	}
}
