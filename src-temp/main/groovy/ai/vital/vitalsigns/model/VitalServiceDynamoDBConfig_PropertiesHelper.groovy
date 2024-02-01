package ai.vital.vitalsigns.model

import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion

class VitalServiceDynamoDBConfig_PropertiesHelper extends ai.vital.vitalsigns.model.VitalServiceConfig_PropertiesHelper {

	public VitalServiceDynamoDBConfig_PropertiesHelper() {
		super('http://vital.ai/ontology/vital-core#VitalServiceDynamoDBConfig');
	}

	protected VitalServiceDynamoDBConfig_PropertiesHelper(String propertyURI) {
		super(propertyURI);
	}


	public VitalGraphQueryPropertyCriterion getAccessKey() {
		return _implementation("accessKey");
	}


	public VitalGraphQueryPropertyCriterion getEndpointURL() {
		return _implementation("endpointURL");
	}


	public VitalGraphQueryPropertyCriterion getLocalEndpoint() {
		return _implementation("localEndpoint");
	}


	public VitalGraphQueryPropertyCriterion getRegion() {
		return _implementation("region");
	}


	public VitalGraphQueryPropertyCriterion getS3AccessKey() {
		return _implementation("s3AccessKey");
	}


	public VitalGraphQueryPropertyCriterion getS3BasePath() {
		return _implementation("s3BasePath");
	}


	public VitalGraphQueryPropertyCriterion getS3Bucket() {
		return _implementation("s3Bucket");
	}


	public VitalGraphQueryPropertyCriterion getS3EndpointURL() {
		return _implementation("s3EndpointURL");
	}


	public VitalGraphQueryPropertyCriterion getS3LocalEndpoint() {
		return _implementation("s3LocalEndpoint");
	}


	public VitalGraphQueryPropertyCriterion getS3Region() {
		return _implementation("s3Region");
	}


	public VitalGraphQueryPropertyCriterion getS3SecretKey() {
		return _implementation("s3SecretKey");
	}


	public VitalGraphQueryPropertyCriterion getSecretKey() {
		return _implementation("secretKey");
	}


	public VitalGraphQueryPropertyCriterion getTablesPrefix() {
		return _implementation("tablesPrefix");
	}

}
