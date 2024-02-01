package ai.vital.vitalsigns;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.vital.vitalsigns.model.property.IProperty;
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalservice.query.VitalSparqlQuery;
import ai.vital.vitalsigns.conf.VitalSignsConfig;
import ai.vital.vitalsigns.global.GlobalHashTable;
import ai.vital.vitalsigns.model.GraphObject;
import ai.vital.vitalsigns.model.RDFStatement;
import ai.vital.vitalsigns.model.properties.Property_hasRdfObject;
import ai.vital.vitalsigns.model.properties.Property_hasRdfPredicate;
import ai.vital.vitalsigns.model.properties.Property_hasRdfSubject;
import ai.vital.vitalsigns.rdf.RDFFormat;
import ai.vital.vitalsigns.rdf.RDFSerialization;
import ai.vital.vitalsigns.ontology.VitalCoreOntology;
import ai.vital.vitalsigns.ontology.VitalOntDocumentManager;
import ai.vital.vitalsigns.query.ModelSparqlQueryImplementation;
import ai.vital.vitalsigns.utils.StringUtils;
import ai.vital.vitalsigns.utils.SystemExit;

import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;


public class ModelManager {

    private final static Logger log = LoggerFactory.getLogger(ModelManager.class);
    
	private OntDocumentManager manager;
	
	//models manager
	Map<String, Model> modelsMap = Collections.synchronizedMap(new HashMap<String, Model>());
	
	public final static String VITAL_SIGNS_MODEL = "vitalsigns";
	
	public final static String CACHE_MODEL = "cache";
	
	
	private VitalOntDocumentManager vitalOntDocumentManager;

	private VitalSignsConfig config;
	
	public ModelManager(VitalSignsConfig config) {
		
		this.config = config;
		this.vitalOntDocumentManager = new VitalOntDocumentManager();
		
		manager = new OntDocumentManager();
		manager.setProcessImports(false);
		
	}
	
	
	/**
	 * Add a new inferred model
	 * @param name - different than 'vitalsigns'
	 * @param ontModelSpec
	 * @return model if it was added, <code>null</code> if already exists
	 */
	public Model addModel(String name, OntModelSpec ontModelSpec) {
	
		if( ontModelSpec == null ) throw new RuntimeException("ontModelSpec cannot be null");
		
		if(VITAL_SIGNS_MODEL.equals(name)) throw new RuntimeException("'" + VITAL_SIGNS_MODEL + "' name is reserved");
		
		if(CACHE_MODEL.equals(name)) throw new RuntimeException("'"+ CACHE_MODEL + "' name is reserved");
		
		if(modelsMap.containsKey(name)) return null;
		
		ontModelSpec.setDocumentManager(vitalOntDocumentManager);
		
		OntModel model = ModelFactory.createOntologyModel(ontModelSpec);
		
		modelsMap.put(name, model);
		
		return model;
		
	}
	
	
	/**
	 * Add a new base model ( no inference )
	 * @param name - different than 'vitalsigns'
	 * @return model if it was added, <code>null</code> if already exists
	 */
	public Model addModel(String name) {
		
		if(name == null) throw new NullPointerException("Model name cannot be null");
		if(name.isEmpty()) throw new RuntimeException("Model name cannot be empty");
		
		if(VITAL_SIGNS_MODEL.equals(name)) throw new RuntimeException("'" + VITAL_SIGNS_MODEL + "' name is reserved");
		
		if(CACHE_MODEL.equals(name)) throw new RuntimeException("'" + CACHE_MODEL+ "' name is reserved");
		
		if(modelsMap.containsKey(name)) return null;
				
		Model model = ModelFactory.createDefaultModel();
				
		modelsMap.put(name, model);
				
		return model;
						
	}
	
	/**
	 * Returns model or <code>null</code>, 'vitalsigns' name for internal vitalsigns ontology model (copy)
	 * @param name
	 * @return
	 */
	public Model getModel(String name) {
		if(name == null) throw new NullPointerException("Model name cannot be null");
		if(name.isEmpty()) throw new RuntimeException("Model name cannot be empty");
		if(VITAL_SIGNS_MODEL.equals(name)) {
//			return ModelFactory.createOntologyModel(ontologyModel.getSpecification(), ontologyModel.getBaseModel())
			return VitalSigns.get().getOntologyModel();
		}
		if(CACHE_MODEL.equals(name)) throw new RuntimeException("Cannot access cache model directly");
		return modelsMap.get(name);
	}
	
