package ai.vital.vitalsigns.query.graph.ref;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a list of paths forming a complete graph match 
 *
 */
public class GraphPattern extends ArrayList<Path> {

	public GraphPattern() {
		super();
	}

	public GraphPattern(Collection<? extends Path> c) {
		super(c);
	}

	public GraphPattern(int initialCapacity) {
		super(initialCapacity);
	}

	private static final long serialVersionUID = 1L;
	
	@Override
	public String toString() {
		
		if(this.size() < 1) return "(empty)";
		
		String s = "";
		
		for(Path p : this) {
			if(s.length() > 0 ) s += " AND ";
			s += p.toString();
		}
		return s;
		
	}

}
