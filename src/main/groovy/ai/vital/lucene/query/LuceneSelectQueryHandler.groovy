package ai.vital.lucene.query;

import gnu.trove.map.hash.TIntDoubleHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.DocIdBitSet;

import ai.vital.lucene.model.LuceneSegment;
import ai.vital.lucene.model.VitalSignsLuceneBridge;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalservice.query.AggregationType;
import ai.vital.vitalservice.query.QueryContainerType;
import ai.vital.vitalservice.query.QueryStats;
import ai.vital.vitalservice.query.QueryTime;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalExportQuery;
import ai.vital.vitalservice.query.VitalGraphArcContainer;
import ai.vital.vitalservice.query.VitalGraphCriteriaContainer;
import ai.vital.vitalservice.query.VitalGraphQueryContainer;
import ai.vital.vitalservice.query.VitalSelectAggregationQuery;
import ai.vital.vitalservice.query.VitalSelectQuery;
import ai.vital.vitalservice.query.VitalSortProperty;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.property.BooleanProperty;
import ai.vital.vitalsigns.model.property.DateProperty;
import ai.vital.vitalsigns.model.property.NumberProperty;
import ai.vital.vitalsigns.model.property.StringProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.properties.PropertyMetadata;

public class LuceneSelectQueryHandler {

	private int maxDoc;
	
	private VitalSelectQuery sq;

	private IndexSearcher searcher;

	private LuceneSegment segment;
	
	private String aggregationPropertyURI;
	
	private AggregationType aggregationType;
	
	private QueryStats queryStats = null;
	
	private long docsGetNanoTime = 0L;
	
	private boolean collectResolvedFields = false;
	
	//field cache !
	private String[] uris;

	private static class BitSetResults {
		
		public BitSetResults(BitSet docs, TIntDoubleHashMap scores) {
			super();
			this.docs = docs;
			this.scores = scores;
		}
		BitSet docs;
		TIntDoubleHashMap scores;
	}
	
	public LuceneSelectQueryHandler(VitalSelectQuery sq, LuceneSegment segment, IndexSearcher searcher, QueryStats queryStats) {
	    this.queryStats = queryStats;
		this.sq = sq;
		if(sq instanceof VitalSelectAggregationQuery) {
			
			if(!segment.getConfig().isStoreObjects()) throw new RuntimeException("Aggregation functions are only supported in an index which also stores field values.");
			
			VitalSelectAggregationQuery vsaq = (VitalSelectAggregationQuery) sq;
			if(vsaq.getAggregationType() == null) throw new NullPointerException("Null aggregation type in " + VitalSelectAggregationQuery.class.getSimpleName());
			aggregationType = vsaq.getAggregationType();
			if(aggregationType != AggregationType.count) {
				if(vsaq.getPropertyURI() == null) throw new NullPointerException("Null vital property in " + VitalSelectAggregationQuery.class.getSimpleName());
			}
			if(vsaq.getPropertyURI() != null) {
				aggregationPropertyURI = vsaq.getPropertyURI();
			}
		} else if(sq.getDistinct()) {
			
			if(!segment.getConfig().isStoreObjects()) throw new RuntimeException("Distinct values selector is only supported in an index which also stored field values");
			
			aggregationPropertyURI = sq.getPropertyURI();
		}
		
		this.segment = segment;
		this.searcher = searcher;
	}

    public boolean isCollectResolvedFields() {
        return collectResolvedFields;
    }

    public void setCollectResolvedFields(boolean collectResolvedFields) {
        this.collectResolvedFields = collectResolvedFields;
    }



