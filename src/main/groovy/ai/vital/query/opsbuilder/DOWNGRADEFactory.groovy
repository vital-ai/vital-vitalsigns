package ai.vital.query.opsbuilder

import java.util.Map

import ai.vital.query.Query;
import ai.vital.query.ops.DOWNGRADE;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class DOWNGRADEFactory extends AbstractFactory {

    public Object newInstance(FactoryBuilderSupport builder,
            Object name, Object value, Map attributes
    ) throws InstantiationException, IllegalAccessException {
    
        if(attributes != null) {
            return new DOWNGRADE(attributes)
        } else {
            return new DOWNGRADE()
        }
    }

    @Override
    public void setParent(FactoryBuilderSupport builder, Object parent,
            Object child) {
        if(parent instanceof Query) {
            Query q = parent
            if(q.downgrade != null) throw new RuntimeException("Exactly 1 DOWNGRADE element expected")
            q.downgrade = ((DOWNGRADE)child)
        } else {
            throw new RuntimeException("Unexpected parent of ${child.class.simpleName}: ${parent.class.simpleName}")
        }
    }
            
}