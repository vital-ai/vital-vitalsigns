package ai.vital.query.querybuilder

import java.util.Map

import ai.vital.query.ARC;
import ai.vital.query.ARC_BASE;
import ai.vital.query.Container;
import ai.vital.query.EndPath;
import ai.vital.query.GRAPH;
import ai.vital.query.Query;
import ai.vital.query.StartPath;
import groovy.util.AbstractFactory;
import groovy.util.FactoryBuilderSupport;

class GRAPHFactory extends AbstractFactory {

	@Override
	public Object newInstance(FactoryBuilderSupport builder,
			Object name, Object value, Map attributes
	) throws InstantiationException, IllegalAccessException {

		GRAPH g = null

		if(attributes != null) {
			g = new GRAPH(attributes)
		} else {
			g = new GRAPH()
		}
		
		return g;
	}

	@Override
	public void setParent(FactoryBuilderSupport builder, Object parent,
			Object child) {

		if(parent instanceof Query) {
			((Query)parent).graph = (GRAPH) child
		} else {
			throw new RuntimeException("Unexecpted parent of GRAPH - ${parent.class}")
		}
			
	}
	
	@Override
	public void onNodeCompleted(FactoryBuilderSupport builder, Object parent,
			Object node) {

		super.onNodeCompleted(builder, parent, node);
		
		Query query = (Query)parent;
		
		GRAPH gq = (GRAPH)node;
		
		if(gq.topArc == null) throw new RuntimeException("Graph's topArc not set!")
		
		//check paths, make it here as toQuery does not implement it yet
		PathsContext ctx = new PathsContext()
		validatePaths(gq.topArc, ctx, []);
		
		
		//check if all paths are ended
		for( String sp : ctx.startPaths ) {
			if(!ctx.endPaths.contains(sp)) {
				throw new RuntimeException("No end path element of path: ${sp}")
			}
		}
		
			
	}
			
	static class PathsContext {
		
		Set<String> startPaths = new HashSet<String>();
		Set<String> endPaths = new HashSet<String>();
		
	}
	
	void validatePaths(Container c, PathsContext ctx,  List parentContainers) {
		
		int containers = 0
		
		Set<String> thisStarts = new HashSet<String>();
		
		int arcs = 0;
		int starts = 0;
		int ends = 0;
		
		for(Object child : c.children ) {
			
			if(child instanceof ARC) {
				arcs ++
			}
			
			if(child instanceof Container) {
				
				containers ++
				
			} else if( child instanceof StartPath) {

				StartPath sp = (StartPath) child
				if ( ! ctx.startPaths.add(sp.name) ) ve("More than 1 path start with name: ${sp.name}")
				
				thisStarts.add(sp.name);
				
				starts++
				
			} else if( child instanceof EndPath) {
			
				EndPath ep = (EndPath) child
				
				if ( !ctx.endPaths.add(ep.name) ) ve("More than 1 path end with name: ${ep.name}")
			
				if ( !ctx.startPaths.contains(ep.name) ) ve("No path start found for end with name: ${ep.name}")
				
				if(thisStarts.contains(ep.name)) ve("A path must not start and end in the same arc");
				
				ends++
				
				ARC startARC = null;
				
				//analyze parents
				List path = [c]
				
				for(int i = parentContainers.size() - 1; i >= 0; i--) {
					
					Container cont = parentContainers[i]

					boolean stop = false
					
					for(Object ch : cont.children) {
						
						if(ch instanceof StartPath) {
							StartPath sp = ch
							if ( sp.name ==  ep.name ) {
								//end the path
								startARC = cont
								break
							} else {
								ve("Crossing paths deteceted, names: ${ep.name} and ${sp.name}")
							}
						}
						
					}					
					path.add(0, cont)
					if(startARC != null) break;
					
				}
				
				if(startARC == null) ve("Start ARC not found end path: ${ep.name}")
				
				String pathEls = ""
				for(Object p : path) {
					if(pathEls.length() > 0) pathEls += " -> "
					pathEls += p.getClass().getSimpleName()
				}
				
				//test integrity
				for(Object p : path) {
					if(p instanceof ARC) {
					} else {
						ve("Path with name: ${ep.name} incorrect, expected a sequence of ARC elements, got: ${pathEls}")
					}					
				}
				
				if(path.size() < 3) {
					ve("Path name ${ep.name} is too short, min length 3, ")
				}
				
			}
			
		}
		
		if(starts > 1) {
			ve("Cannot start more than 1 path in an ARC")
		}
		
		if(ends > 1) {
			ve("Cannot end more than 1 path in an ARC")
		}
		
		if(starts > 0) {
			if(arcs != 1) {
				ve("start_path needs to be located in an ARC that has exactly 1 ARC element, has ${arcs}")
			}
		}
		
		for(Object child : c.children ) {
			if(child instanceof Container) {
				List newParentContainers = new ArrayList(parentContainers)
				newParentContainers.add(c)
				validatePaths((Container)child, ctx, newParentContainers)
			}
		}
		
	}
	
	void ve(String m) { throw new RuntimeException(m) }	
}
