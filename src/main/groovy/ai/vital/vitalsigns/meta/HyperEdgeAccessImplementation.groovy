package ai.vital.vitalsigns.meta


import ai.vital.vitalservice.VitalService;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.admin.VitalServiceAdmin;
import ai.vital.vitalservice.query.Connector;
import ai.vital.vitalservice.query.Destination;
import ai.vital.vitalservice.query.GraphElement;
import ai.vital.vitalservice.query.QueryContainerType;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.Source;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalservice.query.VitalGraphArcElement;
import ai.vital.vitalservice.query.VitalGraphCriteriaContainer;
import ai.vital.vitalservice.query.VitalGraphQueryTypeCriterion;
import ai.vital.vitalservice.query.VitalPathQuery;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.VITAL_Container;

import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.model.VitalSegment;
import ai.vital.vitalsigns.model.property.URIProperty;

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;



/**
 * Implementation for whole edge access functionality
 *
 */

public class HyperEdgeAccessImplementation {

	public static List<?> hyperEdgeAccessImplementation(GraphObject rootObj, Class<? extends VITAL_HyperEdge> hyperEdgeClass, boolean forwardNotReverse, boolean reverseNotForward, Object... args) {

		if(!forwardNotReverse && !reverseNotForward) throw new RuntimeException("No direction specified!");
		
		boolean forwardDomainMatch = false;
		boolean reverseDomainMatch = false;
		
		if(forwardNotReverse) {
		
			List<ClassMetadata> edgeClasses = VitalSigns.get().getClassesRegistry().getHyperEdgeClassesWithSourceOrDestGraphClass(rootObj.getClass(), true);
			
			for(ClassMetadata edgeCM: edgeClasses) {

				if(edgeCM.getClazz().equals(hyperEdgeClass)) {
					
					forwardDomainMatch = true;
					break;
						
				}
				
			}
				
		}
		
		if(reverseNotForward) {
			
			List<ClassMetadata> edgeClasses = VitalSigns.get().getClassesRegistry().getHyperEdgeClassesWithSourceOrDestGraphClass(rootObj.getClass(), false);
			
			for(ClassMetadata edgeCM : edgeClasses) {
				
				if(edgeCM.getClazz().equals(hyperEdgeClass)) {
					reverseDomainMatch = true;
					break;
					
				}
				
			}
			
		}
		
		
		GraphContext graphContext = null;

		List<VITAL_Container> containers = null;
		GraphObject otherNode = null;
		
		Object a0 = args.length > 0 ? args[0] : null;
		Object a1 = args.length > 1 ? args[1] : null;
		Object a2 = args.length > 2 ? args[2] : null;
		
		if(args.length == 0) {
			
		} else if(args.length == 1) {
		
			if(a0 instanceof GraphObject) {
				otherNode = (GraphObject) a0;
			} else if(a0 instanceof GraphContext) {
				graphContext = (GraphContext) a0;
			} else throw new RuntimeException("Unexpected argument at position 0: " + a0);
		
		} else if(args.length == 2) {
		
			if(a0 instanceof GraphContext) {
				graphContext = (GraphContext) a0;
			} else throw new RuntimeException("Unexpected argument at position 0: " + a0);
		
			if(a1 instanceof GraphObject) {
				otherNode = (GraphObject) a1;
			} else if(a1 instanceof List) {
				containers = (List<VITAL_Container>) a1;
			} else throw new RuntimeException("Unexpected argument at position 1: " + a1);
			
		} else if(args.length == 3) {
		
			if(a0 instanceof GraphContext) {
				graphContext = (GraphContext) a0;
			} else throw new RuntimeException("Unexpected argument at position 0: " + a0);
			
			if(a1 instanceof List) {
				containers = (List<VITAL_Container>) a1;
			} else throw new RuntimeException("Unexpected argument at position 1: " + a1);
		
			if(a2 instanceof GraphObject) {
				otherNode = (GraphObject) a2;
			} else throw new RuntimeException("Unexpected argument at position 2: " + a2);
			
		} else {
			throw new RuntimeException("Expected 3 arguments in hyper edges accessor");
		}
		
		VITAL_Container[] containersArray = containers != null ? containers.toArray(new VITAL_Container[containers.size()]) : null;
		
		if(containers != null) {
			for(Object o : containers) {
				if(o == null) throw new NullPointerException("Expected a list of VITAL_Container objects only - it contains null");
				if(!(o instanceof VITAL_Container)) throw new RuntimeException("Expected a list of VITAL_Container objects only - it contains object of class " + o != null ? o.getClass().getCanonicalName() : null);
			}
		}
				
		
		if(graphContext == null) graphContext = GraphContext.Local;
		
		EdgesResolver resolver = VitalSigns.get().getEdgesResolver(graphContext);
		
		if(resolver == null) throw new RuntimeException("No (hyper)edges resolver for context: " + graphContext);
		
		if(graphContext != GraphContext.Container && containers != null && containers.size() > 0) {
			throw new RuntimeException("GraphContext: " + graphContext + " cannot be used with containers." );
		}
		
		Class<?>[] hEdgesFilter = null;
		
		if(hyperEdgeClass != null) {
			hEdgesFilter = new Class[]{hyperEdgeClass};
		}
		
		
		List<VITAL_HyperEdge> hEdges = new ArrayList<VITAL_HyperEdge>();
		
		if(otherNode != null) {
			
			if(forwardNotReverse && forwardDomainMatch) {
				
				hEdges.addAll(resolver.getHyperEdgesForSrcURIAndDestURI(rootObj.getURI(), otherNode.getURI(), (Class<? extends VITAL_HyperEdge>[]) hEdgesFilter, true, containersArray));
				
			}
			
			//check edge domain to see it incoming edges are possible at all
			if(reverseNotForward && reverseDomainMatch) {
				hEdges.addAll(resolver.getHyperEdgesForSrcURIAndDestURI(otherNode.getURI(), rootObj.getURI(), (Class<? extends VITAL_HyperEdge>[]) hEdgesFilter, true, containersArray));
			}
			
			
		} else {
		
			if(forwardNotReverse && forwardDomainMatch) {
				hEdges.addAll(resolver.getHyperEdgesForSrcURI(rootObj.getURI(), (Class<? extends VITAL_HyperEdge>[]) hEdgesFilter, true, containersArray));
			}

			//check edge domain to see it incoming edges are possible at all
			if(reverseNotForward && reverseDomainMatch) {
				hEdges.addAll(resolver.getHyperEdgesForDestURI(rootObj.getURI(), hEdgesFilter, true, containersArray));
			}			
		}
		
		
		return hEdges;
		
	}
	
	
	public static List<?> hyperCollectionImplementation(GraphObject rootObj, Class<? extends VITAL_HyperEdge> hyperEdgeClass, Object graphContext, Object container, boolean forwardNotReverse) {

		/*
		 if(graphContext != null && ! ( graphContext instanceof GraphContext ) ){
			 throw new RuntimeException("First argument has to b");
		 }
		 */
		 
		 String uri = rootObj.getURI();
		 
		 if(( graphContext instanceof GraphContext && graphContext == GraphContext.ServiceWide) || graphContext instanceof String || graphContext instanceof GString ) {

             VitalService si = null;
             VitalServiceAdmin sa = null;
             VitalApp app = null;
             
             if(!(container instanceof Map)) throw new RuntimeException("When using ServiceWide context or named service second param must be a map with depth(Integer) optional, app(VitalApp) optional and segments(List<String>) required.");
             
             Map m = (Map) container;
             
             if(graphContext instanceof String || graphContext instanceof GString) {
                 
                 String serviceName = graphContext.toString();
                 if( NamedServiceProvider.provider == null ) throw new RuntimeException("No namedServiceProvider");
                 Object service = NamedServiceProvider.provider.getNamedService(serviceName);
                 if(service == null) throw new RuntimeException("Service with name not found: " + serviceName);
                 if(service instanceof VitalService) {
                     si = (VitalService) service;
                 } else if(service instanceof VitalServiceAdmin) {
                     Object a = m.get("app");
                     if(a == null) throw new RuntimeException("No app parameter");
                     if(!(a instanceof VitalApp)) throw new RuntimeException("app parameter must be an instance of " + VitalApp.class.getCanonicalName());
                     sa = (VitalServiceAdmin) service;
                     app = (VitalApp) a;
                 } else {
                     throw new RuntimeException("Service type " + service.getClass().getCanonicalName() + " is not supported");
                 }
                 
             } else {
                 si = VitalSigns.get().getVitalService();
                 sa = VitalSigns.get().getVitalServiceAdmin();
                 if(si != null) {
                 } else if(sa != null) {
                     app = VitalSigns.get().getCurrentApp();
                     if(app == null) throw new RuntimeException("No context app set, required when vitalservice admin is set");
                 } else {
                     throw new RuntimeException("No active vital service or admin service set in VitalSigns!");
                 }
                 
             }

             
             Integer depth = (Integer) m.get("depth");
             
             if(depth == null) depth = 1;
             
             if(depth < 0) throw new RuntimeException("Depth cannot be < 0");
             
             //max depth
             if(depth == 0 || depth > 10) depth = 10;
             
             List<String> segments = (List<String>) m.get("segments");
             
             if(segments == null || segments.isEmpty()) throw new RuntimeException("No segments parameter!");
                                 
             //construct simple vital path query
             VitalPathQuery vpq = new VitalPathQuery();
             vpq.setRootURIs(Arrays.asList(URIProperty.withString(uri)));
             List<VitalSegment> segs = new ArrayList<VitalSegment>();
             for(String s : segments) {
                 VitalSegment segment = null;
                 try {
                     if(si != null) {
                         segment = si.getSegment(s);
                     } else {
                         segment = sa.getSegment(app, s);
                     }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                 if(segment == null) throw new RuntimeException("Segment not found: " + s);
                 segs.add(segment);
             }
             
             
             VitalGraphArcElement el = new VitalGraphArcElement(forwardNotReverse ? Source.PARENT_SOURCE : Source.CURRENT, Connector.HYPEREDGE, forwardNotReverse ? Destination.CURRENT : Destination.PARENT_SOURCE);
             VitalGraphArcContainer arc = new VitalGraphArcContainer(QueryContainerType.and, el);
             VitalGraphCriteriaContainer cc = new VitalGraphCriteriaContainer(QueryContainerType.and);
             cc.add(new VitalGraphQueryTypeCriterion(GraphElement.Connector, hyperEdgeClass));
             arc.add(cc);
             vpq.setArcs(Arrays.asList(arc));
             
             vpq.setSegments(segs);
             
             ResultList rl = null;
            try {
                if(si != null) {
                    rl = si.query(vpq);
                } else {
                    rl = sa.query(app, vpq);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            if(rl.getStatus().getStatus() != VitalStatus.Status.ok) throw new RuntimeException("Query error: " + rl.getStatus().getMessage());

             List<VITAL_Node> res = new ArrayList<VITAL_Node>();

             for(GraphObject g : rl) {
                 
                 if(g instanceof VITAL_Node && !g.getURI().equals(uri)) {
                     res.add((VITAL_Node) g);
                 }
                 
             }
             
             return res;
			 
		 }
		 
		 
		 
		 
		 //backward compatibility to change the context
		 if( container == null && graphContext != null && ( graphContext instanceof VITAL_Container || ( graphContext instanceof VITAL_Container[] && ((VITAL_Container[])graphContext).length > 0) || (graphContext instanceof List && ((List)graphContext).size() > 0) ) ) {
			 container = graphContext;
			 graphContext = GraphContext.Container;
		 }
		 
		 if(graphContext == null) graphContext = GraphContext.Local;
		 
		 if(graphContext != GraphContext.Container && container != null) {
			 throw new RuntimeException("GraphContext: " + graphContext + " cannot be used with containers." );
		 }
		 
//						if(container == null) container = new VITAL_Container[0];
		 
//						String edgeTypeURI = edgeRes.getURI();
		 
		 EdgesResolver resolver = VitalSigns.get().getEdgesResolver((GraphContext) graphContext);
		 
		 if( resolver == null ) {
			 throw new RuntimeException("No edges resolver set for context: " + graphContext + " - VitalSigns requires it!");
		 }
		 
		 if(container instanceof List) {
			 container = ((List)container).toArray(new VITAL_Container[((List)container).size()]);
		 }
		 
		 List<GraphObject> res = null;
		 
		 VITAL_Container[] containersA = null;
		 
		 if(container instanceof VITAL_Container[]) {
			 containersA = (VITAL_Container[]) container;
		 } else if(container instanceof List) {
			 containersA = (VITAL_Container[]) ((List) container).toArray(new VITAL_Container[0]);
		 } else if(container instanceof VITAL_Container) {
			 containersA = new VITAL_Container[]{(VITAL_Container)container};
		 }
				 
		 
		 if(forwardNotReverse) {
			res = resolver.getDestGraphObjectsForSrcURI(uri, hyperEdgeClass, containersA);
		 } else {
		    res = resolver.getSourceGraphObjectsForDestURI(uri, hyperEdgeClass, containersA);
		 }
		 
		 
		 return res;
		 
	}
}
