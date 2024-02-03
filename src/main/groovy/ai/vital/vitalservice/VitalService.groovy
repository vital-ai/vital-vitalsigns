package ai.vital.vitalservice

import java.io.InputStream
import java.io.OutputStream
import java.util.List
import java.util.Map

import ai.vital.vitalservice.VitalStatus
import ai.vital.vitalservice.exception.VitalServiceException
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.meta.GraphContext
import ai.vital.vitalsigns.model.DatabaseConnection;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.VitalOrganization
import ai.vital.vitalsigns.model.VitalSegment
import ai.vital.vitalsigns.model.VitalTransaction
import ai.vital.vitalservice.query.VitalGraphQuery
import ai.vital.vitalservice.query.VitalPathQuery
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalservice.query.VitalSelectQuery
import ai.vital.vitalsigns.model.VITAL_Event
import ai.vital.vitalsigns.model.container.GraphObjectsIterable
import ai.vital.vitalsigns.model.property.URIProperty;


interface VitalService {

//    public final static VitalTransaction NO_TRANSACTION = new VitalTransaction(URI: "urn:NO_TRANSACTION");
    
	// info about service connection
	
	public EndpointType getEndpointType()
	
	public VitalOrganization getOrganization()
	
	public VitalApp getApp()

	public String getDefaultSegmentName()
	
	public void setDefaultSegmentName(String defaultsegment)
	
	
	// connection status
	
	public VitalStatus validate() throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus ping() throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus close() throws VitalServiceUnimplementedException, VitalServiceException
	
	// service containers: segments, apps, organizations
	
	public List<VitalSegment> listSegments() throws VitalServiceUnimplementedException, VitalServiceException
	
    //returns segments with connected provisioning and other metadata
    public ResultList listSegmentsWithConfig() throws VitalServiceUnimplementedException, VitalServiceException
	
	// URIs
	
	public URIProperty generateURI(Class<? extends GraphObject> clazz) throws VitalServiceUnimplementedException, VitalServiceException
	
	
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
	
	public ResultList get(GraphContext graphContext, URIProperty uri) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList get(GraphContext graphContext, URIProperty uri, boolean cache) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList get(GraphContext graphContext, List<URIProperty> uris) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList get(GraphContext graphContext, List<URIProperty> uris, boolean cache) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// GraphContext is redundant here as this call only used for containers, but kept for consistency
	
	public ResultList get(GraphContext graphContext, URIProperty uri, List<GraphObjectsIterable> containers) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList get(GraphContext graphContext, List<URIProperty> uris, List<GraphObjectsIterable> containers) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	
	// delete
	public VitalStatus delete(URIProperty uri) throws VitalServiceUnimplementedException, VitalServiceException

	public VitalStatus delete(VitalTransaction transaction, URIProperty uri) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus delete(List<URIProperty> uris) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus delete(VitalTransaction transaction, List<URIProperty> uris) throws VitalServiceUnimplementedException, VitalServiceException

	public VitalStatus deleteObject(GraphObject object) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteObject(VitalTransaction transaction, GraphObject object) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteObjects(List<GraphObject> objects) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteObjects(VitalTransaction transaction, List<GraphObject> objects) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// insert, must be a new object
	
	public ResultList insert(VitalSegment targetSegment, GraphObject graphObject) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList insert(VitalTransaction transaction, VitalSegment targetSegment, GraphObject graphObject) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList insert(VitalSegment targetSegment, List<GraphObject> graphObjectsList) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList insert(VitalTransaction transaction, VitalSegment targetSegment, List<GraphObject> graphObjectsList) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// save, if create=true, create a new object if it doesn't exist
	
	public ResultList save(VitalSegment targetSegment, GraphObject graphObject, boolean create) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList save(VitalTransaction transaction, VitalSegment targetSegment, GraphObject graphObject, boolean create) throws VitalServiceUnimplementedException, VitalServiceException

	public ResultList save(VitalSegment targetSegment, List<GraphObject> graphObjectsList, boolean create) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList save(VitalTransaction transaction, VitalSegment targetSegment, List<GraphObject> graphObjectsList, boolean create) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// object(s) must already exist, determine current segment & update
	
	public ResultList save(GraphObject graphObject) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList save(VitalTransaction transaction, GraphObject graphObject) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList save(List<GraphObject> graphObjectsList) throws VitalServiceUnimplementedException, VitalServiceException
	public ResultList save(VitalTransaction transaction, List<GraphObject> graphObjectsList) throws VitalServiceUnimplementedException, VitalServiceException
		
	public VitalStatus doOperations(ServiceOperations operations) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// Functions
	
	public ResultList callFunction(String function, Map<String, Object> arguments) throws VitalServiceUnimplementedException, VitalServiceException
	
	// Query + Expand
	
