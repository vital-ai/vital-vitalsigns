package ai.vital.query;

import groovy.lang.GString;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import ai.vital.query.AggregateFunction.Average;
import ai.vital.query.AggregateFunction.Count;
import ai.vital.query.AggregateFunction.CountDistinct;
import ai.vital.query.AggregateFunction.Max;
import ai.vital.query.AggregateFunction.Min;
import ai.vital.query.AggregateFunction.Sum;
import ai.vital.query.ops.DELETE;
import ai.vital.vitalservice.query.AggregationType;
import ai.vital.vitalservice.query.Connector;
import ai.vital.vitalservice.query.Destination;
import ai.vital.vitalservice.query.GraphElement;
import ai.vital.vitalservice.query.QueryContainerType;
import ai.vital.vitalservice.query.SortStyle;
import ai.vital.vitalservice.query.Source;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalservice.query.VitalGraphArcContainer.Capture;
import ai.vital.vitalservice.query.VitalExternalSparqlQuery;
import ai.vital.vitalservice.query.VitalExternalSqlQuery;
import ai.vital.vitalservice.query.VitalGraphArcElement;
import ai.vital.vitalservice.query.VitalGraphBooleanContainer;
import ai.vital.vitalservice.query.VitalGraphCriteriaContainer;
import ai.vital.vitalservice.query.VitalGraphQuery;
import ai.vital.vitalservice.query.VitalGraphQueryContainer;
import ai.vital.vitalservice.query.VitalGraphQueryElement;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalservice.query.VitalGraphQueryTypeCriterion;
import ai.vital.vitalservice.query.VitalGraphValue;
import ai.vital.vitalservice.query.VitalGraphValueCriterion;
import ai.vital.vitalservice.query.VitalSparqlQuery;
import ai.vital.vitalservice.query.VitalGraphValueCriterion.Comparator;
import ai.vital.vitalservice.query.VitalPathQuery;
import ai.vital.vitalservice.query.VitalQuery;
import ai.vital.vitalservice.query.VitalSelectAggregationQuery;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalservice.query.VitalSortProperty;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.datatype.Truth;
import ai.vital.vitalsigns.model.VitalSegment;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.TruthProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.utils.StringUtils;

public class ToQueryImplementation {

	boolean alreadyRun = false;
	
	boolean deleteCase = false;
	
	public VitalQuery toQuery(Query query) {
		
		if(alreadyRun) ex("Cannot run toQuery twice with same implementation object");
		
		alreadyRun = true; 
		
		GRAPH graph = query.getGraph();
		
		SELECT select = query.getSelect();
		
		DELETE delete = query.getDelete();
		
		PATH path = query.getPath();
		
		SPARQL sparql = query.getSparql();
		
		SQL sql = query.getSql();
		
		int c = 0;
		if(delete != null) c++;
		if(graph != null) c++;
		if(select != null) c++;
		if(path != null) c++;
		if(sparql != null) c++;
		if(sql != null) c++;
				
		if( query.getDowngrade() != null ) ex("DOWNGRADE element unsupported in toQuery call.");
		if( query.getExport() != null ) ex("EXPORT element unsupported in toQuery call.");
		if( query.getInsert() != null ) ex("INSERT unsupported in toQuery call.");
		if( query.getInstantiates() != null && query.getInstantiates().size() > 0 ) ex("INSTANTIATE element unsupported in toQuery call.");
		if( query.getUpdate() != null) ex("UPDATE element unsupported in toQuery call.");
		if( query.getUpgrade() != null) ex("UPGRADE element unsupported in toQuery call.");
		
		if(c > 1) ex("Query cannot have more than 1 element set: GRAPH, SELECT, PATH, DELETE, SQL or SPARQL ");
		
		if(c == 0) ex("Query must have either GRAPH, SELECT, PATH, DELETE, SQL or SPARQL element (exactly 1)");
		
		if(select != null) {
			// convert that into select element
			// topArc = new ARC
			return handleSelectQuery(select);
		}

		if(path != null) {
			return handlePathQuery(path);
		}
		
		if(sparql != null) {
		    return handleSparqlQuery(sparql);
		}
		
		if(sql != null) {
		    return handleSqlQuery(sql);
		}
		
		if(delete != null) {
			graph = delete;
			deleteCase = true;
		}
		
		if( graph.getTopArc() == null ) ex("Graph query requires top arc element");
		
		VitalGraphQuery gq = new VitalGraphQuery();
		gq.setLimit(graph.getLimit());
		gq.setOffset(graph.getOffset());
		gq.setReturnSparqlString(graph.isReturnSparqlString());
		gq.setCollectStats(toCollectStats(graph.getCollectStats()));
		gq.setBlockOnTransactionID(graph.getBlockOnTransactionID());
		gq.setSegments(processSegments(graph.getSegments()));
		gq.setTimeout(graph.getTimeout());
		gq.setSortProperties(processSortProperties(gq, graph.getSortProperties()));
		gq.setTopContainer(processArcContainer(null, graph.getTopArc()));
		gq.setPayloads(graph.getInlineObjects());
		gq.setIncludeTotalCount(graph.getIncludeTotalCount());
		gq.setProjectionOnly(graph.getProjection());
		Object sortStyle = graph.getSortStyle();
		if(sortStyle != null) {
		    if(sortStyle instanceof SortStyle) {
		        gq.setSortStyle((SortStyle) sortStyle);
		    } else if(sortStyle instanceof String || sortStyle instanceof GString) {
		        gq.setSortStyle(SortStyle.valueOf(sortStyle.toString()));
		    } else {
		        throw new RuntimeException("Unknown sortStyle value type: " + sortStyle.getClass().getCanonicalName() + ". Expected string/enum");
		    }
		}
		validateGraphQuery(gq);
		
		return gq;
		
	}
	
    private ai.vital.vitalservice.query.CollectStats toCollectStats(String collectStats) {

        if(collectStats == null) return ai.vital.vitalservice.query.CollectStats.none;
        
        return ai.vital.vitalservice.query.CollectStats.valueOf(collectStats);
    }

    private VitalQuery handleSqlQuery(SQL sql) {

	    VitalExternalSqlQuery sqlQuery = new VitalExternalSqlQuery();
	    
	    if(StringUtils.isEmpty(sql.getDatabase())) ex("No database set in SQL");
	    if(StringUtils.isEmpty(sql.getSql())) ex("No sql string set in SQL");
	    
	    sqlQuery.setDatabase(sql.getDatabase());
	    sqlQuery.setSql(sql.getSql());
	    sqlQuery.setTimeout(sql.getTimeout());
	    
        return sqlQuery;
    }

