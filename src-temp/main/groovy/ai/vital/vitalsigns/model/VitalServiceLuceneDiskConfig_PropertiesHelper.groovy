package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceLuceneDiskConfig_PropertiesHelper extends ai.vital.vitalsigns.model.VitalServiceConfig_PropertiesHelper {

	public VitalServiceLuceneDiskConfig_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceLuceneDiskConfig');
	}

	protected VitalServiceLuceneDiskConfig_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getRootPath() {
		return _implementation("rootPath");
	}

}
