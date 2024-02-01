package ai.vital.lucene.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;

import ai.vital.lucene.model.VitalSignsLuceneBridge;
import ai.vital.lucene.model.VitalWhitespaceAnalyzer;
import ai.vital.vitalservice.query.GraphElement;
import ai.vital.vitalservice.query.QueryContainerType;
import ai.vital.vitalservice.query.VitalGraphCriteriaContainer;
import ai.vital.vitalservice.query.VitalGraphQueryElement;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion;
import ai.vital.vitalservice.query.VitalGraphQueryPropertyCriterion.Comparator;
import ai.vital.vitalservice.query.VitalGraphQueryTypeCriterion;
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.datatype.Truth;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.property.StringProperty;
import ai.vital.vitalsigns.model.property.URIProperty;
import ai.vital.vitalsigns.properties.PropertyMetadata;

public class LuceneQueryGenerator {

	
	//TODO move these definitions?
	public static final String LOWER_CASE_SUFFIX = "_LC";
	public static final String EQ_LOWER_CASE_SUFFIX = "_EQ_LC";

	public static final String EQ_SUFFIX = "_EQ";

	
	static Set<Comparator> numericRangeComparators = new HashSet<Comparator>(Arrays.asList(Comparator.GE, Comparator.GT, Comparator.LE, Comparator.LT)); 
	
	//how to handle query path elements ? use matching document ids as some special
	public static Query selectQueryWithoutPathElement(VitalGraphCriteriaContainer sq, IndexSearcher searcher) throws IOException {
	
		BooleanQuery mainQuery = new BooleanQuery();
		
		int valueIndex = 1;
		
		valueIndex = processContainer(sq, mainQuery, valueIndex);
		
		return mainQuery;
		
		
	}

