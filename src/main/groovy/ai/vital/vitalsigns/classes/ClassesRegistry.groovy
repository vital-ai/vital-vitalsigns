package ai.vital.vitalsigns.classes


import java.util.Map.Entry

import ai.vital.vitalsigns.meta.EdgeAccessAssigner;
import ai.vital.vitalsigns.meta.PathElement;
import ai.vital.vitalsigns.model.GraphObject;

import ai.vital.vitalsigns.model.VITAL_PeerEdge;
import ai.vital.vitalsigns.model.VITAL_TaxonomyEdge;

import ai.vital.vitalsigns.model.VITAL_Edge;
import ai.vital.vitalsigns.model.VITAL_Node;


public class ClassesRegistry {

	private Map<String, ClassMetadata> classURI2MDMap = new LinkedHashMap<String, ClassMetadata>();

	private Map<String, ClassMetadata> classCNameMDMap = new HashMap<String, ClassMetadata>();
	
	private Map<String, List<ClassMetadata>> shortnameMap = new HashMap<String, List<ClassMetadata>>();
	
	private HashSet<String> restrictedClassNames = new HashSet<String>();
	
	public ClassMetadata getClass(String uri) {
		return classURI2MDMap.get(uri);
	}

	public void addClass(ClassMetadata parentClass, ClassMetadata classMD) {

		classURI2MDMap.put(classMD.getURI(), classMD);
	
		classCNameMDMap.put(classMD.getCanonicalName(), classMD);
		
		String shortname = classMD.getSimpleName();
		List<ClassMetadata> list = shortnameMap.get(shortname);
		if(list == null) {
			list = new ArrayList<ClassMetadata>();
			shortnameMap.put(shortname, list);
		}
		list.add(classMD);
		
		
		if(parentClass != null) {
			
			parentClass.getChildren().add(classMD);
			
		}
		
	}

	public List<ClassMetadata> getSubclasses(ClassMetadata domain, boolean includeThis) {

		List<ClassMetadata> r = new ArrayList<ClassMetadata>();
		
		if(includeThis) r.add(domain);
		
		processSubclassesDomain(r, domain);
		
		return r;
	}

	private void processSubclassesDomain(List<ClassMetadata> r,
			ClassMetadata domain) {

		List<ClassMetadata> children = domain.getChildren();
		
		if(children == null) return;
		
		for(ClassMetadata child : children) {
			
			r.add(child);
			
			processSubclassesDomain(r, child);
			
		}
		
	}

	public String getClassURI(Class<? extends GraphObject> class1) {
	    ClassMetadata cm = classCNameMDMap.get(class1.getCanonicalName());
	    return cm != null ? cm.getURI() : null;
	}

	public HashSet<String> getRestrictedClassNames() {
		return restrictedClassNames;
	}

	public List<ClassMetadata> getEdgeClassesWithSourceOrDestNodeClass(Class<? extends VITAL_Node> sourceOrDestDomain, boolean sourceNotDestination) {

		List<ClassMetadata> edgeClasses = new ArrayList<ClassMetadata>();
		
		for(ClassMetadata edgeCM : classURI2MDMap.values()) {
		
			if(sourceNotDestination) {
				
				if(edgeCM.getEdgeSourceDomains() == null) continue;
				
				for(ClassMetadata c : edgeCM.getEdgeSourceDomains()) {
					if(c.getClazz().isAssignableFrom( sourceOrDestDomain ) ) {
						edgeClasses.add(edgeCM);
						break;
					}
				}
				
			} else {
				
				if(edgeCM.getEdgeDestinationDomains() == null) continue;
				
				for(ClassMetadata c : edgeCM.getEdgeDestinationDomains()) {
					if(c.getClazz().isAssignableFrom(sourceOrDestDomain)) {
						edgeClasses.add(edgeCM);
						break;
					}
				}
				
			}
			
			
		}
		
		return edgeClasses;
		
	}
	
