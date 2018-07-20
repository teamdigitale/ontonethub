package it.cnr.istc.stlab.ontonethub.impl;

import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.ANNOTATION_PROPERTIES;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.DATATYPE_PROPERTIES;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.HAS_BUNDLE;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.HAS_ONTOLOGY_IRI;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.IMPORTED_ONTOLOGIES;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.INDIVIDUALS;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.OBJECT_PROPERTIES;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.ONTOLOGY;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.OWL_CLASSES;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.jobs.api.JobManager;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

import it.cnr.istc.stlab.ontonethub.Constants;
import it.cnr.istc.stlab.ontonethub.NoSuchOntologyException;
import it.cnr.istc.stlab.ontonethub.OntoNetHub;
import it.cnr.istc.stlab.ontonethub.OntologyAlreadyExistingException;
import it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary;
import it.cnr.istc.stlab.ontonethub.OntologyInfo;
import it.cnr.istc.stlab.ontonethub.SPARQLQueryManager;
import it.cnr.istc.stlab.ontonethub.UnmappableOntologyException;
import it.cnr.istc.stlab.ontonethub.job.IndexingJob;
import it.cnr.istc.stlab.ontonethub.job.IndexingJobInput;
import it.cnr.istc.stlab.ontonethub.job.RDFIndexingJob;
import it.cnr.istc.stlab.ontonethub.solr.OntoNetHubSiteManager;

/**
 * Default implementation of the {@link OntoNetHub}.<br>
 * Such an implementation provide an OSGi component for the OntoNetHub.
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */

@Component(immediate = true)
@Service(OntoNetHub.class)
public class OntoNetHubImpl implements OntoNetHub {
	
	public static final String RUNNABLE_INDEXER_EXECUTABLES_FOLDER = "ontonethub-indexing" + File.separator + "executables";
	public static final String RUNNABLE_INDEXER_EXECUTABLES = RUNNABLE_INDEXER_EXECUTABLES_FOLDER + File.separator + "indexing-genericrdf.jar";
	public static final String INNER_INDEXER_EXECUTABLES = "executables" + File.separator + "indexing-genericrdf.jar";
	public static final String DEFAULT_ONTOLOGIES_FOULDER = "default_ontologies";
	
	private static final String INDEX_DUMP_DOWNLOAD = "it.cnr.istc.stlab.ontonethub.impl.OntoNetHubImpl.index.dump.download";
	private static final boolean _INDEX_DUMP_DOWNLOAD_DEFAULT_ = true;
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private ComponentContext ctx;
	
	@Property(name=INDEX_DUMP_DOWNLOAD, boolValue=_INDEX_DUMP_DOWNLOAD_DEFAULT_)
	private boolean indexDumpDownloadMode;
	
	@Reference
	private OntoNetHubSiteManager siteManager;
	
	@Reference
	private SPARQLQueryManager sparqlQueryManager;
	
	@Reference
	private JobManager jobManager;
	
	@Reference
	private TcManager tcManager;
	
	private File ontologiesFolder;
	
	/**
	 * OSGi constructor.<br>
	 * Do not use outside OSGi environments.
	 */
	public OntoNetHubImpl() {
		
	}
	
	/**
	 * Non-OSGi constructor.<br>
	 * Use this constructor outside OSGi environment.
	 * 
	 * @param siteManager
	 * @param jobManager
	 * @param tcManager
	 */
	public OntoNetHubImpl(OntoNetHubSiteManager siteManager, JobManager jobManager, TcManager tcManager){
		this.siteManager = siteManager;
		this.jobManager = jobManager;
		this.tcManager = tcManager;
	}

	@Override
	public String indexOntology(IndexingJobInput input) throws OntologyAlreadyExistingException {
		
		Site site = siteManager.getSite(input.getName());
		
		if(site == null){
			IndexingJob job = null;
			if(input.getOntologyId() == null)
				job = new IndexingJob(this.indexDumpDownloadMode, sparqlQueryManager, siteManager, input.getName(), input.getDescription(), input.getBaseURI(), input.getData(), ctx.getBundleContext(), tcManager, ontologiesFolder);
			else 
				job = new IndexingJob(this.indexDumpDownloadMode, sparqlQueryManager, siteManager, input.getOntologyId(), input.getDescription(), input.getBaseURI(), input.getData(), ctx.getBundleContext(), tcManager, ontologiesFolder);
			String jid = jobManager.execute(job);
			
			return jid;
		}
		else throw new OntologyAlreadyExistingException(input.getName());
		
	}
	
