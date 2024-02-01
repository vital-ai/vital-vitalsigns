package ai.vital.query.ops

import java.util.List;

abstract class UPGRADEDOWNGRADEBase {

    String oldOntologyFileName
    
    String oldOntologiesDirectory
    
    List<String> domainJars
    
    String sourcePath
    
    String sourceSegment
    
    String destinationPath
    
    String destinationSegment
    
    boolean deleteSourceSegment = false
    
    List<DropDef> dropDefs = []
    
}
