package ai.vital.vitalservice

import java.util.List;

class BaseDowngradeUpgradeOptions implements Serializable {

    private static final long serialVersionUID = 1L;
    
    String oldOntologyFileName
    
    //only for non-vital-home case
    String oldOntologiesDirectory;
    
    List<String> domainJars
    
    String sourcePath
    
    String sourceSegment
    
    String destinationPath
    
    String destinationSegment
    
    boolean deleteSourceSegment = false
    
    List<DropMapping> dropMappings = []
    
}