	// ServiceWide
	public ResultList query(VitalQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	
	// Local
	public ResultList queryLocal(VitalQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	
	// Containers
	public ResultList queryContainers(VitalQuery query, List<GraphObjectsIterable> containers) throws VitalServiceUnimplementedException, VitalServiceException
	
	// ServiceWide
	public ResultList getExpanded(URIProperty uri, boolean cache) throws VitalServiceUnimplementedException, VitalServiceException
	
	// path query determines segments list
	// there can be a prepared default path query helper that does the same as expand
	// i.e. VitalPathQuery.getDefaultExpandQuery(List<VitalSegment> segments, VitalSelectQuery rootselector) --> VitalPathQuery
	// for the case to query against containers or local, no segments specified:
	// VitalPathQuery.getDefaultExpandQuery(VitalSelectQuery rootselector) --> VitalPathQuery
	
	
	public ResultList getExpanded(URIProperty uri, VitalPathQuery query, boolean cache) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteExpanded(URIProperty uri) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteExpanded(VitalTransaction transaction, URIProperty uri) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteExpandedObject(GraphObject object) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteExpandedObject(VitalTransaction transaction, GraphObject object) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteExpanded(URIProperty uri, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteExpanded(VitalTransaction transaction, URIProperty uri, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteExpanded(List<URIProperty> uris, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteExpanded(VitalTransaction transaction, List<URIProperty> uris, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteExpandedObjects(List<GraphObject> objects, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	public VitalStatus deleteExpandedObjects(VitalTransaction transaction, List<GraphObject> objects, VitalPathQuery query) throws VitalServiceUnimplementedException, VitalServiceException
	
	// Import and Export
	
	/**
	 * Imports the data in bulk mode. 
	 * In case of indexeddb endpoint it only writes data to database, index is untouched (needs refresh).
	 * Import fails if there's any active transaction. 
	 * @param segment
	 * @param inputStream
	 * @return operation status
	 * @throws VitalServiceUnimplementedException
	 * @throws VitalServiceException
	 */
	public VitalStatus bulkImport(VitalSegment segment, InputStream inputStream) throws VitalServiceUnimplementedException, VitalServiceException
    
	/**
	 * Imports the data in bulk mode. 
	 * In case of indexeddb endpoint it only writes data to database, index is untouched (needs refresh).
	 * Import fails if there's any active transaction. 
	 * @param segment
	 * @param inputStream
     * @param datasetURI, null - no dataset, empty string - don't set provenance, other value set it 
	 * @return operation status
	 * @throws VitalServiceUnimplementedException
	 * @throws VitalServiceException
	 */
	public VitalStatus bulkImport(VitalSegment segment, InputStream inputStream, String datasetURI) throws VitalServiceUnimplementedException, VitalServiceException

	/**
	 * Exports the data in bulk mode.
	 * Export fails if there's any active transaction.
	 * @param segment
	 * @param outputStream
	 * @return operation status
	 * @throws VitalServiceUnimplementedException
	 * @throws VitalServiceException
	 */
	public VitalStatus bulkExport(VitalSegment segment, OutputStream outputStream) throws VitalServiceUnimplementedException, VitalServiceException
    
	/**
	 * Exports the data in bulk mode.
	 * Export fails if there's any active transaction.
	 * @param segment
	 * @param outputStream
	 * @param datasetURI, null - no dataset
	 * @return operation status
	 * @throws VitalServiceUnimplementedException
	 * @throws VitalServiceException
	 */
	public VitalStatus bulkExport(VitalSegment segment, OutputStream outputStream, String datasetURI) throws VitalServiceUnimplementedException, VitalServiceException
	
	
	// Events
	
	public VitalStatus sendEvent(VITAL_Event event, boolean waitForDelivery) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus sendEvents(List<VITAL_Event> events, boolean waitForDelivery) throws VitalServiceUnimplementedException, VitalServiceException
			
	
	// Files
	
	public VitalStatus uploadFile(URIProperty uri, String fileName, InputStream inputStream, boolean overwrite) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus downloadFile(URIProperty uri, String fileName, OutputStream outputStream, boolean closeOutputStream) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus fileExists(URIProperty uri, String fileName) throws VitalServiceUnimplementedException, VitalServiceException
	
	public VitalStatus deleteFile(URIProperty uri, String fileName) throws VitalServiceUnimplementedException, VitalServiceException
	
	public ResultList listFiles(String filepath) throws VitalServiceUnimplementedException, VitalServiceException
    
    
    //named databases
    public ResultList listDatabaseConnections() throws VitalServiceUnimplementedException, VitalServiceException


    /**
     * @return name of this service instance
     */
    public String getName()
    
    /**
     * Returns a segment with given ID or <code>null</code> if not found
     * @param segmentID
     * @return
     * @throws VitalServiceUnimplementedException
     * @throws VitalServiceException
     */
    public VitalSegment getSegment(String segmentID) throws VitalServiceUnimplementedException, VitalServiceException
    
}
