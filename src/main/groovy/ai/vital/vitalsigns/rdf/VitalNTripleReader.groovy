package ai.vital.vitalsigns.rdf;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * 
 * from http://parliament.semwebcentral.org/javadoc/com/bbn/parliament/jena/joseki/bridge/util/NTriplesUtil.html
 *
 */

public class VitalNTripleReader {

	private static Model temp = ModelFactory.createDefaultModel();
	
    /**
     * Parses an N-Triples literal, creates an object for it using the
     * supplied Model and returns this object.
     *
     * @param nTriplesLiteral The N-Triples literal to parse.
     * @param model The Model to use for creating the object.
     * @return A Literal object representing the parsed literal.
     * @exception IllegalArgumentException If the supplied literal could not be
     * parsed correctly.
     **/

    public static Literal parseLiteral(String nTriplesLiteral)
        throws IllegalArgumentException
    {
        if (nTriplesLiteral.startsWith("\"")) {
            // Find string separation points
            int endLabelIdx = _findEndOfLabel(nTriplesLiteral);

            if (endLabelIdx != -1) {
                int startLangIdx = nTriplesLiteral.indexOf("@", endLabelIdx);
                int startDtIdx = nTriplesLiteral.indexOf("^^", endLabelIdx);

                if (startLangIdx != -1 && startDtIdx != -1) {
                    throw new IllegalArgumentException("Literals can not have both a language and a datatype");
                }

                // Get label
                String label = nTriplesLiteral.substring(1, endLabelIdx);
                label = unescapeString(label);

                if (startLangIdx != -1) {
                    // Get language
                    String language = nTriplesLiteral.substring(startLangIdx + 1);
                    return temp.createLiteral(label, language);
                }
                else if (startDtIdx != -1) {
                    // Get datatype
                    String datatype = nTriplesLiteral.substring(startDtIdx + 2);
                    return temp.createTypedLiteral(label, datatype);
                }
                else {
                    return temp.createLiteral(label);
                }
            }
        }

        throw new IllegalArgumentException(
                "Not a legal N-Triples literal: " + nTriplesLiteral);
    }

    /**
     * Finds the end of the label in a literal string. This method
     * takes into account that characters can be escaped using
     * backslashes.
     *
     * @return The index of the double quote ending the label, or
     * <tt>-1</tt> if it could not be found.
     **/
    private static int _findEndOfLabel(String nTriplesLiteral) {
        // First character of literal is guaranteed to be a double
        // quote, start search at second character.

        boolean previousWasBackslash = false;

        for (int i = 1; i < nTriplesLiteral.length(); i++) {
            char c = nTriplesLiteral.charAt(i);

            if (c == '"' && !previousWasBackslash) {
                return i;
            }
            else if (c == '\\' && !previousWasBackslash) {
                // start of escape
                previousWasBackslash = true;
            }
            else if (previousWasBackslash) {
                // c was escaped
                previousWasBackslash = false;
            }
        }

        return -1;
    }
    

    /**
     * Unescapes an escaped Unicode string. Any Unicode sequences
     * (<tt>&#x5C;uxxxx</tt> and <tt>&#x5C;Uxxxxxxxx</tt>) are restored to the
     * value indicated by the hexadecimal argument and any backslash-escapes
     * (<tt>\"</tt>, <tt>\\</tt>, etc.) are decoded to their original form.
     *
     * @param s An escaped Unicode string.
     * @return The unescaped string.
     * @exception IllegalArgumentException If the supplied string is not a
     * correctly escaped N-Triples string.
     **/
    public static String unescapeString(String s) {
        int backSlashIdx = s.indexOf('\\');

        if (backSlashIdx == -1) {
            // No escaped characters found
            return s;
        }

        int startIdx = 0;
        int sLength = s.length();
        StringBuffer buf = new StringBuffer(sLength);

        while (backSlashIdx != -1) {
            buf.append(s.substring(startIdx, backSlashIdx));

            if (backSlashIdx + 1 >= sLength) {
                throw new IllegalArgumentException("Unescaped backslash in: " + s);
            }

            char c = s.charAt(backSlashIdx + 1);

            if (c == 't') {
                buf.append('\t');
                startIdx = backSlashIdx + 2;
            }
            else if (c == 'r') {
                buf.append('\r');
                startIdx = backSlashIdx + 2;
            }
            else if (c == 'n') {
                buf.append('\n');
                startIdx = backSlashIdx + 2;
            }
            else if (c == '"') {
                buf.append('"');
                startIdx = backSlashIdx + 2;
            }
            else if (c == '\'') {
               buf.append('\'');
               startIdx = backSlashIdx + 2;
            }
            else if (c == '\\') {
                buf.append('\\');
                startIdx = backSlashIdx + 2;
            }
            else if (c == 'f') {
               buf.append("\f");
               startIdx = backSlashIdx + 2;
            }
            else if (c == 'b') {
               buf.append("\b");
               startIdx = backSlashIdx + 2;
            }
            else if (c == 'u') {
                // \\uxxxx
                if (backSlashIdx + 5 >= sLength) {
                    throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
                }
                String xx = s.substring(backSlashIdx + 2, backSlashIdx + 6);

                try {
                    c = (char)Integer.parseInt(xx, 16);
                    buf.append(c);

                    startIdx = backSlashIdx + 6;
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Illegal Unicode escape sequence '\\u" + xx + "' in: " + s);
                }
            }
            else if (c == 'U') {
                // \\Uxxxxxxxx
                if (backSlashIdx + 9 >= sLength) {
                    throw new IllegalArgumentException("Incomplete Unicode escape sequence in: " + s);
                }
                String xx = s.substring(backSlashIdx + 2, backSlashIdx + 10);

                try {
                    c = (char)Integer.parseInt(xx, 16);
                    buf.append(c);

                    startIdx = backSlashIdx + 10;
                }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Illegal Unicode escape sequence '\\U" + xx + "' in: " + s);
                }
            }
            else {
                throw new IllegalArgumentException("Unescaped backslash in: " + s);
            }

            backSlashIdx = s.indexOf('\\', startIdx);
        }

        buf.append( s.substring(startIdx) );

        return buf.toString();
    }

}
