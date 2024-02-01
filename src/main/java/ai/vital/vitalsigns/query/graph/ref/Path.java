package ai.vital.vitalsigns.query.graph.ref;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a tuple of arcs
 * A -> B - C
 *
 */
class Path extends ArrayList<PathElement> {

	private static final long serialVersionUID = 1L;

	public Path() {
		super();
	}

	public Path(Collection<? extends PathElement> c) {
		super(c);
	}

	public Path(int initialCapacity) {
		super(initialCapacity);
	}
	

	@Override
	public String toString() {
		if(this.size() < 1) return "(empty path)";
		
		String s = "";
		for(PathElement a : this) {
			if(s.length() > 0 ) s += " -> ";
			s += a.arc.getLabel();
		}
		return s;
	}

	public boolean containsArc(Arc arc) {

		for(PathElement p : this) {
			if(p.arc == arc) return true;
		}
		
		return false;
	}

	
	
}
