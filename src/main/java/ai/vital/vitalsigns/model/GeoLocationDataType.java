package ai.vital.vitalsigns.model;

import ai.vital.vitalsigns.model.property.GeoLocationProperty;
import ai.vital.vitalsigns.model.property.GeoLocationProperty.GeoLocationFormatException;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.impl.LiteralLabel;

public class GeoLocationDataType extends BaseDatatype {

	public static final String theTypeURI = VitalCoreOntology.geoLocation.getURI();
	public static final RDFDatatype theGeoLocationDataType = new GeoLocationDataType();

	/** private constructor - single global instance */
	private GeoLocationDataType() {
		super(theTypeURI);
	}

	/**
	 * Convert a value of this datatype out
	 * to lexical form.
	 */
	public String unparse(Object value) {
		GeoLocationProperty r = (GeoLocationProperty) value;
		return r.toRDFValue();
	}

	/**
	 * Parse a lexical form of this datatype to a value
	 * @throws DatatypeFormatException if the lexical form is not legal
	 */
	public Object parse(String lexicalForm) throws DatatypeFormatException {
		
		try {
			return GeoLocationProperty.fromRDFString(lexicalForm);	
		} catch(GeoLocationFormatException e) {
			throw new DatatypeFormatException(lexicalForm, theGeoLocationDataType, e.getLocalizedMessage());
		}
		 
	}

	/**
	 * Compares two instances of values of the given datatype.
	 * This does not allow rationals to be compared to other number
	 * formats, Lang tag is not significant.
	 */
	public boolean isEqual(LiteralLabel value1, LiteralLabel value2) {
		return value1.getDatatype() == value2.getDatatype() && value1.getValue().equals(value2.getValue());
	}
	
}
