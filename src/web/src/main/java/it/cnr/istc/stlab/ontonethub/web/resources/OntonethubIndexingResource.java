package it.cnr.istc.stlab.ontonethub.web.resources;

import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.ONTOLOGY;
import static it.cnr.istc.stlab.ontonethub.web.utils.JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.getAcceptableMediaType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

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
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.web.base.format.KRFormat;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSite;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.rdf.model.Model;

import it.cnr.istc.stlab.ontonethub.NoSuchOntologyException;
import it.cnr.istc.stlab.ontonethub.OntoNetHub;
import it.cnr.istc.stlab.ontonethub.OntologyAlreadyExistingException;
import it.cnr.istc.stlab.ontonethub.OntologyInfo;
import it.cnr.istc.stlab.ontonethub.UnmappableOntologyException;
import it.cnr.istc.stlab.ontonethub.job.IndexingJobInput;
import it.cnr.istc.stlab.ontonethub.solr.OntoNetHubSiteManager;
import it.cnr.istc.stlab.ontonethub.web.adapter.MissingRepresentationAdapterException;
import it.cnr.istc.stlab.ontonethub.web.adapter.RepresentationAdapter;
import it.cnr.istc.stlab.ontonethub.web.adapter.RepresentationAdapterFactory;
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
@Path("/ontonethub/ontology2")
public class OntonethubIndexingResource extends BaseStanbolResource {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	public static final String RUNNABLE_INDEXER_EXECUTABLES_FOLDER = "ontonethub-indexing" + File.separator + "executables";
	public static final String RUNNABLE_INDEXER_EXECUTABLES = RUNNABLE_INDEXER_EXECUTABLES_FOLDER + File.separator + "indexing-genericrdf.jar";
	public static final String INNER_INDEXER_EXECUTABLES = "executables" + File.separator + "indexing-genericrdf.jar";
	
	/**
     * The Field used for find requests if not specified TODO: This will be replaced by the EntitySearch. With
     * this search the Site is responsible to decide what properties to use for label based searches.
     */
	private static final String DEFAULT_FIND_FIELD = "http://dati.gov.it/onto/ann-voc/synonym"; 
    private static final String DEFAULT_SELECTED_FIELD = RDFS.label.getUnicodeString();
    
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
	private OntoNetHubSiteManager siteManager;
	
	@Reference
	private TcManager tcManager;
	
	@Reference
    private NamespacePrefixService nsPrefixService;
	
	@Context 
	private UriInfo uriInfo;
	
	@Reference
    private OntoNetHubSiteManager referencedSiteManager;
	
	@Reference
	private OntoNetHub ontonetHub;
	
	private ObjectMapper objectMapper;
	
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
		return Response.ok("Welcome to the new Home of OntoNetHub for Semantic DAF!").build();
	}
	
	@GET
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	public Response getOntologyInfo(@PathParam("id") String id){
		
		log.info("This is an ontonethub search");
		ResponseBuilder responseBuilder = null;
		try {
			OntologyInfo ontologyInfo = ontonetHub.getOntologyInfo(id);
			String json;
			try {
				json = objectMapper.writeValueAsString(ontologyInfo);
				
				JSONObject jsonObject = new JSONObject(json);
				String ontologySourceURI = uriInfo.getBaseUri() + "ontonethub/ontology/" + id + "/source";
				jsonObject.put("ontologySource", ontologySourceURI);
				
				responseBuilder = Response.ok(json);
			} catch (JsonProcessingException | JSONException e) {
				JSONObject jsonO = new JSONObject();
				try {
					jsonO.put("error", e.getMessage());
				} catch (JSONException e1) {
					log.error(e1.getMessage(), e1);
				}
				responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(jsonO.toString());
			}
		} catch (NoSuchOntologyException e2) {
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
			model = ontonetHub.getOntologySource(id);
			responseBuilder = Response.ok(model);
		} catch (NoSuchOntologyException e1) {
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
		
		
		try {
			String jid = ontonetHub.indexOntology(input);
			URI location = URI.create(getPublicBaseUri() + "jobs/" + jid);
			
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("monitoringService", location.toString());
				jsonObject.put("ontologyId", jid);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			responseBuilder = Response.ok(jsonObject.toString());
			
		} catch (OntologyAlreadyExistingException e1) {
			responseBuilder = Response.status(Status.CONFLICT);
		}
		
		return responseBuilder.build();
		
	}
	
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	public Response deleteOntologyIndex(@PathParam("id") String id){
		ResponseBuilder responseBuilder = null;
		
		try {
			ontonetHub.deleteOntologyIndex(id);
			responseBuilder = Response.ok();
		} catch (UnmappableOntologyException e) {
			JSONObject json = new JSONObject();
			try {
				json.put("error", e.getE().getMessage());
			} catch (JSONException e1) {
				log.error(e1.getMessage(), e1);
			}
			responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(json.toString());
		} catch (NoSuchOntologyException e) {
			JSONObject json = new JSONObject();
			try {
				json.put("error", "No ontology exists with the ID provided.");
			} catch (JSONException e1) {
				log.error(e.getMessage(), e1);
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
		
		String ontologyURI = ONTOLOGY + id;
		Graph g = tcManager.getMGraph(new IRI("ontonethub-graph"));
		Iterator<Triple> tripleIt = g.filter(new IRI(ontologyURI), 
				DC.identifier, 
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
	        
	        String lemmatizedString = JerseyUtils.lemmatize(name) + "*";
	        log.debug("Searching {}", lemmatizedString);
	        
	        FieldQuery query = JerseyUtils.createFieldQueryForFindRequest(lemmatizedString, DEFAULT_SELECTED_FIELD, property, language,
	                limit == null || limit < 1 ? DEFAULT_FIND_RESULT_LIMIT : limit, offset, "");
	            
	        return executeQuery(site, query, language, headers);
		}
    }
	
	protected void activate(ComponentContext ctx) throws ConfigurationException, FileNotFoundException, IOException {
		this.ctx = ctx;
		
		this.objectMapper = new ObjectMapper();
	}
	
	protected void deactivate(ComponentContext ctx) {
		this.ctx= null;
		this.objectMapper = null;
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
    private Response executeQuery(Site site, FieldQuery query, String lang, HttpHeaders headers) throws WebApplicationException {
        MediaType mediaType = getAcceptableMediaType(headers, ENTITY_SUPPORTED_MEDIA_TYPES, 
            APPLICATION_JSON_TYPE);
        
        QueryResultList<Entity> result;
        try {
            result = referencedSiteManager.findEntities(site, query);
        } catch (SiteException e) {
            String message = String.format("Unable to Query Site '%s' (message: %s)",
                site.getId(),e.getMessage());
            log.error(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
            .entity(message)
            .header(HttpHeaders.ACCEPT, mediaType).build();
        }
        
        JSONArray arr = transformResult(result, lang);
        ResponseBuilder rb = Response.ok(arr.toString());
        
        rb.header(HttpHeaders.CONTENT_TYPE, mediaType+"; charset=utf-8");
        
        return rb.build();
    }
    
    private JSONArray transformResult(QueryResultList<Entity> entities, String lang){
    	
    	JSONArray array = new JSONArray();
    	try {
			RepresentationAdapter representationAdapter = RepresentationAdapterFactory.getAdapter(JSONObject.class);
			entities.forEach(entity -> {
				Representation representation = entity.getRepresentation();
				try {
					JSONObject jsonObject = representationAdapter.adapt(referencedSiteManager, representation, lang);
					array.put(jsonObject);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			});
		} catch (MissingRepresentationAdapterException e) {
			log.error(e.getMessage(), e);
		}
    	
    	return array;
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
