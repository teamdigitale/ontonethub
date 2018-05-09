package it.cnr.istc.stlab.ontonethub.job;

import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.domainUsage;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.rangeUsage;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.synonym;
import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.usage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.commons.io.FileUtils;
import org.apache.stanbol.commons.jobs.api.JobResult;
import org.apache.stanbol.commons.jobs.impl.JobManagerImpl;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.SimilarityConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat.ParseException;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.InfModel;
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
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDFS;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import it.cnr.istc.stlab.ontonethub.Constants;
import it.cnr.istc.stlab.ontonethub.SPARQLQueryManager;
import it.cnr.istc.stlab.ontonethub.impl.OntoNetHubImpl;
import it.cnr.istc.stlab.ontonethub.solr.OntoNetHubSiteManager;

/**
 * Implementation of the Stanbol Job interface that allows to execute the indexing of an ontology.
 * 
 * @author Andrea Nuzzolese (https://github.com/anuzzolese)
 *
 */

public class IndexingJob extends AbstractIndexingJob {
	
	private String ontologyID, ontologyName, ontologyDescription, baseURI;
	private Model data;
	
	private Logger log = LoggerFactory.getLogger(getClass());
	private BundleContext ctx;
	private String stanbolHome;
	private TcManager tcManager;
	private OntoNetHubSiteManager siteManager;
	private SPARQLQueryManager sparqlQueryManager;
	
	public static final String BUNDLE_NAME_PATTERN = "org.apache.stanbol.data.site.{$name}-1.0.0.jar";
	public static final String ZIPPED_INDEX_NAME_PATTERN = "{$name}.solrindex.zip";
	private File ontologiesFolder;
	
	private boolean useIndexDump;
	
	public IndexingJob(boolean useIndexDump, SPARQLQueryManager sparqlQueryManager, OntoNetHubSiteManager siteManager, String ontologyName, String ontologyDescription, String baseURI, Model data, BundleContext ctx, TcManager tcManager, File ontologiesFolder) {
		this.useIndexDump = useIndexDump;
		this.sparqlQueryManager = sparqlQueryManager;
		this.siteManager = siteManager;
		this.ontologyName = ontologyName;
		this.ontologyDescription = ontologyDescription;
		this.baseURI = baseURI;
		this.data = data;
		this.ctx = ctx;
		this.stanbolHome = ctx.getProperty("stanbol.home");
		this.tcManager = tcManager;
		this.ontologiesFolder = ontologiesFolder;
		
	}
	