    public URIResultList handleSelectQuery() throws IOException {
		
		URIResultList rs = new URIResultList();
		rs.setOffset(this.sq.getOffset());
		rs.setLimit(this.sq.getLimit());
		
//		uris = FieldCache.DEFAULT.getStrings(searcher.getIndexReader(), Index.URI_FIELD);
		
		long start = System.currentTimeMillis();
		this.maxDoc = searcher.getIndexReader().maxDoc();
		if(queryStats != null) {
		    long time = queryStats.addDatabaseTimeFrom(start);
		    if(queryStats.getQueriesTimes() != null) queryStats.getQueriesTimes().add(new QueryTime("Get maxDoc value", "maxDoc", time));
		}
		
		//start processing bottom to top
		
//		List<DataObject> dataObjects = new ArrayList<DataObject>();
		
		//determine the container
		VitalGraphArcContainer topArcContainer = sq.getTopContainer();
		
		List<VitalGraphCriteriaContainer> topContainers = new ArrayList<VitalGraphCriteriaContainer>();
		
		//analyze it
		for(VitalGraphQueryContainer<?> c : topArcContainer) {
			
			if(c instanceof VitalGraphArcContainer) {
				throw new RuntimeException("Select query must not have sub arcs");
			} else if(c instanceof VitalGraphCriteriaContainer) {
				topContainers.add((VitalGraphCriteriaContainer) c);
			} else {
				throw new RuntimeException("Unexpected element in select query: " + c);
			}
			
		}
		
		
		if(topContainers.size() == 0) {
			throw new RuntimeException("No constraint containers in select query");
		}
		
		VitalGraphCriteriaContainer topContainer = null;
		if(topContainers.size() > 1) {
			topContainer = new VitalGraphCriteriaContainer(QueryContainerType.or);
			topContainer.addAll(topContainers);
		} else {
			topContainer = topContainers.get(0);
		}
		
		BitSetResults matchingDocsResults = processContainer(topContainer);

		final BitSet matchingDocs = matchingDocsResults.docs;
		
		if(sq.isProjectionOnly()) {
			rs.setResults(new ArrayList<URIResultElement>(0));
			rs.setTotalResults(matchingDocs.cardinality());
			addBatchGetStats(rs);
			return rs;
		}
		
		//shortcut for agg count without property
		if(aggregationType == AggregationType.count && aggregationPropertyURI == null) {
			rs.setResults(new ArrayList<URIResultElement>(0));
			rs.setAgg_count(matchingDocs.cardinality());
			rs.setTotalResults(matchingDocs.cardinality());
			addBatchGetStats(rs);
			return rs;
		}
		
		int offset = sq.getOffset();
		if(offset < 0) offset = 0;
		
		int limit = sq.getLimit();
		//backward compatibility?
		if(limit <= 0) limit = 20;
		
		//so we have matching docs, now it's time to sort it and page it
		List<VitalSortProperty> sortProperties = sq.getSortProperties();
		
		
		//aggregation collectors
		Integer agg_count = null;
		Integer agg_count_distinct = null;
		Double agg_sum = null;
		
		Double agg_min = null;
		Double agg_max = null;
		
		
		Set<String> aggregationField = null;
		
		Set<Object> uniqueValues = null;
		
		Set<String> aggregationPropertiesURIs = new HashSet<String>();
		
		if( aggregationType != null || sq.isDistinct() ) {
		
			offset = 0;
			limit = Integer.MAX_VALUE;
			
			//do not sort, use indexorder non-inversed
			sortProperties = Arrays.asList(new VitalSortProperty(VitalSortProperty.INDEXORDER, "providedName", false));
			
			agg_count = new Integer(0);
			agg_sum   = new Double(0);
			
			aggregationField = new HashSet<String>();
			aggregationField.add(aggregationPropertyURI);
			aggregationPropertiesURIs.add(aggregationPropertyURI);
			
			if(sq.isDistinct()) {
				agg_count_distinct = new Integer(0);
				uniqueValues = new HashSet<Object>();
			}
			
		}
		
		if(aggregationType == null && sq.isDistinct()) {
			uniqueValues = new HashSet<Object>();
			
			if(sq.isDistinctExpandProperty()) {
				
				PropertyMetadata pm = VitalSigns.get().getPropertiesRegistry().getProperty(aggregationPropertyURI);
				if(pm == null) throw new RuntimeException("Property not found: " + aggregationPropertyURI);
				for(PropertyMetadata p : VitalSigns.get().getPropertiesRegistry().getSubProperties(pm, false)) {
					aggregationPropertiesURIs.add(p.getURI());
					aggregationField.add(p.getURI());
				}
				
			}
		}
		
		
		boolean useIndexedOrder = false;
		
		boolean useScoreOrder = false;
		
		boolean inverse = false;
		
		
		VitalSortProperty sp = null;
		
//		FieldSelector spfs = null;
		Set<String> spfs = null;
		
		SortField sortField = null;
		
		List<SortPropertyColumn> sortPropertiesColumns = null;
		
		boolean innerSort = false;
		
		if( sortProperties.size() == 0 ) {
			
			useScoreOrder = true;
			inverse = true;
			
			innerSort = true;
			
		} else if(sortProperties.size() == 1) {
			
			
			sp = sortProperties.get(0);
			
			inverse = sp.isReverse();
			
			if(sp.getPropertyURI() == null || sp.getPropertyURI().isEmpty()) throw new RuntimeException("VitalSortProperty in a SELECT query must provide property");
			
			if(sp.getPropertyURI().equals(VitalSortProperty.INDEXORDER)) {
				useIndexedOrder = true;
				innerSort = true;
			} else if(sp.getPropertyURI().equals(VitalSortProperty.RELEVANCE)) {
				useScoreOrder = true;
				innerSort = true;
			} else {
				/*
				final String pURI = sp.getPropertyURI();

				String fixed = pURI;
				
				String prefix = "";
				int delta = 0;
				if(fixed.endsWith(VitalSignsLuceneBridge.EQ_LOWER_CASE_SUFFIX)) {
					delta = VitalSignsLuceneBridge.EQ_LOWER_CASE_SUFFIX.length();
				}
				if(fixed.endsWith(VitalSignsLuceneBridge.EQ_SUFFIX)) {
					delta = VitalSignsLuceneBridge.EQ_SUFFIX.length();
				}
				if(fixed.endsWith(VitalSignsLuceneBridge.LOWER_CASE_SUFFIX)) {
					delta = VitalSignsLuceneBridge.LOWER_CASE_SUFFIX.length();
				}				

				if(delta > 0) {
					fixed = fixed.substring(0, fixed.length() - delta);
				}
				
				OntProperty p = VitalSigns.get().getOntologyModel().getOntProperty(fixed);
				
				SortField.Type type = SortField.Type.STRING_VAL;
				
				if( p != null) {
					
					OntResource range = p.getRange();
					String rangeURI = range.getURI();
					
					if( XSDDatatype.XSDstring.getURI().equals(rangeURI) ) {
						type = SortField.Type.STRING_VAL;
						prefix = VitalSignsLuceneBridge.EQ_LOWER_CASE_SUFFIX;
					} else if(XSDDatatype.XSDint.getURI().equals(rangeURI) || XSDDatatype.XSDinteger.getURI().equals(rangeURI)) {
						type = SortField.Type.INT;
					} else if(XSDDatatype.XSDlong.getURI().equals(rangeURI) || XSDDatatype.XSDdateTime.getURI().equals(rangeURI)) {
						type = SortField.Type.LONG;
					} else if(XSDDatatype.XSDfloat.getURI().equals(rangeURI)) {
						type = SortField.Type.FLOAT;
					} else if(XSDDatatype.XSDdouble.getURI().equals(rangeURI)) {
						type = SortField.Type.DOUBLE;
					}
					
				}
				
				sortField = new SortField(fixed + prefix, type, inverse);
				
				spfs = new HashSet<String>(Arrays.asList(pURI)); 
				*/
			}
		} else {
//			throw new IOException("Sort with more than 1 property not supported yet.");
		}
		
		if(!innerSort && sortProperties.size() > 0) {
			
			if(!segment.getConfig().isStoreObjects()) throw new RuntimeException("Sorting with arbitrary properties is only supported in an index that also stores field values");
			
			sortPropertiesColumns = toSortPropertyColumns(sortProperties);
			
		}
		
		List<String> toCollect = new ArrayList<String>();
		
		Map<String,Double> docURI2Score = new HashMap<String, Double>();
		Map<String,ResolvedFieldsResult> docURI2ResolvedFieldResult = null;
		if( this.collectResolvedFields) {
		    docURI2ResolvedFieldResult = new HashMap<String, ResolvedFieldsResult>();
		}
		
		//this also handles aggregation functions
		if(useIndexedOrder) {
			
			int cursor = 0;
			
			if(inverse) {
				
				for (int i = matchingDocs.length(); (i = matchingDocs.previousSetBit(i-1)) >= 0; cursor++  ) {

					if(cursor >= offset && cursor < offset + limit) {
							
						String u = getURI(i);
						if(u != null) {
							toCollect.add(u);
							docURI2Score.put(u, matchingDocsResults.scores.get(i));
						}
							
						
					}
					
				 }
				
			} else {
				
				for( int i = matchingDocs.nextSetBit(0); i >= 0; i = matchingDocs.nextSetBit(i + 1), cursor++ ) {
					
					if(cursor >= offset && cursor < offset + limit) {

						if(aggregationType != null || uniqueValues != null) {

//							this.searcher.doc(docID)
							
							//count without URI won't hit this
							if(aggregationPropertyURI != null) {
								
							    long nano = queryStats != null ? System.nanoTime() : 0L;
								Document doc = this.searcher.getIndexReader().document(i, aggregationField);
								
								if(queryStats != null) {
								    docsGetNanoTime += (System.nanoTime() - nano);
								}
								
								for(String apu : aggregationPropertiesURIs) {
									
									IndexableField field = doc.getField(apu);
									
									if(field != null) {
										
										Number number = field.numericValue();
										
										agg_count++;
										
										if(number != null) {
											
											double val = number.doubleValue();
											
											
											agg_sum += val;
											
											if(agg_min == null || agg_min.doubleValue() > val) {
												agg_min = val;
											}
											
											if(agg_max == null || agg_max.doubleValue() < val) {
												agg_max = val;
											}
											
											if(uniqueValues != null) {
												uniqueValues.add(val);
											}
											
										} else if(uniqueValues != null ) {
											
											String stringValue = field.stringValue();
											if(stringValue != null) {
												uniqueValues.add(stringValue);
											}
											
										}
										
									}
									
								}
								
							} else {
								
								agg_count++;
								
								agg_sum += 1d;
								
							}
							
						} else {
							
							String u = getURI(i);
							
							if(u != null) {
								toCollect.add(u);
								docURI2Score.put(u, matchingDocsResults.scores.get(i));
							}
							
						}
						
					}
					
					
				}
				
				
			}
		} else if( !useScoreOrder) {
			
			Filter f = new Filter(){

				int base = 0;
				
				@Override
				public DocIdSet getDocIdSet(AtomicReaderContext arc, Bits bits)
						throws IOException {
					
					int mdoc = arc.reader().maxDoc();
					
					BitSet subSet = matchingDocs.get(base, base + mdoc);
					
					DocIdBitSet bs = new DocIdBitSet(subSet);
					
					base += mdoc;
					
					return bs;
					
				}

			};
			
			
			//lucene default sort over single column
			//TODO consider single sort value optimization
			/* 
			Sort s = new Sort(sortField);
				
			TopDocs topSearchDocs = searcher.search(new MatchAllDocsQuery(), f, offset + limit, s);

			if(topSearchDocs.totalHits > offset) {
				
				ScoreDoc[] scoreDocs = topSearchDocs.scoreDocs;
				
				
				for(int i = offset ; i < offset + limit; i++) {

					if(i < scoreDocs.length) {
						
						int docID = scoreDocs[i].doc;
						
						String u = getURI(docID);
						
						if(u != null) {
							toCollect.add(u);
							docURI2Score.put(u, matchingDocsResults.scores.get(docID));
						}
						
					}
					
				}
			}
			*/
			
			long y = System.currentTimeMillis();
			TopDocs topSearchDocs = searcher.search(new MatchAllDocsQuery(), f, Integer.MAX_VALUE);
			
			if(queryStats != null) {
			    long time = queryStats.addDatabaseTimeFrom(y);
			    if(queryStats.getQueriesTimes() != null) queryStats.getQueriesTimes().add(new QueryTime("matchAllDocsQuery", "matchAllDocsQuery", time));
			}
			
			if(topSearchDocs.totalHits > offset) {
				
				List<ResolvedFieldsResult> resolved = new ArrayList<ResolvedFieldsResult>();
				
				ScoreDoc[] scoreDocs = topSearchDocs.scoreDocs;
				
				//collect all values to be used in sorting
				Set<String> fields= new HashSet<String>();
				fields.add(VitalSignsLuceneBridge.URI_FIELD);
				for(SortPropertyColumn p : sortPropertiesColumns) {
				    if(p.externalPropertyURI != null) {
				        fields.add(p.externalPropertyURI);
				    } else {
				        for(PropertyMetadata x : p.props) {
				            fields.add(x.getURI());
				        }
				    }
				}
				
				for(ScoreDoc sd : scoreDocs) {
					
				    long nano = queryStats != null ? System.nanoTime() : 0L;
					Document doc = searcher.doc(sd.doc, fields);
					
					if(queryStats != null) {
					    docsGetNanoTime += (System.nanoTime() - nano);
					}
					
					ResolvedFieldsResult rf = new ResolvedFieldsResult();
					rf.docid = sd.doc;
					rf.URI = doc.get(VitalSignsLuceneBridge.URI_FIELD);
					
//					List<Object[]> vals = new ArrayList<Object[]>();
//					rf.values = vals;
					
					for(SortPropertyColumn c : sortPropertiesColumns) {
					
					    if(c.externalPropertyURI != null) {
					        
					        if(!segment.areAllObjectFieldsStored()) {
					            
                                GraphObject g = segment.get(rf.URI);
                                if(g == null) throw new RuntimeException("Inmemory object not returned by index: " + rf.URI);
                                
                                IProperty pr = (IProperty) g.getProperty(c.externalPropertyURI);
                                if(pr != null) {
                                    rf.firstValue = pr.rawValue();
                                }
                                
					        } else {
					         
	                            IndexableField field = doc.getField(c.externalPropertyURI);
	                         
	                            if(field != null) {
	                                Number n = field.numericValue();
	                                if(n != null) {
	                                    rf.firstValue = n;
	                                } else {
	                                    String s = field.stringValue();
	                                    if(s != null) {
	                                        rf.firstValue = s;
	                                    }
	                                }
	                                
	                            }
					            
					        }

					        
					    } else {
					       
	                       List<PropertyMetadata> p = c.props;
	                        
	                        for(int i = 0; i < p.size(); i++) {
	                            
	                            Object v = null;
	                            
	                            PropertyMetadata x = p.get(i);
	                            
	                            if( ! segment.areAllObjectFieldsStored() ) {
	                            
	                                GraphObject g = segment.get(rf.URI);
	                                if(g == null) throw new RuntimeException("Inmemory object not returned by index: " + rf.URI);
	                                
	                                IProperty pr = (IProperty) g.getProperty(x.getShortName());
	                                if(pr != null) {
	                                    v = pr.rawValue();
	                                }
	                                
	                            } else {
	                                
	                                IndexableField field = doc.getField(x.getURI());
	                            
	                                if(field != null) {
	                                    v = VitalSignsLuceneBridge.deserializeValue(x.getBaseClass(), field, null);
	                                }
	                                
	                            }
	                            
	                            if(v != null) {
	                                rf.firstValue = v;
	                                break;
	                            }
	                            
	                        }
		                        
					    }

						
					}
					
					resolved.add(rf);
					
				}
				
				Collections.sort(resolved, new ExternalSortComparator(sortPropertiesColumns, false));
				
				for(int i = offset ; i < offset + limit; i++) {
					
					if(i < resolved.size()) {
						
						ResolvedFieldsResult rfr = resolved.get(i);
						int docID = rfr.docid;
						
						String u = rfr.URI;
						
						if(u != null) {
							toCollect.add(u);
							docURI2Score.put(u, matchingDocsResults.scores.get(docID));
						}
						
					}
					
				}
				
				if(this.isCollectResolvedFields()) {
				    rs.setResolvedFields(resolved);
				}
				
			}
			
			
			
		} else {
			
			int maxQSize = offset + limit;
			
			List<Object[]> priorityQueue = new ArrayList<Object[]>(maxQSize);
			
			
			
			TIntDoubleHashMap scoresMap = matchingDocsResults.scores;
			
			//collect all documents into priority queue with offset+limit size
			for( int i = matchingDocs.nextSetBit(0); i >= 0; i = matchingDocs.nextSetBit(i + 1)) {
				
				double score;
				
				String stringValue = null;
				
				if(useScoreOrder) {
					score = scoresMap.get(i);
				} else {
					score = 0;
					long nano = queryStats != null ? System.nanoTime() : 0L;
					Document doc = searcher.doc(i, spfs);
					
					if(queryStats != null) {
					    docsGetNanoTime += (System.nanoTime() - nano);
					}
					
					if( doc != null ){
						IndexableField fieldable = doc.getField(sp.getPropertyURI());
//						Fieldable fieldable = doc.getFieldable(sp.getPropertyURI());
						if(fieldable != null) {
							String v = fieldable.stringValue();
							try {
								score = Double.parseDouble(v);
							} catch(Exception e) {
								stringValue = v;
							}
						}
					}
				}
				
				//same score, higher id wins ?
				int insertionIndex = -1;
				
				for(int j = 0; j < priorityQueue.size(); j++) {
					
					Object[] r = priorityQueue.get(j);
					
					String sv = (String) r[2];
					
					if(sv != null && stringValue != null) {
						
						
						if(inverse) {
							
							int _c = stringValue.compareTo(sv);
							
							if(_c > 0) {
								insertionIndex = j;
								break;
								
							} else if(_c == 0){
								
								if(i >= ((Integer)r[0]).intValue()) {
									insertionIndex = j;
									break;
								}
								
							}
							
						} else {
							
							int _c = stringValue.compareTo(sv);
							
							if(_c < 0 ) {
								insertionIndex = j;
								break;
							} else if(_c == 0 ) {
								if(i >= ((Integer)r[0]).intValue()) {
									insertionIndex = j;
									break;
								}
							}
							
						}
						
					} else {
						
						if(inverse) {
							
							if(score > ((Double)r[1]).doubleValue() ) {
								
								//insert it here
								insertionIndex = j;
								break;
								
							} else if(score == ((Double)r[1]).doubleValue()) {
								
								if(i >= ((Integer)r[0]).intValue()) {
									insertionIndex = j;
									break;
								}
								
							}
							
						} else {
							
							if(score < ((Double)r[1]).doubleValue() ) {
								insertionIndex = j;
								break;
							} else if(score == ((Double)r[1]).doubleValue() ) {
								if(i >= ((Integer)r[0]).intValue()) {
									insertionIndex = j;
									break;
								}
							}
							
						}
						
					}
					
				}
				
				if(insertionIndex < 0 && priorityQueue.size() < maxQSize) {
					//append it
					priorityQueue.add(new Object[]{i, score, stringValue});
				} else if(insertionIndex >= 0) {
					
					if( priorityQueue.size() == maxQSize ) {
						priorityQueue.remove(maxQSize-1);
					}
					priorityQueue.add(insertionIndex, new Object[]{i, score, stringValue});
					
				}
				
			}
			
			//now select page
			
			for(int cursor = offset; cursor < offset + limit && cursor < priorityQueue.size(); cursor++) {
				
				Object[] res = priorityQueue.get(cursor);
				
				int docID = ((Integer)res[0]).intValue();
				
				String u = getURI(docID);
				if(u != null) {
					toCollect.add(u);
					docURI2Score.put(u, matchingDocsResults.scores.get(docID));
				}
				
			}
			
			
		}
		
		
		//
		
		int matchedDocs = matchingDocs.cardinality();
		
		rs.setTotalResults(matchedDocs);
		
		rs.setAgg_count(agg_count);
		
		if(sq.isDistinct()) {
			
			agg_count_distinct = uniqueValues.size();
			
			if(aggregationType == null) {
				
				List<Object> distinctValues = new ArrayList<Object>(uniqueValues);
				
				if(sq.getDistinctSort() != null) {
					
					final boolean ascNotDesc = VitalSelectQuery.asc.equals(sq.getDistinctSort());
					
					Collections.sort(distinctValues, new DistinctValuesComparator(ascNotDesc));
					
				}
				
				
				if(sq.getDistinctFirst() && distinctValues.size() > 1) {
					distinctValues = new ArrayList<Object>(Arrays.asList(distinctValues.get(0)));
				} else if(sq.getDistinctLast() && distinctValues.size() > 1){
					distinctValues = new ArrayList<Object>(Arrays.asList(distinctValues.get(distinctValues.size()-1)));
				}
				
				rs.setDistinctValues(distinctValues);
			}
			
		}
		
		rs.setAgg_count_distinct(agg_count_distinct);
		
		
		
		rs.setAgg_sum(agg_sum);
		rs.setAgg_min(agg_min);
		rs.setAgg_max(agg_max);
		
		if(toCollect.size() > 0) {
			List<URIResultElement> listOfObjects = new ArrayList<URIResultElement>(toCollect.size());
			for(String s : toCollect) {
				URIResultElement e = new URIResultElement();
				e.URI = s;
				e.segment = this.segment;
				Double score = docURI2Score.get(s);
				e.score = score != null ? score.doubleValue() : 0d;
				listOfObjects.add(e);
			}
			rs.setResults(listOfObjects);
		} else {
			rs.setResults(new ArrayList<URIResultElement>()); 
		}
		
		addBatchGetStats(rs);
		return rs;
		
	}

