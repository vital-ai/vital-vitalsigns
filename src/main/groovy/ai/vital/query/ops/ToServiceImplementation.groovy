package ai.vital.query.ops;

import java.util.ArrayList;
import java.util.List;

import groovy.lang.GString;
import ai.vital.query.Query;
import ai.vital.query.ToQueryImplementation;
import ai.vital.vitalservice.BaseDowngradeUpgradeMapping;
import ai.vital.vitalservice.BaseDowngradeUpgradeOptions;
import ai.vital.vitalservice.BaseImportExportOptions;
import ai.vital.vitalservice.DowngradeMapping;
import ai.vital.vitalservice.DowngradeOptions;
import ai.vital.vitalservice.DropMapping;
import ai.vital.vitalservice.ExportOptions;
import ai.vital.vitalservice.FileType;
import ai.vital.vitalservice.ImportOptions;
import ai.vital.vitalservice.ServiceDeleteOperation;
import ai.vital.vitalservice.ServiceInsertOperation;
import ai.vital.vitalservice.ServiceOperations;
import ai.vital.vitalservice.UpgradeMapping;
import ai.vital.vitalservice.UpgradeOptions;
import ai.vital.vitalservice.ServiceOperations.Type;
import ai.vital.vitalservice.ServiceUpdateOperation;
import ai.vital.vitalservice.query.VitalGraphQuery;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VitalSegment;
import ai.vital.vitalsigns.model.property.URIProperty;

public class ToServiceImplementation {

	
	public ServiceOperations toService(Query query) {
		
		if( query.getGraph() != null || query.getSelect() != null || query.getPath() != null || query.getInstantiates() != null) ex("toService cannot accept GRAPH, SELECT, INSTANTIATE or PATH query object");
		
		ServiceOperations ops = new ServiceOperations();
		
		int c = 0;
		if(query.getDelete() != null) c++;
		if(query.getInsert() != null) c++;
		if(query.getUpdate() != null) c++;
		if(query.get_import() != null) c++;
		if(query.getExport() != null) c++;
		if(query.getDowngrade() != null) c++;
		if(query.getUpgrade() != null) c++;
		
//		if(query.getInstantiate() != null) c++;
		
		if(c == 0) ex("No service operations node set, exactly one of DELETE, INSERT, UPDATE, IMPORT, EXPORT, DOWNGRADE, UPGRADE expected");
		if(c > 1) ex("More than 1 service operations node set, exactly one of DELETE, INSERT, UPDATE, IMPORT, EXPORT, DOWNGRADE, UPGRADE expected, found: " +
				( query.getDelete() != null ? "DELETE " : "" ) +
				( query.getInsert() != null ? "INSERT " : "" ) +
				( query.getUpdate() != null ? "UPDATE " : "" ) +
				( query.get_import() != null ? "IMPORT" : "" ) +
				( query.getExport() != null ? "EXPORT" : "")
//				( query.getInstantiate() != null ? "INSTANTIATE" : "")
				);
		

		boolean transaction = false;
		
		Type type = null;
		
		ops.setDomainsList(query.getDomainsList());
		
		ops.setOtherClasses(query.getOtherClasses());
		
		if(query.getDelete() != null) {
			
			transaction = query.getDelete().isTransaction();
			
			type = Type.DELETE;
			
			handleDelete(ops, query, query.getDelete());
			
		} else if(query.getInsert() != null) {
			
			transaction = query.getInsert().isTransaction();
			
			if(query.getInsert().getSegment() == null) ex("No segment set in INSERT");
			
			type = Type.INSERT;
			
			handleInsert(ops, query.getInsert());
			
		} else if(query.getUpdate() != null) {
			
			transaction = query.getUpdate().isTransaction();
			
			if(query.getUpdate().getSegment() == null) ex("No segment set in UPDATE");
			
			type = Type.UPDATE;
			
			handleUpdate(ops, query.getUpdate());
			
		} else if(query.get_import() != null) {
			
			IMPORT _import = query.get_import();
			
			type = Type.IMPORT;
			

			VitalSegment segment = _import.getSegment();
			if(segment == null) throw new RuntimeException("No 'segment' attribute");
			ops.setSegment(segment);
			
			handleImport(ops, _import);
			
		} else if(query.getExport() != null) {
			
			type = Type.EXPORT;

			VitalSegment segment = query.getExport().getSegment();
			if(segment == null) throw new RuntimeException("No 'segment' attribute");
			ops.setSegment(segment);
			
			handleExport(ops, query.getExport());
			
		} else if(query.getDowngrade() != null) {
		    
		    type = Type.DOWNGRADE;
		    
		    DOWNGRADE downgrade = query.getDowngrade();
		    
		    handleDowngrade(ops, downgrade);
		    
		} else if(query.getUpgrade() != null) {
			
		    type = Type.UPGRADE;
		    
		    UPGRADE upgrade = query.getUpgrade();
		    
		    handleUpgrade(ops, upgrade);
		    
		} else {
			ex("Unhandled case!");
		}
		
		ops.setTransaction(transaction);
		ops.setType(type);
		
		
		return ops;
		
		
	}
	
