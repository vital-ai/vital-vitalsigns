package ai.vital.vitalsigns.command;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.vital.vitalsigns.VitalManifest;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.utils.StringUtils;

//http://stackoverflow.com/questions/1272648/reading-my-own-jars-manifest
public class VitalModulesVersionsCheck {

    private final static Logger log = LoggerFactory.getLogger(VitalModulesVersionsCheck.class);
    
    public static List<String> checkModulesVersions() {

        List<String> warnings = new ArrayList<String>();
        
        Enumeration<URL> resources = null;
        
        try {
            resources = VitalModulesVersionsCheck.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
        } catch(Exception e) {
            log.error(e.getLocalizedMessage(), e);
            return warnings;
        }

        while (resources.hasMoreElements()) {

            InputStream stream = null;

            try {

                stream = resources.nextElement().openStream();
                Manifest manifest = new Manifest(stream);

                Attributes attr = manifest.getMainAttributes();
                if(attr == null) continue;
                
                String value = attr.getValue(VitalManifest.VITAL_MODULE_NAME);
                String version = attr.getValue(VitalManifest.VITAL_MODULE_VERSION);
                
                if( ! StringUtils.isEmpty(value) && ! StringUtils.isEmpty(version) ) {
                    
                    if( ! VitalSigns.VERSION.equals(version) ) {
                        
                        warnings.add(value + " version: " + version + " is different from VDK version: " + VitalSigns.VERSION);
                        
                    }
                    
                }
                
                // check that this is your manifest and do what you need or get
                // the next one

            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
                // handle
            } finally {
                IOUtils.closeQuietly(stream);
            }
        }
        
        return warnings;

    }

}
