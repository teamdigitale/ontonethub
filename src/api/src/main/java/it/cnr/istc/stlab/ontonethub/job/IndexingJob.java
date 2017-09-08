package it.cnr.istc.stlab.ontonethub.job;


import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
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
			
			File rdfDataFolder = new File(tempFolder, "indexing" + File.separator + "resources" + File.separator + "rdfdata");
			
			String tempFileName = "_" + System.currentTimeMillis() + ".rdf";
			data.write(new FileOutputStream(new File(rdfDataFolder, tempFileName)), "RDF/XML");
			
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
					
					String jobId = JobManagerImpl.buildId(this);
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
					data.write(new FileOutputStream(ontologyFile));
					
					OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
					ontModel.add(data);
					
					/*
					 * Ontology IRI
					 */
					g.add(new TripleImpl(
							jobIRI,
							new IRI(HAS_ONTOLOGY_IRI),
							new IRI(baseURI)));
					
					/*
					 * OWL classes
					 */
					int classes = 0;
					ExtendedIterator<OntClass> classesIt = ontModel.listClasses();
					while(classesIt.hasNext()) {
						classesIt.next();
						classes++;
					}
					
					g.add(new TripleImpl(
							jobIRI,
							new IRI(OWL_CLASSES),
							new TypedLiteralImpl(String.valueOf(classes), XSD.int_)));
					
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
	
	

	@Override
	public String buildResultLocation(String jobId) {
		
		return "ontonethub/ontology/" + jobId;
	}

}