	private void addBatchGetStats(URIResultList rs) {

	    if(queryStats != null) {
	        
	        long time = queryStats.addObjectsBatchGetTimeFrom(docsGetNanoTime / 1000000);
	        if( queryStats.getQueriesTimes() != null ) queryStats.getQueriesTimes().add(new QueryTime("segment query batch get objects time " + segment.getID(), "segment query batch get objects time" + segment.getID(), time));
	        
	    }
        
    }

    static Set<String> uriFieldsToLoad = new HashSet<String>(Arrays.asList(VitalSignsLuceneBridge.URI_FIELD));
	
	private String getURI(int i) throws IOException {

		if(uris != null) {
			
			if(i < uris.length) {
				return uris[i];
			}
			
			return null;
		}
		
		long nano = queryStats != null ? System.nanoTime() : 0L;
		
		Document doc = searcher.doc(i, uriFieldsToLoad);
		
		if(queryStats != null) {
		    docsGetNanoTime += (System.nanoTime() - nano);
		}
		
		if(doc != null) return doc.get(VitalSignsLuceneBridge.URI_FIELD);
		
		return null;
	}

	private BitSetResults processContainer(VitalGraphCriteriaContainer queryContainer) throws IOException {

		QueryContainerType type = queryContainer.getType();
		
		int otherElements = 0;
		
		//first process containers ? 

		BitSetResults containerResults = null;
		
		/* no need to split select query now
		for(VitalGraphQueryElement c : queryContainer) {

			if(c instanceof VitalGraphCriteriaContainer) {
				BitSetResults containerSubResults = processContainer((VitalGraphCriteriaContainer) c);
				if(type == QueryContainerType.and && containerSubResults.docs.cardinality() < 1) {
					//no point to process it further
					return new BitSetResults(new BitSet(0), new TIntDoubleHashMap(0));
				}
				if(containerResults == null) {
					containerResults = containerSubResults;
				} else {
				
					//merge results
					if(type == QueryContainerType.and) {
						
						containerResults.docs.and(containerSubResults.docs);
						
						if(containerResults.docs.cardinality() < 1) {
							return new BitSetResults(new BitSet(0), new TIntDoubleHashMap(0));
						}

						//XXX retain entries with higher score?
						containerResults.scores.putAll(containerSubResults.scores);
						
					} else {
						containerResults.docs.or(containerSubResults.docs);
						containerResults.scores.putAll(containerSubResults.scores);
					}
					
				}
			}  else {
				otherElements++;
			}
			
		}
		*/
		
		//execute query here
		BitSetCollector bitSetCollector = null;
		
		Query selectQueryWithoutPathElement = LuceneQueryGenerator.selectQueryWithoutPathElement(queryContainer, searcher);
		
		
//		if(otherElements > 0) {

			bitSetCollector = new BitSetCollector(maxDoc);
			
			long start = System.currentTimeMillis();
			
			searcher.search(selectQueryWithoutPathElement, bitSetCollector);
			
			if(queryStats != null) {
			    long time = queryStats.addDatabaseTimeFrom(start);
			    if(queryStats.getQueriesTimes() != null) queryStats.getQueriesTimes().add(new QueryTime(queryContainer.printContainer(""), selectQueryWithoutPathElement.toString(), time));
			}
			
			if(bitSetCollector.bitSet.cardinality() < 1 && type == QueryContainerType.and) {
				//no point of processing since root is empty;
				return new BitSetResults(new BitSet(0), new TIntDoubleHashMap(0));
			}
			
			//merge container results
//			if(containerResults != null) {
//				if(type == QueryContainerType.and) {
//					bitSetCollector.bitSet.and(containerResults.docs);
//					containerResults.scores.putAll(containerResults.scores);
//				} else {
//					bitSetCollector.bitSet.or(containerResults.docs);
//					containerResults.scores.putAll(containerResults.scores);
//				}
//			}

			if(bitSetCollector.bitSet.cardinality() < 1 && type == QueryContainerType.and) {
				//no point of processing since root is empty;
				return new BitSetResults(new BitSet(0), new TIntDoubleHashMap(0));
			}
			
//		} else {
//			
//			//if not other results then assume equal scoring
//			if(containerResults != null) {
//				bitSetCollector = new BitSetCollector(maxDoc);
//				bitSetCollector.bitSet = containerResults.docs;
//				bitSetCollector.scoreMap = containerResults.scores;
//				
//			}
//		}
		
		
//		if(bitSetCollector == null) {
//			bitSetCollector = new BitSetCollector(maxDoc);
//		}
		
		return new BitSetResults(bitSetCollector.bitSet, bitSetCollector.scoreMap);
	}
	
