package ai.vital.vitalsigns.xml;

public class XMLUtils {

	// XML 1.0
	// #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
	static String xml10pattern = "[^" +
			"\u0009\r\n" +
			"\u0020-\uD7FF" +
			"\uE000-\uFFFD" +
			"\ud800\udc00-\udbff\udfff" +
			"]";
			
			
	public static String cleanupString(String input) {
		
		if(input == null) return null;
		
		return input.replaceAll(xml10pattern, "");
		
	}
	
}
