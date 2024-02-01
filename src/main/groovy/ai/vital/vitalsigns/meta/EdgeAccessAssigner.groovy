package ai.vital.vitalsigns.meta

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_Node;


/**
 * Bridge between java and groovy realms to assign edge access methods dynamically
 *
 */

class EdgeAccessAssigner {

	// http://jira.codehaus.org/browse/GROOVY-4189
	def static removedMethod = {throw new MissingMethodException()}
	
	public static void removeDynamicEdgeAccess(Class<? extends VITAL_Edge> edgeClass, Class<? extends VITAL_Node> nodeClass, boolean sourceNotDestination, boolean bothSrcAndDestination,
		String localNameSingle, String localNamePlural) {
		_updateClass(edgeClass, nodeClass, sourceNotDestination, bothSrcAndDestination, localNameSingle, localNamePlural, false)
	}
	
	public static void assignDynamicEdgeAccess(Class<? extends VITAL_Edge> edgeClass, Class<? extends VITAL_Node> nodeClass, boolean sourceNotDestination, boolean bothSrcAndDestination,
		String localNameSingle, String localNamePlural) {
		_updateClass(edgeClass, nodeClass, sourceNotDestination, bothSrcAndDestination, localNameSingle, localNamePlural, true)
	}
		
	private static void _updateClass(Class<? extends VITAL_Edge> edgeClass, Class<? extends VITAL_Node> nodeClass, boolean sourceNotDestination, boolean bothSrcAndDestination,
		String localNameSingle, String localNamePlural, boolean add) {
		
		
		if(sourceNotDestination) {
			
			
			String m1 = "get${localNameSingle}EdgesOut"

			if(add) {
				nodeClass.metaClass."${m1}" = { Object... args ->
					return EdgeAccessImplementation.edgeAccessImplementation(delegate, edgeClass, true, false, args)
				}
			} else {
				nodeClass.metaClass."${m1}" = removedMethod
			}
			
	
			//always assing it!		
//			if(bothSrcAndDestination) {
				
				/*
				String m2 = "get${localNameSingle}EdgesIn"
				if(add) {
					
					nodeClass.metaClass."${m2}" = { Object... args ->
						return EdgeAccessImplementation.edgeAccessImplementation(delegate, edgeClass, false, true, args)
					}
						
				} else {
					nodeClass.metaClass."${m2}" = removedMethod
				}
				*/
			
				String m3 = "get${localNameSingle}Edges"
				if(add) {
					nodeClass.metaClass."${m3}" = { Object... args ->
						return EdgeAccessImplementation.edgeAccessImplementation(delegate, edgeClass, true, true, args)
					}
				} else {
					nodeClass.metaClass."${m3}" = removedMethod
				}
//			}
			
			
			
			String m4 = "get${localNamePlural}"
			if(add) {
				nodeClass.metaClass."${m4}" = { graphContext = null, container = null ->
					return EdgeAccessImplementation.collectionImplementation(delegate, edgeClass, graphContext, container, true)
				}				
			} else {
				nodeClass.metaClass."${m4}" = removedMethod
			}
			
		} else {
		
		
			String m2 = "get${localNameSingle}EdgesIn"
			if(add) {
				nodeClass.metaClass."${m2}" = { Object... args ->
			
					return EdgeAccessImplementation.edgeAccessImplementation(delegate, edgeClass, false, true, args)
				}
			} else {
				nodeClass.metaClass."${m2}" = removedMethod
			}
			
			
			//assign it only if there so no source method already
			if(!bothSrcAndDestination) {
				String m3 = "get${localNameSingle}Edges"
				if(add) {
					nodeClass.metaClass."${m3}" = { Object... args ->
						return EdgeAccessImplementation.edgeAccessImplementation(delegate, edgeClass, true, true, args)
					}
				} else {
					nodeClass.metaClass."${m3}" = removedMethod
				}
			}
						
			
			String m4 = "get${localNamePlural}Reverse"
			if(add) {
				nodeClass.metaClass."${m4}" = { graphContext = null, container = null ->
					return EdgeAccessImplementation.collectionImplementation(delegate, edgeClass, graphContext, container, false)
				}
			} else {
				nodeClass.metaClass."${m4}" = removedMethod
			}
			
		}
		
	}
	
}
