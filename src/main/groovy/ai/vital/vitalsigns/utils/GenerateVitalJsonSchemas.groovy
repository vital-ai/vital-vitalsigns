package ai.vital.vitalsigns.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.json.JSONSchemaGenerator;

public class GenerateVitalJsonSchemas extends AbstractUtil {

	public static void main(String[] args) throws Exception {

		if(args.length != 6) {
			e("usage: <vital_core_owl> <vital_domain_owl> <vital_superadmin_domain_owl> <vital_core_json> <vital_domain_json> <vital_superadmin_domain_json>");
			return;
		}
		
		o("Generating vital JSON schemas...");
		
		File core = new File(args[0]);
		File domain = new File(args[1]);
		File superadmin = new File(args[2]);
		
		File coreOut = new File(args[3]);
		File domainOut = new File(args[4]);
		File superadminOut = new File(args[5]);
		
		o("Vital core owl: " + core.getAbsolutePath());
		o("Vital domain owl: " + domain.getAbsolutePath());
		o("Vital superadmin owl: " + superadmin.getAbsolutePath());
		o("Vital core json out: " + coreOut.getAbsolutePath());
		o("Vital domain json out: " + domainOut.getAbsolutePath());
		o("Vital superadmin json out: " + superadminOut.getAbsolutePath());
		
		FileInputStream cis = null;
		FileInputStream dis = null;
		FileInputStream ais = null;
		
		FileOutputStream cos = null;
		FileOutputStream dos = null;
		FileOutputStream aos = null;
		
		try {
			
			cis = new FileInputStream(core);
			cos = new FileOutputStream(coreOut);
			
			JSONSchemaGenerator cg = VitalSigns.get().createJSONSchemaGenerator(cis);
			cg.generateSchema();
			cg.writeSchemaToOutputStream(true, coreOut.getName(), cos);
			
			
			dis = new FileInputStream(domain);
			dos = new FileOutputStream(domainOut);
			
			JSONSchemaGenerator dg = VitalSigns.get().createJSONSchemaGenerator(dis);
			dg.generateSchema();
			dg.writeSchemaToOutputStream(false, domainOut.getName(), dos);
			
			
			ais = new FileInputStream(superadmin);
			aos = new FileOutputStream(superadminOut);
			JSONSchemaGenerator ag = VitalSigns.get().createJSONSchemaGenerator(ais);
			ag.generateSchema();
			ag.writeSchemaToOutputStream(false, superadminOut.getName(), aos);
			
		} finally {
			IOUtils.closeQuietly(cis);
			IOUtils.closeQuietly(cos);
			IOUtils.closeQuietly(dis);
			IOUtils.closeQuietly(dos);
			IOUtils.closeQuietly(ais);
			IOUtils.closeQuietly(aos);
		}
		
	}

}
