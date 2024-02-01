package ai.vital.query.querybuilder

import ai.vital.query.Query
import ai.vital.query.SPARQL
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

import java.util.Map;

class SPARQLFactory extends AbstractFactory {

    @Override
    public Object newInstance(FactoryBuilderSupport builder,
            Object name, Object value, Map attributes
    ) throws InstantiationException, IllegalAccessException {
    
        if(name != "SPARQL") throw new RuntimeException("Expected node with name: SPARQL")
        
        if(attributes != null) {
            return new SPARQL(attributes)
        } else {
            return new SPARQL()
        }
    
    }
    
    @Override
    public void setParent(FactoryBuilderSupport builder,
            Object parent, Object s) {
        if(parent instanceof Query) {
            ((Query)parent).sparql = (SPARQL) s
        } else {
            throw new RuntimeException("Unexecpted parent of SPARQL - ${parent.class}")
        }
    }
    
}
