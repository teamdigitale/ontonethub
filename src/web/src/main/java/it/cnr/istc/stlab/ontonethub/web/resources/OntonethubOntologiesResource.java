package it.cnr.istc.stlab.ontonethub.web.resources;

import static it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary.HAS_BUNDLE;
import static it.cnr.istc.stlab.ontonethub.web.utils.LDPathHelper.getLDPathParseExceptionMessage;
import static it.cnr.istc.stlab.ontonethub.web.utils.LDPathHelper.prepareQueryLDPathProgram;
import static it.cnr.istc.stlab.ontonethub.web.utils.LDPathHelper.transformQueryResults;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.getAcceptableMediaType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.SiteManagerBackend;
import org.apache.stanbol.entityhub.ldpath.query.LDPathSelect;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator;
import org.codehaus.jettison.json.JSONArray;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.istc.stlab.ontonethub.OntologyDescriptionVocabulary;
import it.cnr.istc.stlab.ontonethub.web.utils.JerseyUtils;

@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/ontonethub/ontologies")
public class OntonethubOntologiesResource extends BaseStanbolResource {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	/**
     * The Field used for find requests if not specified TODO: Will be depreciated as soon as EntityQuery is
     * implemented
     */
    private static final String DEFAULT_FIND_FIELD = RDFS.label.getUnicodeString();

    /**
     * The default number of maximal results of searched sites.
     */
    private static final int DEFAULT_FIND_RESULT_LIMIT = 5;
	
	
	@Reference
	private TcManager tcManager;
	
	@Reference
    private NamespacePrefixService nsPrefixService;
	
	@Reference
    private SiteManager referencedSiteManager;
	
	@Context 
	private UriInfo uriInfo;
	
	private ComponentContext ctx;
	
	@OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        return res.build();
    }
    @OPTIONS
    @Path("/find")
    public Response handleCorsPreflightFind(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        return res.build();
    }
	
	@GET
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public Response listOntologies(){
		
		Graph g = tcManager.getMGraph(new IRI("ontonethub-graph"));
		Iterator<Triple> tripleIt = g.filter(null, 
				new IRI(HAS_BUNDLE), 
				null);
		
		JSONArray array = new JSONArray();
		while(tripleIt.hasNext()){
			Triple triple = tripleIt.next();
			BlankNodeOrIRI subject = triple.getSubject();
			String uri = subject.toString();
			uri = uri.substring(1, uri.length()-1)
					.replace(OntologyDescriptionVocabulary.ONTOLOGY , uriInfo.getBaseUri() + "ontonethub/ontology/");
			array.put(uri);
		}
		return Response.ok(array.toString()).build();
	}
	
	@POST
    @Path("/find")
    public Response findEntity(@FormParam(value = "name") String name,
                               @FormParam(value = "field") String parsedField,
                               @FormParam(value = "lang") String language,
                               // @FormParam(value="select") String select,
                               @FormParam(value = "limit") Integer limit,
                               @FormParam(value = "offset") Integer offset,
                               @FormParam(value = "ldpath") String ldpath,
                               @Context HttpHeaders headers) {
        log.debug("findEntity() Request");
        Collection<String> supported = new HashSet<String>(JerseyUtils.QUERY_RESULT_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = getAcceptableMediaType(
            headers, supported, MediaType.APPLICATION_JSON_TYPE);
        if(name == null || name.isEmpty()){
            if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                ResponseBuilder rb =  Response.ok(new Viewable("find", this));
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
        FieldQuery query = JerseyUtils.createFieldQueryForFindRequest(name, property, language,
            limit == null || limit < 1 ? DEFAULT_FIND_RESULT_LIMIT : limit, offset,ldpath);
        return executeQuery(referencedSiteManager, query, acceptedMediaType, headers);
    }
	
	protected void activate(ComponentContext ctx) throws ConfigurationException, FileNotFoundException, IOException {
		this.ctx = ctx;
	}
	
	protected void deactivate(ComponentContext ctx) {
		this.ctx= null;
	}
	
	/**
     * Executes the query parsed by {@link #queryEntities(String, File, HttpHeaders)} or created based
     * {@link #findEntity(String, String, String, int, int, HttpHeaders)}
     * 
     * @param manager The {@link SiteManager}
     * @param query
     *            The query to execute
     * @param headers the request headers
     * @return the response (results of error)
     */
    private Response executeQuery(SiteManager manager,
                                  FieldQuery query, MediaType mediaType, 
                                  HttpHeaders headers) throws WebApplicationException {
        if(query instanceof LDPathSelect && ((LDPathSelect)query).getLDPathSelect() != null){
            //use the LDPath variant to process this query
            return executeLDPathQuery(manager, query, ((LDPathSelect)query).getLDPathSelect(),
                mediaType, headers);
        } else { //use the default query execution
            QueryResultList<Representation> result = manager.find(query);
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
    private Response executeLDPathQuery(SiteManager manager,FieldQuery query, String ldpathProgramString, MediaType mediaType, HttpHeaders headers) {
        QueryResultList<Representation> result;
        ValueFactory vf = new RdfValueFactory(new IndexedGraph());
        SiteManagerBackend backend = new SiteManagerBackend(manager);
        EntityhubLDPath ldPath = new EntityhubLDPath(backend,vf);
        //copy the selected fields, because we might need to delete some during
        //the preparation phase
        Set<String> selectedFields = new HashSet<String>(query.getSelectedFields());
        //first prepare (only execute the query if the parameters are valid)
        Program<Object> program;
        try {
            program = prepareQueryLDPathProgram(ldpathProgramString, selectedFields, backend, ldPath);
        } catch (LDPathParseException e) {
            log.warn("Unable to parse LDPath program used as select for a Query to the '/sites' endpoint:");
            log.warn("FieldQuery: \n {}",query);
            log.warn("LDPath: \n {}",((LDPathSelect)query).getLDPathSelect());
            log.warn("Exception:",e);
            return Response.status(Status.BAD_REQUEST)
            .entity(("Unable to parse LDPath program (Messages: "+
                    getLDPathParseExceptionMessage(e)+")!\n"))
            .header(HttpHeaders.ACCEPT, mediaType).build();
        } catch (IllegalStateException e) {
            log.warn("parsed LDPath program is not compatible with the Query " +
            		"parsed to the '/sites' endpoint!",e);
            return Response.status(Status.BAD_REQUEST)
            .entity(e.getMessage())
            .header(HttpHeaders.ACCEPT, mediaType).build();
        }
        //2. execute the query
        // we need to adapt from Entity to Representation
        //TODO: should we add the metadata to the result?
        Iterator<Representation> resultIt = new AdaptingIterator<Entity,Representation>(manager.findEntities(query).iterator(),
            new AdaptingIterator.Adapter<Entity,Representation>() {
                @Override
                public Representation adapt(Entity value, Class<Representation> type) {
                    return value.getRepresentation();
                }},Representation.class);
        //process the results
        Collection<Representation> transformedResults = transformQueryResults(resultIt, program,
            selectedFields, ldPath, backend, vf);
        result = new QueryResultListImpl<Representation>(query, transformedResults, Representation.class);
        ResponseBuilder rb = Response.ok(result);
        rb.header(HttpHeaders.CONTENT_TYPE, mediaType+"; charset=utf-8");
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
	
}
