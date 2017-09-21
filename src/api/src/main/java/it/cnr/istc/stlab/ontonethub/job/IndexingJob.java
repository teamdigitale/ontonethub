package it.cnr.istc.stlab.ontonethub.job;


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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TypedLiteralImpl;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.clerezza.rdf.ontologies.XSD;
import org.apache.commons.io.FileUtils;
import org.apache.stanbol.commons.jobs.api.Job;
import org.apache.stanbol.commons.jobs.api.JobResult;
import org.apache.stanbol.commons.jobs.impl.JobManagerImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.google.protobuf.TextFormat.ParseException;
import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary;
import it.cnr.istc.stlab.ontonethub.impl.OntoNetHubImpl;

/**
 * Implementation of the Stanbol Job interface that allows to execute the indexing of an ontology.
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */

public class IndexingJob implements Job {
	
	public static final String JOB_NS = "http://dati.gov.it/onto/job/";
	
	private String ontologyName, ontologyDescription, baseURI;
	private Model data;
	
	private Logger log = LoggerFactory.getLogger(getClass());
	private BundleContext ctx;
	private String stanbolHome;
	private TcManager tcManager;
	
	private String bundleNamePattern = "org.apache.stanbol.data.site.{$name}-1.0.0.jar";
	private String zippedIndexNamePattern = "{$name}.solrindex.zip";
	private File ontologiesFolder;
	
	public IndexingJob(String ontologyName, String ontologyDescription, String baseURI, Model data, BundleContext ctx, TcManager tcManager, File ontologiesFolder) {
		this.ontologyName = ontologyName;
		this.ontologyDescription = ontologyDescription;
		this.baseURI = baseURI;
		this.data = data;
		this.ctx = ctx;
		this.stanbolHome = ctx.getProperty("stanbol.home");
		this.tcManager = tcManager;
		this.ontologiesFolder = ontologiesFolder;
		
	}

