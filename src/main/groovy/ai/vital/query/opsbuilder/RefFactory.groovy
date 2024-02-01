package ai.vital.query.opsbuilder

import ai.vital.query.ops.INSERT;
import ai.vital.query.ops.INSTANTIATE;
import ai.vital.query.ops.InsertOp;
import ai.vital.query.ops.InstanceOp;
import ai.vital.query.ops.Ref;
import ai.vital.query.ops.UPDATE;
import ai.vital.query.ops.UpdateOp;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class RefFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {

		if(value == null) throw new InstantiationException("${name} requires string value ('name')");
	
		if(!(name instanceof String || name instanceof GString)) throw new InstantiationException("${name} value must be a String")
		
		Ref ref = attributes != null ? new Ref(attributes) : new Ref()
		
		ref.name = value
		
		return ref;
	}
	
	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

			
		//check if it's valid
		Ref ref = (Ref)child
		if(!ref.name) throw new RuntimeException("No name set in ref node")
		
		//XXX bug!
			
		/*
		InsertOp ins = null
			
		if(parent instanceof InsertOp) {
			ins = parent
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
			
		
		

		if(ins.provides == ref.name) throw new RuntimeException("Cannot self-reference provided name")
		*/
			
		if(parent instanceof INSERT) {
			
			INSERT insert = parent
			
			InsertOp foundRef = null
			
			for(InsertOp op : insert.insertions) {
				
				if(ref.name == op.provides) {
					foundRef = op
					break
				}
				
			}
			
			if(foundRef == null) throw new RuntimeException("Referenced name not found: ${ref.name}");
			
			ref.referencedURI = foundRef.instance.URI
			
		} else if(parent instanceof UPDATE) {
		
			UPDATE update = parent
			
			UpdateOp foundRef = null;
			
			for(UpdateOp op : update.updates) {
				
				if(ref.name == op.provides) {
					foundRef = op
					break
				}
				
			}
			
			if(foundRef == null) throw new RuntimeException("Referenced name not found: ${ref.name}");
			
			ref.referencedURI = foundRef.uri
		
		} else if(parent instanceof INSTANTIATE) {
        
            INSTANTIATE inst = parent
            
            InstanceOp foundRef = null;
            
            for(InstanceOp op : inst.instances) {
                
                if(ref.name == op.provides) {
                    foundRef = op
                    break
                }
                
            }
            
            if(foundRef == null) throw new RuntimeException("Referenced name not found: ${ref.name}");
            
            ref.referencedURI = foundRef.instance.URI
            
		} else {
			throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
		}
		
	}
	
	@Override
	public boolean isLeaf() {
		return true
	}

	
}
