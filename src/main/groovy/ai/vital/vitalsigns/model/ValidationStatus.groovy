package ai.vital.vitalsigns.model

import ai.vital.vitalsigns.uri.URIGenerator.URIResponse;

/**
 * Graph object validation status.
 *
 */
class ValidationStatus {

	public static enum Status {
		ok,
		error
	}
	
	public ValidationStatus() {
		this.status = Status.ok
	}
	
	Status status
	
	URIResponse uriResponse

	Map<String, String> errors
	
	// lazy init errors map sets
	public String putError(String field, String error) {
		
		this.status = Status.error
		
		if(this.errors == null) {
			this.errors = new HashMap<String, String>()
		}
		
		return errors.put(field, error)
	}
}
