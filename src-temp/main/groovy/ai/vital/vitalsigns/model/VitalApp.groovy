package ai.vital.vitalsigns.model

class VitalApp extends VITAL_Node {

	private static final long serialVersionUID = 1L;


	public VitalApp() {
		super()
	}

	public VitalApp(Map<String, Object> props) {
		super(props)
	}

    public static VitalApp withId(String id) {
        VitalApp app = new VitalApp(appID: id)
        app.generateURI((VitalApp) null)
        return app
    }

}