	static String edgeSrcProp = VitalSignsLuceneBridge.EDGE_SRC_URI_FIELD;
	
	static String edgeDestProp = VitalSignsLuceneBridge.EDGE_DEST_URI_FIELD;
	
	static Set<String> edgeFieldsSelector = new HashSet<String>(Arrays.asList(edgeSrcProp, edgeDestProp));
	
	/*
	static FieldSelector edgeFieldsSelector = new FieldSelector() {

		private static final long serialVersionUID = -4055740714721936132L;

		@Override
		public FieldSelectorResult accept(String arg0) {

			if(edgeSrcPropURI.equals(arg0) || edgeDestPropURI.equals(arg0)) {
				return FieldSelectorResult.LOAD;
			} else {
				return FieldSelectorResult.NO_LOAD;
			}
			
		}
		
	};
	*/

	private static class BitSetCollector extends Collector {

		private BitSet bitSet = null;
		private TIntDoubleHashMap scoreMap = null;
		private IndexReader reader;
		private int docBase = 0;
		private Scorer scorer;
		
		public BitSetCollector(int maxDoc) {
			bitSet = new BitSet(maxDoc);
			scoreMap = new TIntDoubleHashMap(maxDoc);
		}

		@Override
		public boolean acceptsDocsOutOfOrder() {
			return true;
		}

