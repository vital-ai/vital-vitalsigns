package ai.vital.vitalsigns.query.graph.ref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ai.vital.vitalservice.query.Connector;
import ai.vital.vitalservice.query.GraphElement;
import ai.vital.vitalservice.query.QueryContainerType;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalservice.query.VitalGraphBooleanContainer;
import ai.vital.vitalservice.query.VitalGraphCriteriaContainer;
import ai.vital.vitalservice.query.VitalGraphQueryContainer;
import ai.vital.vitalservice.query.VitalGraphQueryElement;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalservice.query.VitalGraphQueryTypeCriterion;
import ai.vital.vitalservice.query.VitalGraphValue;
import ai.vital.vitalservice.query.VitalGraphValueCriterion;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.properties.PropertyMetadata;

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_HyperEdge;
import ai.vital.vitalsigns.model.VITAL_Node;
import ai.vital.vitalsigns.model.VITAL_HyperNode;


class QueryAnalysis {

	public static class ProvidesValueParent {
		
		VitalGraphArcContainer container;
		VitalGraphValue value;
		Arc arc;
		
	}
	
	static class WrappedContainer {
		
		public VitalGraphCriteriaContainer container = new VitalGraphCriteriaContainer(QueryContainerType.and);
		public int connectorCriteria = 0;
		public int endpointCriteria = 0;
		
	}
	
