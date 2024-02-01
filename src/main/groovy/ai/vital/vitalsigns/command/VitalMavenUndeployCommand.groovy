package ai.vital.vitalsigns.command

import groovy.cli.picocli.CliBuilder

import java.util.Map;
import java.util.Set

import ai.vital.vitalsigns.VitalSigns;;

class VitalMavenUndeployCommand {

    static final String VMU = "vitalmavenundeploy"
    
    static final String CMD_HELP = 'help'
    
    static final String CMD_UNINSTALL_VITAL_CORE = 'uninstallvitalcore'
     
    static final String CMD_UNINSTALL_VITAL_DOMAIN = 'uninstallvitaldomain'
    
    //installs all modules
    static final String CMD_UNINSTALL_VDK_MODULES = 'uninstallvdkmodules'
    
    static final String CMD_UNINSTALL_VDK_MODULE = 'uninstallvdkmodule'
    
    //install all jars
    static final String CMD_UNINSTALL_DOMAIN_JARS = 'uninstalldomainjars'
    
    //install particular jar
    static final String CMD_UNINSTALL_DOMAIN_JAR = 'uninstalldomainjar'
    
    static Map cmd2CLI = [:];
    
    static Set allCommands = null;

    static {
        
        //installs vitalsigns and vital-domain jars
        def cmdUninstallVitalCoreCLI = new CliBuilder(usage: "${VMU} ${CMD_UNINSTALL_VITAL_CORE}", stopAtNonOption: false)
        cmd2CLI[CMD_UNINSTALL_VITAL_CORE] = cmdUninstallVitalCoreCLI
        
        def cmdUninstallVitalDomainCLI = new CliBuilder(usage: "${VMU} ${CMD_UNINSTALL_VITAL_DOMAIN}", stopAtNonOption: false)
        cmd2CLI[CMD_UNINSTALL_VITAL_DOMAIN] = cmdUninstallVitalDomainCLI
        
        def cmdUninstallVdkModulesCLI = new CliBuilder(usage: "${VMU} ${CMD_UNINSTALL_VDK_MODULES}", stopAtNonOption: false)
        cmd2CLI[CMD_UNINSTALL_VDK_MODULES] = cmdUninstallVdkModulesCLI
        
        def cmdUninstallVdkModuleCLI = new CliBuilder(usage: "${VMU} ${CMD_UNINSTALL_VDK_MODULE}", stopAtNonOption: false)
        cmdUninstallVdkModuleCLI.with {
            j longOpt: "jar", "module jar path, must be located in \$VITAL_HOME/", args: 1, required: true
        }
        cmd2CLI[CMD_UNINSTALL_VDK_MODULE] = cmdUninstallVdkModuleCLI
        
        def cmdUninstallDomainJarsCLI = new CliBuilder(usage: "${VMU} ${CMD_UNINSTALL_DOMAIN_JARS}", stopAtNonOption: false)
        cmd2CLI[CMD_UNINSTALL_DOMAIN_JARS] = cmdUninstallDomainJarsCLI
        
        def cmdUninstallDomainJarCLI = new CliBuilder(usage: "${VMU} ${CMD_UNINSTALL_DOMAIN_JAR}", stopAtNonOption: false)
        cmdUninstallDomainJarCLI.with {
            j longOpt: "jar", "domain jar path, must be located in \$VITAL_HOME/domain-groovy-jar/", args: 1, required: true
        }
        cmd2CLI[CMD_UNINSTALL_DOMAIN_JAR] = cmdUninstallDomainJarCLI
        
        allCommands = new LinkedHashSet(cmd2CLI.keySet())
        allCommands.add(CMD_HELP)
    }
    
        
    def static main(args) {
        
        String VITAL_HOME = System.getenv(VitalSigns.VITAL_HOME);
        if(!VITAL_HOME) error("VITAL_HOME environment variable not set.");
        
        File vitalHome = new File(VITAL_HOME);
        if(!vitalHome.exists()) error("$VITAL_HOME path does not exist: ${VITAL_HOME} ");
        if(!vitalHome.isDirectory()) error("VITAL_HOME does not denote a directory: ${VITAL_HOME}");
        
        if(args.length == 0 || args[0] == CMD_HELP) {
            println "Usage: ${VMU} <command> [options]"
            println "   where command is one of: ${allCommands}"
            
            println "\n"
            
            for(def e : cmd2CLI.entrySet() ) {
                String cmd = e.key
                def cli = e.value
                cli.usage();
//                println "\n"
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
        
        println "command: ${cmd}"
        
        def options = cli.parse(params.toArray(new String[params.size()]))
        
        if(!options) return
        
        print "checking maven command... "
                
        //test maven command
        StringBuilder sb = new StringBuilder()
        int status = ProcessUtils.runProcess(vitalHome, [VitalMavenDeployCommand.mvnCommand(), '-v'], false, sb)
        if(status != 0) {
            throw new Exception("mavenCommand Command error: ${status} - ${sb.toString()}")
        }
        
        println "ok"
        
        
        if(cmd == CMD_UNINSTALL_VITAL_CORE) {
            
            VitalMavenDeployCommand.installVitalCore(vitalHome, false)
        } else if(cmd == CMD_UNINSTALL_VITAL_DOMAIN) {
            
            VitalMavenDeployCommand.installVitalDomain(vitalHome, false)
            
        } else if(cmd == CMD_UNINSTALL_DOMAIN_JAR || cmd == CMD_UNINSTALL_DOMAIN_JARS) {
            
            if(cmd == CMD_UNINSTALL_DOMAIN_JAR) {
               
                String jarPath = options.j
                
                File jar = new File(jarPath)
            
                VitalMavenDeployCommand.installDomainJars(vitalHome, jar, null, false)
                    
            }
            
            if(cmd == CMD_UNINSTALL_DOMAIN_JARS) {
                
                VitalMavenDeployCommand.installDomainJars(vitalHome, null, null, false)
                
            }
            
        } else if(cmd == CMD_UNINSTALL_VDK_MODULE) {
        
            File modFile = new File(options.j)
        
            println "Module jar file: ${modFile.absolutePath}"
            
            VitalMavenDeployCommand.installVDKModules(vitalHome, modFile, false)
            
        } else if(cmd == CMD_UNINSTALL_VDK_MODULES) {
        
            VitalMavenDeployCommand.installVDKModules(vitalHome, null, false)
            
        } else {
            error("Unhandled command: ${cmd}")
        }
        
    }

    static void error(String m) {
        System.err.println(m);
        System.exit(-1);
    }
}