	private void handleUpgrade(ServiceOperations ops, UPGRADE upgrade) {

	    UpgradeOptions uops = new UpgradeOptions();
	    setBaseUpgradeDowngradeOptions(upgrade, uops);
	    
	    List<UpgradeMapping> ums = new ArrayList<UpgradeMapping>();
	    for(UpgradeDef ud : upgrade.getUpgradeDefs()) {
	        UpgradeMapping um = new UpgradeMapping();
	        setMappingProps(ud, um);
	        checkDropClass(um.getOldClass(), uops, false);
	        checkDropClass(um.getNewClass(), uops, true);
	        ums.add(um);
	    }
	    
	    uops.setUpgradeMappings(ums);
	    ops.setUpgradeOptions(uops);
	    ops.setParsed(true);
        
	    
	    
    }

    private void checkDropClass(Class<? extends GraphObject> oldClass,
            BaseDowngradeUpgradeOptions ops, boolean newNotOld) {

        for(DropMapping dm : ops.getDropMappings()) {
            
            if(oldClass.equals(dm.getDropClass())) {
                throw new RuntimeException("OldClass: " + oldClass + " also appears in a drop block");
            }
            
        }
        
        
    }

    private void setMappingProps(UpgradeDowngradeDefBase ud, BaseDowngradeUpgradeMapping um) {

        um.setClosure(ud.getClosure());
        um.setNewClass(ud.getNewClass());
        um.setOldClass(ud.getOldClass());
        
    }

    private void setBaseUpgradeDowngradeOptions(UPGRADEDOWNGRADEBase udb,
            BaseDowngradeUpgradeOptions uops) {

        uops.setDestinationPath(udb.getDestinationPath());
        uops.setDestinationSegment(udb.getDestinationSegment());
        uops.setOldOntologyFileName(udb.getOldOntologyFileName());
        uops.setOldOntologiesDirectory(udb.getOldOntologiesDirectory());
        uops.setDomainJars(udb.getDomainJars());
        uops.setSourcePath(udb.getSourcePath());
        uops.setSourceSegment(udb.getSourceSegment());
        uops.setDeleteSourceSegment(udb.isDeleteSourceSegment());
        
        List<DropMapping> dms = new ArrayList<DropMapping>();
        for(DropDef dd : udb.getDropDefs()) {
            DropMapping dm = new DropMapping();
            dm.setDropClass(dd.getDropClass());
            dms.add(dm);
        }
        uops.setDropMappings(dms);
        
    }

    private void handleDowngrade(ServiceOperations ops, DOWNGRADE downgrade) {

        DowngradeOptions dops = new DowngradeOptions();
        setBaseUpgradeDowngradeOptions(downgrade, dops);
        
        List<DowngradeMapping> dms = new ArrayList<DowngradeMapping>();
        for(DowngradeDef dd : downgrade.getDowngradeDefs()) {
            DowngradeMapping dm = new DowngradeMapping();
            setMappingProps(dd, dm);
            checkDropClass(dm.getOldClass(), dops, false);
            checkDropClass(dm.getNewClass(), dops, true);
            dms.add(dm);
        }
        
        dops.setDowngradeMappings(dms);
        ops.setDowngradeOptions(dops);
        ops.setParsed(true);
        
    }

    private void handleImport(ServiceOperations ops, IMPORT _import) {

		ImportOptions io = new ImportOptions();

		setBaseOptions(io, _import);
		
		if(_import.getCreateSegment() != null) {
			io.setCreateSegment(_import.getCreateSegment().booleanValue());
		}
		
		if(_import.getRemoveData() != null) {
			io.setRemoveData(_import.getRemoveData().booleanValue());
		}
		
		if(_import.getReindexSegment() != null) {
			io.setReindexSegment(_import.getReindexSegment().booleanValue());
		}
		
		
		ops.setImportOptions(io);
		
	}
	
	private void handleExport(ServiceOperations ops, EXPORT export) {
		
		ExportOptions eo = new ExportOptions();
		
		setBaseOptions(eo, export);
		
		ops.setExportOptions(eo);
		
	}