	public static List<WrappedContainer> splitArc(VitalGraphArcContainer arc) {

		List<VitalGraphCriteriaContainer> rootContainers = new ArrayList<VitalGraphCriteriaContainer>();
		
		for( VitalGraphQueryContainer<?> c : arc ) {
			
//			if(c instanceof VitalGraphArcContainer) ex("Nested ARCs forbidden");
//			
//			if(c instanceof VitalGraphBooleanContainer) ex("ARC boolean containers forbidden");
			
			if(c instanceof VitalGraphCriteriaContainer) {
				
				rootContainers.add((VitalGraphCriteriaContainer) c);
				
			}
			
		}
		
//		if(rootContainers.size() < 1) ex("No criteria containers found in an ARC");
		
		VitalGraphCriteriaContainer topContainer = null;
		
		if(rootContainers.size() == 0) {
		
			topContainer = new VitalGraphCriteriaContainer();
			topContainer.setType(QueryContainerType.and);
			
		} else if(rootContainers.size() == 1) {
			
			topContainer = rootContainers.get(0);
			
			if(topContainer.getType() != QueryContainerType.and) ex("Top criteria container must be of type AND");
			
		} else {
			
			topContainer = new VitalGraphCriteriaContainer(QueryContainerType.and);
			
			topContainer.addAll(rootContainers);
			
		}
		
		
		return splitTopContainer(arc, topContainer);
		
		
		
	}
	
	
	private static void ex(String m) { throw new RuntimeException(m); }

	
	private static List<WrappedContainer> splitTopContainer(VitalGraphArcContainer arc, VitalGraphCriteriaContainer topContainer) {

//		int topEdges = 0;
//		int topNodes = 0;

		if(Connector.EMPTY == arc.getArc().connector ) {
			
		}
		
		WrappedContainer endpointContainer = new WrappedContainer();
		WrappedContainer connectorContainer = new WrappedContainer();
		
		
		for( VitalGraphQueryElement el : topContainer ) {
			
			if(el instanceof VitalGraphQueryTypeCriterion) {
				
				VitalGraphQueryTypeCriterion tc = ((VitalGraphQueryTypeCriterion)el);
				
				Class<? extends GraphObject> c = tc.getType();

				GraphElement symbol = tc.getSymbol();
				
				if(symbol == GraphElement.Connector) {
					
					if(VITAL_Edge.class.isAssignableFrom(c)) {
						
						if(arc.getArc().connector == Connector.EMPTY) {
							ex("Cannot use connector contraint in an empty connector arc");
						} else if(arc.getArc().connector == Connector.HYPEREDGE) {
							ex("Cannot use edge connector constraint in a hyper edge arc");
						} 

						connectorContainer.container.add(el);
						connectorContainer.connectorCriteria++;
						
					} else if(VITAL_HyperEdge.class.isAssignableFrom(c)) {
						
						if(arc.getArc().connector == Connector.EMPTY) {
							ex("Cannot use connector contraint in an empty connector arc");
						} else if(arc.getArc().connector == Connector.EDGE) {
							ex("Cannot use edge connector constraint in an edge arc");
						} 
						
						connectorContainer.container.add(el);
						connectorContainer.connectorCriteria++;
						
					} else {
						ex("invalid graph type used as a connector contraint: " + c.getCanonicalName());
					}
					
				} else {

					if(arc.getArc().connector == Connector.EDGE) {
						if(!VITAL_Node.class.isAssignableFrom(c)) {
							throw new RuntimeException("Cannot use non vitalnode type constraint in an edge arc endpoint:" + c.getCanonicalName());
						}
					}
					
					//everything is an endpoint container
					endpointContainer.container.add(el);
					endpointContainer.endpointCriteria++;

				}
				
				
			} else if(el instanceof VitalGraphQueryPropertyCriterion) {
				
				VitalGraphQueryPropertyCriterion pc = ((VitalGraphQueryPropertyCriterion) el);
				
				String pURI = ((VitalGraphQueryPropertyCriterion) el).getPropertyURI();
				
				
				boolean isNodeClass = false;
				boolean isEdgeClass = false;
				boolean isHyperNodeClass = false;
				boolean isHyperEdgeClass = false;
				
				String s = "";
				
				if(VitalGraphQueryPropertyCriterion.URI.equals(pURI)) {
					
					isNodeClass = true;
					
				} else {
					
					PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(pURI);
					
					
					if(pm == null) ex("Property with URI not found: " + pURI);
					
					
					for( ClassMetadata domain : pm.getDomains() ) {
						if(VITAL_Node.class.isAssignableFrom(domain.getClazz())) {
							isNodeClass = true;
						} else if(VITAL_Edge.class.isAssignableFrom(domain.getClazz())) {
							isEdgeClass = true;
						} else if(VITAL_HyperNode.class.isAssignableFrom(domain.getClazz())) {
							isHyperNodeClass = true;
						} else if(VITAL_HyperEdge.class.isAssignableFrom(domain.getClazz())) {
							isHyperEdgeClass = true;
						}
						if(s.length() > 0) {
							s += ", ";
						}
						s += domain.getClazz().getCanonicalName();
					}
				}
				
				
				
				int score = 0;

				if(isEdgeClass) score++;
				if(isHyperEdgeClass) score++;
				if(isHyperNodeClass) score++;
				if(isNodeClass) score++;
					
				if(score == 0) {
					ex("No property domain determined: " + pURI + " " + s);
				} else if(score > 1) {
					ex("Ambiguous property domains " + s + " - edge? " + isEdgeClass + "  node? " + isNodeClass + "  hypernode? " + isHyperNodeClass + "  hyperedge? " + isHyperEdgeClass);
				}

				if(GraphElement.Connector == pc.getSymbol()) {
					
					if(arc.getArc().connector == Connector.EMPTY) ex("Cannot use connector property constraint in non-connector arc");
					if(arc.getArc().connector == Connector.EDGE) {
						if( ! isEdgeClass) {
							ex("Only edge property constraints may be used in edge arc");
						}
					} else {
						if(!isHyperEdgeClass) {
							ex("Only hyper edge property constraints may be used in hyperedge arc");
						}
					}
					
					connectorContainer.container.add(el);
					connectorContainer.connectorCriteria++;
					
				} else {

					if(arc.getArc().connector == Connector.EDGE) {
						
						if(!isNodeClass) ex("Only node constraints may be used as endpoint constraints in edge arc");
						
					}
					
					endpointContainer.container.add(el);
					endpointContainer.endpointCriteria++;
					
				}
				
			} else if(el instanceof VitalGraphCriteriaContainer) {
				
				VitalGraphCriteriaContainer cc = (VitalGraphCriteriaContainer) el;
				
				WrappedContainer wrapped = new WrappedContainer();
				wrapped.container = cc;
				
				analyzeContainer(wrapped, cc);
				
				
				if(wrapped.endpointCriteria > 0 && wrapped.connectorCriteria > 0) ex("connector and endpoint criteria cannot be mixed in same container (except root)");
//				if(wrapped.endpointCriteria == 0 && wrapped.connectorCriteria == 0) ex("No nodes or edges criteria found in a sub criteria container");
				if(wrapped.endpointCriteria > 0) {
					endpointContainer.container.add(cc);
					endpointContainer.endpointCriteria++;
				} else {
					connectorContainer.container.add(cc);
					connectorContainer.connectorCriteria++;
				}
				
				
			} else {
				ex("unexpected child of a criteria container");
			}
			
		}
		
		//don't throw it here yet
//		if(nodesContainer.nodesCriteria < 1) ex("No node constraints found in an ARC");
//		if(edgesContainer.edgesCriteria < 1) ex("No edge constraints found in an ARC");
		
		return Arrays.asList(endpointContainer, connectorContainer);
		
		
	}
	
