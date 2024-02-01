package ai.vital.vitalservice

import ai.vital.vitalsigns.model.VitalSegment
import java.util.List;

class ServiceOperations implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static enum Type {
		DELETE,
		INSERT,
		UPDATE,
		IMPORT,
		EXPORT,
        DOWNGRADE,
        UPGRADE
	}
	
	Type type
	
	//required for insert/update only
	VitalSegment segment
	
	boolean transaction = false
	
	List<ServiceOperation> operations = []
	
	
	ImportOptions importOptions
	
	ExportOptions exportOptions
	
    
    //for serialization purposes
    String downgradeUpgradeBuilderContents
    
    //indicates whether builder contents have been parsed
    boolean parsed = false;
    
    
    DowngradeOptions downgradeOptions
    
    UpgradeOptions upgradeOptions
    
    
    //referenced domain URIs list
    List<String> domainsList
    
    //referenced non-domain classes
    List<String> otherClasses
    

}
