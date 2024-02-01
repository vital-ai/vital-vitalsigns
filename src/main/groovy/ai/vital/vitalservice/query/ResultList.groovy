package ai.vital.vitalservice.query


import ai.vital.lucene.model.LuceneSegment;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Container

import ai.vital.vitalsigns.model.container.GraphObjectsIterable;

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;
import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_HyperNode;


public class ResultList implements Serializable, GraphObjectsIterable {

	private static final long serialVersionUID = 987654321123123123L;
	
	List<ResultElement> results = []
	
	Integer totalResults
	
	Integer offset
	
	Integer limit

	VitalStatus status = VitalStatus.withOK()
	
    QueryStats queryStats

    //for VitalGraphQuery results only, contains the order of bound variables
    List<String> bindingNames
    	
	public GraphObject first() {
		if(results.size() > 0) return results[0].graphObject
		return null
	}
	
	@Override
	public Iterator<GraphObject> iterator() {

		List<GraphObject> l = new ArrayList<GraphObject>(results.size())
		for(ResultElement e : results) {
			l.add(e.graphObject)
		}
		
		return l.iterator()
				
	}
	
	@Override
	public <T extends GraphObject> Iterator<T> iterator(Class<T> cls,
			boolean strict) {

		List<T> objects = []
		
		for(ResultElement e : this.results) {
			
			GraphObject g = e.graphObject;
			
			if(strict && g.getClass().equals(cls)) {
				
				objects.add(g)
				
			} else if(!strict && cls.isInstance(g)) {
			
				objects.add(g)
				
			}
			
		}
			
		return objects.iterator();
	}
			
	@Override
	public <T extends GraphObject> Iterator<T> iterator(Class<T> cls) {
		return this.iterator(cls, false);
	}
	
	@Override
	public GraphObject get(String uri) {
		for(ResultElement e : results) {
			if(e.graphObject.URI == uri) {
				return e.graphObject
			}
		}
		return null;
	}
	
	@Override
	public boolean isEdgeIndexEnabled() {
		return false;
	}
	@Override
	public Map<String, Set<String>> getSrcURI2Edge() {
		return null;
	}
	@Override
	public Map<String, Set<String>> getDestURI2Edge() {
		return null;
	}
	@Override
	public Map<String, Set<String>> getSrcURI2HyperEdge() {
		return null;
	}
	@Override
	public Map<String, Set<String>> getDestURI2HyperEdge() {
		return null;
	}	

	@Override
	public LuceneSegment getLuceneSegment() {
		throw new RuntimeException("ResultList is not queryable - use a different container");
	}
    
    public VITAL_Container toContainer() {
        return this.toContainer(false)
    }
    
    public VITAL_Container toContainer(boolean queryable) {
        
        VITAL_Container c = new VITAL_Container(queryable)
        for(ResultElement e : results) {
            c.putGraphObject(e.graphObject)
        }
        
        return c
        
    }
    
    
    public List<GraphObject> getGraphRoots() {
        
        List<GraphObject> gos = []
        
        //look for top objects, objects that don't have
        Set<String> nonTopObjects = new HashSet<String>()
        
        for(ResultElement e : results) {
            GraphObject g = e.graphObject
            if(g instanceof VITAL_Edge) {
                VITAL_Edge edge = g;
                nonTopObjects.add(edge.destinationURI)
            } else if(g instanceof VITAL_HyperEdge) {
                VITAL_HyperEdge hedge = g;
                nonTopObjects.add(hedge.destinationURI)
            }
        }
        
        for(ResultElement e : results) {
            GraphObject g = e.graphObject
            if(g instanceof VITAL_Node || g instanceof VITAL_HyperNode) {
                if(!nonTopObjects.contains(g.URI)) {
                    gos.add(g)
                }
            }
        }
        
        return gos
        
    }

    public void addResult(GraphObject g) {
        addResult(g, 1d)
    }
    
    public void addResult(GraphObject g, double score) {
        results.add(new ResultElement(g, score))
    }
    
    
}
