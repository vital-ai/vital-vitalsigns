package ai.vital.vitalservice

class BaseImportExportOptions implements Serializable {

	private static final long serialVersionUID = 1L;
	
	String path
	
	FileType fileType = null
	
	Boolean compressed = null
    
    //don't override by default
    String datasetURI = ''
	
}
