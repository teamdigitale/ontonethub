package it.cnr.istc.stlab.ontonethub.web.resources;

import static it.cnr.istc.stlab.ontonethub.web.utils.JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES;
import static it.cnr.istc.stlab.ontonethub.web.utils.JerseyUtils.createFieldQueryForFindRequest;
import static it.cnr.istc.stlab.ontonethub.web.utils.LDPathHelper.getLDPathParseExceptionMessage;
import static it.cnr.istc.stlab.ontonethub.web.utils.LDPathHelper.prepareQueryLDPathProgram;
import static it.cnr.istc.stlab.ontonethub.web.utils.LDPathHelper.transformQueryResults;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.getAcceptableMediaType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.jobs.api.JobManager;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.SiteBackend;
import org.apache.stanbol.entityhub.ldpath.query.LDPathSelect;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSite;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;

import it.cnr.istc.stlab.ontonethub.web.IndexingJob;
import it.cnr.istc.stlab.ontonethub.web.IndexingJobInput;
import it.cnr.istc.stlab.ontonethub.web.utils.JerseyUtils;

/**
 * Web resource that deals with the management of the ontologies.
 * This means that this resource provides the framework with HTTP REST capabilities for indexing and storing ontologies
 * that should be part of the ontology network. 
 * 
 * @author Andrea Nuzzolese
 *
 */
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/ontonethub/ontology")
public class OntonethubIndexingResource extends BaseStanbolResource {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	public static final String RUNNABLE_INDEXER_EXECUTABLES_FOLDER = "ontonethub-indexing" + File.separator + "executables";
	public static final String RUNNABLE_INDEXER_EXECUTABLES = RUNNABLE_INDEXER_EXECUTABLES_FOLDER + File.separator + "indexing-genericrdf.jar";
	public static final String INNER_INDEXER_EXECUTABLES = "executables" + File.separator + "indexing-genericrdf.jar";
	
	/**
     * The Field used for find requests if not specified TODO: This will be replaced by the EntitySearch. With
     * this search the Site is responsible to decide what properties to use for label based searches.
     */
    private static final String DEFAULT_FIND_FIELD = RDFS.label.getUnicodeString();
    
    /**
     * The Field used as default as selected fields for find requests TODO: Make configurable via the
     * {@link ConfiguredSite} interface! NOTE: This feature is deactivated, because OPTIONAL selects do have
     * very weak performance when using SPARQL endpoints
     */
    // private static final Collection<String> DEFAULT_FIND_SELECTED_FIELDS =
    // Arrays.asList(RDFS.comment.getUnicodeString());
    
    /**
     * The default number of maximal results.
     */
    private static final int DEFAULT_FIND_RESULT_LIMIT = 5;
	
	@Reference
	private JobManager jobManager;
	
	@Reference
	private SiteManager siteManager;
	
	@Reference
	private TcManager tcManager;
	
	@Reference
    private NamespacePrefixService nsPrefixService;
	
	@Context 
	private UriInfo uriInfo; 
	
	private File ontologiesFolder;
	
	private ComponentContext ctx;
	//private Scope onScope; 
	
	@OPTIONS
    @Path("/{id}")
    public Response handleCorsPreflightOntology(@PathParam(value = "id") String id,
            @Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        return res.build();
    }
	
	@OPTIONS
    @Path("/{id}/source")
    public Response handleCorsPreflightOntologySource(@PathParam(value = "id") String id,
            @Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        return res.build();
    }
	
	@OPTIONS
    @Path("/{id}/find")
    public Response handleCorsPreflightOntologyFind(@PathParam(value = "id") String id,
            @Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        return res.build();
    }
	