	/**
	 * Lists loaded models (including 'vitalsigns' one)
	 * @return
	 */
	public List<String> listModels() {
		
		List<String> names = new ArrayList<String>(modelsMap.keySet());
		
		names.add(0, VITAL_SIGNS_MODEL);
		
		return names;
		
	}
	
	/**
	 * Removes a model
	 * @param name
	 * @return true if model exists, false otherwise
	 */
	public boolean removeModel(String name) {
	
		if(name == null) throw new NullPointerException("Model name cannot be null");
		if(name.isEmpty()) throw new RuntimeException("Model name cannot be empty");
		
		if(VITAL_SIGNS_MODEL.equals(name)) throw new RuntimeException("'" + VITAL_SIGNS_MODEL + "' name is reserved");
		if(CACHE_MODEL.equals(name)) throw new RuntimeException("'" + CACHE_MODEL + "' name is reserved");
		
		return modelsMap.remove(name) != null;
			
	}

	
	/**
	 * Loads rdf data into existing model.
	 * Note: Input stream is not closed
	 * @return true if model exists and data loaded, false otherwise
	 */
	public boolean loadModel(String name, RDFFormat format, InputStream inputStream) {
		
		if(VITAL_SIGNS_MODEL.equals(name)) throw new RuntimeException("'" + VITAL_SIGNS_MODEL + "' name is reserved");
		
		if(CACHE_MODEL.equals(name)) throw new RuntimeException("'" + CACHE_MODEL + "' name is reserved");
		
		if(format == null) throw new NullPointerException("RDFFormat cannot be null");
		
		Model m = getModel(name);
		
		if(m == null) return false;
		
		m.read(inputStream, null, format.toJenaTypeString());
		
		return true;
	}
	
