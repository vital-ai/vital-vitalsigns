package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class DomainModel_PropertiesHelper extends ai.vital.vitalsigns.model.VITAL_Node_PropertiesHelper {

	public DomainModel_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#DomainModel');
	}

	protected DomainModel_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getAppID() {
		return _implementation("appID");
	}


	public VitalGraphQueryPropertyCriterion getBackwardCompVersion() {
		return _implementation("backwardCompVersion");
	}


	public VitalGraphQueryPropertyCriterion getDefaultPackageValue() {
		return _implementation("defaultPackageValue");
	}


	public VitalGraphQueryPropertyCriterion getDomainOWL() {
		return _implementation("domainOWL");
	}


	public VitalGraphQueryPropertyCriterion getDomainOWLHash() {
		return _implementation("domainOWLHash");
	}


	public VitalGraphQueryPropertyCriterion getOrganizationID() {
		return _implementation("organizationID");
	}


	public VitalGraphQueryPropertyCriterion getPreferred() {
		return _implementation("preferred");
	}


	public VitalGraphQueryPropertyCriterion getPreferredImportVersions() {
		return _implementation("preferredImportVersions");
	}


	public VitalGraphQueryPropertyCriterion getVersionInfo() {
		return _implementation("versionInfo");
	}

}
