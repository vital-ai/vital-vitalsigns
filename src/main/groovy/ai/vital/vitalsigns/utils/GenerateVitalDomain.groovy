package ai.vital.vitalsigns.utils;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.ontology.DomainGenerator;

public class GenerateVitalDomain {

	public static void main(String[] args) throws Exception {

		if(args.length != 4){
			System.err.println("usage: <owl_file> <target_src> <target_classes> <target_jar>");
			return;
		}
		
		VitalSigns vs = VitalSigns.get();
		
		FileInputStream fis = null;
		
		File targetClasses = null;
		
		try {
			
			File owlFile = new File(args[0]);

			File targetSource = new File(args[1]);
			FileUtils.deleteQuietly(targetSource);
			targetSource.mkdirs();
			
			targetClasses = new File(args[2]);
			FileUtils.deleteQuietly(targetClasses);
			targetClasses.mkdirs();
			
			File targetJar = new File(args[3]);
			fis = new FileInputStream(owlFile);
			DomainGenerator generator = vs.generateDomainClasses(fis, targetSource.toPath(), DomainGenerator.GROOVY, "ai.vital.domain");
			generator.compileSource(targetClasses.toPath());
			generator.generateJar(targetClasses.toPath(), targetJar.toPath());
			
		} finally {
			FileUtils.deleteDirectory(targetClasses);
			IOUtils.closeQuietly(fis);
		}

	}

}