	public List<ClassMetadata> getEdgeAndHyperEdgeClassesWithSourceOrDestGraphClass(Class<? extends GraphObject> sourceOrDestDomain, boolean sourceNotDestination) {
		
		List<ClassMetadata> l = getHyperEdgeClassesWithSourceOrDestGraphClass(sourceOrDestDomain, sourceNotDestination);
		
		if(VITAL_Node.class.isAssignableFrom(sourceOrDestDomain)) {
			l.addAll(getEdgeClassesWithSourceOrDestNodeClass((Class<? extends VITAL_Node>) sourceOrDestDomain, sourceNotDestination));
		}
		
		return l;
		
	}
	
	public List<ClassMetadata> getHyperEdgeClassesWithSourceOrDestGraphClass(Class<? extends GraphObject> sourceOrDestDomain, boolean sourceNotDestination) {
		

		List<ClassMetadata> hyperEdgeClasses = new ArrayList<ClassMetadata>();
		
		for(ClassMetadata hEdgeCM : classURI2MDMap.values()) {
		
			if(sourceNotDestination) {
				
				if(hEdgeCM.getHyperEdgeSourceDomains() == null) continue;
				
				for(ClassMetadata c : hEdgeCM.getHyperEdgeSourceDomains()) {
					if(c.getClazz().isAssignableFrom( sourceOrDestDomain ) ) {
						hyperEdgeClasses.add(hEdgeCM);
						break;
					}
				}
				
			} else {
				
				if(hEdgeCM.getHyperEdgeDestinationDomains() == null) continue;
				
				for(ClassMetadata c : hEdgeCM.getHyperEdgeDestinationDomains()) {
					if(c.getClazz().isAssignableFrom(sourceOrDestDomain)) {
						hyperEdgeClasses.add(hEdgeCM);
						break;
					}
				}
				
			}
			
			
		}
		
		return hyperEdgeClasses;
		
		
	}
	
	public ClassMetadata getClassMetadata(Class<? extends GraphObject> clazz) {
		return classCNameMDMap.get(clazz.getCanonicalName());
	}
	
	public ClassMetadata getClassMetadata(String canonicalName) {
	    return classCNameMDMap.get(canonicalName);
	}

	public Class<? extends GraphObject> getGraphObjectClass(String clsName) {
		ClassMetadata cm = classCNameMDMap.get(clsName);
		if(cm != null) {
			return cm.getClazz();
		}
		return null;
	}

	//fix for dynamic classes from different classloaders
	private String getPkg(Class<?> cls) {
		if( cls.getPackage() != null ) {
			return cls.getPackage().getName();
		} else {
			String cname = cls.getCanonicalName();
			return cname.substring(0, cname.lastIndexOf('.'));
		}
	}
	
	public void deregisterDomain(String ontologyURI, String _package) {

		
		//first iteration to remove edge access
		for( Entry<String, ClassMetadata> entry : classURI2MDMap.entrySet() ) {
			
			ClassMetadata classMD = entry.getValue();
			if(!ontologyURI.equals( classMD.getOntologyURI())) {
				continue;
			}
			
			if(VITAL_Edge.class.isAssignableFrom(classMD.getClazz())) {
				
				Set<ClassMetadata> srcTypes = new HashSet<ClassMetadata>();  
				Set<ClassMetadata> destTypes = new HashSet<ClassMetadata>();  
				for(ClassMetadata sourceClass : classMD.getEdgeSourceDomains()) {
					srcTypes.add(sourceClass);
				}
				for(ClassMetadata destClass : classMD.getEdgeSourceDomains()) {
					destTypes.add(destClass);
				}
				
				for(ClassMetadata sourceClass : classMD.getEdgeSourceDomains()) {
					boolean bothSrcAndDest = srcTypes.contains(sourceClass) && destTypes.contains(sourceClass);
					EdgeAccessAssigner.removeDynamicEdgeAccess((Class<? extends VITAL_Edge>)classMD.getClazz(), (Class<? extends VITAL_Node>)sourceClass.getClazz(), true, bothSrcAndDest, classMD.getEdgeSingleName(), classMD.getEdgePluralName());
				}
				
				for(ClassMetadata destClass : classMD.getEdgeDestinationDomains()) {
					boolean bothSrcAndDest = srcTypes.contains(destClass) && destTypes.contains(destClass);
					EdgeAccessAssigner.removeDynamicEdgeAccess((Class<? extends VITAL_Edge>)classMD.getClazz(), (Class<? extends VITAL_Node>)destClass.getClazz(), false, bothSrcAndDest, classMD.getEdgeSingleName(), classMD.getEdgePluralName());
				}
				
			}
			
		}
		
		for( Iterator<Entry<String, ClassMetadata>> iter = classURI2MDMap.entrySet().iterator(); iter.hasNext(); ) {
			
			Entry<String, ClassMetadata> entry = iter.next(); 
			
			ClassMetadata classMD = entry.getValue();

			if(!ontologyURI.equals(classMD.getOntologyURI())) {
				continue;
			}
			
			ClassMetadata parentClass = classMD.getParentClass();
			if(parentClass != null) {
				parentClass.getChildren().remove(classMD);
			}

			
			String shortname = classMD.getSimpleName();
			List<ClassMetadata> list = shortnameMap.get(shortname);
			if(list != null) {
				list.remove(classMD);
				if(list.size() == 0) shortnameMap.remove(shortname);
			}
			
			classCNameMDMap.remove(classMD.getCanonicalName());
			
			iter.remove();
		}
		
		
	}