    private VitalQuery handleSparqlQuery(SPARQL sparql) {

        if(StringUtils.isEmpty(sparql.getSparql())) ex("No sparql string set in SPARQL");
        
        
        String database = sparql.getDatabase();
        List<?> segments = sparql.getSegments();
        
        if(database != null && segments != null ) {
            
            throw new RuntimeException("SPARQL 'database' and 'segments' values are mutually exclusive");
        } else if(database != null) {
            VitalExternalSparqlQuery sparqlQuery = new VitalExternalSparqlQuery();
            if(StringUtils.isEmpty(sparql.getDatabase())) ex("SPARQL database must not be empty");
            sparqlQuery.setDatabase(sparql.getDatabase());
            sparqlQuery.setSparql(sparql.getSparql());
            sparqlQuery.setTimeout(sparql.getTimeout());
            return sparqlQuery;
        } else if(segments != null) {
            if(segments.size() < 1) ex("SPARQL segments list must not be empty");
            VitalSparqlQuery sparqlQuery = new VitalSparqlQuery();
            sparqlQuery.setSparql(sparql.getSparql());
            sparqlQuery.setSegments(processSegments(sparql.getSegments()));
            sparqlQuery.setTimeout(sparql.getTimeout());
            return sparqlQuery;
            
        } else {
            throw new RuntimeException("SPARQL requires either 'database' or 'segments' values");
        }
        
        
    }

    private VitalQuery handlePathQuery(PATH path) {
		VitalPathQuery pq = new VitalPathQuery();
		pq.setSegments(processSegments(path.getSegments()));
		pq.setReturnSparqlString(path.isReturnSparqlString());
		pq.setCollectStats(toCollectStats(path.getCollectStats()));
		pq.setBlockOnTransactionID(path.getBlockOnTransactionID());
		pq.setMaxdepth((Integer)path.getMaxdepth());
		pq.setTimeout(path.getTimeout());
		pq.setCountRoot(path.getCountRoot());
	    pq.setLimit(path.getLimit());
	    pq.setOffset(path.getOffset());
	    pq.setProjectionOnly(path.getProjection());

		ARC rootArc = path.getRoot();
		List<?> rootURIs = path.getRootURIs();
		
		if(rootArc != null && rootURIs != null) {
			throw new RuntimeException("PATH must not have both root arc and root uris list");
		}
		
		if(rootArc != null) {
			pq.setRootArc(processArcContainer(null, rootArc));
		}
		
		if(rootURIs != null) {
			
			List<URIProperty> rootURIsList = new ArrayList<URIProperty>();
			
			for(Object o : rootURIs) {
				if(o instanceof String || o instanceof GString) {
					rootURIsList.add(URIProperty.withString(o.toString()));
				} else if(o instanceof URIProperty) {
					rootURIsList.add((URIProperty) o);
				} else {
					throw new RuntimeException("Only string/gstring/uriproperty objects in path's rootURIs list allowed!");
				}
			}
			
			pq.setRootURIs(rootURIsList);
			
		}
		
		
		if( path.getArcs() == null || path.getArcs().size() < 1 ) ex("No ARC elements in a PATH");
		
		for(ARC arc : path.getArcs()) {
			VitalGraphArcContainer c = processArcContainer(null, arc);
			//override arc el
			if(arc.getDirection().equals("forward")) {
				c.setArc(new VitalGraphArcElement(Source.PARENT_SOURCE, Connector.EDGE, Destination.CURRENT));
			} else { 
				c.setArc(new VitalGraphArcElement(Source.CURRENT, Connector.EDGE, Destination.PARENT_SOURCE));
			}
			pq.getArcs().add(c);
		}
		
		return pq;
	}

	private void validateGraphQuery(VitalGraphQuery gq) {

		Set<String> bindNames = new HashSet<String>();
		
		VitalGraphArcContainer tc = gq.getTopContainer();
		
		validateArcContainer(bindNames, null, tc);
		
	}

	private void validateArcContainer(Set<String> bindNames, VitalGraphQueryContainer<?> parent,
			VitalGraphQueryContainer<?> tcx) {

		if(tcx instanceof VitalGraphArcContainer) {
			
			VitalGraphArcContainer tc = (VitalGraphArcContainer) tcx;
			
//			VitalGraphArcElement arc = tc.getArc();
			
			if(tc.getSourceBind() != null) {
				
//				if(parent != null) {
//					ex("source bind only allowed in top arc container");
//				}
				
				if(!deleteCase) {
//					if(tc.getCapture() != Capture.SOURCE) {
//						ex("in top arc capture must be set to SOURCE in order to use bound variable");
//					}
				}
				
				if(!bindNames.add(tc.getSourceBind())) {
					ex("variable bound more than once: " + tc.getSourceBind());
				}
				
//				if(!( tc.getCapture() == Capture.BOTH || tc.getCapture() == Capture.TARGET) ) ex("cannot set bound target variable in a non-captured arc target: " + tc.getSourceBind());
				
//				if(arc.source != Source.CURRENT) ex("Cannot bind variable to source, expected CURRENT, got: " + arc.source);
				
				
			}
			
			if(tc.getConnectorBind() != null) {
				
				if( !bindNames.add(tc.getConnectorBind()) ) {
					ex("variable bound more than once: " + tc.getConnectorBind());
				}
				
				if(!deleteCase) {
					if(!( tc.getCapture() == Capture.BOTH || tc.getCapture() == Capture.CONNECTOR) ) ex("cannot set  bound variable in a non-captured arc: " + tc.getConnectorBind());
				}
				
//				if(arc.connector != Connector.EDGE && arc.connector != Connector.HYPEREDGE) ex("Cannot bind variable to connector, expected EDGE|HYOERDGE, got: " + arc.connector);
			}
			
			if(tc.getTargetBind() != null) {
				
				if( !bindNames.add(tc.getTargetBind()) ) {
					ex("variable bound more than once: " + tc.getTargetBind());
				}
				
				if(!deleteCase) {
					if(!( tc.getCapture() == Capture.BOTH || tc.getCapture() == Capture.TARGET) ) ex("cannot set bound variable in non-captured arc: " + tc.getTargetBind());
				}
				
//				if(arc.destination != Destination.CURRENT) ex("Cannot bind variable to destination, expected CURRENT, got: " + arc.destination);
			}
			
		}
		
		
		for(Object o : tcx) {
			
			VitalGraphQueryContainer<?> c = (VitalGraphQueryContainer<?>) o;
			
			if(c instanceof VitalGraphArcContainer || c instanceof VitalGraphBooleanContainer) {
				
				validateArcContainer(bindNames, tcx, c);
				
			}
			
			
		}
		
		
	}

