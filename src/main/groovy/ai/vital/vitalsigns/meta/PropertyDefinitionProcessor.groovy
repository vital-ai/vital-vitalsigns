package ai.vital.vitalsigns.meta;

import java.util.ArrayList
import java.util.Date
import java.util.HashSet
import java.util.List
import java.util.Set
import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.classes.ClassMetadata;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.property.DateProperty;
import ai.vital.vitalsigns.model.property.DoubleProperty;
import ai.vital.vitalsigns.model.property.FloatProperty;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalsigns.model.property.IntegerProperty;
import ai.vital.vitalsigns.model.property.LongProperty;
import ai.vital.vitalsigns.model.property.NumberProperty;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.properties.PropertyMetadata;
import ai.vital.vitalsigns.rdf.RDFDate;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class PropertyDefinitionProcessor {

	public static class PropertyValidationException extends Exception {

		private static final long serialVersionUID = 1L;

		public PropertyValidationException() {
			super();
		}

		public PropertyValidationException(String arg0, Throwable arg1) {
			super(arg0, arg1);
		}

		public PropertyValidationException(String arg0) {
			super(arg0);
		}

		public PropertyValidationException(Throwable arg0) {
			super(arg0);
		}
		
	}
	
	static Set<Class<? extends IProperty>> numericClasses = new HashSet<Class<? extends IProperty>>();
	
	public static final Property xsd_minInclusive = ResourceFactory.createProperty(XSD.getURI(), "minInclusive");
	
	public static final Property xsd_minExclusive = ResourceFactory.createProperty(XSD.getURI(), "minExclusive");
	
	public static final Property xsd_maxInclusive = ResourceFactory.createProperty(XSD.getURI(), "maxInclusive");
	
	public static final Property xsd_maxExclusive = ResourceFactory.createProperty(XSD.getURI(), "maxExclusive");
	
	static {
		numericClasses.add(IntegerProperty.class);
		numericClasses.add(LongProperty.class);
		numericClasses.add(DoubleProperty.class);
		numericClasses.add(FloatProperty.class);
		numericClasses.add(DateProperty.class);
	}
	
	public static class RestrictionValue {
		
		//number or date
		public Object val;
		
		public String restrictionIndividualURI;
		
		//optional list of specific classes to apply restriction to
		public List<ClassMetadata> classes = null;

		public RestrictionValue(String restrictionIndividualURI, List<ClassMetadata> classes, Object val) {
			super();
			this.restrictionIndividualURI = restrictionIndividualURI;
			this.classes = classes;
			this.val = val;
		}
		
		
	}
	
	public static void setPropertyDefinitionProperties(OntModel model, OntProperty ontProperty, PropertyMetadata pDef) {
		
		//only numerical properties!
		
		Class<? extends IProperty> cls = pDef.getBaseClass();
		
		if(numericClasses.contains(cls)) {
				
			List<RestrictionValue> maximumValuesExclusive = pDef.maximumValuesExclusive;
			if(maximumValuesExclusive == null) maximumValuesExclusive = new ArrayList<RestrictionValue>();
						
			List<RestrictionValue> maximumValuesInclusive = pDef.maximumValuesInclusive;
			if(maximumValuesInclusive == null) maximumValuesInclusive = new ArrayList<RestrictionValue>();
								
			List<RestrictionValue> minimumValuesExclusive = pDef.minimumValuesExclusive;
			if(minimumValuesExclusive == null) minimumValuesExclusive = new ArrayList<RestrictionValue>();				
										
			List<RestrictionValue> minimumValuesInclusive = pDef.minimumValuesInclusive;
			if(minimumValuesInclusive == null) minimumValuesInclusive = new ArrayList<RestrictionValue>();				
												
			handleMinMaxAnnotation(model, pDef, maximumValuesExclusive, VitalCoreOntology.hasMaxValueExclusive);
																				
			handleMinMaxAnnotation(model, pDef, maximumValuesInclusive, VitalCoreOntology.hasMaxValueInclusive);
																				
			handleMinMaxAnnotation(model, pDef, minimumValuesExclusive, VitalCoreOntology.hasMinValueExclusive);
																				
			handleMinMaxAnnotation(model, pDef, minimumValuesInclusive, VitalCoreOntology.hasMinValueInclusive);
																				
			if(pDef.maximumValuesExclusive == null && maximumValuesExclusive.size() > 0) {
				pDef.maximumValuesExclusive = maximumValuesExclusive;
			}
				
			if(pDef.maximumValuesInclusive == null && maximumValuesInclusive.size() > 0) {
				pDef.maximumValuesInclusive = maximumValuesInclusive;
			}
				
			if(pDef.minimumValuesExclusive == null && minimumValuesExclusive.size() > 0) {
				pDef.minimumValuesExclusive = minimumValuesExclusive;
			}
				
			if(pDef.minimumValuesInclusive == null && minimumValuesInclusive.size() > 0) {
				pDef.minimumValuesInclusive = minimumValuesInclusive;
			}

				
				//advanced restrictions here
				
			OntResource rangeRes = ontProperty.getRange();
				
			//type has been determine before
			if(RDFS.Datatype.equals(rangeRes.getPropertyValue(RDF.type))) {
					
				//check if it has valid constraints
				Resource restrictions = rangeRes.getPropertyResourceValue(OWL2.withRestrictions);
					
				//collect restrictions
				List<Statement> restrictionsList = new ArrayList<Statement>();
					
				if(restrictions != null) {
						
					collectRestrictions(restrictionsList, restrictions);
						
				}
					
					
				for(Statement restriction : restrictionsList) {
						
					Property p = restriction.getPredicate();
					
					RDFNode n = restriction.getObject();
					if(!n.isLiteral()) throw new RuntimeException("Restriction statement object must a literal!");
						
					Object v = toJavaValue(p, pDef, n);
						
					if(xsd_maxExclusive.equals(p)) {

						if(pDef.maximumValuesExclusive == null) {
							pDef.maximumValuesExclusive = new ArrayList<RestrictionValue>();
						}		
						pDef.maximumValuesExclusive.add(new RestrictionValue(null, null, v));
							
						} else if(xsd_maxInclusive.equals(p)) {
						
							if(pDef.maximumValuesInclusive == null) {
								pDef.maximumValuesInclusive = new ArrayList<RestrictionValue>();
							}
							pDef.maximumValuesInclusive.add(new RestrictionValue(null, null, v));
						
						} else if(xsd_minExclusive.equals(p)) {
						
							if(pDef.minimumValuesExclusive == null) {
								pDef.minimumValuesExclusive = new ArrayList<RestrictionValue>();
							}	
							pDef.minimumValuesExclusive.add(new RestrictionValue(null, null, v));
							
						} else if(xsd_minInclusive.equals(p)) {
						
							if(pDef.minimumValuesInclusive == null) {
								pDef.minimumValuesInclusive = new ArrayList<RestrictionValue>();
							}
							pDef.minimumValuesInclusive.add(new RestrictionValue(null, null, v));
						
						} else {
							throw new RuntimeException("Restriction predicate must be one of: " + xsd_maxExclusive +", " + xsd_maxInclusive + ", " + xsd_minExclusive + ", " + xsd_minInclusive);
						}
						
						
					}
					
					
					
				}
				
								
			}
			
	}
	
	static void collectRestrictions(List<Statement> restrictionsList, Resource restrictions) {

		Resource first = restrictions.getPropertyResourceValue(RDF.first);
		
		for(StmtIterator i = first.asResource().listProperties(); i.hasNext(); ) {
			
			Statement n = i.nextStatement();
			
			restrictionsList.add(n);
			
		}
		
		Resource rest = restrictions.getPropertyResourceValue(RDF.rest);
		
		if(!rest.equals(RDF.nil)) {
			
			collectRestrictions(restrictionsList, rest);
			
		}
	
	}
	
	static void handleMinMaxAnnotation(OntModel ontModel, PropertyMetadata pDef, List<RestrictionValue> valuesList, Property ann) {
	
		OntProperty ontProperty = ontModel.getOntProperty(pDef.getURI());
		
		for(Statement stmt : ontProperty.listProperties(ann).toList()) {
			
			RDFNode object = stmt.getObject();
			
			if(object.isLiteral()) {
				
				Object value = toJavaValue(ann, pDef, object); 
				
				Class<? extends IProperty> bc = pDef.getBaseClass();
				
				if(NumberProperty.class.isAssignableFrom(bc)) {
					
					if(!(value instanceof Number)) throw new RuntimeException(ann.getURI() + " annotation literal value must be a number, set: " + value);
					
				} else if(DateProperty.class.isAssignableFrom(bc)) {
					
					Date date = null;
					try {
						date = RDFDate.fromXSDString(object.asLiteral().getLexicalForm());
					} catch(Exception e) {
						throw new RuntimeException(ann.getURI() + " annotation literal value must be xsd:datetime, error: " + e.getLocalizedMessage()); 
					}
					
					value = date;
					
				} else {
					throw new RuntimeException(ann.getURI() + " annotation cannot be set on property " + pDef.getURI());
//					new RuntimeException(ann.getURI() + " literal value must be either number or date");
				}
				
				valuesList.add(new RestrictionValue(null, null, value));
				
			} else {
				
				Resource resource = object.asResource();
				if(resource.getURI() == null) throw new RuntimeException(ann.getURI() + " value must be either a literal or URI resource");
				
				Individual individual = ontModel.getIndividual(resource.getURI());
				
				if(individual == null) throw new RuntimeException("Individual not found: " + resource.getURI());
				
				RDFNode rvn = individual.getPropertyValue(VitalCoreOntology.hasRestrictionValue);
				
				if(rvn == null) throw new RuntimeException("No hasRestrictionValue in individual");		

				if(!rvn.isLiteral()) throw new RuntimeException("No hasRestrictionValue must be a literal");
				
				Object lv = toJavaValue(ann, pDef, rvn);
				
				List<ClassMetadata> classes = null;
				
				for(NodeIterator ni = individual.listPropertyValues(VitalCoreOntology.hasRestrictionClasses); ni.hasNext(); ) {
					
					RDFNode n = ni.next();
					
					if(! n.isURIResource() ) throw new RuntimeException("hasRestrictionValue annotations values must be URI resources");
					
					
					ClassMetadata cm = VitalSigns.get().getClassesRegistry().getClass(n.asResource().getURI());
					
					if(cm == null) throw new RuntimeException("hasRestrictionClasses class uri " + n.asResource().getURI() + " of property " + pDef.getURI() + " individual " + individual.getURI() + " not found");
			
					if(classes == null) classes = new ArrayList<ClassMetadata>();
					classes.add(cm);
					
				}
				
				valuesList.add(new RestrictionValue(individual.getURI(), classes, lv));
					
			}
			
		}
		
		
	}

	private static Object toJavaValue(Property ann, PropertyMetadata pDef, RDFNode object) {
		
		Object value = object.asLiteral().getValue();

		Class<? extends IProperty> bc = pDef.getBaseClass();

		if (NumberProperty.class.isAssignableFrom(bc)) {

			if (!(value instanceof Number))
				throw new RuntimeException(ann.getURI()
						+ " annotation literal value must be a number, set: "
						+ value);

		} else if (DateProperty.class.isAssignableFrom(bc)) {

			Date date = null;
			try {
				date = RDFDate.fromXSDString(object.asLiteral()
						.getLexicalForm());
			} catch (Exception e) {
				throw new RuntimeException(
						ann.getURI()
								+ " annotation literal value must be xsd:datetime, error: "
								+ e.getLocalizedMessage());
			}

			value = date;

		} else {
			throw new RuntimeException(ann.getURI()
					+ " annotation cannot be set on property " + pDef.getURI());
			// new RuntimeException(ann.getURI() +
			// " literal value must be either number or date");
		}

		return value;
	}

	/**
	 * throws runtime exceptions on errors
	 * @param class1 
	 * @param thisDefinition
	 * @param newValue
	 * @throws PropertyValidationException
	 */
	public static void validateProperty(Class<? extends GraphObject> class1, PropertyMetadata pDef, Object newValue) throws PropertyValidationException {
		
		if(newValue == null) return;
		
		//don't check types
		if( numericClasses.contains(pDef.getBaseClass()) ) {
		
			validateNumericRestrictions(class1, pDef, newValue, pDef.maximumValuesExclusive, true, false);

			validateNumericRestrictions(class1, pDef, newValue, pDef.maximumValuesInclusive, true, true);
			
			validateNumericRestrictions(class1, pDef, newValue, pDef.minimumValuesExclusive, false, false);
			
			validateNumericRestrictions(class1, pDef, newValue, pDef.minimumValuesInclusive, false, true);
				
		}
		
		
	}
	
	private static void validateNumericRestrictions(Class<? extends GraphObject> class1, PropertyMetadata pDef, Object newValue, List<RestrictionValue> maximumValuesExclusive, boolean maximumNotMinimum, boolean inclusiveNotExclusive) 
		throws PropertyValidationException {
		
		if( maximumValuesExclusive != null ) {
			
			//check list
			checkList(class1, pDef, newValue, maximumValuesExclusive, maximumNotMinimum, inclusiveNotExclusive);
			
		}
		
	}
	
	private static void checkList(Class<? extends GraphObject> class1, PropertyMetadata pDef, Object newValue, List<RestrictionValue> valuesList, boolean maximumNotMinimum, boolean inclusiveNotExclusive) throws PropertyValidationException {
	
		for(RestrictionValue rv : valuesList) {
			
			if(rv.classes != null && rv.classes.size() > 0) {
				
				boolean matches = false;
				
				for( ClassMetadata cm : rv.classes ) {
					
					if(cm.getClazz().isAssignableFrom(class1)) {
						matches = true;
						break;
					}
					
				}
				
				//skip validation for given class
				if(!matches) continue;
				
			}
			
			Object mv = rv.val;
			
			if(newValue instanceof Number) {
			
				int comp = new Double( ((Number)newValue).doubleValue() ).compareTo(((Number)mv).doubleValue());
				
				checkComparison(pDef, newValue, mv, comp, "numeric", maximumNotMinimum, inclusiveNotExclusive);
					
			} else if(newValue instanceof Date) {
			
				int comp = ((Date)newValue).compareTo((Date)mv);

				checkComparison(pDef, newValue, mv, comp, "date", maximumNotMinimum, inclusiveNotExclusive);
			
			} else {
				throw new PropertyValidationException("Unhandled restriction data type: " + newValue.getClass());
			}
			
		}
	}
	
	private static void checkComparison(PropertyMetadata pDef, Object newValue, Object mv, int comp, String label, boolean maximum, boolean inclusiveNotExclusive) throws PropertyValidationException {
		
		boolean fail = false;
		
		String sign = null;
		
		String m = null;
		
		if( maximum && inclusiveNotExclusive && comp > 0) {
			
			fail = true;
			
			sign = ">";
			
			m = "inclusive maximum";
			
		} else if(maximum && !inclusiveNotExclusive && comp >= 0) {
		
			fail = true;

			sign = ">=";
			
			m = "exclusive maximum";
		
		} else if(!maximum && inclusiveNotExclusive && comp < 0) {
		
			fail = true;
			
			sign = "<";
			
			m = "inclusive minimum";
		
		} else if(!maximum && !inclusiveNotExclusive && comp <= 0) {
		
			fail = true;
			
			sign = "<=";
			
			m = "exclusive minimum";
		
		}
		
		if(fail) {
			
			throw new PropertyValidationException("Restriction check failed - property " + pDef.getShortName() + " " + label + " value " + newValue + " " + sign + " " + mv + " " + m);
			
		}
		
	}
		
}

