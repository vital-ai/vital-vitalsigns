package ai.vital.vitalsigns.model.property


import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalsigns.query.VitalNTripleWriter;
import ai.vital.vitalsigns.rdf.VitalNTripleReader;

import com.hp.hpl.jena.rdf.model.Literal
import com.hp.hpl.jena.rdf.model.impl.LiteralImpl
import com.hp.hpl.jena.sparql.util.NodeFactory;

/**
 * Special property type for unknown datatype properties or strings with language tag - not supported in vitalsigns
 * 
 */

class OtherProperty implements IProperty {

	private static final long serialVersionUID = 1L;
	
	private String lexicalForm
	
	private String datatypeURI
	
	private String langTag 
	
	public OtherProperty(String lexicalForm, String datatypeURI,
			String langTag) {
		super();
		this.lexicalForm = lexicalForm;
		this.datatypeURI = datatypeURI;
		this.langTag = langTag;
	}



	@Override
	public Object rawValue() {
		return this;
	}
	
	@Override
	public boolean equals(Object obj) {

		if(obj instanceof OtherProperty) {
			String otherRDFString = ((OtherProperty)obj).toRDFString()
			String thisRDFString = this.toRDFString()
			return otherRDFString.equals(thisRDFString)
		}
		
		return false
		
	}

	@Override
	public IProperty unwrapped() {
		return this;
	}


	public String getLexicalForm() {
		return lexicalForm;
	}


	public String getDatatypeURI() {
		return datatypeURI;
	}


	public String getLangTag() {
		return langTag;
	}

	public String toRDFString() {
		String s = VitalNTripleWriter.escapeRDFNode(new LiteralImpl(NodeFactory.createLiteralNode(lexicalForm, langTag, datatypeURI), null));
		return s
	}
	
	@Override
	public String toString() {
		return this.toRDFString()
	}
	
	public static OtherProperty fromRDFString(String s) {
		
		//parse it as rdf string "lexical"[^^datatype|@langtag]
		Literal l = VitalNTripleReader.parseLiteral(s);
		
		String dt = l.getDatatypeURI()
		if(dt != null && dt.startsWith("<") && dt.endsWith(">")) {
			dt = dt.substring(1, dt.length() - 1)
		}
		
		OtherProperty op = new OtherProperty(l.getLexicalForm(), dt, l.getLanguage());
		
		return op
		
	}


	public static OtherProperty fromJSONMap(LinkedHashMap<String, Object> m) {
		return new OtherProperty(m.get("lexicalForm"), m.get("datatypeURI"), m.get("langTag"));
	}


	public LinkedHashMap<String, Object> toJSONMap() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<String, Object>();
		if(lexicalForm != null) {
			m.put("lexicalForm", lexicalForm);
		}
		if(datatypeURI != null) {
			m.put("datatypeURI", datatypeURI);
		}
		if(langTag != null) {
			m.put("langTag", langTag);
		}
		return m;
	}

	
	private String externalURI
	
	public String getURI() {
		if(externalURI != null) return externalURI
		throw new RuntimeException("getURI should be implemented by property trait");
	}
	
	
	public static OtherProperty createInstance(String propertyURI, String lexicalForm, String datatypeURI, String lang) {
		OtherProperty sp = new OtherProperty(lexicalForm, datatypeURI, lang)
		sp.externalURI = propertyURI
		return sp
	}
	
	
	/**
	 * creates external property critertion
	 * @param propertyURI
	 * @return
	 */
	public static VitalGraphQueryPropertyCriterion create(String propertyURI) {
		VitalGraphQueryPropertyCriterion c = new VitalGraphQueryPropertyCriterion(propertyURI);
		c.externalProperty = true
		return c;
	}

    @Override
    public void setExternalPropertyURI(String externalPropertyURI) {
        this.externalURI = externalPropertyURI;
    }
}