	public static Pattern variablePattern = Pattern.compile("^\\?[a-z][a-z0-9]+\$");
	
	private VitalGraphArcContainer processArcContainer(VitalGraphArcContainer parentArcContainer, ARC_BASE arc) {

		String direction = arc.getDirection();
		
		boolean forward = true;
		if("forward".equals(direction)) {
		} else if("reverse".equals(direction)){
			forward = false;
		} else {
			ex("Unknown direction string: " + direction);
		}
		
		//determine parent
		Source src = null;
		Connector connector = null;
		Destination dest = null;

		HYPER_ARC hArc  = (HYPER_ARC) (arc instanceof HYPER_ARC ? arc : null);

		boolean toSource = false;
		boolean toDestination = false;
		

		if(parentArcContainer == null) {
			
				//just current empty empty or empty current empty
				src = Source.CURRENT;
				connector = Connector.EMPTY;
				dest = Destination.EMPTY;
				
				//always connect source in top arc
				toSource = true;
				
		} else {
			
			VitalGraphArcElement parentArc = parentArcContainer.getArc();
			
			//check if parent is the root arc, then connect to source!
			boolean parentISRoot = false;
			if(parentArc.source == Source.CURRENT && parentArc.connector == Connector.EMPTY && parentArc.destination == Destination.EMPTY) {
				parentISRoot = true;
			}
			
			Source parentSource = parentArc.source;
			Destination parentDestination = parentArc.destination;

			if(parentSource == Source.CURRENT) {
				toSource = true;
			} else if(parentDestination == Destination.CURRENT) {
				toDestination = true;
			}
			
			if(hArc != null) {
				
				connector = Connector.HYPEREDGE;
				
				ArcParent parent = hArc.getParent();
				
				//target is not necessary
				
				if(parent == ArcParent.CONNECTOR) {
					
					if(forward) {
						src = parentISRoot ? Source.PARENT_SOURCE : Source.PARENT_CONNECTOR;
						dest = Destination.CURRENT;
					} else {
						src = Source.CURRENT;
						dest = parentISRoot ? Destination.PARENT_SOURCE : Destination.PARENT_CONNECTOR;
					}
					
				} else if(parent == ArcParent.TARGET) {
					
					if(toSource) {
						
						if(forward) {
							src = parentISRoot ? Source.PARENT_SOURCE : Source.PARENT_SOURCE;
							dest = Destination.CURRENT;
						} else {
							src = Source.CURRENT;
							dest = parentISRoot ? Destination.PARENT_SOURCE : Destination.PARENT_SOURCE;
						}
						
	 				} else if(toDestination) {
	 					
	 					if(forward) {
	 						src = parentISRoot ? Source.PARENT_SOURCE : Source.PARENT_DESTINATION;
	 						dest = Destination.CURRENT;
	 					} else {
	 						src = Source.CURRENT;
	 						dest = parentISRoot ? Destination.PARENT_SOURCE : Destination.PARENT_DESTINATION;
	 					}
	 					
	 				} else ex("Cannot connect HYPER_ARC to parent - not a source or destiantion exposed");
					
				} else if(parent == ArcParent.EMPTY){
					
					ex("Empty hyper arc parent not supported, top arc must be always a regular ARC");
					
				} else {
					ex("Unhanlded parent: " + parent);
				}
				
			} else {

				//determine parent variable
				connector = Connector.EDGE;
				
				
				if(toSource) {
					
					if(forward) {
						src = parentISRoot ? Source.PARENT_SOURCE : Source.PARENT_SOURCE;
						dest = Destination.CURRENT;
					} else {
						src = Source.CURRENT;
						dest = parentISRoot ? Destination.PARENT_SOURCE : Destination.PARENT_SOURCE;
					}
					
 				} else if(toDestination) {
 					
 					if(forward) {
 						src = parentISRoot ? Source.PARENT_SOURCE : Source.PARENT_DESTINATION;
 						dest = Destination.CURRENT;
 					} else {
 						src = Source.CURRENT;
 						dest = parentISRoot ? Destination.PARENT_SOURCE : Destination.PARENT_DESTINATION;
 					}
 					
 				} else ex("Cannot connect ARC to parent - not a source or destiantion exposed");
				
				//check parent
//				parentArcContainer.getArc().source != Source.CURRENT
//				src = forward ? Source.PARENT_SOURCE 
				
			}
			
		}
		
		VitalGraphArcElement arcEl = new VitalGraphArcElement(src, connector, dest);
		
		VitalGraphArcContainer c = new VitalGraphArcContainer(QueryContainerType.and, arcEl);
		c.setCountArc(arc.getCountArc());
		
		if(deleteCase) {
			
//			if(arc.getCapture() != null) ex("Cannot use capture element in DELETE case");
			
			Delete d = arc.getDelete();

			if(d == null) ex("delete value not set in an arc!");
			
			Capture capture = null;
			
			if(parentArcContainer == null) {
				
				if(d != Delete.Source && d != Delete.None) ex("Top arc's delete value expected to be either Source or None: " + d);
				
				if(d == Delete.Source) {
				
					capture = Capture.SOURCE;
					
				} else if(d == Delete.None) {
					
					capture = Capture.NONE;
					
				}
				
			} else {
				
				if(d == Delete.Source) {
					ex("delete: Source may only be set in a top arc");
				} else if(d == Delete.Both || d == Delete.Target) {
					capture = Capture.BOTH;
				} else if(d == Delete.Connector) {
					capture = Capture.CONNECTOR;
				} else if( d == Delete.None) {
					capture = Capture.NONE;
				} else {
					ex("Unhandled delete value: " + d);
				}
				
			}
			
			c.setCapture(capture);
			
		} else {
			
			Delete d = arc.getDelete();
			if(d != null) ex("delete value must not be used in a GRAPH query ARC!");
			
			if(parentArcContainer == null) {
				c.setCapture(Capture.SOURCE);
			} else {
				Capture capture = Capture.valueOf(arc.getCapture().name());
				c.setCapture(capture);
			}
			
			
		}
		
		c.setOptional(arc.getOptional());
		
		c.setLabel(arc.getLabel());
		
		VitalGraphCriteriaContainer topCC = new VitalGraphCriteriaContainer(QueryContainerType.and);
		
		for( Object o : arc.getChildren() ) {
			
			if(o instanceof ARC_BASE) {
				
				c.add(processArcContainer(c, (ARC_BASE) o));
				
			} else if(o instanceof ARC_BOOLEAN) {
				
				c.add( processARCBooleanContainer(c, arc, (ARC_BOOLEAN)o) );
				
			} else if(o instanceof CONSTRAINT_BOOLEAN) {
				
				VitalGraphCriteriaContainer container = processCONSTRAINTBOOLEAN(c, arc, (CONSTRAINT_BOOLEAN) o, false, parentArcContainer == null);
				
				topCC.add(container);
				
			} else if(o instanceof BaseConstraint) {
				
				BaseConstraint bc = (BaseConstraint) o;
				VitalGraphQueryPropertyCriterion crit = processBaseConstraint(c, arc, bc, parentArcContainer == null);
				
				topCC.add(truthCriterionFilter( crit ));
				
				//validate the type constraint
				
			} else if(o instanceof Constraint) {
				
				//root constraints may provide eval, other not
				
				Constraint con = (Constraint) o;
				
				String eval = con.getEval();
				
				if(eval != null) {
					
					//evaluate
					String[] split = eval.trim().split("\\s+");
					
					if(split.length != 3) ex("Expected 3 columns in " + eval);
					
					String name1 = split[0];
					String comparator = split[1];
					String name2 = split[2];
					
					if(!name1.startsWith("?")) ex("variable 1 name must start with a question mark");
					if(!name2.startsWith("?")) ex("variable 2 name must start with a question mark");
					
					if( ! variablePattern.matcher(name1).matches() ) ex("variable 1 name does not match pattern: " + variablePattern.pattern());
					if( ! variablePattern.matcher(name2).matches() ) ex("variable 2 name does not match pattern: " + variablePattern.pattern());
					
//					if(name1.length() < 2) ex('')
					
					Comparator comp = Comparator.fromString(comparator);
					
					VitalGraphValueCriterion valCrit = new VitalGraphValueCriterion(name1.substring(1), comp, name2.substring(1));
					
					c.getValueCriteria().add(valCrit);
					
//					String
					
//					!=, ==, >, <, <=, >=,
					
					
				} else {
					ex("Only constraints with evaluation string allowed in arcs");
				}
				
			} else if(o instanceof BaseProvides) {
				
				BaseProvides p = (BaseProvides) o;
				
				VitalGraphValue val = processProvides(c, arc, p, parentArcContainer == null);

				c.addProvides(p.getAlias(), val);
				
			} else if(o instanceof Target) {
				
				processTarget(c, topCC, arc, (Target) o, true, parentArcContainer == null);
				
			} else if(o instanceof ai.vital.query.Connector) {
				
				ai.vital.query.Connector con = (ai.vital.query.Connector) o;
				
				processConnector(c, topCC, arc, con, true, parentArcContainer == null);
				
			} else if(o instanceof ai.vital.query.Source) {
				
				ai.vital.query.Source s = (ai.vital.query.Source) o;
				
//				if(parentArcContainer != null) throw new RuntimeException("Source nodes only allowed in top ARC");
				
				Bind b = s.getBind();
				if(b == null) throw new RuntimeException("No Bind node in Source!");
				if(b.getName() == null || b.getName().isEmpty()) ex("No bind name in Source.Bind");
				c.setSourceBind(b.getName());
				
			} else if(o instanceof BaseBind) {
				
				BaseBind bb = (BaseBind) o;
				
				if(bb.getName() == null || bb.getName().isEmpty()) ex("No bind name in " + bb.getClass().getSimpleName());
				
				if(bb instanceof NodeBind) {
					if(parentArcContainer == null) {
						//special case for root arc
						c.setSourceBind(bb.getName());
					} else {
					    
					    if(forward) {
					        c.setTargetBind(bb.getName());
					    } else {
					        c.setSourceBind(bb.getName());
					    }
					    
					}
				} else if(bb instanceof EdgeBind) {
					c.setConnectorBind(bb.getName());
				} else {
					ex("Unhandled basebind: " + bb.getClass().getSimpleName());
				}
				
			} else if(o == null) {
				
				ex("Null " + arc.getClass().getSimpleName() + " child");
				
			} else if(o instanceof StartPath) {
				
				ex("toQuery with start_path elements unsupported");
				
			} else if(o instanceof EndPath) {
				
				ex("toQuery with end_path element unsupported");
				
			} else {
				
				ex("Unexpected child of an " + arc.getClass().getSimpleName() + " : " + o.getClass().getCanonicalName());
				
			}
			
		}
		
		if(topCC.size() == 1 && topCC.get(0) instanceof VitalGraphCriteriaContainer) {
			topCC = (VitalGraphCriteriaContainer) topCC.get(0);
		}
		
		if(topCC.size() > 0) {
			c.add(topCC);
		}
		
		return c;
	}

