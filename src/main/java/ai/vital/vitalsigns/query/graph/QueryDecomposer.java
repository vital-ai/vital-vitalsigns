package ai.vital.vitalsigns.query.graph;

import java.util.ArrayList;
import java.util.List;

import ai.vital.vitalservice.query.QueryContainerType;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalservice.query.VitalGraphBooleanContainer;
import ai.vital.vitalservice.query.VitalGraphQueryContainer;
import ai.vital.vitalsigns.query.graph.QueryAnalysis.WrappedContainer;

public class QueryDecomposer {

	int arcCounter = 0;
	
//	Set<String>
	
	VitalGraphArcContainer topContainer;
	
	public QueryDecomposer(VitalGraphArcContainer topContainer) {
		this.topContainer = topContainer;
	}
	
	public List<GraphPattern> decomposeQuery() {
		
		
		//use depth first method to decompose the query into list of graph patterns
		
		
		Arc arc = new Arc();
		arc.arcContainer = topContainer;
		arc.topArc = true;
		
		arc.defaultSource = "s" + arcCounter;
		arc.defaultConnector = "c" + arcCounter;
		arc.defaultDestination = "d" + arcCounter;
		
		arcCounter++;
		
		List<WrappedContainer> splitArc = QueryAnalysis.splitArc(topContainer);
		arc.endpointContainer = splitArc.get(0);
		arc.connectorContainer = splitArc.get(1);
		
		return processArc(arc, arc.arcContainer);
		
	}

	private List<GraphPattern> processArc(Arc arc, VitalGraphQueryContainer<?> container) {

		
		//process all children and get the graph patterns
		
		List<GraphPattern> patterns = null;
		
		int bools = 0;
		int arcs = 0;
		
		QueryContainerType type = container.getType();
		
		
		for(Object c : container ) {
			
			List<GraphPattern> newList = null;
			
			if(c instanceof VitalGraphArcContainer) {
				
				arcs++;
				
				Arc subArc = new Arc();
				VitalGraphArcContainer topContainer = (VitalGraphArcContainer) c ;
				subArc.arcContainer = topContainer;
				subArc.topArc = false;
				
				
				subArc.defaultSource = "s" + arcCounter;
				subArc.defaultConnector = "c" + arcCounter;
				subArc.defaultDestination = "d" + arcCounter;
				
				arcCounter++;
				
				List<WrappedContainer> splitArc = QueryAnalysis.splitArc(topContainer);
				subArc.endpointContainer = splitArc.get(0);
				subArc.connectorContainer = splitArc.get(1);
				
				newList = processArc(subArc, topContainer);
				
			} else if(c instanceof VitalGraphBooleanContainer) {
				
				newList =  processArc(arc, (VitalGraphQueryContainer<?>) c);
				
				bools++;
				
			}
			
			if(newList != null) {
				
				if(patterns == null) {
					
					patterns = newList;
					
				} else {
					//merge based on container type
					
					if(type == QueryContainerType.and) {
						
						if(newList.size() > 0 ) {
							
							List<GraphPattern> cartesian = new ArrayList<GraphPattern>();
							
							for(GraphPattern s : patterns) {
								
								//cartesian product
								for(GraphPattern n : newList) {
									
									GraphPattern p = cloneGraphPattern(s);
									p.addAll(cloneGraphPattern(n));
									
									cartesian.add(p);
									
								}
								
							}
							
							patterns = cartesian;
							
						}
						
					} else {
						
						
						patterns.addAll(newList);
						
					}
					
				}
				
			}
			
		}
		
		if(arcs== 0 && bools == 0) {
			
			patterns = new ArrayList<GraphPattern>();
			
			GraphPattern p = new GraphPattern();
			
			Path x = new Path();
			x.add(new PathElement(arc));
			p.add(x);
			patterns.add(p);
			
			return patterns;
			
		}
		
		if(container instanceof VitalGraphArcContainer) {
			
			for(GraphPattern p : patterns) {
					
				for(Path path : p ) {
					
					if(!path.containsArc(arc)) {
						
						PathElement pe = new PathElement(arc);
						path.add(0, pe);
						
						if(path.size() > 0) {
							pe.getChildren().add(path.get(1));
						}
						
						
					}
					
				}
					
			}
			
			
		}
		
		return patterns;
		
	}

	private GraphPattern cloneGraphPattern(GraphPattern s) {
		
		GraphPattern c = new GraphPattern();
		
		for(Path p : s) {
			
			Path cp = new Path(p);
			
			c.add(cp);
			
		}
		
		return c;
	}
	
	
}
