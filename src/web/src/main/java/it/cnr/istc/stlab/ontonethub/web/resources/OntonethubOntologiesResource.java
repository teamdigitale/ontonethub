package it.cnr.istc.stlab.ontonethub.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.getAcceptableMediaType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import it.cnr.istc.stlab.ontonethub.OntoNetHub;
import it.cnr.istc.stlab.ontonethub.OntologyInfo;
import it.cnr.istc.stlab.ontonethub.solr.OntoNetHubSiteManager;
import it.cnr.istc.stlab.ontonethub.web.adapter.MissingRepresentationAdapterException;
import it.cnr.istc.stlab.ontonethub.web.adapter.RepresentationAdapter;
import it.cnr.istc.stlab.ontonethub.web.adapter.RepresentationAdapterFactory;
import it.cnr.istc.stlab.ontonethub.web.utils.JerseyUtils;

@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/ontonethub/ontologies")
public class OntonethubOntologiesResource extends BaseStanbolResource {
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
	/**
     * The Field used for find requests if not specified
     */
    private static final String DEFAULT_FIND_FIELD = "http://dati.gov.it/onto/ann-voc/synonym"; 
    private static final String DEFAULT_SELECTED_FIELD = RDFS.label.getUnicodeString();

    /**
     * The default number of maximal results of searched sites.
     */
    private static final int DEFAULT_FIND_RESULT_LIMIT = 5;
	
	
	@Reference
	private TcManager tcManager;
	
	@Reference
    private NamespacePrefixService nsPrefixService;
	
	@Reference
    private OntoNetHubSiteManager referencedSiteManager;
	
	@Reference
	private OntoNetHub ontonetHub;
	
	@Context 
	private UriInfo uriInfo;
	
	private ObjectMapper objectMapper;
	
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
    
    @OPTIONS
    @Path("/context")
    public Response handleCorsPreflightContext(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        return res.build();
    }
	
	@GET
	@Consumes(MediaType.WILDCARD)
	@Produces(MediaType.APPLICATION_JSON)
	public Response listOntologies(){
		JSONArray array = new JSONArray();
		
		OntologyInfo[] ontologyInfos = ontonetHub.getOntologiesInfo();
		for(OntologyInfo ontologyInfo : ontologyInfos){
			try {
				String jsonString = objectMapper.writeValueAsString(ontologyInfo);
				JSONObject json = new JSONObject(jsonString);
				String ontologyId = json.getString("ontologyID");
				
				if(ontologyId != null){
					String ontologySourceURI = uriInfo.getBaseUri() + "ontonethub/ontology/" + ontologyId + "/source";
					json.put("ontologySource", ontologySourceURI);
				}
				array.put(json);
				
			} catch (JsonProcessingException | JSONException e) {
				log.error(e.getMessage(), e);
			}	
		}
		
		
		return Response.ok(array.toString()).build();
	}
	
	@GET
	@Path("/context")
	public Response getContext(@QueryParam(value = "id") String id, @QueryParam(value = "lang") String lang){
		Collection<Representation> representations = ontonetHub.getOntologyEntityContext(id, lang);
		QueryResultList<Representation> queryResultList = new QueryResultListImpl<Representation>(null, representations, Representation.class);
		return Response.ok(queryResultList).build();
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
        String ldpathQuery = "";
        /*
        if(language != null && !language.isEmpty())
        	ldpathQuery = LDPATH_QUERY.replace("%LANG%", "[@" + language + "]");
        else ldpathQuery = LDPATH_QUERY.replace("%LANG%", "");
        
        if(ldpath != null && !ldpath.isEmpty()) ldpathQuery += ldpath;
        	*/
        
        String lemmatizedString = JerseyUtils.lemmatize(name) + "*";
        log.debug("Searching {}", lemmatizedString);
        FieldQuery query = JerseyUtils.createFieldQueryForFindRequest(lemmatizedString, DEFAULT_SELECTED_FIELD, property, language,
            limit == null || limit < 1 ? DEFAULT_FIND_RESULT_LIMIT : limit, offset, ldpathQuery);
        
        return executeQuery(referencedSiteManager, query, acceptedMediaType, language, headers);
    }
	