	private VitalGraphQueryElement truthCriterionFilter(
            VitalGraphQueryPropertyCriterion crit) {

	    if(crit instanceof VitalGraphQueryTypeCriterion) return crit;
	    
	    Object value = crit.getValue();
	    
	    if(!( value instanceof Truth)) return crit;
	    
	    String pURI = crit.getPropertyURI();
	    PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(pURI);
//	    if(pm == null) return crit;
	    
	    Truth t = (Truth) value;
	    
	    if( t != Truth.UNKNOWN) return crit;
	    
	    if( pm != null && pm.getBaseClass() == TruthProperty.class ) {
	        
	        VitalGraphCriteriaContainer cc = new VitalGraphCriteriaContainer(crit.isNegative() ? QueryContainerType.and : QueryContainerType.or);
	        
	        VitalGraphQueryPropertyCriterion newCrit = new VitalGraphQueryPropertyCriterion(crit.getSymbol(), pm.getPattern(), null);
	        newCrit.setExpandProperty(crit.isExpandProperty());
	        
	        newCrit.setComparator(crit.isNegative() ? VitalGraphQueryPropertyCriterion.Comparator.EXISTS: VitalGraphQueryPropertyCriterion.Comparator.NOT_EXISTS);
	        
	        cc.add(crit);
	        cc.add(newCrit);
	        
	        return cc;
	        
	        
	    } else {
	        
	        crit.setValue(null);
	        crit.setComparator(crit.isNegative() ? VitalGraphQueryPropertyCriterion.Comparator.EXISTS : VitalGraphQueryPropertyCriterion.Comparator.NOT_EXISTS );
	        
	        return crit;
	        
	    }
	    
    }

