/**
 * 
 */
package org.iplantc.service.apps.resources;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

/**
 * @author dooley
 *
 */
public interface QuartzResource {

	@GET
	public Response getSummary();
	
}
