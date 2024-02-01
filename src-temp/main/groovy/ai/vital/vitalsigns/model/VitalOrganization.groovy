package ai.vital.vitalsigns.model

class VitalOrganization extends VITAL_Node {

	private static final long serialVersionUID = 1L;

	public VitalOrganization() {
		super()
	}

	public VitalOrganization(Map<String, Object> props) {
		super(props)
	}

    public static VitalOrganization withId(String id) {

        VitalOrganization org = new VitalOrganization( organizationID: id )

		org.generateURI( (VitalApp) null)

        return org
    }

}