	private static void analyzeContainer(WrappedContainer wrapped, VitalGraphCriteriaContainer cc) {

		for( VitalGraphQueryElement el : cc ) {
			
			if(el instanceof VitalGraphQueryTypeCriterion) {
				
				Class<? extends GraphObject> c = ((VitalGraphQueryTypeCriterion)el).getType();
				
				if(VITAL_Node.class.isAssignableFrom(c)) {
					wrapped.endpointCriteria++;
				} else if(VITAL_Edge.class.isAssignableFrom(c)) {
					wrapped.connectorCriteria++;
				} else {
					ex("only node/edge constraints allowed, invalid type: " + c);
				}
				
			} else if(el instanceof VitalGraphQueryPropertyCriterion) {
				
				String pURI = ((VitalGraphQueryPropertyCriterion) el).getPropertyURI();
				
				PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(pURI);
				if(pm == null) ex("Property with URI not found: " + pURI);
				
				boolean isNodeClass = false;
				boolean isEdgeClass = false;
				String s = "";
				for( ClassMetadata domain : pm.getDomains() ) {
					if(VITAL_Node.class.isAssignableFrom(domain.getClazz())) {
						isNodeClass = true;
					} else if(VITAL_Edge.class.isAssignableFrom(domain.getClazz())) {
						isEdgeClass = true;
					}
					if(s.length() > 0) {
						s += ", ";
					}
					s += domain.getClazz().getCanonicalName();
				}
				
				if(isNodeClass && isEdgeClass) ex("Ambiguous property - both edge and node domain: " + pURI + " " + s);
				if(!isNodeClass && !isEdgeClass) ex("Property not a node nor edge property: " + pURI);
				
				if(isNodeClass) {
					wrapped.endpointCriteria++;
				} else if(isEdgeClass) {
					wrapped.connectorCriteria++;
				}
				
			} else if(el instanceof VitalGraphCriteriaContainer) {
				
				analyzeContainer(wrapped, (VitalGraphCriteriaContainer) el);
				
			} else {
				ex("unexpected child of a criteria container");
			}
			
		}
		
	}


	public static Set<String> collectAndValidateProvides(
			Map<String, ProvidesValueParent> providesName2ValueParent,
			VitalGraphQueryContainer<?> topContainer) {

		Set<String> names = new HashSet<String>();
		
		for( Object c : topContainer ) {
			
			if(c instanceof VitalGraphArcContainer) {
				
				Set<String> subnames = collectAndValidateProvides(providesName2ValueParent, (VitalGraphArcContainer)c);
				names.addAll(subnames);
				
			} else if( c instanceof VitalGraphBooleanContainer) {
				
				Set<String> subnames = collectAndValidateProvides(providesName2ValueParent, (VitalGraphBooleanContainer)c);
				names.addAll(subnames);
				
			}
			
		}
		
		if(topContainer instanceof VitalGraphArcContainer) {
			
			VitalGraphArcContainer arc = (VitalGraphArcContainer) topContainer;
			
			for( Entry<String, VitalGraphValue> entry : arc.getProvidesMap().entrySet() ) {
				
				String name = entry.getKey();
				
				if(providesName2ValueParent.containsKey(name)) throw new RuntimeException("Provides name appears more than once in the query: " + name);
				
				ProvidesValueParent value = new ProvidesValueParent();
				value.container = arc;
				value.value = entry.getValue();
				
				providesName2ValueParent.put(name, value);
			
				names.add(name);
				
			}
			
			List<VitalGraphValueCriterion> valueCriteria = arc.getValueCriteria();
			if(valueCriteria != null) {
				
				for(VitalGraphValueCriterion c : valueCriteria) {
					
					String name1 = c.getName1();
					String name2 = c.getName2();
					
					if(!names.contains(name1)) throw new RuntimeException("name from criterion not found (not provided): " + name1);
					if(!names.contains(name2)) throw new RuntimeException("name from criterion not found (not provided): " + name2);
					
				}
				
			}
			
		}
		
		
		return names;
		
	}
}
