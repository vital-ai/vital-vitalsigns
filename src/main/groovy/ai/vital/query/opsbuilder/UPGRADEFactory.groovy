package ai.vital.query.opsbuilder

import java.util.Map

import ai.vital.query.Query;
import ai.vital.query.ops.UPGRADE;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class UPGRADEFactory extends AbstractFactory {

    public Object newInstance(FactoryBuilderSupport builder,
            Object name, Object value, Map attributes
    ) throws InstantiationException, IllegalAccessException {
    
        if(attributes != null) {
            return new UPGRADE(attributes)
        } else {
            return new UPGRADE()
        }
    }

    @Override
    public void setParent(FactoryBuilderSupport builder, Object parent,
            Object child) {
        if(parent instanceof Query) {
            Query q = parent
            if(q.upgrade != null) throw new RuntimeException("Exactly 1 UPGRADE element expected")
            q.upgrade = ((UPGRADE)child)
        } else {
            throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
        }
    }
            
}