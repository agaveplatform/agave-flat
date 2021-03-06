/**
 * 
 */
package org.iplantc.service.tags.resource.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;
import org.iplantc.service.common.clients.AgaveLogServiceClient;
import org.iplantc.service.common.persistence.HibernateUtil;
import org.iplantc.service.common.representation.AgaveSuccessRepresentation;
import org.iplantc.service.tags.exceptions.TagException;
import org.iplantc.service.tags.exceptions.TagPermissionException;
import org.iplantc.service.tags.exceptions.TagValidationException;
import org.iplantc.service.tags.managers.TagManager;
import org.iplantc.service.tags.model.Tag;
import org.iplantc.service.tags.resource.TagResource;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * @author dooley
 *
 */
@Path("{entityId}")
public class TagResourceImpl extends AbstractTagResource implements TagResource {

	private static final Logger log = Logger.getLogger(TagResourceImpl.class);
	
	public TagResourceImpl() {}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagsCollection#getTags()
	 */
	@Override
	public Response represent(@PathParam("entityId") String entityId) throws Exception {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagsGetByID);
        
		try
        {
        	Tag tag = getResourceFromPathValue(entityId);
    		return Response.ok(new AgaveSuccessRepresentation(tag.toJSON().toString())).build();
        }
        catch (ResourceException e) {
            throw e;
        }
        catch (Throwable e) {
        	log.error("Failed to retrieve tag " + entityId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while fetching tag  " + entityId + ". "
                			+ "If this continues, please contact your tenant administrator.", e);
        }
		finally {
			try { HibernateUtil.closeSession(); } catch (Throwable ignored) {}
		}
		
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagsCollection#addTagFromForm(java.lang.String, java.util.List)
	 */
	@Override
	public Response remove(@PathParam("entityId") String entityId) throws Exception {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagsDelete);
        
        try
        {
        	Tag tag = getResourceFromPathValue(entityId);  	
        	TagManager manager = new TagManager();
        	manager.deleteUserTag(tag, getAuthenticatedUsername());
        	
        	return Response.ok().entity(new AgaveSuccessRepresentation()).build();
        }
        catch (TagPermissionException e) {
        	throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, e.getMessage(), e);
        }
		catch (TagException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Failed to delete tag.", e);
		}
		catch (ResourceException e) {
        	if (e.getStatus().equals(Status.CLIENT_ERROR_NOT_FOUND)) {
				return Response.ok().entity(new AgaveSuccessRepresentation()).build();
			} else {
        		throw e;
			}
		}
        catch (Exception e) {
        	log.error("Failed to delete tag " + entityId, e);
        	throw new ResourceException(Status.SERVER_ERROR_INTERNAL, 
        			"An unexpected error occurred while deleting tag  " + entityId + ". "
                			+ "If this continues, please contact your tenant administrator.", e);
        }
		finally {
			try { HibernateUtil.closeSession(); } catch (Throwable ignored) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.iplantc.service.tags.resource.TagResource#store(java.lang.String, org.restlet.representation.Representation)
	 */
	@Override
	public Response accept(@PathParam("entityId") String entityId, Representation input) throws Exception {
		
		logUsage(AgaveLogServiceClient.ActivityKeys.TagsUpdate);
        
		try
        {
        	Tag tag = getResourceFromPathValue(entityId);
        	JsonNode json = getPostedContentAsJsonNode(input);  	
        	TagManager manager = new TagManager();
        	Tag updatedTag = manager.updateTag(tag, json, getAuthenticatedUsername());
        	
        	return Response.ok(new AgaveSuccessRepresentation(updatedTag.toJSON().toString())).build();
            
        }
        catch (TagValidationException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage(), e);
		}
		catch (TagException e) {
			log.error("Failed to save updated tag " + entityId + ". " + e.getMessage());
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Failed to save updated tag.", e);
		}
		catch (ResourceException e) {
			throw e;
		}
		catch (Throwable e) {
        	log.error("Failed to update tag " + entityId, e);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
            		"An unexpected error occurred while updating tag  " + entityId + ". "
                			+ "If this continues, please contact your tenant administrator.", e);
        }
		finally {
			try { HibernateUtil.closeSession(); } catch (Throwable ignored) {}
		}
	}
}