	private String indexRDF(IndexingJobInput input) throws OntologyAlreadyExistingException {
		
		Site site = siteManager.getSite(input.getName());
		
		if(site == null){
			RDFIndexingJob job = null;
			if(input.getOntologyId() == null)
				job = new RDFIndexingJob(input.getName(), input.getDescription(), input.getBaseURI(), input.getData(), ctx.getBundleContext(), tcManager, ontologiesFolder);
			else 
				job = new RDFIndexingJob(input.getOntologyId(), input.getDescription(), input.getBaseURI(), input.getData(), ctx.getBundleContext(), tcManager, ontologiesFolder);
			String jid = jobManager.execute(job);
			
			return jid;
		}
		else throw new OntologyAlreadyExistingException(input.getName());
		
	}
	
	@Override
	public void deleteOntologyIndex(String id) throws UnmappableOntologyException, NoSuchOntologyException {
		
		boolean found = false;
		
		String ontologyURI = ONTOLOGY + id;
		
		Graph g = tcManager.getMGraph(new IRI("ontonethub-graph"));
		Iterator<Triple> tripleIt = g.filter(new IRI(ontologyURI), 
				new IRI(HAS_BUNDLE), 
				null);
		
		String bundleId = null;
		Triple tripleToRemove = null;
		if(tripleIt.hasNext()){
			tripleToRemove = tripleIt.next();
			Literal bundleIdLiteral = (Literal) tripleToRemove.getObject();
			bundleId = bundleIdLiteral.getLexicalForm(); 
		}
		
		tripleIt = g.filter(new IRI(ontologyURI), 
				RDFS.label, 
				null);
		
		String indexLabel = null;
		if(tripleIt.hasNext()){
			Triple triple = tripleIt.next();
			Literal nameLiteral = (Literal) triple.getObject();
			indexLabel = nameLiteral.getLexicalForm(); 
		}
		
		if(bundleId != null && indexLabel != null){
		
			Bundle bundle = ctx.getBundleContext().getBundle(Long.valueOf(bundleId));
			if(bundle != null){
				String symbolicName = bundle.getSymbolicName();
				String siteName = symbolicName.replace("org.apache.stanbol.data.site.", "");
				String path = "org.apache.stanbol.data.site.".replaceAll("\\.", File.separator) + siteName;
				
				URL resourceURL = bundle.getResource(path + File.separator + "org.apache.stanbol.entityhub.site.referencedSite-" + indexLabel + ".config");
				
				if(resourceURL != null){
					InputStream is;
					try {
						is = resourceURL.openStream();
					
						Properties properties = new Properties();
						properties.load(is);
						
						String ontologyName = properties.getProperty("org.apache.stanbol.entityhub.site.id");
						if(ontologyName != null){
							/*
							 * Uninstall the bundle managing the index. 
							 */
							bundle.uninstall();
							/*
							 * Remove the triple from the graph that represents the ontology catalogue.
							 */
							g.remove(tripleToRemove);
							
							/*
							 * Remove the ontology from OntoNet
							 */
							
							File ontologyFile = new File(ontologiesFolder, id + ".rdf");
							ontologyFile.delete();
							
							/*
							 * Clean the catalogue.
							 */
							tripleIt = g.filter(new IRI(ontologyURI), null, null);
							List<Triple> triplesToRemove = new ArrayList<Triple>();
							while(tripleIt.hasNext())
								triplesToRemove.add(tripleIt.next());
							
							for(Triple triple : triplesToRemove)
								g.remove(triple);
							
							found = true;
						}
					} catch (IOException e) {
						throw new UnmappableOntologyException(id, e);
					} catch (BundleException e) {
						throw new UnmappableOntologyException(id, e);
					}
				}
					
			}
			
		}
		
		
		if(!found) {
			throw new NoSuchOntologyException(id);
		}
	}
	
	@Override
	public OntologyInfo getOntologyInfo(String id) throws NoSuchOntologyException {
		String ontologyURI = ONTOLOGY + id;
		
		Graph g = tcManager.getMGraph(new IRI("ontonethub-graph"));
		Iterator<Triple> tripleIt = g.filter(new IRI(ontologyURI), 
				new IRI(HAS_BUNDLE), 
				null);
		
		String bundleId = null;
		if(tripleIt.hasNext()){
			Literal bundleIdLiteral = (Literal) tripleIt.next().getObject();
			bundleId = bundleIdLiteral.getLexicalForm();
		}
		
		OntologyInfo ontologyInfo = null;
		
		if(bundleId != null) ontologyInfo = ontologyInfo(id, bundleId, g);
		
		if(ontologyInfo == null) throw new NoSuchOntologyException(id);
		else return ontologyInfo;
	}
	
