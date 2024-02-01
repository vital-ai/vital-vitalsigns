package ai.vital.query

import java.util.List;

import ai.vital.query.ops.DELETE;
import ai.vital.query.ops.DOWNGRADE;
import ai.vital.query.ops.EXPORT
import ai.vital.query.ops.IMPORT
import ai.vital.query.ops.INSERT;
import ai.vital.query.ops.INSTANTIATE;
import ai.vital.query.ops.ToBlockImplementation
import ai.vital.query.ops.ToServiceImplementation;
import ai.vital.query.ops.UPDATE;
import ai.vital.query.ops.UPGRADE;
import ai.vital.vitalservice.ServiceOperations;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalservice.query.VitalGraphQuery;
import ai.vital.vitalservice.query.VitalQuery
import ai.vital.vitalsigns.block.BlockCompactStringSerializer.VitalBlock;

class Query {

	
	GRAPH graph
	
	SELECT select
	
	INSERT insert
	
	UPDATE update
	
	DELETE delete
	
    List<INSTANTIATE> instantiates
    
	PATH path
	
	EXPORT export
	
	IMPORT _import
    
    
    UPGRADE upgrade
    
    DOWNGRADE downgrade
    
    
    SPARQL sparql
    
    SQL sql
    
    
    //referenced domain URIs list
    List<String> domainsList
    
    //referenced non-domain classes
    List<String> otherClasses
    
	
	/**
	 * Unified to query method - select and graph query cases
	 * @return
	 */
	public VitalQuery toQuery() {
		return new ToQueryImplementation().toQuery(this);
	}

	public ServiceOperations toService() {
		return new ToServiceImplementation().toService(this);
	}	
    
    public List<VitalBlock> toBlock() {
        return new ToBlockImplementation(this).toBlock();
    }
}
