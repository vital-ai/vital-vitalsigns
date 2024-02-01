package ai.vital.vitalsigns.meta;

import ai.vital.vitalsigns.model.GraphObject

class NodesListComparator implements java.util.Comparator<GraphObject> {

	private Map<String, Integer> node2Index;
	
	public NodesListComparator(Map<String, Integer> _node2Index) {
		this.node2Index = _node2Index;
	}
	
	@Override
	public int compare(GraphObject o1, GraphObject o2) {

		Integer i1 = node2Index.get(o1.URI);
		Integer i2 = node2Index.get(o2.URI);
		
		if(i1 == null) i1 = Integer.MAX_VALUE;
		if(i2 == null) i2 = Integer.MAX_VALUE;
		
		return i1.compareTo(i2)
	}
	
}