	@Override
	public Collection<Representation> getOntologyEntityContext(String entityID, String lang){
		
		Collection<Representation> transformedResults = new ArrayList<Representation>();
		Entity entity = siteManager.getEntity(entityID);
		if(entity != null){
			Representation representation = entity.getRepresentation();
			if(representation != null){
				Iterator<Object> it = representation.get("http://dati.gov.it/onto/ann-voc/usage");
				while(it.hasNext()){
					Object context = it.next();
					if(context instanceof org.apache.stanbol.entityhub.servicesapi.model.Reference){
						org.apache.stanbol.entityhub.servicesapi.model.Reference contextReference = 
								(org.apache.stanbol.entityhub.servicesapi.model.Reference) context;
						String reference = contextReference.getReference();
						
						Entity entityRef = siteManager.getEntity(reference);
						
				    	ValueFactory vf = InMemoryValueFactory.getInstance();
				    	
			    		Representation repr = entityRef.getRepresentation();
			    		Representation newRepresentation = vf.createRepresentation(repr.getId());
			    		
			    		addTextToRepresentation("http://www.w3.org/2000/01/rdf-schema#label", "label", repr, newRepresentation, lang, vf);
			    		addTextToRepresentation("http://www.w3.org/2000/01/rdf-schema#comment", "comment", representation, newRepresentation, lang, vf);
			    		addObjectToRepresentation("http://dati.gov.it/onto/ann-voc/dafLabel", "dafLabel", representation, newRepresentation, vf);
			    		addObjectToRepresentation("http://dati.gov.it/onto/ann-voc/dafId", "dafId", representation, newRepresentation, vf);
			    		addTextToRepresentation("http://dati.gov.it/onto/ann-voc/domainClassLabel", "label.class", representation, newRepresentation, lang, vf);
			    		addTextToRepresentation("http://dati.gov.it/onto/ann-voc/domainClassComment", "comment.class", representation, newRepresentation, lang, vf);
			    		addTextToRepresentation("http://dati.gov.it/onto/ann-voc/ontologyLabel", "label.ontology", representation, newRepresentation, lang, vf);
			    		addTextToRepresentation("http://dati.gov.it/onto/ann-voc/ontologyComment", "comment.ontology", representation, newRepresentation, lang, vf);
			    		addObjectToRepresentation("http://stanbol.apache.org/ontology/entityhub/query#score", "score", representation, newRepresentation, vf);
			    		
			    		transformedResults.add(newRepresentation);
						
					}
				}
				
			}
		}
		
		return transformedResults;
	}

	private void addTextToRepresentation(String oldFieldName, 
		String newFieldName, 
		Representation oldRepresentation, 
		Representation newRepresentation, String lang, ValueFactory vf){
	Iterator<Text> it = oldRepresentation.get(oldFieldName, lang);
	
		if(it != null){
			it.forEachRemaining(obj -> {
				Text t = vf.createText(obj.getText(), lang);
				newRepresentation.set(newFieldName, t);
			});
		}
	}
	
	private void addObjectToRepresentation(String oldFieldName, 
    		String newFieldName, 
    		Representation oldRepresentation, 
    		Representation newRepresentation, ValueFactory vf){
    	Iterator<Object> it = oldRepresentation.get(oldFieldName);
    	if(it != null)
			it.forEachRemaining(obj -> {
				newRepresentation.set(newFieldName, obj);
			});
    }
	