	/**
	 * Saves rdf data into output string
	 * @param name
	 * @param format
	 * @param outputStream
	 * @return
	 */
	public boolean saveModel(String name, RDFFormat format, OutputStream outputStream) {
		
		if(format == null) throw new NullPointerException("RDFFormat cannot be null");
		
		Model m = getModel(name);
		
		if(m == null) return false;
		
		m.write(outputStream, format.toJenaTypeString());
		
		return true;
		
	}

	
	/**
	 * Saves the model to string of requested format
	 * @return model string or <code>null</code> if not found
	 */
	public String modelToString(String name, RDFFormat format) {
		
		if(format == null) throw new NullPointerException("RDFFormat cannot be null");
		
		Model m = getModel(name);
		
		if(m == null) return null;
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		m.write(os, format.toJenaTypeString());
		
		try {
			os.close();
			return os.toString("UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	

	/**
	 * Creates a new inferred model for given schema and instance models.
	 * Schema model must be an inferred model.
	 * Throws exceptions in any case
	 * @param newInferredModelName
	 * @param schemaModelName
	 * @param instanceModelName
	 * @return new inferred bound model
	 */
	public Model bind(String newInferredModelName, String schemaModelName, String instanceModelName) throws Exception {
		return this.bind(newInferredModelName, schemaModelName, instanceModelName, null);
	}

	/**
	 * Creates a new inferred model for given schema and instance models.
	 * Schema model must be an inferred model.
	 * Throws exceptions in any case
	 * @param newInferredModelName
	 * @param schemaModelName
	 * @param instanceModelName
	 * @param optionalReasonerSpec if <code>null</code> then schema model reasoner is used
	 * @return new inferred bound model
	 */
	public Model bind(String newInferredModelName, String schemaModelName, String instanceModelName, OntModelSpec optionalReasonerSpec) throws Exception {
		
		Model newModel = this.getModel(newInferredModelName);
		
		if(newModel != null) throw new RuntimeException("Model with name " + newInferredModelName + " already exists");
		
		Model schemaModel = this.getModel(schemaModelName);
		
		if(schemaModel == null) throw new RuntimeException("Schema model not found: " + schemaModelName);
		
		if(!( schemaModel instanceof OntModel) ) throw new RuntimeException("Schema model must be an ontology one: " + schemaModelName);

		Model instanceModel	= this.getModel(instanceModelName);
		
		if(instanceModel == null) throw new RuntimeException("Instance model not found: " + instanceModelName);
		
		if(optionalReasonerSpec == null) {
			optionalReasonerSpec = ((OntModel)schemaModel).getSpecification();
		}
		
		
		OntModel boundModel = ModelFactory.createOntologyModel(optionalReasonerSpec, schemaModel);
		
		boundModel.add(instanceModel);
		
		modelsMap.put(newInferredModelName, boundModel);
		
		return boundModel;
		
	}

	/**
	 * Checks if a model contains only valid vital graph objects
	 * @param model 'vitalsigns' and 'cache' not supported
	 * @param ontologyIRIFilter optional ontology to validate only against it (with imports)
	 * @return
	 */
	public VitalStatus checkModel(String name, String ontologyIRIFilter) {
		
		if(name == null) throw new NullPointerException("Name mustn't be null");
		
		if(VITAL_SIGNS_MODEL.equals(name) || CACHE_MODEL.equals(name)) throw new RuntimeException("Cannot validate '" + VITAL_SIGNS_MODEL + "' or '" + CACHE_MODEL + "' model");
		
		Model model = this.getModel(name);
		
		if(model == null) throw new RuntimeException("Model not found: " + name);
		
		List<String> validNamespaces = null;
		
		if(ontologyIRIFilter != null && !ontologyIRIFilter.isEmpty()) {
			
			validNamespaces = new ArrayList<String>();
			
			List<String> imports = VitalSigns.get().getOntologyURI2ImportsTree().get(ontologyIRIFilter);
			
			if(imports == null) throw new RuntimeException("Ontology with IRI " + ontologyIRIFilter + " not found in vitalsigns");
			
			validNamespaces.addAll(imports);
			validNamespaces.add(ontologyIRIFilter);
			
		}
		
		int n = 0;
		
		for(ResIterator iter = model.listSubjects(); iter.hasNext(); ) {
			
			Resource r = iter.nextResource();

			//first try to deserialize the object
			
			try {
				GraphObject go = RDFSerialization.deserialize(r.getURI(), model, true);
				if(go == null) {
					throw new Exception("No vital graph object deserialized for resource " + r.getURI() + ", statements: " + r.listProperties());
				}
				if(validNamespaces != null) {
					
					String rdfClass = VitalSigns.get().getClassesRegistry().getClassURI(go.getClass());
					
					String ns = rdfClass.substring(0, rdfClass.indexOf('#'));
					
					if(!validNamespaces.contains(ns)) {
						throw new Exception("Graph object " + r.getURI() + " is valid but not in the specified ontology " + ontologyIRIFilter);
					}
					
				}
			} catch(Exception e) {
				return VitalStatus.withError(e.getLocalizedMessage());
			}
			
			n++;
			
		}

		return VitalStatus.withOKMessage("Validated " + n + " graph objects");
		
	}

	public VitalStatus importModel(String name) {

		if(name == null) throw new NullPointerException("Name mustn't be null");
		
		if(VITAL_SIGNS_MODEL.equals(name) || CACHE_MODEL.equals(name)) throw new RuntimeException("Cannot import " + VITAL_SIGNS_MODEL + " or " + CACHE_MODEL + " model");
		
		Model model = this.getModel(name);
		
		if(model == null) throw new RuntimeException("Model not found: " + name);
		
		List<GraphObject> gos = new ArrayList<GraphObject>();
		
		for(ResIterator iter = model.listSubjects(); iter.hasNext(); ) {
			
			Resource r = iter.nextResource();

			//first try to deserialize the object
			
			try {
				GraphObject go = RDFSerialization.deserialize(r.getURI(), model, true);
				if(go == null) {
					throw new Exception("No vital graph object deserialized for resource " + r.getURI() + ", statements: " + r.listProperties());
				}
				gos.add(go);
			} catch(Exception e) {
				return VitalStatus.withError(e.getLocalizedMessage());
			}
			
		}

		GlobalHashTable.get().putAll(gos);
		
		return VitalStatus.withOKMessage("Imported " + gos.size() + " graph objects into cache");
		
	}
	
	/**
	 * Returns all individuals (vital objects) found in given managed model
	 * @param name
	 * @return list of objects
	 */
	public List<GraphObject> getModelIndividuals(String name) {
		return getModelIndividuals(name, null);
	}
	
	/**
	 * Returns all individuals (vital objects) found in given managed model.
	 * Optionally filtered by ontology IRI (must exist)
	 * @param name
	 * @return list of objects
	 */
	public List<GraphObject> getModelIndividuals(String name, String ontologyIRIFilter) {
		

		if(name == null) throw new NullPointerException("Name mustn't be null");

		if(ontologyIRIFilter != null) {
			if(!VitalSigns.get().getNs2Package().containsKey(ontologyIRIFilter)) throw new RuntimeException("Ontology with IRI: " + ontologyIRIFilter + " not found");
		}
		
		List<GraphObject> res = new ArrayList<GraphObject>();
		
		if(CACHE_MODEL.equals(name)) {
			
			for ( GraphObject go : GlobalHashTable.get() ) {
				if(ontologyIRIFilter != null) {
					
					String rdfClass = VitalSigns.get().getClassesRegistry().getClassURI(go.getClass());
					String ns = rdfClass.substring(0, rdfClass.indexOf('#'));
					
					if(!ontologyIRIFilter.equals(ns)) continue;
					
				}
				res.add(go);
			}
			
		} else {
		
			Model model = this.getModel(name);
			
			if(model == null) throw new RuntimeException("Model not found: " + name);
			
			for(ResIterator iter = model.listSubjects(); iter.hasNext(); ) {
				
				Resource r = iter.nextResource();
	
				//first try to deserialize the object
				
				GraphObject go = RDFSerialization.deserialize(r.getURI(), model, true);
				if(go == null) {
					throw new RuntimeException("No vital graph object deserialized from resource " + r.getURI() + ", statements: " + r.listProperties());
				}
				
				if(ontologyIRIFilter != null) {
						
					String rdfClass = VitalSigns.get().getClassesRegistry().getClassURI(go.getClass());
					String ns = rdfClass.substring(0, rdfClass.indexOf('#'));
						
					if(!ontologyIRIFilter.equals(ns)) continue;
						
				}
				
				res.add(go);
				
			}
		
		}

		return res;
		
	}
	
	public ResultList doSparqlQuery(String modelName, VitalSparqlQuery sparqlQuery) {
		
		if(modelName == null) throw new NullPointerException("Name mustn't be null");
		
		if(sparqlQuery == null) throw new NullPointerException("sparqlQuery mustn't be null");
		
		if(CACHE_MODEL.equals(modelName)) throw new RuntimeException("Cannot query '" + CACHE_MODEL + "' model");
		
		Model model = this.getModel(modelName);
		
		if(model == null) throw new RuntimeException("Model not found: " + modelName );
		
		return ModelSparqlQueryImplementation.handleSparqlQuery(model, sparqlQuery);
	}

	/**
	 * Adds a statement to a named model
	 * @param modelName
	 * @param statement
	 */
	public void addModelStatement(String modelName, RDFStatement statement) {
		
		if(modelName == null) throw new NullPointerException("Name mustn't be null");
		
		if(CACHE_MODEL.equals(modelName) ||VITAL_SIGNS_MODEL.equals(modelName)) throw new RuntimeException("Cannot add to '" + CACHE_MODEL+ "' or '" + VITAL_SIGNS_MODEL + "' model");
		
		if(statement == null) throw new NullPointerException("Statement mustn't be null");
		
		Model model = getModel(modelName);
		
		if(model == null) throw new RuntimeException("Model not found: " + modelName);
		
		IProperty rdfSubject   = (IProperty) statement.get(Property_hasRdfSubject.class);
		IProperty rdfPredicate = (IProperty) statement.get(Property_hasRdfPredicate.class);
		IProperty rdfObject    = (IProperty) statement.get(Property_hasRdfObject.class);
			
		if( StringUtils.isEmpty(rdfSubject) ) throw new RuntimeException("No rdfSubject property");
		if( StringUtils.isEmpty(rdfPredicate) ) throw new RuntimeException("No rdfPredicate property");
		if( StringUtils.isEmpty(rdfObject) ) throw new RuntimeException("No rdfObject property");
			
		try {
			model.read(new ByteArrayInputStream((rdfSubject + " " + rdfPredicate + " " + rdfObject + " .\n").getBytes("UTF-8")), null, RDFFormat.N_TRIPLE.toJenaTypeString());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	/**
	 * Removes a statement from a named model
	 * @param modelName
	 * @param statement
	 */
	public void removeModelStatement(String modelName, RDFStatement statement) {
		
		if(modelName == null) throw new NullPointerException("Name mustn't be null");
		
		if(CACHE_MODEL.equals(modelName) ||VITAL_SIGNS_MODEL.equals(modelName)) throw new RuntimeException("Cannot remove statement from '" + CACHE_MODEL+ "' or '" + VITAL_SIGNS_MODEL + "' model");
		
		if(statement == null) throw new NullPointerException("Statement mustn't be null");
		
		Model model = getModel(modelName);
		
		if(model == null) throw new RuntimeException("Model not found: " + modelName);
		
		IProperty rdfSubject   = (IProperty) statement.get(Property_hasRdfSubject.class);
		IProperty rdfPredicate = (IProperty) statement.get(Property_hasRdfPredicate.class);
		IProperty rdfObject    = (IProperty) statement.get(Property_hasRdfObject.class);
			
		if( StringUtils.isEmpty(rdfSubject) ) throw new RuntimeException("No rdfSubject property");
		if( StringUtils.isEmpty(rdfPredicate) ) throw new RuntimeException("No rdfPredicate property");
		if( StringUtils.isEmpty(rdfObject) ) throw new RuntimeException("No rdfObject property");
			
		Model tempModel = ModelFactory.createDefaultModel();
		try {
			tempModel.read(new ByteArrayInputStream((rdfSubject + " " + rdfPredicate + " " + rdfObject + " .\n").getBytes("UTF-8")), null, RDFFormat.N_TRIPLE.toJenaTypeString());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
			
		model.remove(tempModel);
		
	}
	
	static Resource NamedIndividual = null;
	
	static {
		NamedIndividual = ResourceFactory.createResource(OWL.NS + "NamedIndividual");
	}
	
	/**
	 * A method that normalizes domain individuals - adds vitaltype property
	 * @param m
	 * @return number of updated individuals
	 */
	public int normalizeIndividuals(Model m) {
		
		int c = 0;
		
		for( ResIterator iter = m.listSubjectsWithProperty(RDF.type, NamedIndividual); iter.hasNext(); ) {
			
			Resource indiv = iter.nextResource();
			
			List<Statement> list = indiv.listProperties(VitalCoreOntology.vitaltype).toList();
			
			if(list.size() > 0 ) continue;
				
			List<Statement> types = indiv.listProperties(RDF.type).toList();
				
			Resource firstType = null;
				
			for(Statement t : types) {
					
				Resource r = t.getResource();
				if(!r.equals(NamedIndividual)) {
					firstType = r;
					break;
				}
				
			}
				
			if(firstType == null) throw new RuntimeException("No rdf:type other than owl:NamedIndividual in individual: " + indiv.getURI());
			
			indiv.addProperty(VitalCoreOntology.vitaltype, firstType);
				
			c++;
			
		}
		
		return c;
		
		
	}
	
	public OntModel createNewOntModel() throws Exception {
		
		Field specField = OntModelSpec.class.getField(config.inferenceLevel);
		
		if(specField == null) {
		    String m = "Unknown inference level: " + config.inferenceLevel + ", use one of constants from " + OntModelSpec.class.getCanonicalName();
		    log.error(m);
			System.err.println(m);			
			SystemExit.exit(1, 3000);
		}
		
		OntModelSpec spec = (OntModelSpec) specField.get(null);
				
		if(spec == null) {
		    String m = "Unknown " + OntModelSpec.class.getCanonicalName() + " field: " + config.inferenceLevel;
		    log.error(m);
			System.err.println(m);
			SystemExit.exit(1, 3000);
		}
		
//		OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);
		spec.setDocumentManager(manager);

		OntModel m = ModelFactory.createOntologyModel(spec, null);

		return m;
		
	}

	/**
	 * This method creates a model that is used for domain generation or ontology processing only
	 * @return
	 * @throws Exception
	 */
	public OntModel createIntermediateOntModel() throws Exception {

		OntModelSpec spec = OntModelSpec.OWL_MEM;
				
//		OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);
		spec.setDocumentManager(manager);

		OntModel m = ModelFactory.createOntologyModel(spec, null);

		return m;
		
	}
}