    private void processConnector(VitalGraphArcContainer c,
			VitalGraphCriteriaContainer parentCC, ARC_BASE arc,
			ai.vital.query.Connector con, boolean root, boolean topArc) {

		
		if(arc instanceof ARC) {
			
//			ex("target may only be set in HYPER_ARC");
			if(topArc) ex("target element must not be located in top arc");
			VitalGraphQueryPropertyCriterion criterion = null;
			if( con.getRawConstraint() != null ) {
				criterion = con.getRawConstraint().getCriterion();
			} else if(con.getConstraint() instanceof EdgeConstraint) {
				criterion = con.getConstraint().getCriterion();
			} else {
				ex("only raw constraint or edge_constraint expected in an ARC container ");
			}
			
			if(criterion == null) ex("No criterion set in ARC/connector/ element");
			
			criterion.setSymbol( GraphElement.Connector );
			
			parentCC.add(truthCriterionFilter(criterion));

			return;
			
		}
		
		BaseConstraint constraint = con.getConstraint();
		BaseProvides provides = con.getProvides();
		
		if(constraint != null && provides != null) ex("Connector element mustn't have both constraint and 'provides' set");
		
		int elementsCount = 0;
		
		if(constraint != null) {
			
		    elementsCount ++;
		    
			if(!( constraint instanceof HyperEdgeConstraint)) ex("Only hyperedge_constraint allowed in connector element");
			
			VitalGraphQueryPropertyCriterion criterion = constraint.getCriterion();
			criterion.setSymbol(GraphElement.Connector);
			parentCC.add(truthCriterionFilter(criterion));
			
		}
		
		if(provides != null) {
		
		    elementsCount++;
		    
			if(!root) ex("*_provides elements are only allowed in connectors of HYPER_ARC element, not nested boolean containers");
			
			if(!(provides instanceof HyperEdgeProvides)) ex("Only hyperedge_provides allowed in connector element");
			
			VitalGraphValue value = null;
			GraphElement symbol = "forward".equals( arc.getDirection() ) ? GraphElement.Destination : GraphElement.Source;
			
			
		    if(provides.isUri()) {
	            value = new VitalGraphValue(symbol, VitalGraphValue.URI);
	        } else if(provides.getPropertyURI() != null) {
	            value = new VitalGraphValue(symbol, provides.getPropertyURI());
	        } else if(provides.getProperty() != null) {
	            value = new VitalGraphValue(symbol, provides.getProperty());
	        } else {
	            throw new RuntimeException("Neither URI, propertyURI nor property set in BaseProvides object");
	        }

			c.addProvides(provides.getAlias(), value);

		}
		
		Bind b = con.getBind();
		
        if(b != null) {
            elementsCount++;
            if(b.getName() == null || b.getName().isEmpty()) ex("No bind name in Connector.Bind");
            c.setConnectorBind(b.getName());
        }
		
		if(elementsCount == 0) {
			ex("Connector element mustn't be empty");
		}
		
	}

	private void processTarget(VitalGraphArcContainer c, VitalGraphCriteriaContainer parentCC, ARC_BASE arc,
			Target t, boolean root, boolean topArc) {
		
		if(arc instanceof ARC) {
			
//			ex("target may only be set in HYPER_ARC");
			if(topArc) ex("target element must not be located in top arc");
			VitalGraphQueryPropertyCriterion criterion = null;
			if( t.getRawConstraint() != null ) {
				criterion = t.getRawConstraint().getCriterion();
			} else if(t.getConstraint() instanceof NodeConstraint) {
				criterion = t.getConstraint().getCriterion();
			} else {
				ex("only raw constraint or node_constraint expected in an ARC container ");
			}
			
			if(criterion == null) ex("No criterion set in ARC/target/ element");
			
			criterion.setSymbol("forward".equals( arc.getDirection() ) ? GraphElement.Destination : GraphElement.Source );
			
			parentCC.add(truthCriterionFilter(criterion));

			return;
			
		}
		
		ArcTarget target = ((HYPER_ARC)arc).getTarget();
		
		BaseConstraint constraint = t.getConstraint();
		BaseProvides provides = t.getProvides();
		
		Bind bind = t.getBind();
		
		if(constraint != null && provides != null) ex("Target element mustn't have both constraint and 'provides' set");
		
		int elementsCount = 0;
		
		if(constraint != null) {
		
		    elementsCount++;
		    
			if( (constraint instanceof NodeConstraint && target != ArcTarget.NODE)
					|| (constraint instanceof EdgeConstraint && target != ArcTarget.EDGE)
					|| (constraint instanceof HyperEdgeConstraint && target != ArcTarget.HYPEREDGE)
					|| (constraint instanceof HyperNodeConstraint && target != ArcTarget.HYPERNODE)
				) {
				ex("non-matching constraint: " + constraint.getClass().getSimpleName() + " and target type: " + target);
			}

			VitalGraphQueryPropertyCriterion criterion = constraint.getCriterion();
			criterion.setSymbol("forward".equals( arc.getDirection() ) ? GraphElement.Destination : GraphElement.Source );
			
			parentCC.add(truthCriterionFilter(criterion));
			
//			VitalGraphQueryPropertyCriterion crit = constraint.getCriterion();
			
		}
		
		if(provides != null) {
			
		    elementsCount++;
		    
			if(!root) ex("*_provides elements are only allowed in targets of HYPER_ARC element, not nested boolean containers");
			
			if( ( provides instanceof NodeProvides && target != ArcTarget.NODE)
					|| (provides instanceof EdgeProvides && target != ArcTarget.EDGE)
					|| (provides instanceof HyperEdgeProvides && target != ArcTarget.HYPEREDGE)
					|| (provides instanceof HyperNodeProvides && target != ArcTarget.HYPERNODE)
			) {
				ex("non-matching 'provides': " + provides.getClass().getSimpleName());
			}
			
			VitalGraphValue value = null;

			GraphElement symbol = "forward".equals( arc.getDirection() ) ? GraphElement.Destination : GraphElement.Source;
			
            if(provides.isUri()) {
                value = new VitalGraphValue(symbol, VitalGraphValue.URI);
            } else if(provides.getPropertyURI() != null) {
                value = new VitalGraphValue(symbol, provides.getPropertyURI());
            } else if(provides.getProperty() != null) {
                value = new VitalGraphValue(symbol, provides.getProperty());
            } else {
                throw new RuntimeException("Neither URI, propertyURI nor property set in BaseProvides object");
            }			
			c.addProvides(provides.getAlias(), value );

		}
		
		if(bind != null) {
		    elementsCount++;
		    if(bind.getName() == null || bind.getName().isEmpty()) ex("No bind name in Target.Bind");
		    c.setTargetBind(bind.getName());
		    
		}
		
		if(elementsCount == 0) {
			ex("Target element mustn't be empty: constraint/provides or bind expected");
		}
		
	}

