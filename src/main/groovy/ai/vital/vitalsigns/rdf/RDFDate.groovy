package ai.vital.vitalsigns.rdf

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import javax.xml.bind.DatatypeConverter

public class RDFDate {

	public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	public static DateFormat simpleFormat = new SimpleDateFormat("MMM d, yyyy");
	static {
		simpleFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	static {
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public static String toXSDString(Date date) {
		return dateFormat.format(date);
	}
	
	public static Date fromXSDString(String s) {
		Date date = null;
		try {
			date = DatatypeConverter.parseDateTime(s).getTime();
		} catch(Exception e) {
			
		}
		
		if(date == null) {
			try {
				date = simpleFormat.parse(s);
			} catch(Exception e) {}
		}
		
		if(date == null) {
			throw new RuntimeException("Unparseable date string: " + s);
		}
		
		return date;
	}
	
}
