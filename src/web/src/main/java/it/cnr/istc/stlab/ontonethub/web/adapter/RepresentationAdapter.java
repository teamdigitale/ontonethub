package it.cnr.istc.stlab.ontonethub.web.adapter;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;

import it.cnr.istc.stlab.ontonethub.solr.OntoNetHubSiteManager;

public interface RepresentationAdapter {

	<T> T adapt(OntoNetHubSiteManager manager, Representation representation, String lang) throws Exception; 
	
}