	@Override
	public OntologyInfo[] getOntologiesInfo() {
		List<OntologyInfo> ontologiesInfo =  new ArrayList<OntologyInfo>();
		
		Graph g = tcManager.getMGraph(new IRI("ontonethub-graph"));
		Iterator<Triple> tripleIt = g.filter(null, 
				new IRI(HAS_BUNDLE), 
				null);
		
		String bundleId = null;
		while(tripleIt.hasNext()){
			Triple triple = tripleIt.next();
			BlankNodeOrIRI subject = triple.getSubject();
			String ontologyId = subject.toString().replace("<", "").replace(">", "");
			ontologyId = ontologyId.substring(ontologyId.lastIndexOf("/")+1);
			
			Literal bundleIdLiteral = (Literal) triple.getObject();
			bundleId = bundleIdLiteral.getLexicalForm();
			if(bundleId != null){
				OntologyInfo ontologyInfo;
				try {
					ontologyInfo = ontologyInfo(ontologyId, bundleId, g);
				} catch (NoSuchOntologyException e) {
					ontologyInfo = null;
				}
				if(ontologiesInfo != null) ontologiesInfo.add(ontologyInfo);
			}
		}
		
		OntologyInfo[] ontologiesInfoArray = new OntologyInfo[ontologiesInfo.size()];
		
		return ontologiesInfo.toArray(ontologiesInfoArray);
	}
	
	private OntologyInfo ontologyInfo(String ontologyId, String bundleId, Graph g) throws NoSuchOntologyException {
		String ontologyURI = ONTOLOGY + ontologyId;
		
		OntologyInfo ontologyInfo = null;
		
		Bundle bundle = ctx.getBundleContext().getBundle(Long.valueOf(bundleId));
		if(bundle != null){
			ontologyInfo = new OntologyInfo();
			ontologyInfo.setOntologyID(ontologyId);
			
			String ontologyName = null;
			String ontologyDescription = null;
			String ontologyIRI = null;
			
			
			Iterator<Triple> tripleIt = g.filter(new IRI(ontologyURI), 
					RDFS.label, 
					null);
			if(tripleIt.hasNext()){
				Literal literal = (Literal) tripleIt.next().getObject();
				ontologyName = literal.getLexicalForm();
			}
			
			tripleIt = g.filter(new IRI(ontologyURI), 
					DC.description, 
					null);
			if(tripleIt.hasNext()){
				Literal literal = (Literal) tripleIt.next().getObject();
				ontologyDescription= literal.getLexicalForm();
			}
			
			String sourceIRI = ontologyURI + "/source";
			ontologyInfo.setOntologySource(sourceIRI);
			ontologyInfo.setOntologyName(ontologyName);
			ontologyInfo.setOntologyDescription(ontologyDescription);
			
			tripleIt = g.filter(new IRI(ontologyURI), 
					new IRI(HAS_ONTOLOGY_IRI), 
					null);
			if(tripleIt.hasNext()){
				IRI iri = (IRI) tripleIt.next().getObject();
				ontologyIRI = iri.toString().replace("<", "").replace(">", "");
			}
			ontologyInfo.setOntologyIRI(ontologyIRI);
			
			/*
			 * OWL classes
			 */
			tripleIt = g.filter(new IRI(ontologyURI), 
					new IRI(OWL_CLASSES), 
					null);
			int owlClasses = 0;
			if(tripleIt.hasNext()){
				String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
				owlClasses = Integer.valueOf(lexicalForm);
			}
			ontologyInfo.setOwlClasses(owlClasses);
			
			/*
			 * Object properties
			 */
			tripleIt = g.filter(new IRI(ontologyURI), 
					new IRI(OBJECT_PROPERTIES), 
					null);
			int objectProperties = 0;
			if(tripleIt.hasNext()){
				String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
				objectProperties = Integer.valueOf(lexicalForm);
			}
			ontologyInfo.setObjectProperties(objectProperties);
			
			/*
			 * Datatype properties
			 */
			tripleIt = g.filter(new IRI(ontologyURI), 
					new IRI(DATATYPE_PROPERTIES), 
					null);
			int datatypeProperties = 0;
			if(tripleIt.hasNext()){
				String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
				datatypeProperties = Integer.valueOf(lexicalForm);
			}
			ontologyInfo.setDatatypeProperties(datatypeProperties);
			
			/*
			 * Annotation properties
			 */
			tripleIt = g.filter(new IRI(ontologyURI), 
					new IRI(ANNOTATION_PROPERTIES), 
					null);
			int annotationProperties = 0;
			if(tripleIt.hasNext()){
				String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
				annotationProperties = Integer.valueOf(lexicalForm);
			}
			ontologyInfo.setAnnotationProperties(annotationProperties);
			
			/*
			 * Individuals
			 */
			tripleIt = g.filter(new IRI(ontologyURI), 
					new IRI(INDIVIDUALS), 
					null);
			int individuals = 0;
			if(tripleIt.hasNext()){
				String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
				individuals = Integer.valueOf(lexicalForm);
			}
			ontologyInfo.setIndividuals(individuals);
			
			/*
			 * Individuals
			 */
			tripleIt = g.filter(new IRI(ontologyURI), 
					new IRI(IMPORTED_ONTOLOGIES), 
					null);
			int importedOntologies = 0;
			if(tripleIt.hasNext()){
				String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
				importedOntologies = Integer.valueOf(lexicalForm);
			}
			ontologyInfo.setImportedOntologies(importedOntologies);
			
			
			/*
			 * All map
			 */
			tripleIt = g.filter(new IRI(ontologyURI), 
					null, 
					null);
			Map<String, List<String>> map = new HashMap<String, List<String>>();
			while(tripleIt.hasNext()){
				Triple triple = tripleIt.next();
				IRI predicate = triple.getPredicate();
				String predicateString = predicate.toString().replace("<", "").replace(">", "");
				List<String> values = map.get(predicateString);
				if(values == null){
					values = new ArrayList<String>();
					map.put(predicateString, values);
				}
				RDFTerm objectTerm = triple.getObject();
				String value = null;
				if(objectTerm instanceof IRI) value = ((IRI)objectTerm).toString().replace("<", "").replace(">", "");
				else value = ((Literal)objectTerm).getLexicalForm();
				values.add(value);
			}
			ontologyInfo.setRdfMetadata(map);
			
			
		}
		
		if(ontologyInfo == null) throw new NoSuchOntologyException(ontologyId);
		else return ontologyInfo;
	}
	
