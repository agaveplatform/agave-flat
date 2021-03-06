/**
 * 
 */
package org.iplantc.service.apps.resources;

import org.restlet.representation.Representation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author dooley
 *
 */
@Path("{softwareId}/pems")
public interface SoftwarePermissionCollection {
    
    @GET
    Response getSoftwarePermissions(@PathParam("softwareId") String softwareId);
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
    Response addSoftwarePermission(@PathParam("softwareId") String softwareId,
                                   Representation input);
	
	@DELETE
    Response clearAllSoftwarePermissions(@PathParam("softwareId") String softwareId);
	
//	@POST
//    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
//    public Response addSoftwarePermissionFromForm(@PathParam("softwareId") String softwareId,
//                                            @FormParam("username") String callbackUrl,
//                                            @FormParam("permission") String permission);
//    @POST
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response addSoftwarePermission(@PathParam("softwareId") String softwareId,
//                                    byte[] bytes);
    
    
}