	private void setBaseOptions(BaseImportExportOptions bieo, IMPORTEXPORTBase iobase) {

		String path = iobase.getPath();
		
		if(path == null || path.isEmpty()) throw new RuntimeException("No 'path' attribute");
		
		Boolean inferredCompressed = path.endsWith(".gz") ? true : null;
		FileType inferredFT = null;

		Boolean compressed = iobase.getCompressed();
		FileType ft = iobase.getFileType();
		
		if(path.endsWith(".vital") || path.endsWith(".vital.gz")) {
			inferredFT = FileType.block;
			if(path.endsWith(".vital")) {
				inferredCompressed = false;
			}
		} else if(path.endsWith(".nt") || path.endsWith(".nt.gz")) {
			inferredFT = FileType.ntriples;
			if(path.endsWith(".nt")) {
				inferredCompressed = false;
			}
		}
		
		if(inferredCompressed != null && compressed != null) {
			
			if(inferredCompressed.booleanValue() != compressed.booleanValue()) {
				throw new RuntimeException("Inferred compression [" + inferredCompressed + "] does not match set value [" + compressed + "]");
			}
			
		} else if(compressed != null) {
			bieo.setCompressed(compressed);
		} else if(inferredCompressed == null) {
			throw new RuntimeException("No inferred nor set compression flag");
		} else {
			bieo.setCompressed(inferredCompressed);
		}
		
		if(inferredFT != null && ft != null) {
			
			if(inferredFT != ft) {
				throw new RuntimeException("Inferred fileType [" + inferredFT + "] does not match set value [" + ft + "]");
			}
			
		} else if(ft != null) {
			bieo.setFileType(ft);
		} else if(inferredFT == null) {
			throw new RuntimeException("No inferred nor set file type");
		} else {
			bieo.setFileType(inferredFT);
		}

		
		bieo.setPath(path);
		
		bieo.setDatasetURI(iobase.getDatasetURI());
		
		
	}

	private void handleUpdate(ServiceOperations ops, UPDATE update) {

		if(update.getUpdates().size() < 1) ex("No 'update' nodes.");
		
		setSegment(ops, update.getSegment());
		
		for(UpdateOp updateOp : update.getUpdates()) {
			
			ServiceUpdateOperation sup = new ServiceUpdateOperation();
			
			if(updateOp.getUri() == null || updateOp.getUri().isEmpty()) ex("No update uri set");
			
			sup.setURI(URIProperty.withString(updateOp.getUri()));
			
			if( updateOp.getClosure() != null && updateOp.getInstance() != null) ex("update cannot have both closure and instance set");
			if( updateOp.getClosure() == null && updateOp.getInstance() == null) ex("update must have exactly one of closure or instance set");
			
			if( updateOp.getClosure() != null ) {
				sup.setClosure(updateOp.getClosure());
			} else if(updateOp.getInstance() != null) {
				sup.setGraphObject(updateOp.getInstance());
			}
			
			ops.getOperations().add(sup);
			
		}
		
	}

	private void setSegment(ServiceOperations ops, Object segment) {

		if(segment instanceof String || segment instanceof GString) {
			
			ops.setSegment(VitalSegment.withId(segment.toString()));
			
		} else if(segment instanceof VitalSegment) {
			
			ops.setSegment((VitalSegment) segment);
			
		} else {
			ex("Invalid type of segment node value: " + segment.getClass().getSimpleName());
		}
		
		
	}

	private void handleInsert(ServiceOperations ops, INSERT insert) {

		if(insert.getInsertions().size() < 1) ex("No 'insert' nodes.");
		
		setSegment(ops, insert.getSegment());
		
		for( InsertOp insertOp : insert.getInsertions() ) {
		
			if( insertOp.getInstance() == null ) ex("No graph object instance in an insert node");
			
			ServiceInsertOperation sio = new ServiceInsertOperation();
			sio.setGraphObject(insertOp.getInstance());

			ops.getOperations().add(sio);
			
		}
		
		
	}

	private void handleDelete(ServiceOperations ops, Query parent, DELETE delete) {
		
		if(delete.getDeleteOps().size() > 0 && delete.getTopArc() != null) ex("DELETE must not have both delete nodes and top ARC");
		
		if(delete.getDeleteOps().size() == 0 && delete.getTopArc() == null) ex("DELETE must have either delete nodes or an ARC element");
		
		if( delete.getDeleteOps().size() > 0 ) {
			
			for(DeleteOp d : delete.getDeleteOps()) {

				if(d.getUri() == null) ex("No uri set in 'delete' node"); 
				
				ServiceDeleteOperation dop = new ServiceDeleteOperation();
				dop.setGraphObjectURI(URIProperty.withString(d.getUri()));
				
				ops.getOperations().add(dop);
			}
			
		} else {
			
			VitalGraphQuery vgq = (VitalGraphQuery) new ToQueryImplementation().toQuery(parent);
			
			ServiceDeleteOperation dop = new ServiceDeleteOperation();
			dop.setGraphQuery(vgq);
			
			ops.getOperations().add(dop);
			
		}
		
	}

	private void ex(String m) { throw new RuntimeException(m); }
	
}
