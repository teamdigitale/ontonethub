package it.cnr.istc.stlab.ontonethub.web.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

/**
 * Base resource for the OntoNetHub. 
 * 
 * @author Andrea Nuzzolese
 *
 */
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/ontonethub")
public class OntonethubResource extends BaseStanbolResource {
	
	@Context 
	private UriInfo uriInfo;
	
	
	@OPTIONS
    public Response handleCorsPreflightOntology(@PathParam(value = "id") String id,
            @Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        return res.build();
    }
	
	@GET
	@Consumes(MediaType.WILDCARD)
	@Produces({MediaType.TEXT_PLAIN})
	public Response sayHello(){
		return Response.ok("Hello, it's the OntoNetHub!").build();
	}
}