		@Override
		public void collect(int doc) throws IOException {

			double score = scorer.score();
			
			int docid = doc + docBase;
			
			bitSet.set(docid);
			scoreMap.put(docid, score);
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException {
			this.scorer = scorer;
		}

		@Override
		public void setNextReader(AtomicReaderContext atomicReaderContext) throws IOException {

			this.reader = atomicReaderContext.reader();
			this.docBase = atomicReaderContext.docBase;
			
		}
		
	}
	
	public static class ResolvedFieldsResult {
		
		public Integer docid;
		
		public String URI;
		
		//each sort column has own values set
//		List<Object[]> values = new ArrayList<Object[]>();
		public Object firstValue;
		
	}
	
	public static class SortPropertyColumn {
		
		boolean ascendingNotDescending;
		
		List<PropertyMetadata> props;
		
		String externalPropertyURI;
		
		
	}
	
	public static class ExternalSortComparator implements Comparator<ResolvedFieldsResult> {

		List<SortPropertyColumn> sortPropertiesColumns;
		
		boolean inverse = false;
		
		public ExternalSortComparator(
				List<SortPropertyColumn> sortPropertiesColumns, boolean inverse) {
			this.sortPropertiesColumns = sortPropertiesColumns;
			this.inverse = inverse;
			
		}