	@Override
	public Model getOntologySource(String id) throws NoSuchOntologyException {
		
		try {
			Model model = FileManager.get().loadModel(new File(ontologiesFolder, id + ".rdf").getCanonicalPath());
			return model;
		} catch (IOException e) {
			throw new NoSuchOntologyException(id);
		}
	}
	
	protected void activate(ComponentContext ctx) throws ConfigurationException, FileNotFoundException, IOException {
		this.ctx = ctx;
		
		String stanbolHome = ctx.getBundleContext().getProperty("stanbol.home");
				
		URL indexerExecutablesUrl = ctx.getBundleContext().getBundle().getResource(INNER_INDEXER_EXECUTABLES);
		File outFolder = new File(stanbolHome + File.separator + "ontonethub-indexing" + File.separator + "executables");
		outFolder.mkdirs();
		File outFile = new File(outFolder, "indexing-genericrdf.jar");
	
		Graph graph = null;
		try{
			graph = tcManager.createGraph(new IRI("ontonethub-graph"));
		} catch(EntityAlreadyExistsException e){
			log.info("The graph managed by the OntonetHub already exists.");
			graph = tcManager.getGraph(new IRI("ontonethub-graph"));
		}
		
		this.ontologiesFolder = new File(stanbolHome + File.separator + "ontonethub-indexing" + File.separator + "ontologies");
		ontologiesFolder.mkdirs();
		if(indexerExecutablesUrl != null){
			IOUtils.copy(indexerExecutablesUrl.openStream(), new FileOutputStream(outFile));
		}
		
		
		
		Bundle bundle = ctx.getBundleContext().getBundle();
		
		URL wnEntryPath = bundle.getEntry("ontologies/wn.ttl");
		if(wnEntryPath != null){
			Model wnModel = ModelFactory.createDefaultModel();
			
			wnModel.read(wnEntryPath.openStream(), Constants.wordNetNamespace, "TURTLE");
			
			try {
				deleteOntologyIndex(Constants.wordNetSiteID);
			} catch (UnmappableOntologyException | NoSuchOntologyException e) {
				log.warn("The index for WordNet does not exist.");
			}
			doIndexing(Constants.wordNetSiteID, 
					Constants.wordNetSiteName, 
					Constants.wordNetSiteDescription, 
					Constants.wordNetNamespace, 
					wnModel);
		}
		
		Boolean downloadIndexDump= (Boolean)(ctx.getProperties()).get(INDEX_DUMP_DOWNLOAD);
    	if(downloadIndexDump  != null) this.indexDumpDownloadMode = downloadIndexDump.booleanValue();
    	
    	if(this.indexDumpDownloadMode){
    		log.info("DUMP mode");
    		File baseStanbolHome = new File(stanbolHome);
    		InputStream is = bundle.getEntry("indexing/FULL-AP_IT.solrindex.zip").openStream();
    		if(is != null){
    			log.info("    found zip.");
    			File indexingFolder = new File(baseStanbolHome, "ontonethub-indexing");
    			File dest = new File(indexingFolder, "all");
    			dest.mkdirs();
    			File zipContent = new File(dest, "FULL-AP_IT.solrindex.zip");
    			FileUtils.copyInputStreamToFile(is, zipContent);
    		}
    		is = bundle.getEntry("indexing/org.apache.stanbol.data.site.FULL-AP_IT-1.0.0.jar").openStream();
    		if(is != null){
    			log.info("    found jar.");
    			File indexingFolder = new File(stanbolHome, "ontonethub-indexing");
    			File dest = new File(indexingFolder, "all");
    			dest.mkdirs();
    			File jarContent = new File(dest, "org.apache.stanbol.data.site.FULL-AP_IT-1.0.0.jar");
    			FileUtils.copyInputStreamToFile(is, jarContent);
    		}
    	}
		
		Enumeration<String> entryPaths = bundle.getEntryPaths("ontologies");
		if(entryPaths != null){
			while(entryPaths.hasMoreElements()){
				String ontologyEntryPath = entryPaths.nextElement();
				if(ontologyEntryPath.toLowerCase().endsWith(".conf")){
					InputStream is = bundle.getEntry(ontologyEntryPath).openStream();
					Properties props = new Properties();
					props.load(is);
					is.close();
					
					String name = props.getProperty("name");
					String description = props.getProperty("description");
					String iri = props.getProperty("iri");
					
					if(graph != null){
					
						try{
							Iterator<Triple> tripleIt = graph.filter(null, new IRI(OntologyDescriptionVocabulary.HAS_ONTOLOGY_IRI), new IRI(iri));
							boolean indexContainsOntology = false;
							if(tripleIt != null && tripleIt.hasNext()) indexContainsOntology = true;
							
							if(!indexContainsOntology){
								log.info("Indexing ontology {}.", name);
								doIndexing(null, name, description, iri);
							}
							else {
								IRI localOntology = null;
								if(tripleIt.hasNext()){
									localOntology = (IRI) tripleIt.next().getSubject();
									
									String localOntologyIri = localOntology.toString().replace("<", "").replace(">", "");
									String ontologyId = localOntologyIri.toString().replace(OntologyDescriptionVocabulary.ONTOLOGY, "");
									File ontologyFile = new File(ontologiesFolder, ontologyId + "." + "rdf");
									log.debug("Local ontology IRI string: {}", ontologyId);
									Model localModel = FileManager.get().loadModel(ontologyFile.getPath());
									Model remoteModel = FileManager.get().loadModel(iri);
									
									
									if(!localModel.isIsomorphicWith(remoteModel)){
										
										log.info("Updating ontology {} as a change occurred remoteley.", ontologyId);
										
										deleteOntologyIndex(ontologyId);
										doIndexing(ontologyId, name, description, iri);
									}
									else log.info("The ontology {} did not change with respect to the remote image {}.", ontologyId, iri);
									
								}
									
								
							}
						} catch(Exception e){
							log.error("Error for ontology " + iri, e);
						}
					}
				}
			}
		}
	}
	
