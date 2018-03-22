package it.cnr.istc.stlab.ontonethub.job;


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

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.commons.io.FileUtils;
import org.apache.stanbol.commons.jobs.api.JobResult;
import org.apache.stanbol.commons.jobs.impl.JobManagerImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.google.protobuf.TextFormat.ParseException;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.vocabulary.RDFS;

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

public class RDFIndexingJob extends AbstractIndexingJob {
	
	public static final String JOB_NS = "http://dati.gov.it/onto/job/";
	
	private String ontologyID, ontologyName, ontologyDescription, baseURI;
	private Model data;
	
	private Logger log = LoggerFactory.getLogger(getClass());
	private BundleContext ctx;
	private String stanbolHome;
	private TcManager tcManager;
	
	private String bundleNamePattern = "org.apache.stanbol.data.site.{$name}-1.0.0.jar";
	private String zippedIndexNamePattern = "{$name}.solrindex.zip";
	private File ontologiesFolder;
	
	public RDFIndexingJob(String ontologyName, String ontologyDescription, String baseURI, Model data, BundleContext ctx, TcManager tcManager, File ontologiesFolder) {
		this.ontologyName = ontologyName;
		this.ontologyDescription = ontologyDescription;
		this.baseURI = baseURI;
		this.data = data;
		this.ctx = ctx;
		this.stanbolHome = ctx.getProperty("stanbol.home");
		this.tcManager = tcManager;
		this.ontologiesFolder = ontologiesFolder;
		
	}
	
	public RDFIndexingJob(String ontologyID, String ontologyName, String ontologyDescription, String baseURI, Model data, BundleContext ctx, TcManager tcManager, File ontologiesFolder) {
		this(ontologyName, ontologyDescription, baseURI, data, ctx, tcManager, ontologiesFolder);
		this.ontologyID = ontologyID;
	}

	@Override
	public JobResult call() throws Exception {
		IndexingJobResult indexingJobResult = null;
		try{
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
			
			log.info("Executing job index for {} is error? {}", ontologyName, error);
			
			
			if(!error){
				String jobId = JobManagerImpl.buildId(this);
				
				File rdfDataFolder = new File(tempFolder, "indexing" + File.separator + "resources" + File.separator + "rdfdata");
				
				String tempFileName = "_" + System.currentTimeMillis() + ".rdf";
				
				
				StmtIterator labelIt = data.listStatements(null, RDFS.label, (RDFNode)null);
				List<Statement> labelStmts = new ArrayList<Statement>();
				List<Statement> removingStmts = new ArrayList<Statement>();
				labelIt.forEachRemaining(stmt -> {
					Resource subj = stmt.getSubject();
					Property pred = stmt.getPredicate();
					RDFNode obj = stmt.getObject();
					if(obj.isLiteral()){
						Literal objLit = (Literal) obj;
						String lexicalForm = objLit.getLexicalForm();
						String lang = objLit.getLanguage();
						
						String lemmatizedLexicalForm = lemmatize(lexicalForm);
						Literal lemmatizedLiteral = null;
						if(lang != null) lemmatizedLiteral = ResourceFactory.createLangLiteral(lemmatizedLexicalForm, lang);
						else lemmatizedLiteral = ResourceFactory.createPlainLiteral(lemmatizedLexicalForm);
						
						log.debug("Wordnet indexing term {}", lemmatizedLexicalForm);
						labelStmts.add(new StatementImpl(subj, pred, lemmatizedLiteral));
						removingStmts.add(stmt);
					}
				});
				
				data.remove(removingStmts);
				data.add(labelStmts);
				
				data.write(new FileOutputStream(new File(rdfDataFolder, tempFileName)), "RDF/XML");
				
				Process indexingProcess = Runtime.getRuntime().exec("java -jar "  + stanbolHome + File.separator + OntoNetHubImpl.RUNNABLE_INDEXER_EXECUTABLES + " index " + tempFolder.getPath());
				indexingProcess.waitFor();
				
				
				String bundleFileName = bundleNamePattern.replace("{$name}", ontologyName);
				File bundleFile = new File(tempFolder, "indexing" + File.separator + "dist" + File.separator +  bundleFileName);
				
				String zippedIndexFileName = zippedIndexNamePattern.replace("{$name}", ontologyName);
				File zippedIndexFile = new File(tempFolder, "indexing" + File.separator + "dist" + File.separator +  zippedIndexFileName);
				
				log.info("bundleFile {} AND zippedIndexFile {}", bundleFile.exists(), zippedIndexFile.exists());
				if(bundleFile.exists() && zippedIndexFile.exists()){
					
					File stanbolDatafiles = new File(stanbolHome + File.separator + "datafiles");
					File deployedIndex = new File(stanbolDatafiles, zippedIndexFileName);
					Files.copy(zippedIndexFile, deployedIndex);
					
					try{
						log.info("Bundle URI: {} - URL: {}", bundleFile.toURI(), bundleFile.toURI().toURL());
						Bundle bundle = ctx.installBundle(bundleFile.toURI().toString());
						bundle.start();
						
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
		} catch(Exception e){
			log.error(e.getMessage(), e);
			throw e;
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