		// @SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public int compare(ResolvedFieldsResult arg0, ResolvedFieldsResult arg1) {

			//keep comparing 
			for(int i = 0 ; i < sortPropertiesColumns.size(); i++) {
				SortPropertyColumn spc = sortPropertiesColumns.get(i);
				Comparable v1 = (Comparable) arg0.firstValue;
				Comparable v2 = (Comparable) arg1.firstValue;
				int c = 0;
				if(spc.ascendingNotDescending) {
					if(v1 != null && v2 != null) {
						c = v1.compareTo(v2);
					} else if(v1 != null) {
						return inverse ? 1 : -1;
					} else if(v2 != null) {
						return inverse ? 1 : -1;
					}
				} else {
					if(v1 != null && v2 != null) {
						c = v2.compareTo(v1);
					} else if(v1 != null) {
						return inverse ? -1 : 1;
					} else if(v2 != null) {
						return inverse ? 1 : -1;
					}
				}
				if(c != 0) return inverse ? ((-1) * c) : c;
			}
			
			return 0;
		}
		
	}
	
//	static StoredFieldVisitor edgeFieldsVisitor = new StoredFieldVisitor(){
//
//		@Override
//		public Status needsField(FieldInfo fi) throws IOException {
//			return fi.;
//		}};

	public static class DistinctValuesComparator implements Comparator {
		
