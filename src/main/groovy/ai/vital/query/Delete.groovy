package ai.vital.query

enum Delete {
	
	Source,
	Connector,
	Target,
	Both,
	None
	// source (top level ARC)
	// target (this implies connector too as we can’t have hanging edges)
	// connector
	// both (referring to target + connector, same as target)

}
