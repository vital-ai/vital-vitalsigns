package ai.vital.vitalsigns.model;

//import com.hp.hpl.jena.vocabulary.XSD;


public class XSD {


    public static XSD xboolean = new XSD();

    public static XSD xdatetime = new XSD();

    public static XSD xdouble = new XSD();

    public static XSD xfloat = new XSD();

    public static XSD xint = new XSD();

    public static XSD xlong = new XSD();

    public static XSD xstring = new XSD();

    public static XSD xanyURI = new XSD();

    public static XSD fromString( String s) {

        if("xsd:boolean".equals(s) || com.hp.hpl.jena.vocabulary.XSD.xboolean.getURI().equals(s)) {
            return xboolean;
        } else if("xsd:datetime".equals(s) || com.hp.hpl.jena.vocabulary.XSD.dateTime.getURI().equals(s)) {
            return xdatetime;
        } else if("xsd:double".equals(s) || com.hp.hpl.jena.vocabulary.XSD.xdouble.getURI().equals(s)) {
            return xdouble;
        } else if("xsd:float".equals(s) || com.hp.hpl.jena.vocabulary.XSD.xfloat.getURI().equals(s)) {
            return xfloat;
        } else if("xsd:int".equals(s) || "xsd:integer".equals(s) || com.hp.hpl.jena.vocabulary.XSD.xint.getURI().equals(s)) {
            return xint;
        } else if("xsd:string".equals(s) || com.hp.hpl.jena.vocabulary.XSD.xstring.getURI().equals(s)) {
            return xstring;
        } else if("xsd:anyuri".equals(s) || com.hp.hpl.jena.vocabulary.XSD.anyURI.getURI().equals(s)) {
            return xanyURI;
        } else {
            return null;
        }
    }

    public static XSDDateTimeFormat xdatetime(String format) {
        return new XSDDateTimeFormat(format);
    }

}
