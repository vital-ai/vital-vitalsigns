package ai.vital.vitalsigns.meta

public enum GraphContext {

	/*
	 add a set of Contexts added as constants, such as "Local", "ServiceWide", "Container". 
	 using these constants in calls to VitalService will dictate where objects are to be "get" from. 
	 ServiceWide does database queries back to dataserver. 
	 Local stays within the local cache only. 
	 Container is limited to the container that's provided in the call. 
	 for ServiceWide we may add some additional parameters to control if or how much an object is "expanded"
	 */
	
	Local,
	
	ServiceWide,
	
	Container
	
}
