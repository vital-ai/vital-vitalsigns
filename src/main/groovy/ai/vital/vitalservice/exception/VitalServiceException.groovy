package ai.vital.vitalservice.exception

class VitalServiceException extends Exception {

	private static final long serialVersionUID = 1L;

    //for remote services only
    boolean sessionNotFound = false
    	
	public VitalServiceException(String message) {
		super(message)
	}

	public VitalServiceException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public VitalServiceException(Throwable arg0) {
		super(arg0);
	}
	
}
