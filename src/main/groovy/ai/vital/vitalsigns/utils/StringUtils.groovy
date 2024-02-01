package ai.vital.vitalsigns.utils;

import ai.vital.vitalsigns.model.property.IProperty;

public class StringUtils {

	//extended method to determine if a string is null or empty
	public static boolean isEmpty(String s ) {
		return s == null || s.isEmpty();
	}

	public static boolean isEmpty(IProperty s) {
		return s == null || s.toString().isEmpty();
	}
	
}
