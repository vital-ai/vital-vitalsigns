package ai.vital.vitalsigns.utils;

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;

import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.ontology.DomainGenerator;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Utility 
 *
 */
public class GenerateVitalCoreDomain {

	public static void main(String[] args) throws Exception {
		
		VitalSigns vs = VitalSigns.initEmptyVitalSigns();

		OntModelSpec owlMem = OntModelSpec.OWL_MEM;
		owlMem.getDocumentManager().setProcessImports(false);
		
		OntModel model = ModelFactory.createOntologyModel(owlMem);
		model.read(vs.getCoreModelInputStream(), null);

		File vitalHomePath = vs.getVitalHomePath();
		File ontFile = new File(vitalHomePath, "vital-ontology/" + VitalCoreOntology.getFileName());
		byte[] ontologyBytes = FileUtils.readFileToByteArray(ontFile);
		
		File dir = new File("./src-vital-core-generated");
		FileUtils.deleteQuietly(dir);
		dir.mkdirs();
		Path tempDir = dir.toPath();
		
		DomainGenerator gen = new DomainGenerator(model, ontologyBytes, VitalCoreOntology.ONTOLOGY_IRI, "ai.vital.vitalsigns.model", tempDir);
		gen.generateOntologyDescriptor = false;
		gen.ontologyFilename = VitalCoreOntology.getFileName();
		
		gen.generateDomainSourceClasses();
		gen.generateDomainSourceProperties();
		
		System.out.println("DONE");
		
	}
}
