package ai.vital.vitalservice

import ai.vital.vitalsigns.model.property.URIProperty;

class VitalStatus implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public final static VitalStatus OK = new VitalStatus(Status.ok, "OK") 
	
	public static VitalStatus withOK() {
		return new VitalStatus(Status.ok, "")
	}
	
	public static VitalStatus withOKMessage(String message) {
		return new VitalStatus(Status.ok, message)
	}
	
	public static VitalStatus withError(String message) {
		return new VitalStatus(Status.error, message)
	}
	
	@Override
	public String toString() {
		return "VitalStatus: ${status}${message ? (' - ' + message) : ''}"
	}
	
	public final static enum Status {
		ok,
		error
	}
	
	// ok, or error
	// message
	
	Status status
	
	String message

	//for batch operations etc	
	Integer successes
	Integer errors
	
	List<URIProperty> failedURIs
	
	Integer pingTimeMillis
	
    
    //filled by delete implementations
    List<URIProperty> deletedURIs

	public VitalStatus(Status status, String message) {
		super();
		this.status = status;
		this.message = message;
	}	
	
    public VitalStatus() {
        this.status = Status.ok
    }
	

}
