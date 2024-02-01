package ai.vital.service.lucene.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ai.vital.lucene.model.LuceneSegment;
import ai.vital.vitalservice.exception.VitalServiceException;
import ai.vital.vitalservice.exception.VitalServiceUnimplementedException;
import ai.vital.vitalservice.query.QueryStats;
import ai.vital.vitalservice.query.ResultElement;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.global.GlobalHashTable;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalOrganization;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.query.PathQueryImplementation.PathQueryExecutor;

public class LucenePathQueryExecutor extends PathQueryExecutor {

	private List<LuceneSegment> segments;
    private QueryStats queryStats;

	public LucenePathQueryExecutor(VitalOrganization organization, VitalApp app, List<LuceneSegment> segments, QueryStats queryStats) {
		super(organization, app);
		this.segments = segments;
		this.queryStats = queryStats;
	}

	@Override
	public ResultList get(List<URIProperty> rootURIs)
			throws VitalServiceUnimplementedException, VitalServiceException {

		List<String> strings = new ArrayList<String>(rootURIs.size());
		for(URIProperty p : rootURIs) {
			strings.add(p.get());
		}
		
		ResultList rl = new ResultList();
		
		for(LuceneSegment s : segments) {
			
			//special case for cache segment 
			if(s.getID().equals(VitalSigns.CACHE_DOMAIN)) {
				
				for(String uri : strings) {
					
					GraphObject g = GlobalHashTable.get().get(uri);
					
					if(g != null) {
						rl.getResults().add(new ResultElement(g, 1D));
					}
					
				}
				
			} else {
				
				List<GraphObject> graphObjects = null;
                try {
                    graphObjects = s.getGraphObjectsBatch(strings, queryStats);
                } catch (IOException e) {
                    throw new VitalServiceException(e);
                }
				
				for(GraphObject g : graphObjects) {
					rl.getResults().add(new ResultElement(g, 1D));
				}
				
			}
			
		}
		
		return rl;
	}

	@Override
	public ResultList query(VitalSelectQuery rootSelect)
			throws VitalServiceUnimplementedException, VitalServiceException {
		
		return LuceneServiceQueriesImpl.selectQuery(segments, rootSelect, queryStats);
		
	}

}