	public IndexingJob(boolean useIndexDump, SPARQLQueryManager sparqlQueryManager, OntoNetHubSiteManager siteManager, String ontologyID, String ontologyName, String ontologyDescription, String baseURI, Model data, BundleContext ctx, TcManager tcManager, File ontologiesFolder) {
		this(useIndexDump, sparqlQueryManager, siteManager, ontologyName, ontologyDescription, baseURI, data, ctx, tcManager, ontologiesFolder);
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
				
				if(useIndexDump){
					File indexingFolder = new File(tempFolder, "indexing");
					File distFolder = new File(indexingFolder, "dist");
					
					File srcZip = new File(stanbolHome + File.separator + "ontonethub-indexing" + File.separator + "all" + File.separator + "FULL-AP_IT.solrindex.zip");
					File destZip = new File(distFolder, "FULL-AP_IT.solrindex.zip"); 
					FileUtils.copyFile(srcZip, destZip);
					
					File srcJar = new File(stanbolHome + File.separator + "ontonethub-indexing" + File.separator + "all" + File.separator + "org.apache.stanbol.data.site.FULL-AP_IT-1.0.0.jar");
					File destJar = new File(distFolder, "org.apache.stanbol.data.site.FULL-AP_IT-1.0.0.jar"); 
					FileUtils.copyFile(srcJar, destJar);
				}
				
				
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
			
			
			log.debug("Start indexing? {}", !error);
			if(!error){
				String jobId = JobManagerImpl.buildId(this);
				IndexDeploymentManager indexDeploymentManager = new IndexDeploymentManager(baseURI, tcManager, ctx);
				if(useIndexDump){
					indexDeploymentManager.deploy(ontologyName, ontologyID, ontologyDescription, data, ontologiesFolder, tempFolder);
				}
				else{
				
					File rdfDataFolder = new File(tempFolder, "indexing" + File.separator + "resources" + File.separator + "rdfdata");
					
					String tempFileName = "_" + System.currentTimeMillis() + ".rdf";
					
					OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
					log.info("Base URI is {}", baseURI);
					ontModel.read(baseURI);
					log.info("Read ontology {}", baseURI);
					
					/*
					keepMaxLengthAnnotationsOnly(com.hp.hpl.jena.vocabulary.RDFS.label, ontModel);
					log.info("Max lenght labels");
					keepMaxLengthAnnotationsOnly(com.hp.hpl.jena.vocabulary.RDFS.comment, ontModel);
					log.info("Max lenght comments");
					*/
					
					/*
					 * Add synonyms gathered from WordNet.
					 */
					log.info("Adding synonyms");
					ontModel.add(getSynonyms(RDFS.label, ontModel));
					/*
					 * Add synonyms from super classes.
					 */
					log.info("Adding synonyms from subclasses");
					ontModel.add(expandSubClasses(ontModel));
					/*
					 * Add synonyms from super classes.
					 */
					log.info("Adding synonyms from subproperties");
					ontModel.add(expandSubProperties(ontModel));
					
					/*File ontologyFileName = new File(ontologiesFolder, ontologyName + "." + "rdf");
					ontModel.write(new FileOutputStream(ontologyFileName), "RDF/XML");*/
					
					try{
						log.info("Running indexing subprocess {}", ontologyName);
						
						OntologyContextFactory ontologyContextFactory = new OntologyContextFactory();
						Map<String, Set<String>> propContextMap = ontologyContextFactory.getContexts(ontModel);
						IndexingModelFactory indexingModelFactory = new IndexingModelFactory(propContextMap);
						
						log.info("§§§§§ {} has a context map for {} properties", ontologyName, propContextMap.size());
						
						Model indexingModel = ModelFactory.createDefaultModel();
						
						ResultSet resultSet = sparqlQueryManager.executeSelect(ontModel);
						while(resultSet.hasNext()){
							QuerySolution querySolution = resultSet.next();
							Resource domain = ontModel.getResource(querySolution.getResource("domain").getURI());
							Property property = ontModel.getProperty(querySolution.getResource("property").getURI());
							Resource range = ontModel.getResource(querySolution.getResource("range").getURI());
							
							indexingModel.add(indexingModelFactory.create(jobId, ontologyName, domain, property, range));
							
							
							List<Statement> stmts = getDomainUsage(ModelFactory.createOntologyModel().createClass(domain.getURI()), ontModel);
							
							String domainAsRangeLocalname = domain.getLocalName();
							for(Statement stmt : stmts){
								Resource subj = stmt.getSubject();
								Property pred = stmt.getPredicate();
								
								String domainLocalname = subj.getLocalName();
								String propertyLocalname = pred.getLocalName(); 
								String contextIdHex = null; 
								try {
									contextIdHex = (new HexBinaryAdapter()).marshal(MessageDigest.getInstance("MD5").digest((domainLocalname + "." + propertyLocalname + "." + domainAsRangeLocalname).getBytes()));
								} catch (NoSuchAlgorithmException e) {
									contextIdHex = domainLocalname + "." + propertyLocalname + "." + domainAsRangeLocalname;
								}
							}
							indexingModel.add(stmts);
							stmts = getRangeUsage(ModelFactory.createOntologyModel().createClass(range.getURI()), ontModel);
							indexingModel.add(stmts);
							
							
						}
						
						
							
						indexingModel.write(new FileOutputStream(new File(rdfDataFolder, tempFileName)), "RDF/XML");
					}catch(Exception e){
						log.error(e.getMessage(), e);
					}
					
					
					
					Process indexingProcess = Runtime.getRuntime().exec("java -jar "  + stanbolHome + File.separator + OntoNetHubImpl.RUNNABLE_INDEXER_EXECUTABLES + " index " + tempFolder.getPath());
					indexingProcess.waitFor();
					
					indexDeploymentManager.deploy(ontologyName, ontologyID, ontologyDescription, data, ontologiesFolder, tempFolder);
				}
				
				String message = "Indexing of " + ontologyName + " completed.";
				indexingJobResult = new IndexingJobResult(message, true);
				
				log.debug("Indexing job completed!!!");
				
			}
			else indexingJobResult = new IndexingJobResult(errorMessage, false);
			
			if(tempFolder != null && tempFolder.exists()){
				//FileUtils.deleteDirectory(tempFolder);
			}
		} catch(Exception e){
			log.error(e.getMessage(), e);
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
	
	private List<Statement> getSynonyms(Property property, Model model){
		List<Statement> stmts = new ArrayList<Statement>();
		try{
			
			Site wnSite = siteManager.getSite(Constants.wordNetSiteID);
			
			FieldQuery fieldQuery = DefaultQueryFactory.getInstance().createFieldQuery();
			fieldQuery.addSelectedField(property.getURI());
			fieldQuery.setOffset(0);
			fieldQuery.setLimit(3);
			
			StmtIterator labelIt = model.listStatements(null, property, (RDFNode)null);
			labelIt.forEachRemaining(stmt -> {
				
				Resource subject = stmt.getSubject();
				RDFNode object = stmt.getObject();
				if(object.isLiteral()){
					String label = ((Literal)object).getLexicalForm();
					String lang = ((Literal)object).getLanguage();
					if(lang == null || lang.equals("it")){
						label = lemmatize(label);
						Constraint similarityConstraint = new SimilarityConstraint(label, null);
						fieldQuery.setConstraint(property.getURI(), similarityConstraint);
						
						/* Add label as synonym.
						 * But first we lemmatise the term in order to increase the recall.
						 */
						
						Literal objLiteral = null;
						if(lang != null) objLiteral = ResourceFactory.createLangLiteral(label, lang);
						else objLiteral = ResourceFactory.createPlainLiteral(label);
						
						stmts.add(new StatementImpl(subject, synonym, objLiteral));
						
						final String lab = label;
						
						QueryResultList<Representation> result = wnSite.find(fieldQuery);
						
						result.forEach(representation -> {
							
							float[] score = new float[]{0};
							Iterator<Object> objIt = representation.get(Constants.entityHubScore);
							if(objIt != null){
								objIt.forEachRemaining(obj -> {
									score[0] = (float) obj;
								});
							}
							
							if(score[0] > Constants.wordnetSynonymityConfidence){
								objIt = representation.get(property.getURI());
								if(objIt != null){
									objIt.forEachRemaining(obj -> {
										Text text = (Text) obj;
										String value = text.getText();
										String language = text.getLanguage();
										
										// Add labels of synomyms
										log.debug("Syn {} ({}) - {} : {}", subject, lab, value, score);
										Literal synLabel = ResourceFactory.createLangLiteral(lemmatize(value), language);
										stmts.add(new StatementImpl(subject, synonym, synLabel));
									});
								}
							}
							
						});
						
					}
					
				}
				
			});
		} catch(Exception e){
			log.error(e.getMessage(), e);
		}
		
		return stmts;
	}
	
	private List<Statement> expandSubClasses(Model model){
		List<Statement> stmts = new ArrayList<Statement>();
		
		
		String sparql = "PREFIX rdfs: <" + RDFS.getURI() + ">"
				+ "SELECT DISTINCT ?class ?synonym "
				+ "WHERE { "
				+ "?class rdfs:subClassOf+ ?subClass . "
				+ "?subClass <" + synonym + "> ?synonym"
				+ "}";
		
		Query query = QueryFactory.create(sparql, Syntax.syntaxARQ);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		ResultSet resultSet = queryExecution.execSelect();
		resultSet.forEachRemaining(querySolution -> {
			stmts.add(new StatementImpl(querySolution.getResource("class"), synonym, querySolution.getLiteral("synonym")));
		});
		return stmts;
	}
	
	private List<Statement> expandSubProperties(Model model){
		List<Statement> stmts = new ArrayList<Statement>();
		
		String sparql = "PREFIX rdfs: <" + RDFS.getURI() + ">"
				+ "SELECT DISTINCT ?property ?synonym "
				+ "WHERE { "
				+ "?property rdfs:subPropertyOf+ ?subProperty . "
				+ "?subProperty <" + synonym + "> ?synonym"
				+ "}";
		
		Query query = QueryFactory.create(sparql, Syntax.syntaxARQ);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		ResultSet resultSet = queryExecution.execSelect();
		resultSet.forEachRemaining(querySolution -> {
			stmts.add(new StatementImpl(querySolution.getResource("property"), synonym, querySolution.getLiteral("synonym")));
		});
		return stmts;
	}
	

	@Override
	public String buildResultLocation(String jobId) {
		
		return "ontonethub/ontology/" + jobId;
	}
	
	private List<Statement> getUsage(Property property, Model model){
		
		List<Statement> stmts = new ArrayList<Statement>();
		String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ "SELECT DISTINCT ?concept "
				+ "WHERE{"
				+ "  {<" + property.getURI() + "> rdfs:domain ?concept} "
				+ "  UNION "
				+ "  { "
				+ "    ?concept rdfs:subClassOf|owl:equivalentClass ?restriction . "
				+ "    ?restriction a owl:Restriction; "
				+ "      owl:onProperty <" + property.getURI() + "> "
				+ "  } "
				+ "}";
		Query query = QueryFactory.create(sparql, Syntax.syntaxARQ);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		
		ResultSet resultSet = queryExecution.execSelect();
		while(resultSet.hasNext()){
			QuerySolution querySolution = resultSet.next();
			Resource concept = querySolution.getResource("concept");
			
			stmts.add(new StatementImpl(property, usage, concept));
		}
		
		return stmts;
		
	}
	
	private List<Statement> getDomainUsage(OntClass ontClass, Model model){
		List<Statement> stmts = new ArrayList<Statement>();
		try{
			String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
					+ "SELECT DISTINCT ?concept ?prop "
					+ "WHERE{"
					+ "  {?prop rdfs:range <" + ontClass.getURI() + ">; "
					+ "    rdfs:domain ?concept"
					+ "  }"
					+ "  UNION "
					+ "  { "
					+ "    ?concept rdfs:subClassOf|owl:equivalentClass ?restriction . "
					+ "    ?restriction a owl:Restriction; "
					+ "      ?restr <" + ontClass.getURI() + ">;"
					+ "      owl:onProperty ?prop"
					+ "  } "
					+ "  FILTER(isIRI(?concept)) "
					+ "}";
					
			Query query = QueryFactory.create(sparql, Syntax.syntaxARQ);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
			
			ResultSet resultSet = queryExecution.execSelect();
			while(resultSet.hasNext()){
				QuerySolution querySolution = resultSet.next();
				Resource concept = querySolution.getResource("concept");
				
				stmts.add(new StatementImpl(ontClass, domainUsage, concept));
			}
		} catch(Exception e){
			log.error(e.getMessage(), e);
		}
		
		return stmts;
		
	}
	
	private List<Statement> getRangeUsage(OntClass ontClass, Model model){
		List<Statement> stmts = new ArrayList<Statement>();
		try{
			String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
					+ "SELECT DISTINCT ?concept ?prop "
					+ "WHERE{"
					+ "  {?prop rdfs:range ?concept; "
					+ "    rdfs:domain <" + ontClass.getURI() + ">"
					+ "  }"
					+ "  UNION "
					+ "  { "
					+ "    <" + ontClass.getURI() + "> rdfs:subClassOf|owl:equivalentClass ?restriction . "
					+ "    ?restriction a owl:Restriction; "
					+ "      ?restr ?concept;"
					+ " 	 owl:onProperty ?prop"
					+ "  } "
					+ "  FILTER(isIRI(?concept)) "
					+ "}";
			Query query = QueryFactory.create(sparql, Syntax.syntaxARQ);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
			
			ResultSet resultSet = queryExecution.execSelect();
			while(resultSet.hasNext()){
				QuerySolution querySolution = resultSet.next();
				Resource concept = querySolution.getResource("concept");
				
				stmts.add(new StatementImpl(ontClass, rangeUsage, concept));
			}
		} catch(Exception e){
			log.error(e.getMessage(), e);
		}
		
		return stmts;
		
	}
	
	public static void main(String[] args) {
		OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		ontModel.read("file:///Users/andrea/git/daf-ontologie-vocabolari-controllati/Ontologie/CPV/0.5/CPV-AP_IT.rdf");
		
		
		String rules = "[r1: (?p2 rdfs:range ?c) <- (?p1 rdfs:subPropertyOf ?p2) (?p1 rdfs:range ?c)] "
				+ "[r2: (?restriction owl:onProperty ?p2) <- (?p1 rdfs:subPropertyOf ?p2) (?restriction owl:onProperty ?p1)] ";
		Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(rules));
		InfModel infModel = ModelFactory.createInfModel(reasoner, ontModel);
		
		infModel.prepare();
		
		
		
		OntClass ontClass = ontModel.getOntClass("https://w3id.org/italia/onto/CLV/City");
		List<Resource> resources = new ArrayList<Resource>();
		resources.add(ontClass);
		ontClass.listSuperClasses().forEachRemaining(ontC -> {
			if(ontC.isURIResource()) 
				resources.add(ontC);}
		);
		
		Set<Resource> visited = new HashSet<Resource>();
		while(!resources.isEmpty()){
			Resource c = resources.remove(0);
			if(!c.equals(OWL2.Thing) && !visited.contains(c)){
				System.out.println(c);	
				visited.add(c);
				resources.addAll(query(c, infModel, ontModel));
			}
			
		}
		
		
		
	}
	
