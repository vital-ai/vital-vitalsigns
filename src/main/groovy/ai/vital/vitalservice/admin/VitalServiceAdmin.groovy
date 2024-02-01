package ai.vital.vitalservice.admin

import groovy.lang.Closure;

import java.io.InputStream
import java.io.OutputStream
import java.util.List
import java.util.Map

import ai.vital.vitalservice.EndpointType
//import ai.vital.vitalservice.Transaction
import ai.vital.vitalservice.ServiceOperations
import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalsigns.meta.GraphContext
import ai.vital.vitalservice.exception.VitalServiceException
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalservice.query.VitalGraphQuery
import ai.vital.vitalservice.query.VitalPathQuery
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalservice.query.VitalSelectQuery
import ai.vital.vitalsigns.model.DatabaseConnection;
import ai.vital.vitalsigns.model.VITAL_Event
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization
import ai.vital.vitalsigns.model.VitalProvisioning;
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.VitalServiceKey;
import ai.vital.vitalsigns.model.VitalTransaction
import ai.vital.vitalsigns.model.container.GraphObjectsIterable;
import ai.vital.vitalsigns.model.property.URIProperty;


interface VitalServiceAdmin {

	// use call function for this...
	//public VitalStatus registerOntology(InputStream owl, String namespace, String _package)
	
	
	
	// info about service connection
	
	public EndpointType getEndpointType()
	
	public VitalOrganization getOrganization()
	
	
	// connection status
	
	public VitalStatus validate() throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus ping() throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus close() throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// service containers: segments, apps, organizations
	
	public List<VitalSegment> listSegments(VitalApp app) throws VitalServiceUnimplementedException, VitalServiceException
    
    public ResultList listSegmentsWithConfig(VitalApp app) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalSegment addSegment(VitalApp app, VitalSegment config, boolean createIfNotExists) throws VitalServiceUnimplementedException, VitalServiceException
    
	public VitalSegment addSegment(VitalApp app, VitalSegment config, VitalProvisioning provisioning, boolean createIfNotExists) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus removeSegment(VitalApp app, VitalSegment segment, boolean deleteData) throws VitalServiceUnimplementedException, VitalServiceException

	public List<VitalApp> listApps() throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus addApp(VitalApp app) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus removeApp(VitalApp app) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// URIs
	
	public URIProperty generateURI(VitalApp app, Class<? extends GraphObject> clazz) throws VitalServiceUnimplementedException, VitalServiceException
		
	// Transactions
    // Transactions
    /**
     * Creates a new transaction object, each service instance manages internal list of transactions.
     * A service that does not support transactions should throw a VitalServiceUnimplementedException
     * @return
     * @throws VitalServiceUnimplementedException
     * @throws VitalServiceException
     */
    public VitalTransaction createTransaction() throws VitalServiceUnimplementedException, VitalServiceException
    
    /**
     * Commits an open transaction
     * A service that does not support transactions should throw a VitalServiceUnimplementedException
     * @param transaction
     * @return
     * @throws VitalServiceUnimplementedException
     * @throws VitalServiceException
     */
    public VitalStatus commitTransaction(VitalTransaction transaction) throws VitalServiceUnimplementedException, VitalServiceException

    /**
     * Rolls back an open transaction
     * A service that does not support transactions should throw a VitalServiceUnimplementedException
     * @param transaction
     * @return
     * @throws VitalServiceUnimplementedException
     * @throws VitalServiceException
     */
    public VitalStatus rollbackTransaction(VitalTransaction transaction) throws VitalServiceUnimplementedException, VitalServiceException
    
    /**
     * Lists all transactions. It's not limited to open transaction only.
     * A service that does not support transactions should throw a VitalServiceUnimplementedException
     * @return
     * @throws VitalServiceUnimplementedException
     * @throws VitalServiceException
     */
    public List<VitalTransaction> getTransactions() throws VitalServiceUnimplementedException, VitalServiceException
	
	
	
	// crud operations
	
	// get
	
	// default cache=true, for consistency always include GraphContext
	
	public ResultList get(VitalApp app, GraphContext graphContext, URIProperty uri) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList get(VitalApp app, GraphContext graphContext, URIProperty uri, boolean cache) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList get(VitalApp app, GraphContext graphContext, List<URIProperty> uris) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList get(VitalApp app, GraphContext graphContext, List<URIProperty> uris, boolean cache) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// GraphContext is redundant here as this call only used for containers, but kept for consistency
	