	private VitalGraphValue processProvides(VitalGraphArcContainer c,
			ARC_BASE arc, BaseProvides o, boolean topContainer) {

		GraphElement symbol = null;
		
		boolean forward = arc.getDirection().equals("forward");
		
		GraphElement targetSymbol = forward ? GraphElement.Destination : GraphElement.Source;
		
		
		if(arc instanceof HYPER_ARC) {
			
			ArcTarget target = ((HYPER_ARC)arc).getTarget();

			if(o instanceof EdgeProvides) {
				
				if(target == ArcTarget.EDGE) {
					symbol = targetSymbol;
				} else {
					ex("non matching provdes type: " + o.getClass().getSimpleName() + " target: " + target);
				}
					
			} else if(o instanceof HyperEdgeProvides) {
				
				if(target == ArcTarget.HYPEREDGE) {
					ex("hyperedge_provides without connector/target block if target == HYPEREDGE is ambiguous!");
				} else {
					//ok
					symbol = GraphElement.Connector;
				}
				
			} else if(o instanceof NodeProvides) {
				
				if(target == ArcTarget.NODE) {
					symbol = targetSymbol;
				} else {
					ex("non matching provdes type: " + o.getClass().getSimpleName() + " target: " + target);
				}
				
			} else if(o instanceof HyperNodeProvides) {
				
				if(target == ArcTarget.HYPERNODE) {
					symbol = targetSymbol;
				} else {
					ex("non matching provdes type: " + o.getClass().getSimpleName() + " target: " + target);
				}
				
			} else {
				ex("unhandled provides type: " + o.getClass().getCanonicalName());
			}
			
		} else {
			
			if(topContainer) {
				
				//top container always has
				symbol = GraphElement.Source;
				
			} else {
				
				if(o instanceof NodeProvides) {
					symbol = targetSymbol;
				} else if(o instanceof EdgeProvides) {
					symbol = GraphElement.Connector;
				} else {
					ex("Forbidden 'provides' for ARC: " + o.getClass().getSimpleName());
				}
				
			}
			
			
		}
		
		if(o.isUri()) {
			return new VitalGraphValue(symbol, VitalGraphValue.URI);
		} else if(o.getPropertyURI() != null) {
		    return new VitalGraphValue(symbol, o.getPropertyURI());
		} else if(o.getProperty() != null) {
		    return new VitalGraphValue(symbol, o.getProperty());
		} else {
		    throw new RuntimeException("Neither URI, propertyURI nor property set in BaseProvides object");
		}
		
	}

	private VitalGraphQueryPropertyCriterion processBaseConstraint(
			VitalGraphArcContainer c, ARC_BASE b, BaseConstraint bc, boolean topArc) {

		HYPER_ARC hArc = (HYPER_ARC) (b instanceof HYPER_ARC ? b : null);
		
		VitalGraphQueryPropertyCriterion crit = bc.getCriterion();
		
		if(topArc) {
			crit.setSymbol(GraphElement.Source);
			return crit;
		}
		
		if(hArc == null && ( bc instanceof HyperNodeConstraint || bc instanceof HyperEdgeConstraint) ) {
			ex("Cannot use hypernode or hyperedge constraint in ARC");
		}
		
		boolean forward = b.getDirection().equals("forward");
		
		GraphElement targetSymbol = forward ? GraphElement.Destination : GraphElement.Source;
		
		
		if(hArc != null) {
			
			ArcTarget target = hArc.getTarget();
			
			if(bc instanceof HyperEdgeConstraint) {
				
				if(target == ArcTarget.HYPEREDGE) {
					ex("hyperedge_constraint without connter/target block if target == HYPEREDGE is ambiguous!");
				} else {
					//ok
					crit.setSymbol(GraphElement.Connector);
				}
				
			} else if(bc instanceof EdgeConstraint) {
				
				if(target == ArcTarget.EDGE) {
					crit.setSymbol(targetSymbol);
				} else {
					ex("non matching constraint type: " + bc.getClass().getSimpleName() + " where hyper arc target: " + target);
				}
				
			} else if(bc instanceof NodeConstraint) {
				
				if(target == ArcTarget.NODE) {
					crit.setSymbol(targetSymbol);
				} else {
					ex("non matching constraint type: " + bc.getClass().getSimpleName() + " where hyper arc target: " + target);
				}
				
			} else if(bc instanceof HyperNodeConstraint) {
				
				if(target == ArcTarget.HYPERNODE) {
					crit.setSymbol(targetSymbol);
				} else {
					ex("_non matching constraint type: " + bc.getClass().getSimpleName() + " where hyper arc target: " + target);
				}
				
			} else {
				ex("Unexpected constraint type: " + bc.getClass().getCanonicalName());
			}
			
		} else {
			
			if(bc instanceof EdgeConstraint) {
				crit.setSymbol(GraphElement.Connector);
			} else if(bc instanceof NodeConstraint) {
				crit.setSymbol(targetSymbol);
			}
			
		}
		
		//override symbol if it's top arc
		
		if(crit.getSymbol() == null) ex("Null symbol for " + bc.getClass().getCanonicalName());
		return crit;
		
	}

