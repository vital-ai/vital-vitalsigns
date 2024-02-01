package ai.vital.vitalservice.auth

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.lang3.RandomStringUtils

public class VitalAuthKeyValidation {

    public static class VitalAuthKeyValidationException extends Exception {

        public VitalAuthKeyValidationException(String arg0) {
            super(arg0);
        }
        
    }
    
    public final static Pattern pattern = Pattern.compile("[a-z]{4}\\-[a-z]{4}\\-[a-z]{4}", Pattern.CASE_INSENSITIVE);
    
    public final static int keyLength = 3 * 4 + 2
    
    public static void validateKey (String input) throws VitalAuthKeyValidationException {
        
        if( input == null ) throw new VitalAuthKeyValidationException("Null input");
        
        if( input.length() != keyLength) throw new VitalAuthKeyValidationException("Invalid key length: " + input.length() + ", expected: " + keyLength + " - " + input);
        
        Matcher matcher = pattern.matcher(input)
        
        if(!matcher.matches()) throw new VitalAuthKeyValidationException("Key does not match pattern " + pattern.pattern() + " - " + input )
        
    }
    
    public static String generateKey() {
        return RandomStringUtils.randomAlphabetic(4) + '-' + RandomStringUtils.randomAlphabetic(4) + '-' + RandomStringUtils.randomAlphabetic(4)
    }
    
}