	public ResultList get(VitalApp app, GraphContext graphContext, URIProperty uri, List<GraphObjectsIterable> containers) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList get(VitalApp app, GraphContext graphContext, List<URIProperty> uris, List<GraphObjectsIterable> containers) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// delete
	public VitalStatus delete(VitalApp app, URIProperty uri) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus delete(VitalTransaction transaction, VitalApp app, URIProperty uri) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus delete(VitalApp app, List<URIProperty> uris) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus delete(VitalTransaction transaction, VitalApp app, List<URIProperty> uris) throws VitalServiceUnimplementedException, VitalServiceException

	public VitalStatus deleteObject(VitalApp app, GraphObject object) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteObject(VitalTransaction transaction, VitalApp app, GraphObject object) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteObjects(VitalApp app, List<GraphObject> objects) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteObjects(VitalTransaction transaction, VitalApp app, List<GraphObject> objects) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// insert, must be a new object
	
	public ResultList insert(VitalApp app, VitalSegment targetSegment, GraphObject graphObject) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList insert(VitalTransaction transaction, VitalApp app, VitalSegment targetSegment, GraphObject graphObject) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList insert(VitalApp app, VitalSegment targetSegment, List<GraphObject> graphObjectsList) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList insert(VitalTransaction transaction, VitalApp app, VitalSegment targetSegment, List<GraphObject> graphObjectsList) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// save, if create=true, create a new object if it doesn't exist
	
	public ResultList save(VitalApp app, VitalSegment targetSegment, GraphObject graphObject, boolean create) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList save(VitalTransaction transaction, VitalApp app, VitalSegment targetSegment, GraphObject graphObject, boolean create) throws VitalServiceUnimplementedException, VitalServiceException

	public ResultList save(VitalApp app, VitalSegment targetSegment, List<GraphObject> graphObjectsList, boolean create) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList save(VitalTransaction transaction, VitalApp app, VitalSegment targetSegment, List<GraphObject> graphObjectsList, boolean create) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// object(s) must already exist, determine current segment & update
	
	public ResultList save(VitalApp app, GraphObject graphObject) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList save(VitalTransaction transaction, VitalApp app, GraphObject graphObject) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList save(VitalApp app, List<GraphObject> graphObjectsList) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList save(VitalTransaction transaction, VitalApp app, List<GraphObject> graphObjectsList) throws VitalServiceUnimplementedException, VitalServiceException
		
	
	public VitalStatus doOperations(VitalApp app, ServiceOperations operations) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// Functions
	
	public ResultList callFunction(VitalApp app, String function, Map<String, Object> arguments) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// Query + Expand
	
