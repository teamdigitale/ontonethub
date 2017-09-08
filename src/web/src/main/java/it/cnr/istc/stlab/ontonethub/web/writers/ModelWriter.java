package it.cnr.istc.stlab.ontonethub.web.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.stanbol.commons.web.base.format.KRFormat;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * 
 * @author Andrea Nuzzolese
 *
 */

@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Provider
public class ModelWriter implements MessageBodyWriter<Model>{
	
	public static final Set<String> supportedMediaTypes;
	static {
		Set<String> types = new HashSet<String>();
		types.add(KRFormat.N3);
		types.add(KRFormat.N_TRIPLE);
		types.add(KRFormat.RDF_JSON);
		types.add(KRFormat.TURTLE);
		types.add("application/json-ld");
		supportedMediaTypes = Collections.unmodifiableSet(types);
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		String mediaTypeString = mediaType.getType()+'/'+mediaType.getSubtype();
		return Model.class.isAssignableFrom(type) && supportedMediaTypes.contains(mediaTypeString);
	}

	@Override
	public long getSize(Model t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(Model t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException {
		
		Lang lang = null;
		
		if(mediaType.equals(KRFormat.N3_TYPE))
			lang = Lang.N3;
		else if(mediaType.equals(KRFormat.N_TRIPLE_TYPE))
			lang = Lang.NTRIPLES;
		else if(mediaType.equals(KRFormat.RDF_JSON_TYPE))
			lang = Lang.RDFJSON;
		else if(mediaType.equals(new MediaType("application", "json-ld")))
			lang = Lang.JSONLD;
		else lang = Lang.TURTLE;
		
		RDFDataMgr.write(entityStream, t, lang);
		
	}

}
