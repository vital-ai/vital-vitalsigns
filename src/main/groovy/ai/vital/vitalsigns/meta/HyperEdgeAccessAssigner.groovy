package ai.vital.vitalsigns.meta

import ai.vital.vitalsigns.model.GraphObject
import ai.vital.vitalsigns.model.VITAL_HyperEdge;

/**
 * Bridge between java and groovy realms to assign edge access methods dynamically
 *
 */

class HyperEdgeAccessAssigner {

	// http://jira.codehaus.org/browse/GROOVY-4189
	def static removedMethod = {throw new MissingMethodException()}
	
	public static void removeDynamicEdgeAccess(Class<? extends VITAL_HyperEdge> hyperEdgeClass, Class<? extends GraphObject> graphClass, boolean sourceNotDestination, boolean bothSrcAndDestination,
		String localNameSingle, String localNamePlural) {
		_updateClass(hyperEdgeClass, graphClass, sourceNotDestination, bothSrcAndDestination, localNameSingle, localNamePlural, false)
	}
	
	public static void assignDynamicHyperEdgeAccess(Class<? extends VITAL_HyperEdge> hyperEdgeClass, Class<? extends GraphObject> graphClass, boolean sourceNotDestination, boolean bothSrcAndDestination,
		String localNameSingle, String localNamePlural) {
		_updateClass(hyperEdgeClass, graphClass, sourceNotDestination, bothSrcAndDestination, localNameSingle, localNamePlural, true)
	}
		
	private static void _updateClass(Class<? extends VITAL_HyperEdge> hyperEdgeClass, Class<? extends GraphObject> graphClass, boolean sourceNotDestination, boolean bothSrcAndDestination,
		String localNameSingle, String localNamePlural, boolean add) {
		
		
		if(sourceNotDestination) {
			
			
			String m1 = "get${localNameSingle}HyperEdgesOut"

			if(add) {
				graphClass.metaClass."${m1}" = { Object... args ->
					return HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(delegate, hyperEdgeClass, true, false, args)
				}
			} else {
				graphClass.metaClass."${m1}" = removedMethod
			}
			
	
			//always assing it!		
//			if(bothSrcAndDestination) {
				
				/*
				String m2 = "get${localNameSingle}EdgesIn"
				if(add) {
					
					nodeClass.metaClass."${m2}" = { Object... args ->
						return HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(delegate, edgeClass, false, true, args)
					}
						
				} else {
					nodeClass.metaClass."${m2}" = removedMethod
				}
				*/
			
				String m3 = "get${localNameSingle}HyperEdges"
				if(add) {
					graphClass.metaClass."${m3}" = { Object... args ->
						return HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(delegate, hyperEdgeClass, true, true, args)
					}
				} else {
					graphClass.metaClass."${m3}" = removedMethod
				}
//			}
			
			
			
			String m4 = "get${localNamePlural}"
			if(add) {
				graphClass.metaClass."${m4}" = { graphContext = null, container = null ->
					return HyperEdgeAccessImplementation.hyperCollectionImplementation(delegate, hyperEdgeClass, graphContext, container, true)
				}				
			} else {
				graphClass.metaClass."${m4}" = removedMethod
			}
			
		} else {
		
		
			String m2 = "get${localNameSingle}HyperEdgesIn"
			if(add) {
				graphClass.metaClass."${m2}" = { Object... args ->
			
					return HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(delegate, hyperEdgeClass, false, true, args)
				}
			} else {
				graphClass.metaClass."${m2}" = removedMethod
			}
			
			
			//assign it only if there so no source method already
			if(!bothSrcAndDestination) {
				String m3 = "get${localNameSingle}Edges"
				if(add) {
					graphClass.metaClass."${m3}" = { Object... args ->
						return HyperEdgeAccessImplementation.hyperEdgeAccessImplementation(delegate, hyperEdgeClass, true, true, args)
					}
				} else {
					graphClass.metaClass."${m3}" = removedMethod
				}
			}
						
			
			String m4 = "get${localNamePlural}Reverse"
			if(add) {
				graphClass.metaClass."${m4}" = { graphContext = null, container = null ->
					return HyperEdgeAccessImplementation.hyperCollectionImplementation(delegate, hyperEdgeClass, graphContext, container, false)
				}
			} else {
				graphClass.metaClass."${m4}" = removedMethod
			}
			
		}
		
	}
	
}