	@Override
	public JobResult call() throws Exception {
		Configuration cfg = new Configuration();
		
		TemplateLoader loader = new ClassTemplateLoader(getClass(), File.separator + "templates");
		cfg.setTemplateLoader(loader);
	    cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        
		Properties props = new Properties();
		props.setProperty("name", ontologyName);
		props.setProperty("description", ontologyDescription);

		boolean error = false;
		String errorMessage = null; 
		Template template;
		File tempFolder = null;
		
		try {
			File folder = new File(stanbolHome + File.separator + OntoNetHubImpl.RUNNABLE_INDEXER_EXECUTABLES_FOLDER);
			String tempFolderName = "_" + System.currentTimeMillis();
			tempFolder = new File(folder, tempFolderName);
			
			Process initProcess = Runtime.getRuntime().exec("java -jar " + stanbolHome + File.separator + OntoNetHubImpl.RUNNABLE_INDEXER_EXECUTABLES + " init " + tempFolder.getPath());
			initProcess.waitFor();
			
			template = cfg.getTemplate("indexing.ftl");
			File configFolder = new File(tempFolder, "indexing" + File.separator + "config");
			Writer writer = new FileWriter(new File(configFolder, "indexing.properties"));
			template.process(props, writer);
			writer.close();
			
			template = cfg.getTemplate("mappings.ftl");
			writer = new FileWriter(new File(configFolder, "mappings.txt"));
			template.process(props, writer);
			writer.close();
			
			
			template = cfg.getTemplate("namespaceprefix.ftl");
			writer = new FileWriter(new File(configFolder, "namespaceprefix.mappings"));
			template.process(props, writer);
			writer.close();
			
			
		} catch (ParseException e) {
			log.error(e.getMessage(), e);
			errorMessage = "Indexing failed because of the following error: " + e.getMessage();
			error = true;
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			errorMessage = "Indexing failed because of the following error: " + e.getMessage();
			error = true;
		} catch (TemplateException e) {
			log.error(e.getMessage(), e);
			errorMessage = "Indexing failed because of the following error: " + e.getMessage();
			error = true;
		}
		
		IndexingJobResult indexingJobResult = null;
		if(!error){
			String jobId = JobManagerImpl.buildId(this);
			
			File rdfDataFolder = new File(tempFolder, "indexing" + File.separator + "resources" + File.separator + "rdfdata");
			
			String tempFileName = "_" + System.currentTimeMillis() + ".rdf";
			
			OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
			ontModel.add(data);
			List<Statement> statements = new ArrayList<Statement>();
			
			ExtendedIterator<Ontology> ontologyIt = ontModel.listOntologies();
			Ontology ontology = null;
			if(ontologyIt.hasNext()) ontology = ontologyIt.next();
			
			int classCounter = 0;
			int propCounter = 0;
			ExtendedIterator<OntClass> classesIt = ontModel.listClasses();
			while(classesIt.hasNext()){
				OntClass ontClass = classesIt.next();
				String localName = ontClass.getLocalName();
				String dafLabel = ontologyName + "." + localName;
				classCounter++;
				
				String dafId = jobId + ".class_" + classCounter;
				Statement stmt = new StatementImpl(
						ontClass, 
						ResourceFactory.createProperty(OntologyDescriptionVocabulary.DAF_LABEL),
						ResourceFactory.createPlainLiteral(dafLabel));
				statements.add(stmt);
				
				stmt = new StatementImpl(
						ontClass, 
						ResourceFactory.createProperty(OntologyDescriptionVocabulary.DAF_ID),
						ResourceFactory.createPlainLiteral(dafId));
				statements.add(stmt);
				
				stmt = new StatementImpl(
						ontClass, 
						ResourceFactory.createProperty(OntologyDescriptionVocabulary.DEFINED_IN_ONTOLOGY),
						ontology);
				statements.add(stmt);
				
				statements.addAll(createStatements(com.hp.hpl.jena.vocabulary.RDFS.label, OntologyDescriptionVocabulary.ontologyLabel, ontClass, ontology));
				statements.addAll(createStatements(com.hp.hpl.jena.vocabulary.RDFS.comment, OntologyDescriptionVocabulary.ontologyComment, ontClass, ontology));
				
			}
			
			/*
			 * Add statements to the model and empty the list of statements.
			 */
			ontModel.add(statements);
			statements.removeAll(statements);
			
			
			ExtendedIterator<OntProperty> ontPropertyIt = ontModel.listAllOntProperties();
			while(ontPropertyIt.hasNext()){
				OntProperty ontProperty = ontPropertyIt.next();
				String localName = ontProperty.getLocalName();
				
				propCounter++;
				OntResource domain = ontProperty.getDomain();
				if(domain == null)
					domain = ModelFactory.createOntologyModel().createOntResource(OWL.Thing.getURI());
				
				String classLabel = domain.getLocalName();
				if(classLabel == null) classLabel = "Thing";
				
				String dafLabel = ontologyName + "." + classLabel + "." + localName;
				String dafId = jobId + ".property_" + propCounter;
				
				Statement stmt = new StatementImpl(
						ontProperty, 
						ResourceFactory.createProperty(OntologyDescriptionVocabulary.DAF_LABEL),
						ResourceFactory.createPlainLiteral(dafLabel));
				statements.add(stmt);
				
				stmt = new StatementImpl(
						ontProperty, 
						ResourceFactory.createProperty(OntologyDescriptionVocabulary.DAF_ID),
						ResourceFactory.createPlainLiteral(dafId));
				statements.add(stmt);
				
				stmt = new StatementImpl(
						ontProperty, 
						ResourceFactory.createProperty(OntologyDescriptionVocabulary.DEFINED_IN_ONTOLOGY),
						ontology);
				statements.add(stmt);
				
				statements.addAll(createStatements(com.hp.hpl.jena.vocabulary.RDFS.label, OntologyDescriptionVocabulary.ontologyLabel, ontProperty, ontology));
				statements.addAll(createStatements(com.hp.hpl.jena.vocabulary.RDFS.comment, OntologyDescriptionVocabulary.ontologyComment, ontProperty, ontology));
				
				statements.addAll(createStatements(com.hp.hpl.jena.vocabulary.RDFS.label, OntologyDescriptionVocabulary.domainClassLabel, ontProperty, domain));
				statements.addAll(createStatements(com.hp.hpl.jena.vocabulary.RDFS.comment, OntologyDescriptionVocabulary.domainClassComment, ontProperty, domain));
				
			}
			
			ontModel.add(statements);
			statements.removeAll(statements);
			
			keepMaxLengthAnnotationsOnly(com.hp.hpl.jena.vocabulary.RDFS.label, ontModel);
			keepMaxLengthAnnotationsOnly(com.hp.hpl.jena.vocabulary.RDFS.comment, ontModel);
			
			ontModel.write(new FileOutputStream(new File(rdfDataFolder, tempFileName)), "RDF/XML");
			
			Process indexingProcess = Runtime.getRuntime().exec("java -jar "  + stanbolHome + File.separator + OntoNetHubImpl.RUNNABLE_INDEXER_EXECUTABLES + " index " + tempFolder.getPath());
			indexingProcess.waitFor();
			
			String bundleFileName = bundleNamePattern.replace("{$name}", ontologyName);
			File bundleFile = new File(tempFolder, "indexing" + File.separator + "dist" + File.separator +  bundleFileName);
			
			String zippedIndexFileName = zippedIndexNamePattern.replace("{$name}", ontologyName);
			File zippedIndexFile = new File(tempFolder, "indexing" + File.separator + "dist" + File.separator +  zippedIndexFileName);
			
			log.info("bundleFile {}", bundleFile.getPath());
			if(bundleFile.exists() && zippedIndexFile.exists()){
				
				File stanbolDatafiles = new File(stanbolHome + File.separator + "datafiles");
				File deployedIndex = new File(stanbolDatafiles, zippedIndexFileName);
				Files.copy(zippedIndexFile, deployedIndex);
				
				try{
					log.info("Bundle URI: {} - URL: {}", bundleFile.toURI(), bundleFile.toURI().toURL());
					Bundle bundle = ctx.installBundle(bundleFile.toURI().toString());
					bundle.start();
					long bundleId = bundle.getBundleId();
					
					IRI jobIRI = new IRI(ONTOLOGY + jobId);
					
					Graph g = tcManager.getMGraph(new IRI("ontonethub-graph"));
					g.add(new TripleImpl(
							jobIRI,
							new IRI(HAS_BUNDLE),
							new PlainLiteralImpl(String.valueOf(bundleId))));
					
					
					/*
					 * Ontology name
					 */
					g.add(new TripleImpl(
							jobIRI,
							RDFS.label,
							new PlainLiteralImpl(ontologyName)));
					
					/*
					 * Ontology description
					 */
					g.add(new TripleImpl(
							jobIRI,
							DC.description,
							new PlainLiteralImpl(ontologyDescription)));
					
					/*
					 * Store ontology file
					 */
					File ontologyFile = new File(ontologiesFolder, jobId + "."
							+ "rdf");
					ontModel.write(new FileOutputStream(ontologyFile));
					
					
					
					/*
					 * Ontology IRI
					 */
					g.add(new TripleImpl(
							jobIRI,
							new IRI(HAS_ONTOLOGY_IRI),
							new IRI(baseURI)));
					
					g.add(new TripleImpl(
							jobIRI,
							new IRI(OWL_CLASSES),
							new TypedLiteralImpl(String.valueOf(classCounter), XSD.int_)));
					
					/*
					 * Object properties
					 */
					int objectProperties = 0;
					ExtendedIterator<ObjectProperty> objPropertiesIt = ontModel.listObjectProperties();
					while(objPropertiesIt.hasNext()) {
						objPropertiesIt.next();
						objectProperties++;
					}
					
					g.add(new TripleImpl(
							jobIRI,
							new IRI(OBJECT_PROPERTIES),
							new TypedLiteralImpl(String.valueOf(objectProperties), XSD.int_)));
					
					/*
					 * Datatype properties
					 */
					int dataProperties = 0;
					ExtendedIterator<DatatypeProperty> dataPropertiesIt = ontModel.listDatatypeProperties();
					while(dataPropertiesIt.hasNext()) {
						dataPropertiesIt.next();
						dataProperties++;
					}
					
					g.add(new TripleImpl(
							jobIRI,
							new IRI(DATATYPE_PROPERTIES),
							new TypedLiteralImpl(String.valueOf(dataProperties), XSD.int_)));
					
					
					/*
					 * Annotation properties
					 */
					int annotationProperties = 0;
					ExtendedIterator<AnnotationProperty> annotationPropertiesIt = ontModel.listAnnotationProperties();
					while(annotationPropertiesIt.hasNext()) {
						annotationPropertiesIt.next();
						annotationProperties++;
					}
					
					g.add(new TripleImpl(
							jobIRI,
							new IRI(ANNOTATION_PROPERTIES),
							new TypedLiteralImpl(String.valueOf(annotationProperties), XSD.int_)));
					
					
					/*
					 * OWL individuals
					 */
					int individuals = 0;
					ExtendedIterator<Individual> individualsIt = ontModel.listIndividuals();
					while(individualsIt.hasNext()) {
						individualsIt.next();
						individuals++;
					}
					
					g.add(new TripleImpl(
							jobIRI,
							new IRI(INDIVIDUALS),
							new TypedLiteralImpl(String.valueOf(individuals), XSD.int_)));
					
					
					/*
					 * Closure of imported ontologies
					 */
					Set<String> importedOntologies = ontModel.listImportedOntologyURIs(true);
					g.add(new TripleImpl(
							jobIRI,
							new IRI(IMPORTED_ONTOLOGIES),
							new TypedLiteralImpl(String.valueOf(importedOntologies.size()), XSD.int_)));
					
					
				} catch(Exception e){
					log.error(e.getMessage(), e);
				}
				
				
				
				
				
			}
			
			String message = "Indexing of " + ontologyName + " completed.";
			indexingJobResult = new IndexingJobResult(message, true);
		}
		else indexingJobResult = new IndexingJobResult(errorMessage, false);
		
		if(tempFolder != null && tempFolder.exists()){
			FileUtils.deleteDirectory(tempFolder);
		}
		
		return indexingJobResult;
	}
	