	public List<ClassMetadata> listClassesWithShortName(String name) {
		List<ClassMetadata> list = shortnameMap.get(name);
		if(list == null) return Collections.emptyList();
		return new ArrayList<ClassMetadata>(list);
	}

	
	
	/** 
	 * Paths are limited to taxonomy edges only, peer edges limited to depth=1
	 * @param cls
	 * @return
	 * @throws Exception 
	 */
	public List<List<PathElement>> getPaths(Class<? extends GraphObject> cls) throws Exception {
		return getPaths(cls, true);
	}

	/**
	 * Paths are limited to taxonomy edges only, peer edges limited to depth=1
	 * @param cls
	 * @param forwardNotReverse
	 * @return
	 * @throws Exception 
	 */
	public List<List<PathElement>> getPaths(Class<? extends GraphObject> cls, boolean forwardNotReverse) throws Exception {
		
		List<List<PathElement>> paths = new ArrayList<List<PathElement>>();
		
		if( ! VITAL_Node.class.isAssignableFrom(cls)) return paths; 
		

		List<PathElement> context = new ArrayList<PathElement>();
		
		List<Class<? extends GraphObject>> parentClasses = new ArrayList<Class< ? extends GraphObject>>();
		
		getPathsInner(cls, paths, context, parentClasses, forwardNotReverse);
		
		
		for(Iterator<List<PathElement>> iter = paths.iterator(); iter.hasNext(); ) {
			
			List<PathElement> l = iter.next();
			
			if(l.size() < 1) {
				
				iter.remove();
				
			}
			
		}
		
		return paths;
		
	}
	
	private List<PathElement> clonedList(List<PathElement> input) {
		
		List<PathElement> res = new ArrayList<PathElement>(input.size());
		
		for(PathElement pe : input) {
			
			PathElement pec = new PathElement();
			
			pec.setDestNodeTypeURI(pe.getDestNodeTypeURI());
			pec.setEdgeClass(pe.getEdgeClass());
			pec.setEdgeTypeURI(pe.getEdgeTypeURI());
			pec.setHyperedge(pe.isHyperedge());
			pec.setReversed(pe.isReversed());
			
			res.add(pec);
			
		}
		
		return res;
		
	}

