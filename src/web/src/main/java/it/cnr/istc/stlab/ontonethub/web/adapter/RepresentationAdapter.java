package it.cnr.istc.stlab.ontonethub.web.adapter;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;

public interface RepresentationAdapter {

	<T> T adapt(Representation representation, String lang) throws Exception; 
	
}
