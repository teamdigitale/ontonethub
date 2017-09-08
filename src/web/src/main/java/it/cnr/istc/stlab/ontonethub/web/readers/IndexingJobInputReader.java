package it.cnr.istc.stlab.ontonethub.web.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.system.StreamRDFWrapper;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.graph.GraphFactory;

import it.cnr.istc.stlab.ontonethub.job.IndexingJobInput;

@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Provider
public class IndexingJobInputReader implements MessageBodyReader<IndexingJobInput> {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private FileUpload fu;

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return IndexingJobInput.class.isAssignableFrom(type);
	}

	@Override
	public IndexingJobInput readFrom(Class<IndexingJobInput> type, Type genericType, Annotation[] annotations,
			MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException {
		
		IndexingJobInput indexingJobInput = new IndexingJobInput();
		try{
			
			FileItemIterator fileItemIterator = fu.getItemIterator(new MessageBodyReaderContext(entityStream, mediaType));
			
			while(fileItemIterator.hasNext()){
				FileItemStream fis = fileItemIterator.next();
                if(fis.getFieldName().equals("name")){
                	StringWriter writer = new StringWriter();
                	IOUtils.copy(fis.openStream(), writer);
                	String name = writer.toString();
                	indexingJobInput.setName(name);
                	log.info("Name: " + name);
                }
                else if(fis.getFieldName().equals("description")){
                	StringWriter writer = new StringWriter();
                	IOUtils.copy(fis.openStream(), writer);
                	String description = writer.toString();
                	indexingJobInput.setDescription(description);
                	log.info("Description: " + description);
                }
                else if(fis.getFieldName().equals("baseURI")){
                	StringWriter writer = new StringWriter();
                	IOUtils.copy(fis.openStream(), writer);
                	String baseURI = writer.toString();
                	indexingJobInput.setBaseURI(baseURI);
                	log.info("Base URI: " + baseURI);
                }
                else if(fis.getFieldName().equals("data")){
                	//Model model = ModelFactory.createDefaultModel();
                	/*StringWriter writer = new StringWriter();
                	IOUtils.copy(fis.openStream(), writer);
                	rdf = writer.toString();*/
                	
                	Graph graph = GraphFactory.createDefaultGraph();
                	StreamRDF sink = new StreamRDFWrapper(StreamRDFLib.graph(graph));
                	RDFDataMgr.parse(sink, fis.openStream(), "http://localhost/", Lang.RDFXML);
                	
                	Model model = ModelFactory.createModelForGraph(graph);
                	log.info("Model contains {} triples.", model.size());
                	//model.read(fis.openStream(), "http://localhost/", "RDF/XML");
                	//dataContent = fis.openStream();
                	indexingJobInput.setData(model);
                }
			}
			
    		return indexingJobInput;
    	} catch (UnrecognizedPropertyException e){
    		log.error(e.getMessage(), e);
    		return null;
    	} catch (FileUploadException e) {
    		log.error(e.getMessage(), e);
    		return null;
		}
	}
	
	@Activate
	protected void activate(ComponentContext ctx) {
		this.fu = new FileUpload();
		log.info(getClass() + " activated.");
    }
	    
	@Deactivate
    protected void deactivate(ComponentContext ctx){
		log.info(getClass() + " deactivated.");
    }
	
	/**
     * Adapter from the parameter present in an {@link MessageBodyReader} to
     * the {@link RequestContext} as used by the commons.fileupload framework
     * @author rwesten
     *
     */
    private static class MessageBodyReaderContext implements RequestContext{

        private final InputStream in;
        private final String contentType;
        private final String charEncoding;

        public MessageBodyReaderContext(InputStream in, MediaType mediaType){
            this.in = in;
            this.contentType = mediaType.toString();
            String charset = mediaType.getParameters().get("charset");
            this.charEncoding = charset == null ? "UTF-8" : charset;
        }
        
        @Override
        public String getCharacterEncoding() {
            return charEncoding;
        }

        @Override
        public String getContentType() {
            return  contentType;
        }

        @Override
        public int getContentLength() {
            return -1;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return in;
        }
        
    }

}