	@GET
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.TEXT_PLAIN)
	public Response sayHello(){
		return Response.ok("Hello, it's the OntoNetHub!").build();
	}
	
	@GET
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	public Response getOntologyInfo(@PathParam("id") String id){
		boolean found = true;
		ResponseBuilder responseBuilder = null;
		
		String ontologyURI = uriInfo.getBaseUri() + uriInfo.getPath();
		String vocabNs = uriInfo.getBaseUri() + "ontonethub/vocab/";
		
		Graph g = tcManager.getMGraph(new IRI("ontonethub-graph"));
		Iterator<Triple> tripleIt = g.filter(new IRI(ontologyURI), 
				new IRI(vocabNs + "hasBundle"), 
				null);
		
		String bundleId = null;
		if(tripleIt.hasNext()){
			Literal bundleIdLiteral = (Literal) tripleIt.next().getObject();
			bundleId = bundleIdLiteral.getLexicalForm();
		}
		
		log.info("Bundle ID {} - Ontology ID {}", bundleId, ontologyURI);
		if(bundleId != null){
			Bundle bundle = ctx.getBundleContext().getBundle(Long.valueOf(bundleId));
			if(bundle != null){
				JSONObject json = new JSONObject();
				try {
					json.put("id", id);
					
					String ontologyName = null;
					String ontologyDescription = null;
					String ontologyIRI = null;
					
					
					tripleIt = g.filter(new IRI(ontologyURI), 
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
					json.put("ontologySource", sourceIRI);
					
					json.put("name", ontologyName);
					json.put("description", ontologyDescription);
					
					
					tripleIt = g.filter(new IRI(ontologyURI), 
							new IRI(vocabNs + "hasOntologyIRI"), 
							null);
					if(tripleIt.hasNext()){
						IRI iri = (IRI) tripleIt.next().getObject();
						ontologyIRI = iri.toString().replace("<", "").replace(">", "");
					}
					json.put("ontologyIRI", ontologyIRI);
					
					/*
					 * OWL classes
					 */
					tripleIt = g.filter(new IRI(ontologyURI), 
							new IRI(vocabNs + "owlClasses"), 
							null);
					int owlClasses = 0;
					if(tripleIt.hasNext()){
						String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
						owlClasses = Integer.valueOf(lexicalForm);
					}
					json.put("owlClasses", owlClasses);
					
					/*
					 * Object properties
					 */
					tripleIt = g.filter(new IRI(ontologyURI), 
							new IRI(vocabNs + "objectProperties"), 
							null);
					int objectProperties = 0;
					if(tripleIt.hasNext()){
						String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
						objectProperties = Integer.valueOf(lexicalForm);
					}
					json.put("objectProperties", objectProperties);
					
					/*
					 * Datatype properties
					 */
					tripleIt = g.filter(new IRI(ontologyURI), 
							new IRI(vocabNs + "datatypeProperties"), 
							null);
					int datatypeProperties = 0;
					if(tripleIt.hasNext()){
						String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
						datatypeProperties = Integer.valueOf(lexicalForm);
					}
					json.put("datatypeProperties", datatypeProperties);
					
					/*
					 * Annotation properties
					 */
					tripleIt = g.filter(new IRI(ontologyURI), 
							new IRI(vocabNs + "annotationProperties"), 
							null);
					int annotationProperties = 0;
					if(tripleIt.hasNext()){
						String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
						annotationProperties = Integer.valueOf(lexicalForm);
					}
					json.put("annotationProperties", annotationProperties);
					
					/*
					 * Individuals
					 */
					tripleIt = g.filter(new IRI(ontologyURI), 
							new IRI(vocabNs + "individuals"), 
							null);
					int individuals = 0;
					if(tripleIt.hasNext()){
						String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
						individuals = Integer.valueOf(lexicalForm);
					}
					json.put("individuals", individuals);
					
					/*
					 * Individuals
					 */
					tripleIt = g.filter(new IRI(ontologyURI), 
							new IRI(vocabNs + "importedOntologies"), 
							null);
					int importedOntologies = 0;
					if(tripleIt.hasNext()){
						String lexicalForm = ((Literal)tripleIt.next().getObject()).getLexicalForm();
						importedOntologies = Integer.valueOf(lexicalForm);
					}
					json.put("importedOntologies", importedOntologies);
					
					responseBuilder = Response.ok(json.toString());
				} catch (JSONException e) {
					try {
						json.put("error", e.getMessage());
					} catch (JSONException e1) {
						log.error(e1.getMessage(), e1);
					}
					responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(json.toString());
				} 
			}
			else found = false;
		}
		else found = false;
		
		if(!found) {
			JSONObject json = new JSONObject();
			try {
				json.put("error", "No ontology exists with the ID provided.");
			} catch (JSONException e) {
				log.error(e.getMessage(), e);
			}
			responseBuilder = Response.status(Status.NOT_FOUND).entity(json.toString());
		}
		
		return responseBuilder.build();
	}
	
	@GET
	@Consumes(MediaType.WILDCARD)
	@Produces({
		KRFormat.RDF_XML,
		KRFormat.RDF_JSON,
		KRFormat.TURTLE,
		KRFormat.N_TRIPLE,
		KRFormat.N3,
		"application/json-ld"
		})
	@Path("/{id}/source")
	public Response getOntologySource(@PathParam("id") String id){
		
		ResponseBuilder responseBuilder = null;
		Model model;
		try {
			model = FileManager.get().loadModel(new File(ontologiesFolder, id + ".rdf").getCanonicalPath());
		} catch (IOException e) {
			model = null;
		}
		if(model != null) responseBuilder = Response.ok(model);
		else {
			JSONObject json = new JSONObject();
			try {
				json.put("error", "No ontology exists with the ID provided.");
			} catch (JSONException e) {
				log.error(e.getMessage(), e);
			}
			responseBuilder = Response.status(Status.NOT_FOUND).entity(json);
		}
		
		return responseBuilder.build();
	}
	
	@POST
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public Response indexOntology(IndexingJobInput input){
		
		ResponseBuilder responseBuilder = null;
		JSONObject jsonObject = new JSONObject();
		
		
		Site site = siteManager.getSite(input.getName());
		log.info("Site {} and ontology name {}", site, input.getName());
		if(site == null){
			String ontologyURI = uriInfo.getBaseUri() + uriInfo.getPath();
			log.info(" Ontology IRI {}", ontologyURI);
			String vocabNs = uriInfo.getBaseUri() + "ontonethub/vocab/";
			IndexingJob job = new IndexingJob(input.getName(), input.getDescription(), input.getBaseURI(), input.getData(), ctx.getBundleContext(), tcManager, ontologiesFolder, ontologyURI, vocabNs);
			String jid = jobManager.execute(job);
			
			URI location = URI.create(getPublicBaseUri() + "jobs/" + jid);
			
			try {
				jsonObject.put("monitoringService", location.toString());
				jsonObject.put("ontologyId", jid);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			responseBuilder = Response.ok(jsonObject.toString());
		}
		else{
			responseBuilder = Response.status(Status.CONFLICT);
		}
		
		return responseBuilder.build();
		
	}
	
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	public Response deleteOntologyIndex(@PathParam("id") String id){
		ResponseBuilder responseBuilder = null;
		
		boolean found = true;
		
		String ontologyURI = uriInfo.getBaseUri() + uriInfo.getPath();
		String vocabNs = uriInfo.getBaseUri() + "ontonethub/vocab/";
		
		Graph g = tcManager.getMGraph(new IRI("ontonethub-graph"));
		Iterator<Triple> tripleIt = g.filter(new IRI(ontologyURI), 
				new IRI(vocabNs + "hasBundle"), 
				null);
		
		String bundleId = null;
		Triple tripleToRemove = null;
		if(tripleIt.hasNext()){
			tripleToRemove = tripleIt.next();
			Literal bundleIdLiteral = (Literal) tripleToRemove.getObject();
			bundleId = bundleIdLiteral.getLexicalForm(); 
		}
		
		if(bundleId != null){
		
			Bundle bundle = ctx.getBundleContext().getBundle(Long.valueOf(bundleId));
			if(bundle != null){
				JSONObject json = new JSONObject();
				String symbolicName = bundle.getSymbolicName();
				String siteName = symbolicName.replace("org.apache.stanbol.data.site.", "");
				String path = "org.apache.stanbol.data.site.".replaceAll("\\.", File.separator) + siteName;
				URL resourceURL = bundle.getResource(path + File.separator + "org.apache.stanbol.entityhub.site.referencedSite-" + siteName + ".config");
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
							
							responseBuilder = Response.ok();
						}
						else found = false;
					} catch (IOException e) {
						try {
							json.put("error", e.getMessage());
						} catch (JSONException e1) {
							log.error(e1.getMessage(), e1);
						}
						responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(json.toString());
					} catch (BundleException e) {
						try {
							json.put("error", e.getMessage());
						} catch (JSONException e1) {
							log.error(e1.getMessage(), e1);
						}
						responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(json.toString());
					}
				}
				else found = false;	
			}
			else found = false;
		}
		else found = false;
		
		if(!found) {
			JSONObject json = new JSONObject();
			try {
				json.put("error", "No ontology exists with the ID provided.");
			} catch (JSONException e) {
				log.error(e.getMessage(), e);
			}
			responseBuilder = Response.status(Status.NOT_FOUND).entity(json.toString());
		}
		
		return responseBuilder.build();
	}
	
	@POST
    @Path("/{id}/find")
    public Response findEntity(@PathParam(value = "id") String id,
                               @FormParam(value = "name") String name,
                               @FormParam(value = "field") String parsedField,
                               @FormParam(value = "lang") String language,
                               // @FormParam(value="select") String select,
                               @FormParam(value = "limit") Integer limit,
                               @FormParam(value = "offset") Integer offset,
                               @FormParam(value = "ldpath") String ldpath,
                               @Context HttpHeaders headers) {
		
		
		String ontologyURI = uriInfo.getBaseUri() + uriInfo.getPath().replaceAll("\\/find$", "");
		Graph g = tcManager.getMGraph(new IRI("ontonethub-graph"));
		Iterator<Triple> tripleIt = g.filter(new IRI(ontologyURI), 
				RDFS.label, 
				null);
		String ontologyName = null;
		if(tripleIt.hasNext()){
			ontologyName = ((Literal)tripleIt.next().getObject()).getLexicalForm();
		}
		if(ontologyName == null) return Response.status(Status.NOT_FOUND).build();
		else{
	        Site site = getSite(ontologyName);
	        log.debug("site/{}/find Request",site.getId());
	        Collection<String> supported = new HashSet<String>(JerseyUtils.QUERY_RESULT_SUPPORTED_MEDIA_TYPES);
	        supported.add(TEXT_HTML);
	        final MediaType acceptedMediaType = getAcceptableMediaType(
	            headers, supported, MediaType.APPLICATION_JSON_TYPE);
	        if(name == null || name.isEmpty()){
	            if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
	                ResponseBuilder rb = Response.ok(new Viewable("find", new SiteResultData(site)));
	                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
	                //addCORSOrigin(servletContext, rb, headers);
	                return rb.build();
	            } else {
	                return Response.status(Status.BAD_REQUEST)
	                    .entity("The name must not be null nor empty for find requests. Missing parameter name.\n")
	                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
	            }
	        }
	        final String property;
	        if (parsedField == null) {
	            property = DEFAULT_FIND_FIELD;
	        } else {
	            parsedField = parsedField.trim();
	            if (parsedField.isEmpty()) {
	                property = DEFAULT_FIND_FIELD;
	            } else {
	                property = nsPrefixService.getFullName(parsedField);
	                if(property == null){
	                    String messsage = String.format("The prefix '%s' of the parsed field '%' is not "
	                        + "mapped to any namespace. Please parse the full URI instead!\n",
	                        NamespaceMappingUtils.getPrefix(parsedField),parsedField);
	                    return Response.status(Status.BAD_REQUEST)
	                            .entity(messsage)
	                            .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
	                }
	            }
	        }
	        return executeQuery(site, createFieldQueryForFindRequest(
	                name, property, language,
	                limit == null || limit < 1 ? DEFAULT_FIND_RESULT_LIMIT : limit, 
	                offset,ldpath),
	            headers);
		}
    }
	
	protected void activate(ComponentContext ctx) throws ConfigurationException, FileNotFoundException, IOException {
		this.ctx = ctx;
		
		String stanbolHome = ctx.getBundleContext().getProperty("stanbol.home");
		
		URL indexerExecutablesUrl = ctx.getBundleContext().getBundle().getResource(INNER_INDEXER_EXECUTABLES);
		File outFolder = new File(stanbolHome + File.separator + "ontonethub-indexing" + File.separator + "executables");
		outFolder.mkdirs();
		File outFile = new File(outFolder, "indexing-genericrdf.jar");
	
		try{
			tcManager.createGraph(new IRI("ontonethub-graph"));
		} catch(EntityAlreadyExistsException e){
			log.info("The graph managed by the OntonetHub already exists.");
		}
		
		/*
		try {
			this.onScope = scopeManager.createOntologyScope("ontonethub-scope", 
					new BlankOntologySource());
		} catch (DuplicateIDException e) {
			log.info("The ontology scope already exists.");
			this.onScope = scopeManager.getScope("ontonethub-scope");
		}
		*/
		
		this.ontologiesFolder = new File(stanbolHome + File.separator + "ontonethub-indexing" + File.separator + "ontologies");
		ontologiesFolder.mkdirs();
		if(indexerExecutablesUrl != null){
			IOUtils.copy(indexerExecutablesUrl.openStream(), new FileOutputStream(outFile));
		}
	}
	
	protected void deactivate(ComponentContext ctx) {
		this.ctx= null;
	}


	private Site getSite(String siteId) {
        Site site = siteManager.getSite(siteId);
        if (site == null) {
            log.error("Site {} not found (no referenced site with that ID is present within the Entityhub",
                siteId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        if(site instanceof ManagedSite){
            log.debug("   ... init ManagedSite");
        }
        return site;
    }
	
	/**
     * Executes the query parsed by {@link #queryEntities(String, File, HttpHeaders)} or created based
     * {@link #findEntity(String, String, String, int, int, HttpHeaders)}
     * 
     * @param query
     *            The query to execute
     * @param headers the request headers
     * @return the response (results of error)
     */
    private Response executeQuery(Site site, FieldQuery query, HttpHeaders headers) throws WebApplicationException {
        MediaType mediaType = getAcceptableMediaType(headers, ENTITY_SUPPORTED_MEDIA_TYPES, 
            APPLICATION_JSON_TYPE);
        if(query instanceof LDPathSelect && ((LDPathSelect)query).getLDPathSelect() != null){
            //use the LDPath variant to process this query
            return executeLDPathQuery(site, query, ((LDPathSelect)query).getLDPathSelect(),
                mediaType, headers);
        } else { //use the default query execution
            QueryResultList<Representation> result;
            try {
                result = site.find(query);
            } catch (SiteException e) {
                String message = String.format("Unable to Query Site '%s' (message: %s)",
                    site.getId(),e.getMessage());
                log.error(message, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(message)
                .header(HttpHeaders.ACCEPT, mediaType).build();
            }
            ResponseBuilder rb = Response.ok(result);
            rb.header(HttpHeaders.CONTENT_TYPE, mediaType+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }
    
    /**
     * Execute a Query that uses LDPath to process results.
     * @param query the query
     * @param mediaType the mediaType for the response
     * @param headers the http headers of the request
     * @return the response
     */
    private Response executeLDPathQuery(Site site, FieldQuery query, String ldpathProgramString, MediaType mediaType, HttpHeaders headers) {
        QueryResultList<Representation> result;
        ValueFactory vf = new RdfValueFactory(new IndexedGraph());
        SiteBackend backend = new SiteBackend(site,vf);
        EntityhubLDPath ldPath = new EntityhubLDPath(backend,vf);
        //copy the selected fields, because we might need to delete some during
        //the preparation phase
        Set<String> selectedFields = new HashSet<String>(query.getSelectedFields());
        //first prepare (only execute the query if the parameters are valid)
        Program<Object> program;
        try {
            program = prepareQueryLDPathProgram(ldpathProgramString, selectedFields, backend, ldPath);
        } catch (LDPathParseException e) {
            log.warn("Unable to parse LDPath program used as select for Query:");
            log.warn("FieldQuery: \n {}",query);
            log.warn("LDPath: \n {}",((LDPathSelect)query).getLDPathSelect());
            log.warn("Exception:",e);
            return Response.status(Status.BAD_REQUEST)
            .entity(("Unable to parse LDPath program (Messages: "+
                    getLDPathParseExceptionMessage(e)+")!\n"))
            .header(HttpHeaders.ACCEPT, mediaType).build();
        } catch (IllegalStateException e) {
            log.warn("parsed LDPath program is not compatible with parsed Query!",e);
            return Response.status(Status.BAD_REQUEST)
            .entity(e.getMessage())
            .header(HttpHeaders.ACCEPT, mediaType).build();
        }
        //2. execute the query
        Iterator<Representation> resultIt;
        try { // we need to adapt from Entity to Representation
            resultIt = new AdaptingIterator<Entity,Representation>(site.findEntities(query).iterator(),
                    new AdaptingIterator.Adapter<Entity,Representation>() {
                        @Override
                        public Representation adapt(Entity value, Class<Representation> type) {
                            return value.getRepresentation();
                        }},Representation.class);
        } catch (SiteException e) {
            String message = String.format("Unable to Query Site '%s' (message: %s)",
                site.getId(),e.getMessage());
            log.error(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
            .entity(message)
            .header(HttpHeaders.ACCEPT, mediaType).build();
        }
        //process the results
        Collection<Representation> transformedResults = transformQueryResults(resultIt, program,
            selectedFields, ldPath, backend, vf);
        result = new QueryResultListImpl<Representation>(query, transformedResults, Representation.class);
        ResponseBuilder rb = Response.ok(result);
        rb.header(HttpHeaders.CONTENT_TYPE, mediaType+"; charset=utf-8");
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
    
    public class SiteResultData extends ResultData {

        private Site site;

        public SiteResultData(Site site) {
            this.site = site;
        }

        public boolean isManagedSite() {
            return site instanceof ManagedSite;
        }

        public Site getSite() {
            return site;
        }
    }
	
}