	private VitalGraphBooleanContainer processARCBooleanContainer(VitalGraphArcContainer parentArc, ARC_BASE parentArcDOMAIN, ARC_BOOLEAN o) {
		
		VitalGraphBooleanContainer r = new VitalGraphBooleanContainer(o instanceof ARC_AND ? QueryContainerType.and : QueryContainerType.or);
		
		for(Object c : o.getChildren()) {
			
			if(c instanceof ARC_BASE) {
				
				r.add( processArcContainer(parentArc, (ARC_BASE) c) );
				
			} else if(c instanceof ARC_BOOLEAN) {
				
				r.add( processARCBooleanContainer(parentArc, parentArcDOMAIN, (ARC_BOOLEAN) c));
				
			} else if(c == null) {
				ex("Null child of " + o.getClass().getSimpleName());
			} else {
				ex("Unexpected child of " + o.getClass().getSimpleName() + " : "+ c.getClass().getSimpleName());
			}
			
		}

		return r;
		
	}

	private VitalSelectQuery handleSelectQuery(SELECT select) {

		
		//check aggregation function
		AggregateFunction af = null;
		
		Distinct distinctObj = null;
		
		boolean distinctFirst = false;
		boolean distinctLast = false;
		
		for(Object ch : select.getChildren()) {
			if(ch instanceof AggregateFunction) {
				if(af != null) ex("At most 1 aggregation function allowed in SELECT, got more: " + af.getClass().getSimpleName() + " and " + ch.getClass().getSimpleName());
				af = (AggregateFunction) ch;
			} else if(ch instanceof Distinct) {
				if(distinctObj != null) ex("At most 1 DISTINCT element allowed in SELECT element (optionally wrapped with FIRST/LAST)");
				distinctObj = (Distinct) ch;
			} else if(ch instanceof FirstLast) {
				FirstLast fl = (FirstLast) ch;
				if(fl.getDistinct() == null) throw new RuntimeException((fl.isFirstNotLast() ? "FIRST" : "LAST") + " element must contain DISTINCT node");
				if(distinctObj != null) ex("At most 1 DISTINCT element allowed in SELECT element (optionally wrapped with FIRST/LAST)");
				distinctObj = fl.getDistinct();
				if(fl.isFirstNotLast()) {
					distinctFirst = true;
				} else {
					distinctLast = true;
				}
			}
		}
		
		if(af != null && distinctObj != null) {
			ex("Cannot use both aggregation function and distinct element in a same SELECT");
		}
		
		
		VitalSelectQuery sq = null;
		if(af != null) {
			
			AggregationType at = null;
			
			boolean distinct = false;
			
			if(af instanceof Average) {
				at = AggregationType.average;
			} else if(af instanceof Count || af instanceof CountDistinct){
				at = AggregationType.count;
			} else if(af instanceof Max) {
				at = AggregationType.max;
			} else if(af instanceof Min) {
				at = AggregationType.min;
			} else if(af instanceof Sum) {
				at = AggregationType.sum;
			} else {
				ex("Unsupported aggregation function: " + af.getClass().getSimpleName());
			}
			
			String propertyURI = af.getPropertyURI();
			
			if(af instanceof CountDistinct) {
				
				distinct = true;
				
			}
			
			if(propertyURI == null) ex("Property URI not set in aggregate function");
			
			IProperty property = VitalSigns.get().getProperty(new URIProperty(propertyURI));
			if(property == null) {
				ex("Property with URI not found: " + propertyURI);
			}
			
			sq = new VitalSelectAggregationQuery(at, property);
			sq.setDistinct(distinct);
			
		} else {
			
			sq = new VitalSelectQuery();
			
			if(distinctObj != null) {

				String propertyURI = distinctObj.getPropertyURI();
				
				if(propertyURI == null) ex("Property URI not set in distinct element");
				
				IProperty property = VitalSigns.get().getProperty(new URIProperty(propertyURI));
				if(property == null) {
					ex("Property with URI not found: " + propertyURI);
				}
				
				sq.setDistinct(true);
				sq.setPropertyURI(propertyURI);
				sq.setDistinctExpandProperty(distinctObj.isExpandProperty());
				sq.setDistinctSort(distinctObj.getOrder() != null ? distinctObj.getOrder().toShortString() : null);
				
				if(sq.getDistinctSort() == null && !( distinctFirst || distinctLast)){
					ex("FIRST/LAST child DISTINCT node must define sort order");
				}
				
				
			}
			
		}
				
		
		sq.setProjectionOnly(select.isProjection());
		sq.setLimit(select.getLimit());
		sq.setOffset(select.getOffset());
		sq.setReturnSparqlString(select.isReturnSparqlString());
		sq.setCollectStats(toCollectStats(select.getCollectStats()));
		sq.setBlockOnTransactionID(select.getBlockOnTransactionID());
		sq.setSegments(processSegments( select.getSegments()) );
		sq.setSortProperties(processSortProperties(sq, select.getSortProperties()));
		sq.setTimeout(select.getTimeout());
		sq.setDistinctFirst(distinctFirst);
		sq.setDistinctLast(distinctLast);
		
		VitalGraphArcElement topArcEl = new VitalGraphArcElement(Source.CURRENT, Connector.EMPTY, Destination.EMPTY);
		
		VitalGraphArcContainer topArc = new VitalGraphArcContainer(QueryContainerType.and, topArcEl);
		
		sq.setTopContainer(topArc);
		
		
		VitalGraphCriteriaContainer topCriteriaContainer = new VitalGraphCriteriaContainer(QueryContainerType.and);
		
		for(Object child : select.getChildren() ) {
			
			if(child instanceof CONSTRAINT_BOOLEAN) {
				
				CONSTRAINT_BOOLEAN b = (CONSTRAINT_BOOLEAN) child;
				
				topCriteriaContainer.add(processCONSTRAINTBOOLEAN(topArc, new ARC(), b, true, true));
				
			} else if(child instanceof Constraint) {
				
				Constraint c = (Constraint) child;
				
				VitalGraphQueryPropertyCriterion pc = c.getCriterion();

				if(pc == null) ex("Unsupported constraint in select - constraint: " + pc + " eval: "  + c.getEval() + " type: " + c.getType());
				
				pc.setSymbol(GraphElement.Source);
				
				topCriteriaContainer.add(truthCriterionFilter(pc));
				
				
			} else if(child instanceof BaseConstraint) {
				
				BaseConstraint c = (BaseConstraint) child;
				
				VitalGraphQueryPropertyCriterion pc = c.getCriterion();
				
				if(pc == null) ex("error: property criterion in " + child.getClass().getSimpleName());
				
				pc.setSymbol(GraphElement.Source);
				
				topCriteriaContainer.add(truthCriterionFilter(pc));
				
			} else if(child instanceof BaseBind) {
				
				BaseBind b = (BaseBind) child;
				
				topArc.setSourceBind(b.getName());
				
			} else if(child instanceof AggregateFunction) {
				
				//ignore
				
			} else if(child instanceof Distinct) {
				
				//ignore
				
			} else if(child instanceof FirstLast) {
				
				//ignore
				
			} else if(child instanceof SortBy) {

				SortBy sb = (SortBy) child;
				
				boolean desc = false;
				if(sb.getOrder() != null && sb.getOrder().toShortString().equals("desc")) {
					desc = true;
				}
				
				VitalSortProperty sort = new VitalSortProperty(sb.getPropertyURI(), null, desc);
				
				sq.getSortProperties().add(sort);
				
			} else {
				
				ex("Unexpected child of a SELECT: " + child);
				
			}
			
		}
		
		if(topCriteriaContainer.size() < 1) ex("No constraints specified in SELECT");
		
		
		//unwrap container if it's a single nested element
		if(topCriteriaContainer.size() == 1 && topCriteriaContainer.get(0) instanceof VitalGraphCriteriaContainer) {
			topCriteriaContainer = (VitalGraphCriteriaContainer) topCriteriaContainer.get(0);
		}
		
		topArc.add(topCriteriaContainer);
		
		return sq;
		
	}