	private List<Statement> createStatements(Property property, Property targetProperty, OntResource subjResource, OntResource resource){
		
		List<Statement> stmts = new ArrayList<Statement>();
		
		Map<String, String> literalMap = new HashMap<String, String>();
		NodeIterator nodeIt = resource.listPropertyValues(property);
		nodeIt.forEachRemaining(node -> {
			if(node.isLiteral()){
				Literal lit = node.asLiteral();
				String lang = lit.getLanguage();
				if(lang == null){
					lang = "null";
				}
				String value = literalMap.get(lang);
				if(value == null) value = "";
				
				String lexicalForm = lit.getLexicalForm();
				if(lexicalForm.length() > value.length())
					literalMap.put(lang, lexicalForm);
			}
		});
		
		literalMap.forEach((lang, sb) -> {
			Statement stmt = null;
			if(lang.equals("null"))
				stmt = new StatementImpl(
						subjResource,
						targetProperty,
						ResourceFactory.createPlainLiteral(sb));
			else stmt = new StatementImpl(
					subjResource,
					targetProperty,
					ResourceFactory.createLangLiteral(sb, lang));
			
			stmts.add(stmt);
		});
		
		return stmts;
		
	}
	
	private List<Statement> collapseAnnotations(Property property, Model model){
		
		List<Statement> stmts = new ArrayList<Statement>();
		
		Map<String, Map<String,Statement>> literalMap = new HashMap<String, Map<String,Statement>>();
		StmtIterator stmtIt = model.listStatements(null, property, (RDFNode) null);
		stmtIt.forEachRemaining(stmt -> {
			RDFNode obj = stmt.getObject();
			if(obj.isLiteral()){
				Literal lit = obj.asLiteral();
				String lang = lit.getLanguage();
				if(lang == null){
					lang = "null";
				}
				Map<String, Statement> stmtMap = literalMap.get(lang);
				if(stmtMap == null) {
					stmtMap = new HashMap<String, Statement>();
					literalMap.put(lang, stmtMap);
				}
				
				Statement statement = stmtMap.get(lang);
				boolean insert = false;
				if(statement == null) insert = true;
				else{
					String statementLF = ((Literal)statement.getObject()).getLexicalForm();
					String lexicalForm = lit.getLexicalForm();
					if(lexicalForm.length() > statementLF.length()) insert = true;
				}
					
					
				if(insert) stmtMap.put(lang, statement);
			}
			
		});
		
		model.removeAll(null, property, (RDFNode) null);
		
		literalMap.forEach((subject, stmtMap) -> {
			stmtMap.forEach((lang, stmt) -> {
				model.add(stmt);
			});
		});
		
		return stmts;
		
	}
	
