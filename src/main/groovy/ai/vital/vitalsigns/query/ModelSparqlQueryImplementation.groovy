package ai.vital.vitalsigns.query


import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalSparqlQuery;
import ai.vital.vitalsigns.model.RDFStatement;
import ai.vital.vitalsigns.model.SparqlAskResponse
import ai.vital.vitalsigns.model.SparqlBinding;
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.properties.Property_hasRdfObject;
import ai.vital.vitalsigns.model.properties.Property_hasRdfPredicate;
import ai.vital.vitalsigns.model.properties.Property_hasRdfSubject;
import ai.vital.vitalsigns.model.properties.Property_isPositiveResponse;
import ai.vital.vitalsigns.utils.StringUtils;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class ModelSparqlQueryImplementation {

	public static ResultList handleSparqlQuery(Model model, VitalSparqlQuery sparqlQuery) {
		
		ResultList rl = new ResultList();
		
		if(StringUtils.isEmpty( sparqlQuery.getSparql() ) ) throw new RuntimeException("sparql string not set in query");
		
		if(sparqlQuery.isReturnSparqlString()) {
			rl.setStatus(VitalStatus.withOKMessage(sparqlQuery.getSparql()));
			return rl;
		}
		
		
		Query queryObj = QueryFactory.create(sparqlQuery.getSparql());
		
		QueryExecution execution = QueryExecutionFactory.create(queryObj, model);
		
		if(queryObj.isAskType()) {
		
			boolean res = execution.execAsk();
			
			SparqlAskResponse askResponse = new SparqlAskResponse();
			askResponse.generateURI((VitalApp)null);
			askResponse.set( Property_isPositiveResponse.class, res );
			
			rl.getResults().add(new ResultElement(askResponse, 1D));
			
		} else if(queryObj.isConstructType()) {
		
			Model outputModel = execution.execConstruct();
			
			for(StmtIterator stmtIter = outputModel.listStatements(); stmtIter.hasNext(); ) {
				
				Statement stmt = stmtIter.next();
				
				RDFStatement s = (RDFStatement) new RDFStatement().generateURI((VitalApp)null);
				s.set( Property_hasRdfSubject.class, VitalNTripleWriter.escapeRDFNode(stmt.getSubject()) );
				s.set( Property_hasRdfPredicate.class, VitalNTripleWriter.escapeRDFNode(stmt.getPredicate()) );
				s.set( Property_hasRdfObject.class, VitalNTripleWriter.escapeRDFNode(stmt.getObject()) );
				
				rl.getResults().add(new ResultElement(s, 1D));
				
			}
		
		} else if(queryObj.isSelectType()) {
		
			ResultSet rs = execution.execSelect();
			
			while( rs.hasNext() ) {
				
				QuerySolution solution = rs.nextSolution();

				SparqlBinding b = (SparqlBinding) new SparqlBinding().generateURI((VitalApp)null);
				
				for(Iterator<String> iter = solution.varNames(); iter.hasNext(); ) {
					
					String  vn = iter.next();
					
					RDFNode n = solution.get(vn);
					
					if(n != null) {


						b.setProperty(vn, VitalNTripleWriter.escapeRDFNode(n))
						
					}
					
				}
				
				rl.getResults().add(new ResultElement(b, 1D));
				
			}
		
		} else {
			throw new RuntimeException("Unknown sparql query type: " + sparqlQuery.getSparql() );
		}
		
		rl.setTotalResults( rl.getResults().size() );
		
		return rl;
		
	}

}