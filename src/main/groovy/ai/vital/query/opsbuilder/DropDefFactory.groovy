package ai.vital.query.opsbuilder

import ai.vital.query.ops.DropDef
import ai.vital.query.ops.UPGRADEDOWNGRADEBase;
import ai.vital.vitalsigns.model.GraphObject;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class DropDefFactory extends AbstractFactory {

    public Object newInstance(FactoryBuilderSupport builder,
            Object name, Object value, Map attributes
    ) throws InstantiationException, IllegalAccessException {
    
        if(value == null) throw new InstantiationException("drop node requires value");
    
        if(!(value instanceof Class) || !GraphObject.class.isAssignableFrom(value) ) new InstantiationException("drop node value must be a Class<? extends GraphObject>")
        
        DropDef dd = null
        
        if(attributes != null) {
            dd = new DropDef(attributes)
        } else {
            dd = new DropDef()
        }
        
        dd.dropClass = value
        
        return dd
    }

    @Override
    public boolean isLeaf() {
        return true
    }

    @Override
    public void setParent(FactoryBuilderSupport builder, Object parent,
            Object child) {
    
         DropDef dd = child
            
         if(parent instanceof UPGRADEDOWNGRADEBase) {
             
             UPGRADEDOWNGRADEBase udb = parent;
             
             udb.dropDefs.add(dd)
             
         } else {
         
            throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
         }
            
    }
    
    
            
    

}