		private boolean ascNotDesc;

		public DistinctValuesComparator(boolean ascNotDesc) {
			super();
			this.ascNotDesc = ascNotDesc;
		}

		@Override
		public int compare(Object o1, Object o2) {

			if(o1 instanceof Comparable && o2 instanceof Comparable) {
				if(ascNotDesc) {
					return ((Comparable)o1).compareTo((Comparable)o2);
				} else {
					return ((Comparable)o2).compareTo((Comparable)o1);
				}
			}
					
			return 0;
		}
		
	}
	
	static public List<SortPropertyColumn> toSortPropertyColumns(List<VitalSortProperty> sortProperties) {
	    
	    List<SortPropertyColumn> sortPropertiesColumns = new ArrayList<SortPropertyColumn>();
	    
	    List<Class<? extends IProperty>> supportedTypes = Arrays.asList((Class<? extends IProperty>) BooleanProperty.class, DateProperty.class, NumberProperty.class, StringProperty.class, URIProperty.class);
	    
	    
	    Set<String> sanityCheck = new HashSet<String>();
	    
        for(VitalSortProperty vsp : sortProperties) {
            
            String propertyURI = vsp.getPropertyURI();
            
            if(!sanityCheck.add(propertyURI)) throw new RuntimeException("Property used more than once in sorting: " + propertyURI);
            
            if(propertyURI == null || propertyURI.isEmpty()) throw new RuntimeException("sort property URI not set");
            
            if(VitalSortProperty.INDEXORDER.equals(propertyURI) || VitalSortProperty.RELEVANCE.equals(propertyURI)) {
                throw new RuntimeException("Special sort properties must not be used in more than 1 sort column case: " + propertyURI);
            }
            
            PropertyMetadata prop = VitalSigns.get().getPropertiesRegistry().getProperty(propertyURI);
            
            //external property sort assumed
            if(prop == null) {
                SortPropertyColumn c = new SortPropertyColumn();
                c.ascendingNotDescending = !vsp.isReverse();
                c.externalPropertyURI = propertyURI;
                sortPropertiesColumns.add(c);
                continue;
//              throw new RuntimeException("Sort property not found: " + propertyURI);
            }
            
            Class<? extends IProperty> bc = prop.getBaseClass();
            
            boolean ok = false;
            for( Class<? extends IProperty> c : supportedTypes) {
                if(c.isAssignableFrom(bc)) {
                    ok = true;
                    break;
                }
            }
            
            if(!ok) throw new RuntimeException("Property of base type: " + bc.getSimpleName() + " must not be used in sort, " + propertyURI);
            
            if(vsp.isExpandProperty()) {
                
                List<PropertyMetadata> props = new ArrayList<PropertyMetadata>();
                
                //required to deterministic sort, most specific value is used first, for same depth lexical order 
                final Map<PropertyMetadata, Integer> pm2Depth = new HashMap<PropertyMetadata, Integer>();
                
                for(PropertyMetadata pm : VitalSigns.get().getPropertiesRegistry().getSubProperties(prop, true)) {
                    
                    int depth = 0;
                    
                    PropertyMetadata current = pm;
                    
                    while( current != null && !current.getURI().equals(propertyURI) ) {
                        depth++;
                        current = current.getParent();
                    }
                    
                    pm2Depth.put(pm, depth);
                    
                    props.add(pm);
                    
                }
                
                //sort them
                Collections.sort(props, new Comparator<PropertyMetadata>() {

                    @Override
                    public int compare(PropertyMetadata o1,
                            PropertyMetadata o2) {
                        Integer d1 = pm2Depth.get(o1);
                        Integer d2 = pm2Depth.get(o2);
                        int c = d2.compareTo(d1);
                        if(c != 0) return c;
                        return o1.getURI().compareTo(o2.getURI());
                        
                    }
                });

                SortPropertyColumn c = new SortPropertyColumn();
                c.props = props;
                //reverse means descending
                c.ascendingNotDescending = !vsp.isReverse();
                sortPropertiesColumns.add(c);
                
            } else {
                SortPropertyColumn c = new SortPropertyColumn();
                c.props = Arrays.asList(prop);
                c.ascendingNotDescending = !vsp.isReverse();
                sortPropertiesColumns.add(c);
            }
                
            
        }
    
        return sortPropertiesColumns;
        
	}
	
}
