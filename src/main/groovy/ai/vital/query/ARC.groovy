package ai.vital.query

// containers


// how to separate out the Node/Empty/Empty case
// from the Parent/Edge/Node case?

// the "top" has to be the node/empty/empty case
// a middle/bottom case has to be the parent/edge/node case


enum Direction {
	FORWARD, REVERSE
}

enum Capture {
	
	NONE, SOURCE, BOTH, TARGET, CONNECTOR
	
}

abstract class ARC_BASE extends Container {
	
	// forward or reverse
	String direction = "forward"
			
	// switch to this?
	Direction direction_enum = Direction.FORWARD
			
	boolean optional = false
			
	Capture capture = null
	
	Delete delete = null
	
	
	//for debug purposes only
	String label
    
    
    //path 
    boolean countArc = true
	
}

class ARC extends ARC_BASE {
	
	boolean root = false
	
	boolean pathArc = false
	
}


// hyperedge / hypernode cases

enum ArcParent {
	
	//EMPTY, EDGE, NODE, HYPERNODE, HYPEREDGE
	
	EMPTY, CONNECTOR, TARGET
	
}

enum ArcTarget {
	
	EDGE, NODE, HYPERNODE, HYPEREDGE
}



// if at top, this is:
// hypernode / empty / empty
// empty / hyperedge / empty

// if in middle/bottom, it can be:
// parent-node, hyperedge, hypernode
// parent-edge, hyperedge, hypernode

// parent-node, hyperedge, hyperedge
// parent-edge, hyperedge, hyperedge

// parent-node, hyperedge, node
// parent-node, hyperedge, edge

// parent-edge, hyperedge, node
// parent-edge, hyperedge, edge

// parent-hypernode, hyperedge, hypernode
// parent-hypernode, hyperedge, hyperedge
// parent-hypernode, hyperedge, node
// parent-hypernode, hyperedge, edge

// parent-hyperedge, hyperedge, hypernode
// parent-hyperedge, hyperedge, hyperedge
// parent-hyperedge, hypweredge, node
// parent-hyperedge, hyperedge, edge




// potentially instead of Parent have:
//  HYPER_ARC_NODE, HYPER_ARC_EDGE, HYPER_ARC_HYPERNODE, HYPER_ARC_HYPEREDGE
// these would choose the parent and would need to match the enclosing arc
// this may be more intuitive than setting the "Parent" property
// plain HYPER_ARC would be the "empty" case and would only apply to the top-most

// potentially instead of parent, a case like "HYPER_ARC_NODE" can choose the target of "Node"

// keeping it a single HYPER_ARC and selecting the parent and target seems best choice for now


class HYPER_ARC extends ARC_BASE {

	// connects to containing/parent Node, Edge, HyperNode, HyperEdge
	// or empty if top (default)
	
		
	ArcParent parent = ArcParent.TARGET
	
	// This arc connects the parent to the current object
	// which is either an Edge, Node, HyperNode, or HyperEdge
	
	ArcTarget target = ArcTarget.HYPERNODE

	
}


abstract class ARC_BOOLEAN extends Container {
	
}

class ARC_AND extends ARC_BOOLEAN {
	
}

class ARC_OR extends ARC_BOOLEAN {
	
}

abstract class CONSTRAINT_BOOLEAN extends Container {
	
}

// for grouping constraints
class AND extends CONSTRAINT_BOOLEAN {
	
}

// for grouping constraints
class OR extends CONSTRAINT_BOOLEAN {
	
}