	private VitalGraphCriteriaContainer processCONSTRAINTBOOLEAN(VitalGraphArcContainer parentArc, ARC_BASE parentArcDOMAIN,
			CONSTRAINT_BOOLEAN b, boolean selectQuery, boolean topArc) {

		VitalGraphCriteriaContainer c = new VitalGraphCriteriaContainer(b instanceof AND ? QueryContainerType.and : QueryContainerType.or); 
		
		for(Object o : b.getChildren()) {
			
			if(o instanceof CONSTRAINT_BOOLEAN) {
				
				c.add( processCONSTRAINTBOOLEAN(parentArc, parentArcDOMAIN, (CONSTRAINT_BOOLEAN) o, selectQuery , topArc ));
				
			} else if(o instanceof Constraint ) {
				
				if(!selectQuery) ex("constraint elements only allowed as (hyper)arc children");
				
				Constraint con = (Constraint) o;
				
				VitalGraphQueryPropertyCriterion pc = con.getCriterion();
				
				pc.setSymbol(GraphElement.Source);
				
//				if(pc.getSymbol() == null ) ex("Null symbol: " + pc);
				
				Type type = con.getType();
				
				String eval = con.getEval();
				
				if(selectQuery && pc == null) {
					ex("Unsupported constraint in select - constraint: " + pc + " eval: "  + con.getEval() + " type: " + con.getType());
				}
				
				if(pc != null) {
					c.add(truthCriterionFilter(pc));
				} else if(type != null) {
					ex("Type not implemented yet!");
				} else if(eval != null) {
					ex("Eval not implemented yet!");
				} else { 
					ex("No field set in Constraint");
				}
				
				
			} else if(o instanceof BaseConstraint) {
				
//				if(selectQuery) ex("SELECT query does not support: " + o.getClass().getSimpleName());
				
				VitalGraphQueryPropertyCriterion pc = processBaseConstraint(parentArc, parentArcDOMAIN, (BaseConstraint) o, topArc);
				c.add(truthCriterionFilter(pc));
				
			} else if(o instanceof Target) {
				
				Target t = (Target) o;
				
				processTarget(parentArc, c, parentArcDOMAIN, t, false, topArc);
				
			} else if(o instanceof ai.vital.query.Connector) {
				
				ai.vital.query.Connector con = (ai.vital.query.Connector) o;
				
				processConnector(parentArc, c, parentArcDOMAIN, con, false, topArc);
				
			} else if(o instanceof ai.vital.query.Source) {
			    
			    ai.vital.query.Source src = (ai.vital.query.Source) o;
			    
			    //TODO
				
			} else {
				
				ex("unexpected child of constraint boolean " + b.getClass().getSimpleName() + " : " + o);
			}
		
			
		}
		
//		for(VitalGraphQueryElement el : b)
		
		return c;
	}

	private List<VitalSortProperty> processSortProperties(VitalQuery query, List sortProperties) {

		if(sortProperties == null) ex("sort properties list cannot be null!");
		
		List<VitalSortProperty> res = new ArrayList<VitalSortProperty>(sortProperties.size());
		for(Object o : sortProperties) {
			if(o instanceof VitalSortProperty) {
				VitalSortProperty vsp = (VitalSortProperty) o;
				if(query instanceof VitalGraphQuery) {
				    if(vsp.getProvidedName() == null || vsp.getProvidedName().isEmpty()) {
				        ex("VitalSortProperty in a GRAPH query must have providedName set");
				    }
				} else if(query instanceof VitalSelectQuery) {
				    if(vsp.getPropertyURI() == null || vsp.getPropertyURI().isEmpty()) {
				        ex("VitalSortProperty in a SELECT query must have property set");
				    }
				}
                res.add(vsp);
			} else if(o == null) {
				ex("Sort property cannot be null");
			} else {
				ex("Unexpected sort property type: " + o.getClass().getCanonicalName());
			}
		}
		
		return res;
	}

	private List<VitalSegment> processSegments(List segments) {

		if(segments == null) ex("segments list cannot be null!");
		
		List<VitalSegment> segs = new ArrayList<VitalSegment>(segments.size());
		
		for(Object seg : segments) {
			
			if(seg instanceof VitalSegment) {
				segs.add((VitalSegment) seg);
			} else if(seg instanceof String) {
				segs.add(VitalSegment.withId((String) seg));
			} else if(seg == null) { ex("Segment cannot be null");
			} else {
				ex("Unexpected segment object type: " + seg.getClass().getCanonicalName());
			}
			
		}
		
		return segs;
		
	}

	private void ex(String m) { throw new RuntimeException(m); }
	
}