	private List<Statement> keepMaxLengthAnnotationsOnly(Property property, Model model){
		
		List<Statement> stmts = new ArrayList<Statement>();
		
		Map<Resource, Map<String,Statement>> literalMap = new HashMap<Resource, Map<String,Statement>>();
		StmtIterator stmtIt = model.listStatements(null, property, (RDFNode) null);
		stmtIt.forEachRemaining(stmt -> {
			Resource subj = stmt.getSubject();
			RDFNode obj = stmt.getObject();
			if(obj.isLiteral()){
				Literal lit = obj.asLiteral();
				String lang = lit.getLanguage();
				if(lang == null){
					lang = "null";
				}
				Map<String, Statement> stmtMap = literalMap.get(subj);
				if(stmtMap == null) {
					stmtMap = new HashMap<String, Statement>();
					literalMap.put(subj, stmtMap);
				}
				
				Statement statement = stmtMap.get(lang);
				boolean insert = false;
				if(statement == null) insert = true;
				else{
					String statementLF = ((Literal)statement.getObject()).getLexicalForm();
					String lexicalForm = lit.getLexicalForm();
					if(lexicalForm.length() > statementLF.length()) insert = true;
				}
					
					
				if(insert) stmtMap.put(lang, stmt);
			}
			
		});
		
		model.removeAll(null, property, (RDFNode) null);
		
		literalMap.forEach((subject, stmtMap) -> {
			stmtMap.forEach((lang, stmt) -> {
				model.add(stmt);
			});
		});
		
		return stmts;
		
	}
	
	

	@Override
	public String buildResultLocation(String jobId) {
		
		return "ontonethub/ontology/" + jobId;
	}

}
