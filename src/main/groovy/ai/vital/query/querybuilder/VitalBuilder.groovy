package ai.vital.query.querybuilder


import java.util.Map;

import ai.vital.query.QueryString;
import ai.vital.query.ops.DowngradeDef;
import ai.vital.query.opsbuilder.DELETEFactory;
import ai.vital.query.opsbuilder.DOWNGRADEFactory
import ai.vital.query.opsbuilder.DeleteOpFactory;
import ai.vital.query.opsbuilder.DowngradeDefFactory
import ai.vital.query.opsbuilder.DropDefFactory
import ai.vital.query.opsbuilder.EXPORTFactory
import ai.vital.query.opsbuilder.IMPORTFactory
import ai.vital.query.opsbuilder.INSERTFactory;
import ai.vital.query.opsbuilder.INSTANTIATEFactory
import ai.vital.query.opsbuilder.InsertOpFactory;
import ai.vital.query.opsbuilder.InstanceOpFactory
import ai.vital.query.opsbuilder.RefFactory;
import ai.vital.query.opsbuilder.UPDATEFactory;
import ai.vital.query.opsbuilder.UPGRADEFactory
import ai.vital.query.opsbuilder.UpdateOpFactory;
import ai.vital.query.opsbuilder.UpgradeDefFactory
import ai.vital.vitalsigns.model.VITAL_Query;
import groovy.lang.Closure;
import groovy.util.FactoryBuilderSupport



class VitalBuilder extends FactoryBuilderSupport {
	
	public VitalBuilder(boolean init = true) {
		super(init)
	}

	public QueryString queryString(String query) {
		QueryString q = new QueryString()
		q.query = query
		return q
	}
	
	public QueryString queryString(VITAL_Query queryObject) {
		QueryString q = new QueryString()
		q.query = queryObject.queryString
		return q
	}
	
//	@Override
//	protected void postInstantiate(Object arg0, Map arg1, Object arg2) {
//		println "Post instantiate ${arg0}, ${arg1}, ${arg2}"
//		super.postInstantiate(arg0, arg1, arg2);
//	}

	def registerObjectFactories() {
		
		registerFactory("queryString", new QueryStringFactory())
		
		registerFactory("query", new QueryFactory())
		
		registerFactory("SELECT", new SELECTFactory())
		registerFactory("GRAPH", new GRAPHFactory())
		
		registerFactory("ARC", new ARCFactory())
		
		registerFactory("ARC_OR", new ARC_ORFactory())
		registerFactory("ARC_AND", new ARC_ANDFactory())
		registerFactory("AND", new ANDFactory())
		registerFactory("OR", new ORFactory())
		registerFactory("constraint", new ConstraintFactory())
		registerFactory("node_constraint", new NodeConstraintFactory())
		registerFactory("edge_constraint", new EdgeConstraintFactory())
		registerFactory("provides", new ProvidesFactory())
		registerFactory("node_provides", new NodeProvidesFactory())
		registerFactory("edge_provides", new EdgeProvidesFactory())
		registerFactory("node", new NodeFactory())
		registerFactory("edge", new EdgeFactory())
		registerFactory("eval", new EvalFactory())
		
		registerFactory("hypernode_constraint", new HyperNodeConstraintFactory())
		registerFactory("hyperedge_constraint", new HyperEdgeConstraintFactory())
		
		registerFactory("hyperedge_provides", new HyperEdgeProvidesFactory())
		registerFactory("hypernode_provides", new HyperNodeProvidesFactory())
		
		registerFactory("HYPER_ARC", new HYPER_ARCFactory())
		
		
		registerFactory("connector", new ConnectorFactory())
		
		registerFactory("target", new TargetFactory())
		
		registerFactory("source", new SourceFactory())
		
		registerFactory("value", new ValueFactory())
		
		registerFactory("bind", new BindFactory())
		
		registerFactory("node_bind", new NodeBindFactory())
		
		registerFactory("edge_bind", new EdgeBindFactory())
		
		registerFactory("start_path", new StartPathFactory())
		
		registerFactory("end_path", new EndPathFactory())
		
		
		registerFactory("DISTINCT", new DistinctFactory())
		
		registerFactory("AVERAGE", new AggregateFunctionFactory())
		registerFactory("COUNT", new AggregateFunctionFactory())
		registerFactory("COUNT_DISTINCT", new AggregateFunctionFactory())
		registerFactory("MAX", new AggregateFunctionFactory())
		registerFactory("MIN", new AggregateFunctionFactory())
		registerFactory("SUM", new AggregateFunctionFactory())
		
		registerFactory("SORT_BY", new SortByFactory())
		
		registerFactory("FIRST", new FirstLastFactory())
		registerFactory("LAST", new FirstLastFactory())
		
		
		
		registerFactory("ref", new RefFactory())
		
		registerFactory("INSERT", new INSERTFactory())
		registerFactory("insert", new InsertOpFactory())
		
		registerFactory("UPDATE", new UPDATEFactory())
		registerFactory("update", new UpdateOpFactory())
		
		registerFactory("DELETE", new DELETEFactory())
		registerFactory("delete", new DeleteOpFactory())
		
		registerFactory("PATH", new PATHFactory())
		registerFactory("ROOT", new ARCFactory())
		
		
		registerFactory("IMPORT", new IMPORTFactory())
		registerFactory("EXPORT", new EXPORTFactory())
        
        
        registerFactory("INSTANTIATE", new INSTANTIATEFactory())
        registerFactory("instance", new InstanceOpFactory())
        
        
        registerFactory("DOWNGRADE", new DOWNGRADEFactory())
        registerFactory("downgrade", new DowngradeDefFactory())
        
        registerFactory("UPGRADE", new UPGRADEFactory())
        registerFactory("upgrade", new UpgradeDefFactory())
        
        registerFactory("drop", new DropDefFactory())
        
        //raw queries
        registerFactory("SPARQL", new SPARQLFactory())
        registerFactory("SQL", new SQLFactory())
        
	}
	
}
