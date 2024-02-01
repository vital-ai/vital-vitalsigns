package ai.vital.query.querybuilder

import ai.vital.query.Query
import ai.vital.query.SQL
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class SQLFactory extends AbstractFactory {

    @Override
    public Object newInstance(FactoryBuilderSupport builder,
            Object name, Object value, Map attributes
    ) throws InstantiationException, IllegalAccessException {
    
        if(name != "SQL") throw new RuntimeException("Expected node with name: SQL")
        
        if(attributes != null) {
            return new SQL(attributes)
        } else {
            return new SQL()
        }
    
    }
    
    @Override
    public void setParent(FactoryBuilderSupport builder,
            Object parent, Object s) {
        if(parent instanceof Query) {
            ((Query)parent).sql = (SQL) s
        } else {
            throw new RuntimeException("Unexecpted parent of SQL - ${parent.class}")
        }
    }
    
}
