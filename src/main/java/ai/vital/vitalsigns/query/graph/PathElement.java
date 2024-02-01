package ai.vital.vitalsigns.query.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ai.vital.vitalservice.query.Connector;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.query.graph.Binding.BindingStatus;
import ai.vital.vitalsigns.query.graph.GraphQueryImplementation.ResultsProvider;

class PathElement {

	public Arc arc;

	private List<PathElement> children = new ArrayList<PathElement>();
	
	private int currentChildIndex = 0;

	private ResultsProvider provider;

	private Iterator<BindingEl> iterator;

	private boolean hadAResult = false;
	private boolean optionalReturnedAlready = false;
	
	GraphQueryImplementation queryImpl;
	
	BindingEl currentBinding;

	public PathElement(Arc arc) {
		super();
		this.arc = arc;
	}
	

	public List<PathElement> getChildren() {
		return children;
	}


	public void setChildren(List<PathElement> children) {
		this.children = children;
	}



	public ResultsProvider getProvider() {
		return provider;
	}



	public void setProvider(ResultsProvider provider) {
		this.provider = provider;
	}

	public GraphQueryImplementation getQueryImpl() {
		return queryImpl;
	}

	public void setQueryImpl(GraphQueryImplementation queryImpl) {
		this.queryImpl = queryImpl;
	}

	public boolean isHyperArc() {
		return Connector.HYPEREDGE == arc.arcContainer.getArc().connector;
	}

	public boolean isOptional() {
		return arc.isOptional();
	}

	public boolean isForwardNotReverse() {
		return arc.isForwardNotReverse();
	}
	
	public void setStartEndoint(GraphObject endpoint) {
		if(provider == null) throw new RuntimeException("Provider not set!");
		optionalReturnedAlready = false;
		hadAResult = false;
		this.iterator = provider.getIterator(arc, endpoint);
//		if(iterator.hasNext()) {
//			currentBinding = iterator.next();
//		}
	}

	//optional arc converts no_bindings_found into empty ok binding
	public Binding getNextBinding() {
		
		if(currentBinding == null) {
			if(iterator.hasNext()) {
				hadAResult = true;
				currentBinding = iterator.next();
				for(PathElement child : children) {
					child.setStartEndoint(currentBinding.getEndpoint());
				}
			} else {
				if(!hadAResult && !optionalReturnedAlready && arc.isOptional()) {
					optionalReturnedAlready = true;
					return new Binding(BindingStatus.OK);
				} else {
					return new Binding(BindingStatus.NO_BINDINGS_FOUND);
				}
			}
		}
		
		if(children.size() == 0) {

			Binding b = new Binding(BindingStatus.OK);
			b.add(currentBinding);
			this.currentBinding = null;
			return b;
			
			/*
			boolean hasNext = iterator.hasNext();
		
			if(hasNext) {
				
				Binding b = new Binding(BindingStatus.OK);
				b.add(iterator.next());
				return b;
				
			}
			
			if(firstCheck) {
				firstCheck = false;
				if(isOptional()) {
					return new Binding(BindingStatus.EMPTY_OPTIONAL);
				} else {
					return new Binding(BindingStatus.NO_BINDINGS_FOUND);
				}
			} else {
				return new Binding(BindingStatus.NO_MORE_BINDINGS);
			}
			
			*/
			
			//keep iterating over
		}
		
		
		
		Binding response = null;
		
		
		boolean getNextValue = false;
		
		while(true) {
			
			Binding childBinding = children.get(currentChildIndex).getNextBinding();
			
			BindingStatus status = childBinding.getStatus();
			if(status == BindingStatus.OK) {
				
				childBinding.add(currentBinding);
				
				//fail fast if there are constraints in path
				if( ! queryImpl.providesConstraintTest(this.arc, childBinding, true) ) {
					continue;
				}
				
				response = childBinding;
					
				break;
					
			} else if(false &&status == BindingStatus.NO_BINDINGS_FOUND) {
				
				getNextValue = true;
				
				if(arc.isOptional()) {
					
					//convert it into empty OK
					response = new Binding(BindingStatus.EMPTY_OPTIONAL);
					break;
					
				} else {
					
					//pass it upwards
					response = childBinding;
				
					break;
				}
				
			} else if(status == BindingStatus.EMPTY_OPTIONAL) {
				
				response = new Binding(BindingStatus.OK);
				response.add(currentBinding);
				getNextValue = true;
				break;
				
			} else if(status == BindingStatus.NO_MORE_BINDINGS
					|| status == BindingStatus.NO_BINDINGS_FOUND) {
				
				if(iterator.hasNext()) {
					currentBinding = iterator.next();
					
					if(currentChildIndex < children.size() - 1) {
						//ask another child
						currentChildIndex++;
					} else {
						
						currentChildIndex = 0;
						
						for(PathElement child : children) {
							child.setStartEndoint(currentBinding.getEndpoint());
						}
						
					}
					
				} else {
					if(!hadAResult && !optionalReturnedAlready && arc.isOptional()) {
						optionalReturnedAlready = true;
						return new Binding(BindingStatus.OK);
					} else {
						response = new Binding(BindingStatus.NO_BINDINGS_FOUND);
					}
					break;
				}
				
				
			}
			
		}
		
		if(getNextValue) {
			if(iterator.hasNext()) {
				hadAResult = true;
				currentBinding = iterator.next();
				
				for(PathElement child : children) {
					child.setStartEndoint(currentBinding.getEndpoint());
				}
			} else {
				currentBinding = null;
			}
		}
		
		return response;
		
	}
	

	@Override
	public String toString() {
		if( arc != null ) {
			return ( arc.getLabel() != null && arc.getLabel().length() > 0) ? ("PE:" + arc.getLabel()) : ("PE:(no_label)");
		} else {
			return "PathElement: no arc set!";
		}
	}
	
}
