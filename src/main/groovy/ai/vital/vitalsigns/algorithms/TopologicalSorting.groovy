package ai.vital.vitalsigns.algorithms


import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_Container
import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_Edge;

/**
 * Topological sorting
 * https://en.wikipedia.org/wiki/Topological_sorting
 * 
 * Modified version of http://stackoverflow.com/questions/2739392/sample-directed-graph-and-topological-sort-code
 *
 */

public class TopologicalSorting {

    static class Node {
        public final String name
        public final HashSet<Edge> inEdges
        public final HashSet<Edge> outEdges

        public Node(String name) {
            this.name = name
            inEdges = new HashSet<Edge>()
            outEdges = new HashSet<Edge>()
        }

        public Node addEdge(Node node) {
            Edge e = new Edge(this, node)
            outEdges.add(e)
            node.inEdges.add(e)
            return this
        }

        @Override
        public String toString() {
            return name
        }
    }

    static class Edge {
        public final Node from
        public final Node to

        public Edge(Node from, Node to) {
            this.from = from
            this.to = to
        }

        @Override
        public boolean equals(Object obj) {
            Edge e = (Edge) obj
            return e.from == from && e.to == to
        }
    }
    

    public static List<VITAL_Node> sort(VITAL_Container container) {
        return sort(container, null, false)
    }
    
    public static List<VITAL_Node> sort(VITAL_Container container, Class<? extends VITAL_Edge> optionalEdgeTypeFilter, boolean exactType) {
        
        // L <- Empty list that will contain the sorted elements
        ArrayList<VITAL_Node> L = new ArrayList<VITAL_Node>()
        
        List<VITAL_Node> allNodes = new ArrayList<VITAL_Node>()
        List<VITAL_Edge> allEdges = new ArrayList<VITAL_Edge>()
        
        Map<String, List<VITAL_Edge>> nodeOutgoingEdges = new HashMap<String, List<VITAL_Edge>>()
        Map<String, List<VITAL_Edge>> nodeIncomingEdges = new HashMap<String, List<VITAL_Edge>>()
        
        for(Iterator<GraphObject> iter = container.iterator(); iter.hasNext();  ) {
      
            GraphObject g = iter.next()
            
            if(g instanceof VITAL_Node) {
                
                allNodes.add((VITAL_Node)g)
                
                nodeOutgoingEdges.put(g.getURI(), new ArrayList<VITAL_Edge>())
                nodeIncomingEdges.put(g.getURI(), new ArrayList<VITAL_Edge>())
                
            } else if(g instanceof VITAL_Edge) {

                boolean passed = true
                
                if(optionalEdgeTypeFilter != null) {
                    
                    if(exactType) {
                        
                        if(!optionalEdgeTypeFilter.equals(g.getClass())) {
                            passed = false
                        }
                        
                    } else {
                        if(!optionalEdgeTypeFilter.isAssignableFrom(g.getClass())) {
                            passed = false
                        }
                    }
                    
                }
                
                if(passed) {
                    allEdges.add((VITAL_Edge) g)
                }
                
            }
             
        }
        
        for(VITAL_Edge e : allEdges) {
            
            String src = e.getSourceURI()

            String dest = e.getDestinationURI();
                
            if(src == null) throw new RuntimeException("No edge source URI, edge: " + e)
            if(dest == null) throw new RuntimeException("No edge destination URI, edge: " + e)
                
            if(src.equals(dest)) throw new RuntimeException("Source must not be equal to destination, edge: " + e)
                
            List<VITAL_Edge> o = nodeOutgoingEdges.get(src)
            if(o == null) throw new RuntimeException("Source node not found: " + src + " " + e)
            o.add(e)
                
            List<VITAL_Edge> i = nodeIncomingEdges.get(dest)
            if(i == null) throw new RuntimeException("Destination node not found:" + dest + " "  + e)
            i.add(e)
            
        }
        
        
        // S <- Set of all nodes with no incoming edges
        HashSet<VITAL_Node> S = new HashSet<VITAL_Node>()
        
        for (VITAL_Node n : allNodes) {
            
            if( nodeIncomingEdges.get(n.getURI()).size() == 0 ) {
                S.add(n)
            }
            
        }
        
        
        // while S is non-empty do
        while (!S.isEmpty()) {
            // remove a node n from S
            VITAL_Node n = S.iterator().next()
            S.remove(n)

            // insert n into L
            L.add(n)

            // for each node m with an edge e from n to m do
            
            for (Iterator<VITAL_Edge> it = nodeOutgoingEdges.get(n.getURI()).iterator(); it.hasNext();) {
                // remove edge e from the graph
                VITAL_Edge e = it.next()
                VITAL_Node m = (VITAL_Node) container.get(e.getDestinationURI())
                it.remove() // Remove edge from n
                
                nodeIncomingEdges.get(e.getDestinationURI()).remove(e) // Remove edge from m

                // if m has no other incoming edges then insert m into S
                if (nodeIncomingEdges.get(m.getURI()).isEmpty()) {
                    S.add(m)
                }
            }
        }
        
        // Check to see if all edges are removed
        boolean cycle = false

        for (VITAL_Node n : allNodes) {
            if (! nodeIncomingEdges.get(n.getURI()).isEmpty()) {
                cycle = true
                break
            }
        }
        
        if (cycle) {
            throw new RuntimeException("Cycle present, topological sort not possible")

        // } else {
        // println("Topological Sort: "
        // + Arrays.toString(L.toArray()))
        }

        return L
    }
}