	private static List<Resource> query(Resource concept, Model model, OntModel ontModel){
		String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "PREFIX owl: <http://www.w3.org/2002/07/owl#> "
				+ "SELECT DISTINCT ?concept ?prop "
				+ "WHERE{"
				+ "  {?prop rdfs:range <" + concept.getURI() + ">; "
				+ "    rdfs:domain ?concept"
				+ "  }"
				+ "  UNION "
				+ "  { "
				+ "    ?concept rdfs:subClassOf|owl:equivalentClass ?restriction . "
				+ "    ?restriction a owl:Restriction; "
				+ "      ?restr <" + concept.getURI() + ">;"
				+ "      owl:onProperty ?prop"
				+ "  } "
				+ "  FILTER(isIRI(?concept)) "
				+ "}";
		Query query = QueryFactory.create(sparql, Syntax.syntaxARQ);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		
		ResultSet resultSet = queryExecution.execSelect();
		List<Resource> resources = new ArrayList<Resource>();
		while(resultSet.hasNext()){
			QuerySolution querySolution = resultSet.next();
			Resource c = querySolution.getResource("concept");
			
			resources.add(c);
			
			OntClass ontClass = ontModel.getOntClass(c.getURI());
			ontClass.listSuperClasses().forEachRemaining(ontC -> {
				if(ontC.isURIResource()) 
					resources.add(ontC);}
			);
			
		}
		
		return resources;
	}
	

}
