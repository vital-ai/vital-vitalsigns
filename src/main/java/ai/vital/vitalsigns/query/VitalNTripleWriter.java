package ai.vital.vitalsigns.query;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.impl.NTripleWriter;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class to get access to RDFNode encoding
 *
 */
public class VitalNTripleWriter extends NTripleWriter  {

	public static String escapeRDFNode(RDFNode n) {
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		writeNode(n, pw);
		pw.close();
		return sw.toString();
	}



}