	private void getPathsInner(Class<? extends GraphObject> cls, List<List<PathElement>> paths, List<PathElement> parentContext, List<Class<? extends GraphObject>> parentClasses, boolean forwardNotReverse) throws Exception {

		boolean depth1 = parentClasses.size() < 1;
		
		parentClasses.add(cls);
		
		for(ClassMetadata cm : classURI2MDMap.values()) {
			
			if(!VITAL_Edge.class.isAssignableFrom(cm.getClazz())) continue;
			
			List<ClassMetadata> endpointDomains = null;
			
			List<ClassMetadata> targetDomains = null;
			
			if(forwardNotReverse) {
				endpointDomains = cm.getEdgeSourceDomains();
				targetDomains = cm.getEdgeDestinationDomains();
			} else {
				endpointDomains = cm.getEdgeDestinationDomains();
				targetDomains = cm.getEdgeSourceDomains();
			}
			
			boolean isDomain = false;

			if(endpointDomains == null) throw new RuntimeException("No endpoint domains for edge: " + cm.getClazz().getCanonicalName());
			
			for(ClassMetadata d : endpointDomains) {
				
				if(d.getClazz().isAssignableFrom(cls)) {

					isDomain = true;
					
					break;
					
				}
				
			}
			
			if(!isDomain) continue;
			
			for(ClassMetadata d : targetDomains) {
				
				
				PathElement pe = new PathElement();
				pe.setDestNodeTypeURI(d.getURI());
				pe.setEdgeClass(cm.getClazz());
				pe.setEdgeTypeURI(cm.getURI());
				pe.setHyperedge(VITAL_HyperEdge.class.isAssignableFrom( cm.getClazz() ));
				pe.setReversed( ! forwardNotReverse );
				Class<? extends GraphObject> childClass = d.getClazz();
				
				
				//peer edges are only expanded at depth 1
				if(VITAL_PeerEdge.class.isAssignableFrom(pe.getEdgeClass())) {
					
					if(depth1) {
						
						//close the path here
						List<PathElement> clonedContext = clonedList(parentContext);
						clonedContext.add(pe);
						paths.add(clonedContext);
						
					} else {
					
						//ignore
					 
					}
					
					continue;
					
				}
				
				
				boolean loopDetected = false;
				
				boolean acceptedLoop = false;
				
				if(VITAL_TaxonomyEdge.class.isAssignableFrom(pe.getEdgeClass())) {
				
					
					//ignore cases where a taxonomy edge source and desintation domains are the same
					if(cm.getEdgeDestinationDomains().size() == 1 && cm.getEdgeSourceDomains().size() == 1 &&
							cm.getEdgeDestinationDomains().get(0).getClazz().equals(cm.getEdgeSourceDomains().get(0).getClazz())
					) {
						
						acceptedLoop = true;
						
					} else {
						
						//ignore predictor case
						for(Class<? extends GraphObject> p : parentClasses) {
							
							if( childClass.isAssignableFrom(p) || p.isAssignableFrom(childClass)) {
								loopDetected = true;
								break;
							}
							
						}
						
					}
					
				}
				
				// check if we are not going to infinite loop
				if(loopDetected) {
					//close the path here
//				List<PathElement> clonedContext = clonedList(parentContext);
//				clonedContext.add(pe);
//				paths.add(clonedContext);
//				continue;
					
					List<PathElement> clonedContext = clonedList(parentContext);
					clonedContext.add(pe);
					
					String fullPath = "(start)";
					for(PathElement p : clonedContext) {
						fullPath += ( (p.isReversed() ? " <-- " : " --- ") + p.getEdgeTypeURI() + (p.isReversed() ? " --- " : " --> " ) + p.getDestNodeTypeURI());
					}
					
					throw new Exception("Detected loop in taxonomy edges: " + fullPath);
					
				}
				
				
				//close the path here
				List<PathElement> clonedContext = clonedList(parentContext);
				clonedContext.add(pe);
				paths.add(clonedContext);
				
				
				if(acceptedLoop) {
					
				} else {
					List<Class<? extends GraphObject>> clonedParentClasses = new ArrayList<Class<? extends GraphObject>>(parentClasses);
					getPathsInner(childClass, paths, clonedContext, clonedParentClasses, forwardNotReverse);
				}
				
			}
			
		}
		
	}

	public List<ClassMetadata> listAllClasses() {
		return new ArrayList<ClassMetadata>(classURI2MDMap.values())
	}

}