	// ServiceWide
	public ResultList query(VitalApp app, VitalQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	
	// Local
	public ResultList queryLocal(VitalApp app, VitalQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	
	// Containers
	public ResultList queryContainers(VitalApp app, VitalQuery query, List<GraphObjectsIterable> containers) throws VitalServiceUnimplementedException, VitalServiceException
	
	// ServiceWide
	public ResultList getExpanded(VitalApp app, URIProperty uri, boolean cache) throws VitalServiceUnimplementedException, VitalServiceException

	
	// path query determines segments list
	// there can be a prepared default path query helper that does the same as expand
	// i.e. VitalPathQuery.getDefaultExpandQuery(List<VitalSegment> segments, VitalSelectQuery rootselector) --> VitalPathQuery
	// for the case to query against containers or local, no segments specified:
	// VitalPathQuery.getDefaultExpandQuery(VitalSelectQuery rootselector) --> VitalPathQuery
	
	public ResultList getExpanded(VitalApp app, URIProperty uri, VitalPathQuery query, boolean cache) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteExpanded(VitalApp app, URIProperty uri) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteExpanded(VitalTransaction transaction, VitalApp app, URIProperty uri) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteExpandedObject(VitalApp app, GraphObject object) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteExpandedObject(VitalTransaction transaction, VitalApp app, GraphObject object) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteExpanded(VitalApp app, URIProperty uri, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteExpanded(VitalTransaction transaction, VitalApp app, URIProperty uri, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteExpanded(VitalApp app, List<URIProperty> uris, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteExpanded(VitalTransaction transaction, VitalApp app, List<URIProperty> uris, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteExpandedObjects(VitalApp app, List<GraphObject> objects, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteExpandedObjects(VitalTransaction transaction, VitalApp app, List<GraphObject> objects, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	
	// Import and Export
	
	/**
	 * Imports the data in bulk mode.
	 * In case of indexeddb endpoint it only writes data to database, index is untouched (needs refresh).
	 * Import fails if there's any active transaction.
	 * @param app
	 * @param segment
	 * @param inputStream
	 * @return operation status
	 * @throws VitalServiceUnimplementedException
	 * @throws VitalServiceException
	 */
	public VitalStatus bulkImport(VitalApp app, VitalSegment segment, InputStream inputStream) throws VitalServiceUnimplementedException, VitalServiceException
    
	/**
	 * Imports the data in bulk mode.
	 * In case of indexeddb endpoint it only writes data to database, index is untouched (needs refresh).
	 * Import fails if there's any active transaction.
	 * @param app
	 * @param segment
	 * @param inputStream
     * @param datasetURI, null - no dataset, empty string - don't set provenance, other value set it 
	 * @return operation status
	 * @throws VitalServiceUnimplementedException
	 * @throws VitalServiceException
	 */
	public VitalStatus bulkImport(VitalApp app, VitalSegment segment, InputStream inputStream, String datasetURI) throws VitalServiceUnimplementedException, VitalServiceException
	
	/**
	 * Exports the data in bulk mode.
	 * Export fails if there's any active transaction.
	 * @param app
	 * @param segment
	 * @param outputStream
	 * @return operation status
	 * @throws VitalServiceUnimplementedException
	 * @throws VitalServiceException
	 */
	public VitalStatus bulkExport(VitalApp app, VitalSegment segment, OutputStream outputStream) throws VitalServiceUnimplementedException, VitalServiceException
    
	/**
	 * Exports the data in bulk mode.
	 * Export fails if there's any active transaction.
	 * @param app
	 * @param segment
	 * @param outputStream
	 * @param datasetURI, may be null
	 * @return operation status
	 * @throws VitalServiceUnimplementedException
	 * @throws VitalServiceException
	 */
	public VitalStatus bulkExport(VitalApp app, VitalSegment segment, OutputStream outputStream, String datasetURI) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// Events
	
	public VitalStatus sendEvent(VitalApp app, VITAL_Event event, boolean waitForDelivery) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus sendEvents(VitalApp app, List<VITAL_Event> events, boolean waitForDelivery) throws VitalServiceUnimplementedException, VitalServiceException
			
	
	// Files
	
	public VitalStatus uploadFile(VitalApp app, URIProperty uri, String fileName, InputStream inputStream, boolean overwrite) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus downloadFile(VitalApp app, URIProperty uri, String fileName, OutputStream outputStream, boolean closeOutputStream) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus fileExists(VitalApp app, URIProperty uri, String fileName) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteFile(VitalApp app, URIProperty uri, String fileName) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList listFiles(VitalApp app, String filepath) throws VitalServiceUnimplementedException, VitalServiceException
	

    
    //named databases
    public ResultList listDatabaseConnections(VitalApp app) throws VitalServiceUnimplementedException, VitalServiceException
    
    public VitalStatus addDatabaseConnection(VitalApp app, DatabaseConnection connection) throws VitalServiceUnimplementedException, VitalServiceException
    
    public VitalStatus removeDatabaseConnection(VitalApp app, String databaseName) throws VitalServiceUnimplementedException, VitalServiceException
    

    
    /**
     * @return name of this service instance
     */
    public String getName()

    /**
     * Returns a segment with given ID or <code>null</code> if not found
     * @param app
     * @param segmentID
     * @return
     * @throws VitalServiceUnimplementedException
     * @throws VitalServiceException
     */
    public VitalSegment getSegment(VitalApp app, String segmentID) throws VitalServiceUnimplementedException, VitalServiceException
    
    /**
     * Returns an app with given ID or <code>null</code> if not found
     * @param appID
     * @return
     * @throws VitalServiceUnimplementedException
     * @throws VitalServiceException
     */
    public VitalApp getApp(String appID) throws VitalServiceUnimplementedException, VitalServiceException

    
    /**
     * Adds a new vitalservice key
     * @param app
     * @param serviceKey
     * @return
     * @throws VitalServiceUnimplementedException
     * @throws VitalServiceException
     */
    public VitalStatus addVitalServiceKey(VitalApp app, VitalServiceKey serviceKey) throws VitalServiceUnimplementedException, VitalServiceException
  
    /**
     * Removes a vitalservice key
     * @param app
     * @param serviceKey
     * @return
     * @throws VitalServiceUnimplementedException
     * @throws VitalServiceException
     */
    public VitalStatus removeVitalServiceKey(VitalApp app, VitalServiceKey serviceKey) throws VitalServiceUnimplementedException, VitalServiceException  

    /**
     * Lists vitalservice keys
     * @param app
     * @return
     * @throws VitalServiceUnimplementedException
     * @throws VitalServiceException
     */
    public List<VitalServiceKey> listVitalServiceKeys(VitalApp app) throws VitalServiceUnimplementedException, VitalServiceException

}