	protected void activate(ComponentContext ctx) throws ConfigurationException, FileNotFoundException, IOException {
		this.objectMapper = new ObjectMapper();
	}
	
	protected void deactivate(ComponentContext ctx) {
		this.objectMapper = null;
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
    private Response executeQuery(OntoNetHubSiteManager manager,
                                  FieldQuery query, MediaType mediaType, String lang, 
                                  HttpHeaders headers) throws WebApplicationException {
    	/*
        if(query instanceof LDPathSelect && ((LDPathSelect)query).getLDPathSelect() != null){
            //use the LDPath variant to process this query
            return executeLDPathQuery(manager, query, ((LDPathSelect)query).getLDPathSelect(),
                mediaType, headers);
        } else { //use the default query execution
        */
            QueryResultList<Entity> result = manager.findEntities(query);
            
            /* Serializzazione classica dell'Entityhub
            QueryResultList<Representation> representations  = new QueryResultListImpl<Representation>(query, transformQueryResult(result, lang), Representation.class);
            ResponseBuilder rb = Response.ok(representations);
            */
            
            /* Nuova serializzazione
             */
            JSONArray arr = transformResult(manager, result, lang);
            ResponseBuilder rb = Response.ok(arr.toString());
             
            
            
            rb.header(HttpHeaders.CONTENT_TYPE, mediaType+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        //}
    }
    
    private JSONArray transformResult(OntoNetHubSiteManager manager, QueryResultList<Entity> entities, String lang){
    	
    	JSONArray array = new JSONArray();
    	try {
			RepresentationAdapter representationAdapter = RepresentationAdapterFactory.getAdapter(JSONObject.class);
			entities.forEach(entity -> {
				Representation representation = entity.getRepresentation();
				try {
					JSONObject jsonObject = representationAdapter.adapt(manager, representation, lang);
					array.put(jsonObject);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			});
		} catch (MissingRepresentationAdapterException e) {
			log.error(e.getMessage(), e);
		}
    	/*
    	JSONArray array = new JSONArray();
    	entities.forEach(entity -> {
    		Representation representation = entity.getRepresentation();
    		try {
				array.put(transform(representation, lang));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	});
    	*/
    	
    	return array;
    }
    
    private JSONObject transform(Representation representation, String lang) throws JSONException{
    	JSONObject obj = new JSONObject();
    	Iterator<Object> it = representation.get("http://dati.gov.it/onto/ann-voc/dafLabel");
    	if(it.hasNext()){
    		Text dafLabel = (Text) it.next();
    		obj.put("dafLabel", dafLabel.getText());
    	}
    	
    	it = representation.get("http://dati.gov.it/onto/ann-voc/dafId");
    	if(it.hasNext()){
    		Text dafId = (Text) it.next();
    		obj.put("dafId", dafId.getText());
    	}
    	
    	it = representation.get("http://stanbol.apache.org/ontology/entityhub/query#score");
    	if(it.hasNext()){
    		Float score = (Float) it.next();
    		obj.put("score", score);
    	}
    	
    	JSONObject ontologyObj = new JSONObject();
    	it = representation.get("http://dati.gov.it/onto/ann-voc/ontologyId");
    	if(it.hasNext()){
    		Text ontologyId = (Text)it.next();
    		
    		ontologyObj.put("id", ontologyId.getText());
    	}
    	
    	Iterator<Text> textIt = representation.get("http://dati.gov.it/onto/ann-voc/ontologyLabel", lang);
    	JSONArray ontoLabels = new JSONArray();
    	if(textIt.hasNext()){
    		Text ontologyLabel = textIt.next();
    		
    		JSONObject ontologyLabelObj = new JSONObject();
    		ontologyLabelObj.put("value", ontologyLabel.getText());
    		
    		String language = ontologyLabel.getLanguage();
    		if(language != null) ontologyLabelObj.put("lang", language);
    		
    		ontoLabels.put(ontologyLabelObj);
    	}
    	
    	textIt = representation.get("http://dati.gov.it/onto/ann-voc/ontologyComment", lang);
    	JSONArray ontoComments = new JSONArray();
    	while(textIt.hasNext()){
    		Text ontologyComment = textIt.next();
    		
    		JSONObject ontologyCommentObj = new JSONObject();
    		ontologyCommentObj.put("value", ontologyComment.getText());
    		
    		String language = ontologyComment.getLanguage();
    		if(language != null) ontologyCommentObj.put("lang", language);
    		ontoComments.put(ontologyCommentObj);
    		
    	}
    	
    	ontologyObj.put("label", ontoLabels);
    	ontologyObj.put("comment", ontoComments);
    	obj.put("ontology", ontologyObj);
    	
    	
    	it = representation.get("http://dati.gov.it/onto/ann-voc/universeSignature");
    	if(it.hasNext()){
    		Text contextTriple = (Text) it.next();
    		JSONObject contextObj = new JSONObject();
    		obj.put("context", contextObj);
    		
    		contextObj.put("value", contextTriple.getText());
    		
    		Iterator<Object> fingerprintIt = representation.get("http://dati.gov.it/onto/ann-voc/universeFingerprint");
    		if(fingerprintIt.hasNext()){
    			String fingerprint = ((Text) fingerprintIt.next()).getText();
    			contextObj.put("fingerprint", fingerprint);
    		}
    		
    		Iterator<Object> contextIt = representation.get("http://dati.gov.it/onto/ann-voc/universeDomain");
    		if(contextIt.hasNext()){
    			org.apache.stanbol.entityhub.servicesapi.model.Reference domain = (org.apache.stanbol.entityhub.servicesapi.model.Reference)contextIt.next();
    			JSONObject contextDomainObj = new JSONObject();
    			contextObj.put("domain", contextDomainObj);
    			contextDomainObj.put("id", domain.getReference());
    			
    			JSONArray domainLabels = new JSONArray();
    			contextDomainObj.put("label", domainLabels);
    			Iterator<Text> domainIt = representation.get("http://dati.gov.it/onto/ann-voc/domainLabel", lang);
    			while(domainIt.hasNext()){
    				Text text = domainIt.next();
    				
    				JSONObject domainLabel = new JSONObject();
    				domainLabel.put("value", text.getText());
    				if(lang != null){
    					domainLabel.put("lang", lang);
    				}
    				domainLabels.put(domainLabel);
    			}
    			
    			if(domainLabels.length() == 0){
    				Resource res = ResourceFactory.createResource(domain.getReference());
    				JSONObject labelObj = new JSONObject();
    				labelObj.put("value", res.getLocalName());
    				domainLabels.put(labelObj);
    			}
    			
    			JSONArray domainComments = new JSONArray();
    			contextDomainObj.put("comment", domainComments);
    			domainIt = representation.get("http://dati.gov.it/onto/ann-voc/domainComment", lang);
    			while(domainIt.hasNext()){
    				Text text = domainIt.next();
    				
    				JSONObject domainComment = new JSONObject();
    				domainComment.put("value", text.getText());
    				if(lang != null){
    					domainComment.put("lang", lang);
    				}
    				domainComments.put(domainComment);
    			}
    			
    			Iterator<Object> cvIt = representation.get("http://dati.gov.it/onto/ann-voc/domainControlledVocabulary");
    			JSONArray controlledVocabulariesArr = new JSONArray();
    			contextDomainObj.put("controlledVocabularies", controlledVocabulariesArr);
    			while(cvIt.hasNext()){
    				org.apache.stanbol.entityhub.servicesapi.model.Reference cv = (org.apache.stanbol.entityhub.servicesapi.model.Reference)cvIt.next();
    				controlledVocabulariesArr.put(cv.getReference());
    			}
    			
    			
    		}
    		
    		contextIt = representation.get("http://dati.gov.it/onto/ann-voc/universeProperty");
    		if(contextIt.hasNext()){
    			org.apache.stanbol.entityhub.servicesapi.model.Reference property = (org.apache.stanbol.entityhub.servicesapi.model.Reference)contextIt.next();
    			JSONObject contextPropertyObj = new JSONObject();
    			contextObj.put("property", contextPropertyObj);
    			contextPropertyObj.put("id", property.getReference());
    			
    			JSONArray propertyLabels = new JSONArray();
    			contextPropertyObj.put("label", propertyLabels);
    			Iterator<Text> propertyIt = representation.get("http://dati.gov.it/onto/ann-voc/propertyLabel", lang);
    			while(propertyIt.hasNext()){
    				Text text = propertyIt.next();
    				
    				JSONObject propertyLabel = new JSONObject();
    				propertyLabel.put("value", text.getText());
    				if(lang != null){
    					propertyLabel.put("lang", lang);
    				}
    				propertyLabels.put(propertyLabel);
    			}
    			
    			if(propertyLabels.length() == 0){
    				Resource res = ResourceFactory.createResource(property.getReference());
    				JSONObject labelObj = new JSONObject();
    				labelObj.put("value", res.getLocalName());
    				propertyLabels.put(labelObj);
    			}
    			
    			JSONArray propertyComments = new JSONArray();
    			contextPropertyObj.put("comment", propertyComments);
    			propertyIt = representation.get("http://dati.gov.it/onto/ann-voc/propertyComment", lang);
    			while(propertyIt.hasNext()){
    				Text text = propertyIt.next();
    				
    				JSONObject propertyComment = new JSONObject();
    				propertyComment.put("value", text.getText());
    				if(lang != null){
    					propertyComment.put("lang", lang);
    				}
    				propertyComments.put(propertyComment);
    			}
    			
    			Iterator<Object> cvIt = representation.get("http://dati.gov.it/onto/ann-voc/propertyControlledVocabulary");
    			JSONArray controlledVocabulariesArr = new JSONArray();
    			contextPropertyObj.put("controlledVocabularies", controlledVocabulariesArr);
    			while(cvIt.hasNext()){
    				org.apache.stanbol.entityhub.servicesapi.model.Reference cv = (org.apache.stanbol.entityhub.servicesapi.model.Reference)cvIt.next();
    				controlledVocabulariesArr.put(cv.getReference());
    			}
    			
    		}
    		
    		contextIt = representation.get("http://dati.gov.it/onto/ann-voc/universeRange");
    		if(contextIt.hasNext()){
    			org.apache.stanbol.entityhub.servicesapi.model.Reference range = (org.apache.stanbol.entityhub.servicesapi.model.Reference)contextIt.next();
    			JSONObject contextRangeObj = new JSONObject();
    			contextObj.put("range", contextRangeObj);
    			contextRangeObj.put("id", range.getReference());
    			
    			JSONArray rangeLabels = new JSONArray();
    			contextRangeObj.put("label", rangeLabels);
    			Iterator<Text> rangeIt = representation.get("http://dati.gov.it/onto/ann-voc/rangeLabel", lang);
    			while(rangeIt.hasNext()){
    				Text text = rangeIt.next();
    				
    				JSONObject rangeLabel = new JSONObject();
    				rangeLabel.put("value", text.getText());
    				if(lang != null){
    					rangeLabel.put("lang", lang);
    				}
    				rangeLabels.put(rangeLabel);
    			}
    			
    			if(rangeLabels.length() == 0){
    				Resource res = ResourceFactory.createResource(range.getReference());
    				JSONObject labelObj = new JSONObject();
    				labelObj.put("value", res.getLocalName());
    				rangeLabels.put(labelObj);
    			}
    			
    			JSONArray rangeComments = new JSONArray();
    			contextRangeObj.put("comment", rangeComments);
    			rangeIt = representation.get("http://dati.gov.it/onto/ann-voc/rangeComment", lang);
    			while(rangeIt.hasNext()){
    				Text text = rangeIt.next();
    				
    				JSONObject rangeComment = new JSONObject();
    				rangeComment.put("value", text.getText());
    				if(lang != null){
    					rangeComment.put("lang", lang);
    				}
    				rangeComments.put(rangeComment);
    			}
    			
    			Iterator<Object> cvIt = representation.get("http://dati.gov.it/onto/ann-voc/rangeControlledVocabulary");
    			JSONArray controlledVocabulariesArr = new JSONArray();
    			contextRangeObj.put("controlledVocabularies", controlledVocabulariesArr);
    			while(cvIt.hasNext()){
    				org.apache.stanbol.entityhub.servicesapi.model.Reference cv = (org.apache.stanbol.entityhub.servicesapi.model.Reference)cvIt.next();
    				controlledVocabulariesArr.put(cv.getReference());
    			}
    			
    		}
    		
    	}
    	return obj;
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
}
