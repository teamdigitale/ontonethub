package it.cnr.istc.stlab.ontonethub.web.adapter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import it.cnr.istc.stlab.ontonethub.solr.OntoNetHubSiteManager;

@Component(immediate = true)
@Service(RepresentationAdapter.class)
public class Representation2JSONAdapter implements RepresentationAdapter {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public Representation2JSONAdapter() {
		
	}
	
	public <T> T adapt(OntoNetHubSiteManager manager, Representation representation, String lang) throws JSONException {
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
    		obj.put("universe", contextObj);
    		
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
    			
    			/*
    			 * Here we compute what is called context by DAF for the domain class.
    			 */
    			Entity domainEntity = manager.getEntity(domain.getReference());
    			JSONArray contexts = new JSONArray();
    			Map<String,JSONObject> objs = new HashMap<String,JSONObject>();
    			contextDomainObj.put("contexts", contexts);
    			if(domainEntity != null){
	    			Representation domainRepresentation = domainEntity.getRepresentation();
	    			Iterator<Object> domainContextes = domainRepresentation.get("http://dati.gov.it/onto/ann-voc/isRangeOfUniverse");
	    			while(domainContextes.hasNext()){
	    				JSONObject ctxObj = new JSONObject();
	    				
	    				org.apache.stanbol.entityhub.servicesapi.model.Reference domainContext = 
	    						(org.apache.stanbol.entityhub.servicesapi.model.Reference)domainContextes.next();
	    				
	    				String reference = domainContext.getReference();
	    				int indexOfLastSlash = reference.lastIndexOf("/");
	    				String context = reference.substring(indexOfLastSlash + 1);
	    				
	    				ctxObj.put("id", reference);
	    				ctxObj.put("label", context);
	    				log.info("I am here with {}", reference);
	    				if(!objs.containsKey(reference))
	    					objs.put(reference, ctxObj);
	    				//contexts.put(ctxObj);
	    			}
    			}
    			
    			for(JSONObject jsonObject : objs.values())
    				contexts.put(jsonObject);
    			
    			
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
    			
    			/*
    			 * Here we compute what is called context by DAF for the range class.
    			 */
    			Entity rangeEntity = manager.getEntity(range.getReference());
    			JSONArray contexts = new JSONArray();
    			contextRangeObj.put("contexts", contexts);
    			Map<String,JSONObject> objs = new HashMap<String,JSONObject>();
    			if(rangeEntity != null){
	    			Representation rangeRepresentation = rangeEntity.getRepresentation();
	    			Iterator<Object> rangeContextes = rangeRepresentation.get("http://dati.gov.it/onto/ann-voc/isDomainOfUniverse");
	    			while(rangeContextes.hasNext()){
	    				JSONObject ctxObj = new JSONObject();
	    				org.apache.stanbol.entityhub.servicesapi.model.Reference rangeContext = 
	    						(org.apache.stanbol.entityhub.servicesapi.model.Reference)rangeContextes.next();
	    				
	    				String reference = rangeContext.getReference();
	    				int indexOfLastSlash = reference.lastIndexOf("/");
	    				String context = reference.substring(indexOfLastSlash + 1);
	    				
	    				ctxObj.put("id", reference);
	    				ctxObj.put("label", context);
	    				if(!objs.containsKey(reference))
	    					objs.put(reference, ctxObj);
	    			}
    			}
    			
    			for(JSONObject jsonObject : objs.values())
    				contexts.put(jsonObject);
    			
    		}
    		
    	}
    	return (T) obj;
    }
	
	protected void activate(ComponentContext ctx) {
		RepresentationAdapterFactory.registerAdapter(this, JSONObject.class);
	}
	
	protected void deactivate(ComponentContext ctx) {
		RepresentationAdapterFactory.unregisterAdapter(JSONObject.class);
	}
	
}