	private static int processContainer(VitalGraphCriteriaContainer parentC, BooleanQuery parentQuery,
			int valueIndex) throws IOException {
		
		QueryContainerType type = parentC.getType();
		
		Occur occur = type == QueryContainerType.and ? Occur.MUST : Occur.SHOULD;
		
		int containers = 0;
		int negativeprops = 0;
		int positiveprops = 0;
		
		for( VitalGraphQueryElement qc : parentC ) {
		
			//oneOf noneOf case
			if(qc instanceof VitalGraphQueryPropertyCriterion) {
				
				VitalGraphQueryPropertyCriterion pc = (VitalGraphQueryPropertyCriterion) qc;
				
				Comparator c = pc.getComparator();
				
				String propURI = pc.getPropertyURI();
				
				PropertyMetadata pm = null;
				if(!(pc instanceof VitalGraphQueryTypeCriterion)) {
					pm = VitalSigns.get().getPropertiesRegistry().getProperty(propURI);
					
					
					if(pm != null) {
						
						if(pm.isMultipleValues()) {
							
							if(pm.getBaseClass() == StringProperty.class && ( c == Comparator.CONTAINS_CASE_INSENSITIVE || c == Comparator.CONTAINS_CASE_SENSITIVE)) {
							} else if( c == Comparator.CONTAINS || c == Comparator.NOT_CONTAINS ) {
							} else {
								throw new RuntimeException("Multivalue properties may only be queried with CONTAINS/NOT-CONTAINS comparators, or it it's a string multi value property: " + Comparator.CONTAINS_CASE_INSENSITIVE + " or " + Comparator.CONTAINS_CASE_INSENSITIVE);
							}
						}
						
						if( ( c == Comparator.CONTAINS || c == Comparator.NOT_CONTAINS) && !pm.isMultipleValues() ) {
							throw new RuntimeException("CONTAINS/NOT-CONTAINS may only be used for multi-value properties");
						}
						
					}
				}
				
				//check if it's expand properties case
				if(pc.getExpandProperty()) {
					
					if(pc instanceof VitalGraphQueryTypeCriterion) {
						throw new RuntimeException("VitalGraphQueryTypeCriterion must not have expandProperty flag set!");
					}
					
					if(pm == null) throw new RuntimeException("Property metadata not found: " + propURI);
				
					List<PropertyMetadata> subProperties = VitalSigns.get().getPropertiesRegistry().getSubProperties(pm, true);
					
					if(subProperties.size() > 1) {
						
						BooleanQuery bc = new BooleanQuery();
						
						VitalGraphCriteriaContainer newContainer = new VitalGraphCriteriaContainer(QueryContainerType.or);
						
						for(PropertyMetadata p : subProperties) {
							
							VitalGraphQueryPropertyCriterion c2 = new VitalGraphQueryPropertyCriterion(p.getURI());
							c2.setSymbol(pc.getSymbol());
							c2.setComparator(pc.getComparator());
							c2.setNegative(pc.isNegative());
							c2.setValue(pc.getValue());
							c2.setExpandProperty(false);
							c2.setExternalProperty(pc.getExternalProperty());
							
							newContainer.add(c2);
							
						}
						
						valueIndex = processContainer(newContainer, bc, valueIndex);
						
						if(bc.getClauses().length > 0) {
							parentQuery.add(bc, occur);
						}
//						return valueIndex;
						continue;
					}
					
					
					
				}
				
				
				
				if(c == Comparator.ONE_OF || c == Comparator.NONE_OF) {
					
					boolean none = pc.getComparator() == Comparator.NONE_OF;

					VitalGraphCriteriaContainer newContainer = new VitalGraphCriteriaContainer(none ? QueryContainerType.and : QueryContainerType.or);
					
					Object val = pc.getValue();
					
					if(!(val instanceof Collection)) throw new RuntimeException("Expected collection for comparator: " + pc.getComparator());
					
					for(Object v : (Collection)val) {
						
						VitalGraphQueryPropertyCriterion c2 = null;
						
						if(pc instanceof VitalGraphQueryTypeCriterion) {
							
							VitalGraphQueryTypeCriterion tc = (VitalGraphQueryTypeCriterion) pc;
							
							c2 = new VitalGraphQueryTypeCriterion(tc.getType());
							
						} else {
							
							c2 = new VitalGraphQueryPropertyCriterion(pc.getPropertyURI());
							
							
						}
						
						
//						if(VitalGraphQueryPropertyCriterion.URI.equals(crit.getPropertyURI())) {
							c2.setSymbol(pc.getSymbol());
							c2.setComparator(Comparator.EQ);
							c2.setNegative(none);
							c2.setValue(v);
							c2.setExternalProperty(pc.getExternalProperty());
//						} else {
//							c2
//						}
						
							newContainer.add(c2);
							
					}
					
					BooleanQuery bc = new BooleanQuery();
					
					valueIndex = processContainer(newContainer, bc, valueIndex);
				
					if(bc.getClauses().length > 0) {
						parentQuery.add(bc, occur);
					}
					continue;
//					return valueIndex;
				}
				
			}
			
			//special case for expand types criterion
			if(qc instanceof VitalGraphQueryTypeCriterion) {
				VitalGraphQueryTypeCriterion tc = (VitalGraphQueryTypeCriterion) qc;
				if(tc.isExpandTypes()) {
					
					
					Class<? extends GraphObject> gType = tc.getType();
					
					if(gType == null) throw new RuntimeException("No class set in type criterion" + tc);
					
					ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClassMetadata(gType);
					if(cm == null) throw new RuntimeException("Class metadata not found for type: " + gType.getCanonicalName());
					
					List<ClassMetadata> subclasses = VitalSigns.get().getClassesRegistry().getSubclasses(cm, true);
					
					if(subclasses.size() > 1) {
						
						//only in this case add new container
						VitalGraphCriteriaContainer newContainer = new VitalGraphCriteriaContainer(QueryContainerType.or);
						
						for(ClassMetadata m : subclasses) {
							
							VitalGraphQueryTypeCriterion nt = new VitalGraphQueryTypeCriterion(GraphElement.Source, m.getClazz());
							
							newContainer.add(nt);
							
						}
						
						BooleanQuery bc = new BooleanQuery();
						
						valueIndex = processContainer(newContainer, bc, valueIndex);
					
						if(bc.getClauses().length > 0) {
							parentQuery.add(bc, occur);
						}
//						return valueIndex;
						continue;
						
					}
					
				}
			}
			
			
			if(qc instanceof VitalGraphCriteriaContainer) {

				containers++;
				
				BooleanQuery bc = new BooleanQuery();
				
				
				valueIndex = processContainer((VitalGraphCriteriaContainer)qc, bc, valueIndex);
				
				if(bc.getClauses().length > 0) {
					parentQuery.add(bc, occur);
				}
				
				
			} else if(qc instanceof VitalGraphQueryTypeCriterion) {
			
				VitalGraphQueryTypeCriterion tc = (VitalGraphQueryTypeCriterion) qc;
				
				URIProperty value = (URIProperty) tc.getValue();

				//TODO expand types ?
				
				parentQuery.add(new TermQuery(new Term(VitalSignsLuceneBridge.TYPE_FIELD, value.get())), tc.isNegative() ? Occur.MUST_NOT : occur);
				
				if( tc.isNegative())  negativeprops++; else positiveprops++;
				
			} else if(qc instanceof VitalGraphQueryPropertyCriterion) {
				
				VitalGraphQueryPropertyCriterion pc = (VitalGraphQueryPropertyCriterion) qc;
				
				
				//check whether we need a filter or straight value
				
				//change the order
				
				String pURI = pc.getPropertyURI();				
				
				
				if(VitalGraphQueryPropertyCriterion.URI.equals(pURI)) {
					pURI = VitalSignsLuceneBridge.URI_FIELD;
				}
				
				if( ( pc.getComparator() == Comparator.EXISTS && !pc.isNegative() ) || ( pc.getComparator() == Comparator.NOT_EXISTS && pc.isNegative() ) ) {
					
					parentQuery.add(new TermQuery(new Term(VitalSignsLuceneBridge.PROPERTIES_FIELD, pURI)), occur);
					
					positiveprops++;
					
					continue;
					
				} else if( ( pc.getComparator() == Comparator.EXISTS && pc.isNegative() ) || (pc.getComparator() == Comparator.NOT_EXISTS && !pc.isNegative() ) ) {
					
					parentQuery.add(new TermQuery(new Term(VitalSignsLuceneBridge.PROPERTIES_FIELD, pURI)), Occur.MUST_NOT);
					
					negativeprops ++;
					
					continue;
				}
				
				if(pc.isNegative()) negativeprops++; else positiveprops++;
				
				Occur propOccur = pc.isNegative() ? Occur.MUST_NOT : occur ; 
				
				Comparator c = pc.getComparator();
				
				//not_contains inverts logic 
				if(c == Comparator.NOT_CONTAINS) {
					if(pc.isNegative()) {
						propOccur = occur;
					} else {
						propOccur = Occur.MUST_NOT;
					}
				}
				
				
				if(pc.getValue() != null) {
					
					Object value = pc.getValue();
					
					
					if(value instanceof URIProperty) {
						
						URIProperty upv = (URIProperty) value;
						
						if(c != Comparator.EQ && c != Comparator.CONTAINS && c != Comparator.NOT_CONTAINS) {
							throw new IOException("Cannot use comparator: " + c + " for literal of type: " + value.getClass().getCanonicalName());
						}
						
						String uri = upv.get();
						if(uri == null || uri.isEmpty()) {
							throw new IOException("URIProperty value cannot contain null or empty URI");
						}
						
						parentQuery.add(new TermQuery(new Term(pURI, uri)), propOccur);
						
					} else if(value instanceof Boolean) {
						
						if(c != Comparator.EQ && c != Comparator.CONTAINS && c != Comparator.NOT_CONTAINS) {
							throw new IOException("Cannot use comparator: " + c + " for literal of type: " + value.getClass().getCanonicalName());
						}
						
						parentQuery.add(new TermQuery(new Term(pURI, value.toString())), propOccur);
						
					} else if(value instanceof Truth) {
					    
					    Truth truth = (Truth) value;
					    
					    if(c != Comparator.EQ && c != Comparator.CONTAINS && c != Comparator.NOT_CONTAINS) {
					        throw new IOException("Cannot use comparator: " + c + " for literal of type: " + value.getClass().getCanonicalName());
					    }
					    
					    parentQuery.add(new TermQuery(new Term(pURI, truth.name())), propOccur);
						
					} else if(value instanceof String) {
						
						String s = (String) value;
						
						if(c == Comparator.EQ || c == Comparator.EQ_CASE_INSENSITIVE || c == Comparator.CONTAINS || c == Comparator.NOT_CONTAINS) {
							
							boolean caseSensitive = c != Comparator.EQ_CASE_INSENSITIVE;
							
							parentQuery.add(new TermQuery(new Term(pURI + (caseSensitive ? EQ_SUFFIX : EQ_LOWER_CASE_SUFFIX ), (caseSensitive ? s : s.toLowerCase()))), propOccur);
							
						} else if(c == Comparator.CONTAINS_CASE_INSENSITIVE || c == Comparator.CONTAINS_CASE_SENSITIVE) {
							
							VitalWhitespaceAnalyzer analyzer = new VitalWhitespaceAnalyzer(Version.LUCENE_47);
							
							TokenStream tokenStream = analyzer.tokenStream("xxx", new StringReader(s));
							
							boolean caseSensitive = c == Comparator.CONTAINS_CASE_SENSITIVE;
							
							PhraseQuery phraseQuery = new PhraseQuery();
							
							tokenStream.reset();
							
							int index = -1;
							
							while(tokenStream.incrementToken()) {
								
								CharTermAttribute attribute = tokenStream.getAttribute(CharTermAttribute.class);
								
								int increment = tokenStream.getAttribute(PositionIncrementAttribute.class).getPositionIncrement();
								index += increment;
								
								String term = attribute.toString();
							
								if( ! caseSensitive) {
									
									term = term.toLowerCase();
									
								}
								
								phraseQuery.add(new Term(pURI + (caseSensitive ? "" : LOWER_CASE_SUFFIX ), term), index);
								
							}
							
							tokenStream.close();
							
							analyzer.close();
							
							if(phraseQuery.getTerms().length == 0) throw new RuntimeException("Empty phrase query - cannot use only stopwords to construct a query");
							
							parentQuery.add(phraseQuery, propOccur);
							
						} else if(c == Comparator.REGEXP) {
							
							RegexpQuery regexpQuery = new RegexpQuery(new Term(pURI + LOWER_CASE_SUFFIX, s.toLowerCase()));
							
							parentQuery.add(regexpQuery, propOccur);
							
						} else if(c == Comparator.REGEXP_CASE_SENSITIVE) {
							
							RegexpQuery regexpQuery = new RegexpQuery(new Term(pURI, s));
							
							parentQuery.add(regexpQuery, propOccur);
							
						} else {
							throw new IOException("Cannot use comparator: " + c + " for value of class: " + value.getClass().getCanonicalName());
						}
						
						
					} else if(value instanceof Date || value instanceof Number) {
						
						if(c == Comparator.EQ || c == Comparator.CONTAINS || c == Comparator.NOT_CONTAINS) {
							
							String s = value instanceof Date ? ((Long)((Date)value).getTime()).toString() : value.toString();
							
							parentQuery.add(new TermQuery(new Term(pURI + EQ_SUFFIX, s)), propOccur);
							
						} else if( numericRangeComparators.contains(c) ) {
							
							boolean greater = c == Comparator.GE || c == Comparator.GT;
							boolean lesser  = c == Comparator.LE || c == Comparator.LT;
							
							boolean minInclusive = greater && c == Comparator.GE;
							boolean maxInclusive = lesser  && c == Comparator.LE;
							
							NumericRangeQuery<?> nrq = null;
							
							if( value instanceof Long || value instanceof Date ) {

								Long lValue = (value instanceof Date) ? ((Date) value).getTime() : (Long)value;
								
								Long min = null;
								Long max = null;
								
								if(greater) {
									min = lValue;
									max = Long.MAX_VALUE;
								} else {
									min = Long.MIN_VALUE;
									max = lValue;
								}
								
								nrq = NumericRangeQuery.newLongRange(pURI, min, max, minInclusive, maxInclusive);
								
							} else if( value instanceof Integer ) {
								
								Integer iValue = (Integer) value;
								
								Integer min = null;
								Integer max = null;
								
								if(greater) {
									min = iValue;
									max = Integer.MAX_VALUE;
								} else {
									min = Integer.MIN_VALUE;
									max = iValue;
								}
								
								nrq = NumericRangeQuery.newIntRange(pURI, min, max, minInclusive, maxInclusive);
								
							} else if( value instanceof Double) {
								
								Double dValue = (Double) value;
								
								Double min = null;
								Double max = null;
								
								if(greater) {
									min = dValue;
									max = Double.MAX_VALUE;
								} else {
									min = Double.MIN_VALUE;
									max = dValue;
								}
								
								nrq = NumericRangeQuery.newDoubleRange(pURI, min, max, minInclusive, maxInclusive);
								
							} else if( value instanceof Float ) {
								
								Float fValue = (Float) value;
								
								Float min = null;
								Float max = null;
								
								if(greater) {
									min = fValue;
									max = Float.MAX_VALUE;
								} else {
									min = Float.MIN_VALUE;
									max = fValue;
								}
								
								nrq = NumericRangeQuery.newFloatRange(pURI, min, max, minInclusive, maxInclusive);
								
							} else {
								
								throw new IOException("Unhandled numeric data type: " + value.getClass().getCanonicalName());
								
							}
							
							parentQuery.add(nrq, propOccur);
							
							
						} else {
							
							throw new IOException("Cannot use comparator: " + c + " for literal of type: " + value.getClass().getCanonicalName());
							
						}
						
					} else {
						
						throw new IOException("Unsupported literal value data");
						
					}
					
				} else {
					throw new IOException("No property constraint value set!");
				}
				
			} else {
			
				throw new RuntimeException("Unhandled query component of type: " + qc.getClass().getCanonicalName());
			
			}
			
		}
		
		//special case where there's exactly one negative property constraint
		if(negativeprops > 0 && positiveprops == 0 && containers == 0) {
			
			parentQuery.add(new MatchAllDocsQuery(), Occur.MUST);
			
		}
		
		return valueIndex;
	}


}
