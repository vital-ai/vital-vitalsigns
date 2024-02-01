package ai.vital.vitalservice.exception

/**
 * runtime exception, thrown when a service does not implement particular service api method
 */
class VitalServiceUnimplementedException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public VitalServiceUnimplementedException(String message) {
		super(message)
	}
	
	
}