	protected void deactivate(ComponentContext ctx) throws IOException {
		this.ctx = null;
	}
	
	
	private void doIndexing(String ontologyId, String name, String description, String iri){
		Model model = FileManager.get().loadModel(iri);
		
		IndexingJobInput indexingJobInput = null;
		if(ontologyId == null) indexingJobInput = new IndexingJobInput(name, description, iri, model);
		else indexingJobInput = new IndexingJobInput(ontologyId, name, description, iri, model);
		try {
			String jobId = indexOntology(indexingJobInput);
			while(!jobManager.ping(jobId).isDone()){
				Thread.sleep(1000);
			}
			
		} catch (OntologyAlreadyExistingException | InterruptedException e) {
			log.error(e.getMessage(), e);
		}
		
	}
	
	private void doIndexing(String ontologyId, String name, String description, String iri, Model model){
		
		IndexingJobInput indexingJobInput = null;
		if(ontologyId == null) indexingJobInput = new IndexingJobInput(name, description, iri, model);
		else indexingJobInput = new IndexingJobInput(ontologyId, name, description, iri, model);
		try {
			String jobId = indexRDF(indexingJobInput);
			while(!jobManager.ping(jobId).isDone())
				Thread.sleep(1000);
			
		} catch (OntologyAlreadyExistingException | InterruptedException e) {
			log.error(e.getMessage(), e);
		}
		
	}

}
